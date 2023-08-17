package com.blazemeter.jmeter.correlation.core.automatic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to keep track of the state of a Test Plan, from the Recording
 * and its Recording Trace file, Correlation Suggestions application, and Replays.
 * In future versions, this class will be used to do roll-backs and compare the results
 * of different Correlation Suggestions applications.
 */
public class CorrelationHistory {
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationHistory.class);

  private static Supplier<String> saveCurrentTestPlanSupplier
      = JMeterElementUtils::saveTestPlanSnapshot;
  private List<Step> steps = new ArrayList<>();
  private String filepath = "";

  public CorrelationHistory() {
  }

  public static CorrelationHistory loadFromFile(String correlationHistoryPath) {
    return new FileManagementUtils().loadCorrelationHistoryFile(correlationHistoryPath);
  }

  public void addOriginalRecordingStep(String originalRecordingFilepath,
                                       String originalRecordingTraceFilepath) {
    if (!steps.isEmpty()) {
      int size = steps.size();
      LOG.warn("CorrelationHistory already has {} recording step(s), which will be overwritten",
          size);
    }

    steps = new ArrayList<>();
    Step step = new Step("Original Recording");
    step.setTestPlanFilepath(originalRecordingFilepath);
    step.setRecordingTraceFilepath(originalRecordingTraceFilepath);
    addStep(step);
  }

  public void addSuccessfulReplay(String testPlanFilepath, String replayTraceFilepath,
                                  boolean hadErrors) {
    Step step = new Step("Successful Replay "
        + (hadErrors ? "with original recording errors" : "without errors"));
    step.setTestPlanFilepath(testPlanFilepath);
    step.setReplayTraceFilepath(replayTraceFilepath);
    addStep(step);
  }

  public void addStep(Step step) {
    steps.add(step);
    saveToFile();
  }

  public void addFailedReplay(String testPlanFilepath, String replayTraceFilepath) {
    Step step = new Step("Failed Replay");
    step.setTestPlanFilepath(testPlanFilepath);
    step.setReplayTraceFilepath(replayTraceFilepath);
    addStep(step);
  }

  public void addAnalysisStep(String message, String testPlanFilepath, String traceFilepath) {
    Step step = new Step(message);
    step.setTestPlanFilepath(testPlanFilepath);
    step.setReplayTraceFilepath(traceFilepath);
    addStep(step);
  }

  public void saveToFile() {
    String historyFile = new FileManagementUtils().saveCorrelationHistoryFile(this, filepath);
    if (filepath.isEmpty()) {
      filepath = historyFile;
    }

    LOG.info("Correlation History saved to {}", historyFile);
  }

  public String getOriginalRecordingFilepath() {
    return getRecordingStep().getTestPlanFilepath();
  }

  private Step getRecordingStep() {
    if (steps.isEmpty()) {
      LOG.error("CorrelationHistory has no steps, forcing an auxiliary step");
      Step auxiliaryStep = new Step("Auxiliary Step (No Recording step found)");
      auxiliaryStep.setTestPlanFilepath(saveCurrentTestPlanSupplier.get());
      auxiliaryStep.setRecordingTraceFilepath(JMeterElementUtils.getRecordingFilePath());
      addStep(auxiliaryStep);
    }
    return steps.get(0);
  }

  public String getOriginalRecordingTrace() {
    return getRecordingStep().getRecordingTraceFilepath();
  }

  public String getLastTestPlanFilepath() {
    if (steps.size() - 1 < 0) {
      // This behavior is not ideal, but it is the best we can do for now until
      // Correlation History is properly implemented
      LOG.warn("CorrelationHistory has no steps, returning current test plan snapshot");
      return saveCurrentTestPlanSupplier.get();
    }
    return steps.get(steps.size() - 1).getTestPlanFilepath();
  }

  public String getLastReplayTraceFilepath() {
    if (steps.size() - 1 < 0) {
      LOG.warn("CorrelationHistory has no steps, returning empty string");
      return "";
    }
    return steps.get(steps.size() - 1).getReplayTraceFilepath();
  }

  public String getHistoryPath() {
    return filepath;
  }

  public static Supplier<String> saveCurrentTestPlan() {
    return saveCurrentTestPlanSupplier;
  }

  public static void setSaveCurrentTestPlan(Supplier<String> saveCurrentTestPlan) {
    saveCurrentTestPlanSupplier = saveCurrentTestPlan;
  }

  @Override
  public String toString() {
    return "CorrelationHistory {"
        + ", filepath='" + filepath + '\''
        + ", totalSteps=" + steps.size()
        + ", steps=" + steps + '}';
  }

  public void setHistoryPath(String filepath) {
    this.filepath = filepath;
  }

  public static class Step {
    private String stepMessage; //For now, only used for logging and debugging
    private String testPlanFilepath;
    private String recordingTraceFilepath;
    private String replayTraceFilepath;

    @JsonIgnore
    private Supplier<String> saveCurrentTestPlanSupplier = saveCurrentTestPlan();

    public Step() {
    }

    public Step(String stepMessage) {
      this.stepMessage = stepMessage;
    }

    public String getStepMessage() {
      return stepMessage;
    }

    public void setStepMessage(String stepMessage) {
      this.stepMessage = stepMessage;
    }

    public String getTestPlanFilepath() {
      return testPlanFilepath;
    }

    public void setTestPlanFilepath(String testPlanFilepath) {
      this.testPlanFilepath = testPlanFilepath;
    }

    public String getRecordingTraceFilepath() {
      return recordingTraceFilepath;
    }

    public void setRecordingTraceFilepath(String recordingTraceFilepath) {
      this.recordingTraceFilepath = recordingTraceFilepath;
    }

    public String getReplayTraceFilepath() {
      return replayTraceFilepath;
    }

    public void setReplayTraceFilepath(String replayTraceFilepath) {
      this.replayTraceFilepath = replayTraceFilepath;
    }

    public void addCurrentTestPlan() {
      setTestPlanFilepath(saveCurrentTestPlanSupplier.get());
    }

    @Override
    public String toString() {
      return "Step {" +
          "message='" + stepMessage + '\'' +
          ", testPlanFilepath='" + testPlanFilepath + '\'' +
          ", recordingTraceFilepath='" + recordingTraceFilepath + '\'' +
          ", replayTraceFilepath='" + replayTraceFilepath + '\'' +
          '}';
    }
  }
}
