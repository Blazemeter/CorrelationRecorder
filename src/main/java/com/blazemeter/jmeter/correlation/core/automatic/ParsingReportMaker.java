package com.blazemeter.jmeter.correlation.core.automatic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that creates a markdown report for different kind of operations.
 * Used for debugging purposes.
 */
public class ParsingReportMaker {
  private static final Logger LOG = LoggerFactory.getLogger(ParsingReportMaker.class);
  private static ParsingReportMaker instance = null;
  private static String source;
  private FileWriter fw;
  private BufferedWriter bw;
  private final boolean enabledLogging = false;

  private ParsingReportMaker() {
  }

  public static ParsingReportMaker getInstance() {
    if (instance == null) {
      instance = new ParsingReportMaker();
    }
    return instance;
  }

  public void init(String sourceFilepath) {
    ParsingReportMaker.source = sourceFilepath;
    File file = new File(sourceFilepath);
    LOG.info("Debug file created at: " + file.getAbsolutePath());
    System.out.println("Debug file created at: " + file.getAbsolutePath());

    try {
      // If file doesn't exist, create it, else clear it
      BufferedWriter writer = new BufferedWriter(new FileWriter(file.getName()));
      writer.write("");
      writer.close();
      fw = new FileWriter(file);
    } catch (IOException e) {
      e.printStackTrace();
    }
    bw = new BufferedWriter(fw);

    logHeader(1, "Suggestions");
    log("Path='" + sourceFilepath + "'");
  }

  public void log(String text) {
    // Skip the logging if there is no file
    if (!enabledLogging) {
      return;
    }

    try {
      bw.write(text);
      bw.newLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Adds indentation to the text and then logs it
  public void log(int indentation, String text) {
    StringBuilder indentationString = new StringBuilder();
    for (int i = 0; i < indentation; i++) {
      indentationString.append("\t");
    }
    log(indentationString + text);
  }

  // Log a pair of key and value with the indentation
  public void log(int indentation, String key, String value) {
    log(indentation, "- **" + key + "**: '" + value + "'");
  }

  // Log a jump to a new line
  public void log() {
    log("");
  }

  public void logSample(String name) {
    log("1. Sampler: " + name);
  }

  public void logHeader(int level, String text) {
    StringBuilder header = new StringBuilder();
    for (int i = 0; i < level; i++) {
      header.append("#");
    }
    header.append(" ").append(text);
    log(header.toString());
    log("");
  }

  public void close() {
    try {
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void printDomains(Map<String, Integer> domains) {
    logHeader(2, "Domain's Report");
    log("Rank of detected Domains (sorted by number of occurrences):");

    domains.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .forEach(entry -> log("1.  " + entry.getKey() + " : " + entry.getValue()));

    log("");
    log("Total of detected domains: " + domains.size());
    log("");
  }

  public void printStoredVariables(Map<String, List<Appearances>> storedVariables) {
    logHeader(2, "Stored Variables Report");
    log("Stored Variables ranking by repetitions ");

    // Print the stored variables and the values they have order descending.
    storedVariables.entrySet().stream()
        .sorted(Map.Entry.comparingByValue((o1, o2) -> o2.size() - o1.size()))
        .forEach(entry -> {
          log("1.  **" + entry.getKey() + "**. (" + entry.getValue().size() + " appearances): ");
          // Print the values of the stored variable in a collapsible section
          log("    <details>");
          log("    <summary>Click to expand</summary>");
          log();
          entry.getValue()
              .forEach(appearance -> log(1, "- '" + appearance.getValue() + "'"));
          log("    </details>");
        });
    log();
    log("Total stored variables: " + storedVariables.size());
  }

  public static void setSource(String source) {
    ParsingReportMaker.source = source;
  }

  public static String getSource() {
    return source;
  }

  public void logSampler(String samplerName) {
    log("1. Sampler: " + samplerName);
  }
}
