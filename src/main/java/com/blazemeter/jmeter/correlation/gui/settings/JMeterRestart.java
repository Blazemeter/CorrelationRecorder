package com.blazemeter.jmeter.correlation.gui.settings;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restarts JMeter the way the JMeter Plugins Manager does: this JMeter exits through its own Exit
 * action (which asks to save a modified test plan), and a shutdown hook starts
 * {@link JMeterRestartRelay} in a new JVM, which starts JMeter again with the same JVM arguments
 * and the same test plan.
 */
public final class JMeterRestart {

  private static final Logger LOG = LoggerFactory.getLogger(JMeterRestart.class);
  private static final String LOG_FILE_NAME = "correlation-recorder-jmeter-restart.log";
  private static final String ARGV_FILE_NAME = "correlation-recorder-jmeter-restart.argv";

  private JMeterRestart() {
  }

  /**
   * Exits JMeter and starts it again. Returns only when JMeter did not exit, because the user
   * cancelled the exit; the restart is then dropped.
   *
   * @throws IOException when the restart files cannot be written in the temporary folder
   * @throws IllegalStateException when JMeter's jar or the Java executable cannot be found
   */
  public static void restart() throws IOException {
    File jmeterJar = new File(JMeterUtils.getJMeterBinDir(), "ApacheJMeter.jar");
    if (!jmeterJar.isFile()) {
      throw new IllegalStateException("JMeter's jar not found: " + jmeterJar.getAbsolutePath());
    }
    List<String> jmeterCommand = jmeterCommand(jmeterJar);
    File tmp = new File(System.getProperty("java.io.tmpdir"));
    File log = new File(tmp, LOG_FILE_NAME);
    File argv = new File(tmp, ARGV_FILE_NAME);
    Files.write(argv.toPath(), jmeterCommand, StandardCharsets.UTF_8);
    List<String> relayCommand = relayCommand(log, argv);
    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(log), StandardCharsets.UTF_8))) {
      writer.println("# Auto Correlation Recorder: JMeter restart, " + new Date());
      writer.println("# JMeter command: " + String.join(" ", jmeterCommand));
      writer.println("# relay command: " + String.join(" ", relayCommand));
    }
    LOG.info("Restarting JMeter; restart log: {}", log.getAbsolutePath());

    Thread hook = new Thread(() -> startRelay(relayCommand, log),
        "correlationRecorderJMeterRestart");
    Runtime.getRuntime().addShutdownHook(hook);
    GuiPackage gui = GuiPackage.getInstance();
    // Exits the JVM unless the user cancels when asked to save the test plan
    ActionRouter.getInstance().doActionNow(
        new ActionEvent(gui != null ? gui.getMainFrame() : JMeterRestart.class, 0,
            ActionNames.EXIT));
    try {
      Runtime.getRuntime().removeShutdownHook(hook);
    } catch (IllegalStateException alreadyExiting) {
      // the JVM is shutting down after all: the hook runs
    }
    LOG.info("JMeter restart cancelled: JMeter did not exit");
  }

  private static void startRelay(List<String> relayCommand, File log) {
    try {
      ProcessBuilder builder = new ProcessBuilder(relayCommand);
      builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
      builder.redirectError(ProcessBuilder.Redirect.appendTo(log));
      builder.start();
    } catch (IOException | RuntimeException e) {
      try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
          new FileOutputStream(log, true), StandardCharsets.UTF_8), true)) {
        writer.println("# FAILED to start the restart relay:");
        e.printStackTrace(writer);
      } catch (IOException ignored) {
        // nowhere left to report it: the JVM is exiting
      }
    }
  }

  private static List<String> jmeterCommand(File jmeterJar) {
    List<String> command = new ArrayList<>();
    command.add(javaExecutable());
    command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
    command.add("-jar");
    command.add(jmeterJar.getAbsolutePath());
    GuiPackage gui = GuiPackage.getInstance();
    String testPlan = gui != null ? gui.getTestPlanFile() : null;
    if (testPlan != null && !testPlan.trim().isEmpty()) {
      command.add("-t");
      command.add(new File(testPlan).getAbsolutePath());
    }
    return command;
  }

  private static List<String> relayCommand(File log, File argv) {
    List<String> command = new ArrayList<>();
    command.add(javaExecutable());
    command.add("-classpath");
    command.add(pluginLocation());
    command.add(JMeterRestartRelay.class.getName());
    command.add(log.getAbsolutePath());
    command.add(argv.getAbsolutePath());
    return command;
  }

  private static String javaExecutable() {
    String javaHome = System.getProperty("java.home");
    String executable = System.getProperty("os.name", "").startsWith("Win") ? "java.exe" : "java";
    File java = new File(javaHome + File.separator + "bin", executable);
    if (!java.isFile()) {
      throw new IllegalStateException("Java executable not found: " + java.getAbsolutePath());
    }
    return java.getAbsolutePath();
  }

  private static String pluginLocation() {
    URL location = JMeterRestartRelay.class.getProtectionDomain().getCodeSource().getLocation();
    try {
      return Paths.get(location.toURI()).toAbsolutePath().toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Plugin location is not a valid URI: " + location, e);
    }
  }
}
