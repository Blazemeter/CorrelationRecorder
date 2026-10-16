package com.blazemeter.jmeter.correlation.gui.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Second stage of a JMeter restart, run in its own JVM from the shutdown hook of the JMeter that
 * is exiting, as the JMeter Plugins Manager does with its SafeDeleter: it starts JMeter again with
 * the command line the exiting JMeter wrote, one argument per line.
 *
 * <p>Uses only the JDK, so the plugin jar alone is its classpath.
 */
public final class JMeterRestartRelay {

  private JMeterRestartRelay() {
  }

  /**
   * @param args the restart log to append to, and the file with the JMeter command line
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      throw new IllegalArgumentException(
          "Expected: <restart-log-path> <argv-file>; got " + args.length + " argument(s)");
    }
    File log = new File(args[0]);
    List<String> command = new ArrayList<>();
    for (String line : Files.readAllLines(Paths.get(args[1]), StandardCharsets.UTF_8)) {
      if (!line.trim().isEmpty()) {
        command.add(line);
      }
    }
    if (command.isEmpty()) {
      append(log, "# ERROR: empty command in " + args[1]);
      throw new IllegalStateException("Empty restart command in " + args[1]);
    }
    append(log, "# relay: starting JMeter: " + String.join(" ", command));
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
    builder.redirectError(ProcessBuilder.Redirect.appendTo(log));
    File bin = jarFolder(command);
    if (bin != null) {
      // as when JMeter is started from its bin folder
      builder.directory(bin);
    }
    builder.start();
  }

  private static File jarFolder(List<String> command) {
    int jar = command.indexOf("-jar");
    if (jar < 0 || jar + 1 >= command.size()) {
      return null;
    }
    File folder = new File(command.get(jar + 1)).getParentFile();
    return folder != null && folder.isDirectory() ? folder : null;
  }

  private static void append(File log, String line) throws IOException {
    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(log, true), StandardCharsets.UTF_8), true)) {
      writer.println(line);
    }
  }
}
