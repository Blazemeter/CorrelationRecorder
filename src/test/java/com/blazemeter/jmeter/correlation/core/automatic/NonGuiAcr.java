package com.blazemeter.jmeter.correlation.core.automatic;

import java.io.File;
import java.util.List;
import kg.apc.emulators.TestJMeterUtils;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jorphan.collections.HashTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This class is used to test the automatic correlation process.
 * The main idea is to use this to simulate the Automatic Correlation
 * process without the need of doing the Recording manually.
 *
 * Basically the ACR process is divided in 2 steps:
 * 1. Generate suggestions
 * 2. Apply suggestions
 *
 * If you want to test the Generation of Suggestions, the process needs the following files:
 * 1. The JTL file of the Recording
 * 2. The JTL file of the Replay
 *
 * If you want to test the Application of Suggestions, you will additionally need:
 * 3. The JMX file of the Recording
 *
 * One simple way to obtain these files is to use the generated files in the
 * Correlation History folder.
 *
 * Both the JMX and the JTL of the Recording can be obtained from the "Original Recording" step.
 * The JTL of the Replay can be obtained from the "Failed Replay" step.
 *
 * Note the JTL for the Replay is marked as "Failed", since we don't generate Suggestions for
 * successful replays.
 */

@RunWith(MockitoJUnitRunner.class)
public class NonGuiAcr {

  //Remember to use the absolute path of the files.
  protected static String jmxFilePath;
  protected static String recordingFilePath;
  protected static String replayFilePath;

  protected static String correlationHistoryPath = "";
  protected static CorrelationHistory history;

  @Before
  public void setUp() {
    TestJMeterUtils.createJmeterEnv();
  }

  @Test
  public void shouldGenerateSuggestions() throws IllegalUserActionException {
    //Added to avoid running these tests in the pipeline.
    if (!hasNeededFiles(recordingFilePath, replayFilePath)) {
      return;
    }

    ElementsComparison elementsComparison = new ElementsComparison();
    List<CorrelationSuggestion> suggestions =
        elementsComparison.generateSuggestionsFromFailingReplayTraceOnly(recordingFilePath,
            replayFilePath);

    suggestions.forEach(System.out::println);
  }

  protected boolean hasNeededFiles(String ... files) {
    for (String file : files) {
      if (file == null) {
        return false;
      }

      if (file.isEmpty()) {
        return false;
      }

      if (!new File(file).exists()) {
        return false;
      }
    }
    return true;
  }

  @Test
  public void shouldApplySuggestions() throws IllegalUserActionException {
    if (!hasNeededFiles(recordingFilePath, replayFilePath, jmxFilePath)) {
      return;
    }

    ElementsComparison elementsComparison = new ElementsComparison();
    List<CorrelationSuggestion> suggestions =
        elementsComparison.generateSuggestionsFromFailingReplayTraceOnly(recordingFilePath,
            replayFilePath);

    //When running using the GUI, JMeter would obtain the HashTree using GuiPackage.
    // We need to do it manually.
    HashTree hashTree = JMeterElementUtils.getTestPlan(jmxFilePath);

    ElementsModification.ModificationReport report =
        ElementsModification.applySuggestions(hashTree, suggestions);

    report.getResults().forEach((key, value) -> System.out.println(value + "\n"));
  }


  /**
   * This test is used to make the simulation of generating suggestions from a Correlation History
   * file, as it would be done in the GUI.
   */
  @Test
  public void shouldGenerateSuggestionsFromCorrelationHistoryFile() {
    if (!hasNeededFiles(correlationHistoryPath)) {
      return;
    }

    history = CorrelationHistory.loadFromFile(correlationHistoryPath);

    if (history == null) {
      return;
    }

    if (!hasNeededFiles(history.getOriginalRecordingTrace(), history.getLastReplayTraceFilepath())) {
      return;
    }

    recordingFilePath = history.getOriginalRecordingTrace();
    replayFilePath = history.getLastReplayTraceFilepath();

    System.out.println("Generation suggestions from Correlation History");
    System.out.println("Original Recording: " + recordingFilePath);
    System.out.println("Last Replay: " + replayFilePath);

    ElementsComparison elementsComparison = new ElementsComparison();
    List<CorrelationSuggestion> suggestions =
        elementsComparison.generateSuggestionsFromFailingReplayTraceOnly(recordingFilePath,
            replayFilePath);

    System.out.println("Suggestions generated: " + suggestions.size());
    suggestions.forEach(System.out::println);
  }
}
