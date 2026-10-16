package com.blazemeter.jmeter.correlation.gui.settings;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.jmeter.util.JMeterUtils;

/**
 * The JMeter properties that recording and replaying need for the correlation to see the flow a
 * user's JMeter produces, and how to set them in user.properties.
 */
public final class RecommendedJMeterProperties {

  public static final List<JMeterPropertyRequirement> REQUIREMENTS =
      Collections.unmodifiableList(Arrays.asList(
          new JMeterPropertyRequirement("proxy.redirect.disabling", "false", "true",
              "With true, the recorder stores the requests that come from redirects as disabled "
                  + "samplers. The recorded flow then differs from the one that is replayed, and "
                  + "the correlation is more prone to errors."),
          new JMeterPropertyRequirement("httpclient4.deflate_relax_mode", "true", "false",
              "With false, a response whose deflate stream ends with an EOF fails instead of "
                  + "being read. The replay then reports errors that correlation cannot fix, and "
                  + "those responses cannot be compared.")));

  private static final String HEADER =
      "# Set by BlazeMeter Auto Correlation Recorder"
          + " (Tools > BlazeMeter Auto Correlation Recorder > JMeter Properties)";

  private RecommendedJMeterProperties() {
  }

  public static List<JMeterPropertyRequirement> unmet() {
    return REQUIREMENTS.stream().filter(r -> !r.isMet()).collect(Collectors.toList());
  }

  /**
   * @return the user.properties file JMeter loads, as the user.properties property names it,
   * relative to JMeter's bin folder unless it is absolute
   */
  public static File userPropertiesFile() {
    File file = new File(JMeterUtils.getPropDefault("user.properties", "user.properties"));
    return file.isAbsolute() ? file : new File(JMeterUtils.getJMeterBinDir(), file.getPath());
  }

  /**
   * Sets each property to its expected value in the given properties file: the lines that set it
   * before are removed, so their values are lost, and the other lines are kept as they are.
   */
  public static void persist(File propertiesFile, List<JMeterPropertyRequirement> requirements)
      throws IOException {
    List<String> lines = propertiesFile.isFile()
        ? Files.readAllLines(propertiesFile.toPath(), StandardCharsets.ISO_8859_1)
        : new ArrayList<>();
    List<Pattern> keyLines = requirements.stream()
        .map(r -> Pattern.compile("^\\s*" + Pattern.quote(r.getName()) + "(\\s*[=:\\s].*|\\s*)$"))
        .collect(Collectors.toList());
    List<String> kept = new ArrayList<>();
    boolean skippingContinuation = false;
    for (String line : lines) {
      if (skippingContinuation) {
        skippingContinuation = continuesOnNextLine(line);
        continue;
      }
      if (keyLines.stream().anyMatch(p -> p.matcher(line).matches())) {
        skippingContinuation = continuesOnNextLine(line);
        continue;
      }
      // the header of a previous apply goes too, so applying again leaves the file the same
      if (!HEADER.equals(line.trim())) {
        kept.add(line);
      }
    }
    if (!kept.isEmpty() && !kept.get(kept.size() - 1).trim().isEmpty()) {
      kept.add("");
    }
    kept.add(HEADER);
    for (JMeterPropertyRequirement requirement : requirements) {
      kept.add(requirement.getName() + "=" + requirement.getExpectedValue());
    }
    File parent = propertiesFile.getAbsoluteFile().getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    Files.write(propertiesFile.toPath(), kept, StandardCharsets.ISO_8859_1);
  }

  /**
   * Sets each property to its expected value in this JMeter, as user.properties will after a
   * restart.
   */
  public static void applyToRuntime(List<JMeterPropertyRequirement> requirements) {
    for (JMeterPropertyRequirement requirement : requirements) {
      JMeterUtils.setProperty(requirement.getName(), requirement.getExpectedValue());
    }
  }

  // A properties line continues on the next one when it ends with an odd number of backslashes
  private static boolean continuesOnNextLine(String line) {
    int backslashes = 0;
    for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
      backslashes++;
    }
    return backslashes % 2 == 1;
  }
}
