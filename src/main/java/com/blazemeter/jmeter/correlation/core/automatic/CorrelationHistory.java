package com.blazemeter.jmeter.correlation.core.automatic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to keep track of the state of a Test Plan, from the Recording
 * and its Recording Trace file, Correlation Suggestions application, and Replays.
 * In future versions, this class will be used to do roll-backs and compare the results
 * of different Correlation Suggestions applications.
 */
public class CorrelationHistory {
  public static final String AUXILIARY_STEP_MESSAGE =
          "Auxiliary Iterarion (No Recording iteration found)";
  public static final String ORIGINAL_RECORDING_MESSAGE = "Original Recording";
  public static final String FAILED_REPLAY_MESSAGE =
          "Replay result: %s requests pending resolution.";
  public static final String SUCCESS_REPLAY_TEMPLATE = "Replay %s";
  public static final String SUCCESS_REPLAY_POSTFIX_WITH_ERRORS =
          "without requests to attend";
  public static final String SUCCESS_REPLAY_POSTFIX_WITHOUT_ERRORS =
          "with same failed requests as the original recording.";
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationHistory.class);
  private static final String HISTORY_FOLDER = "History";
  private static final String REPLAY_FOLDER = "Replay";
  private static final String RECORDING_FOLDER = "Recording";
  private static final String HISTORY_NAME_REGEX = ".*history-(\\d+)\\.json$";
  private static Supplier<String> saveCurrentTestPlanSupplier
      = JMeterElementUtils::saveTestPlanSnapshot;
  private static Supplier<String> recordingFilePathSupplier
      = JMeterElementUtils::getRecordingFilePath;
  private List<Step> steps = new ArrayList<>();
  private String historyId = "";

  public CorrelationHistory() {
  }

  public static CorrelationHistory loadFromFile(String correlationHistoryId) {
    return new FileManagementUtils().loadCorrelationHistoryFile(correlationHistoryId);
  }

  public void addOriginalRecordingStep(String originalRecordingFilepath,
                                       String originalRecordingTraceFilepath) {
    if (!steps.isEmpty()) {
      int size = steps.size();
      LOG.warn("CorrelationHistory already has {} recording iterations(s)," +
                      " which will be overwritten",
          size);
    }

    steps = new ArrayList<>();
    Step step = new Step(ORIGINAL_RECORDING_MESSAGE);
    step.setTestPlanFilepath(originalRecordingFilepath);
    step.setRecordingTraceFilepath(originalRecordingTraceFilepath);
    addStep(step);
  }

  public void addSuccessfulReplay(String testPlanFilepath, String replayTraceFilepath,
                                  boolean hadErrors) {
    Step step = new Step(String.format(SUCCESS_REPLAY_TEMPLATE,
            (hadErrors ? SUCCESS_REPLAY_POSTFIX_WITH_ERRORS
                    : SUCCESS_REPLAY_POSTFIX_WITHOUT_ERRORS)));

    step.setTestPlanFilepath(testPlanFilepath);
    step.setReplayTraceFilepath(replayTraceFilepath);
    addStep(step);
  }

  public void addStep(Step step) {
    steps.add(step);
    saveToFile();
  }

  public List<Step> getSteps() {
    return this.steps;
  }

  public void addFailedReplay(String testPlanFilepath, String replayTraceFilepath,
                              Integer newErrors) {
    Step step = new Step(String.format(FAILED_REPLAY_MESSAGE, newErrors));
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

  public void configHistoryId(String path) {
    if (historyId.isEmpty()) {
      Pattern pattern = Pattern.compile(HISTORY_NAME_REGEX);
      Matcher matcher = pattern.matcher(path);
      historyId = matcher.find() ? matcher.group(1) : "";
    }
  }

  public void saveToFile() {
    String historyFile = new FileManagementUtils().saveCorrelationHistoryFile(this);
    LOG.info("Correlation History saved to {}", historyFile);
  }

  public String getOriginalRecordingFilepath() {
    return getRecordingStep().getTestPlanFilepath();
  }

  @VisibleForTesting
  public Step getRecordingStep() {
    if (steps.isEmpty()) {
      LOG.error("CorrelationHistory has no iterations, forcing an auxiliary iteartion");
      Step auxiliaryStep = new Step(AUXILIARY_STEP_MESSAGE);
      auxiliaryStep.setTestPlanFilepath(saveCurrentTestPlanSupplier.get());
      auxiliaryStep.setRecordingTraceFilepath(recordingFilePathSupplier.get());
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
      LOG.warn("CorrelationHistory has no iterations, returning current test plan snapshot");
      return saveCurrentTestPlanSupplier.get();
    }
    return steps.get(steps.size() - 1).getTestPlanFilepath();
  }

  public String getLastReplayTraceFilepath() {
    if (steps.size() - 1 < 0) {
      LOG.warn("CorrelationHistory has no iterations, returning empty string");
      return "";
    }
    return steps.get(steps.size() - 1).getReplayTraceFilepath();
  }

  public String getHistoryId() {
    return historyId;
  }

  public static Supplier<String> saveCurrentTestPlan() {
    return saveCurrentTestPlanSupplier;
  }

  public static void setSaveCurrentTestPlan(Supplier<String> saveCurrentTestPlan) {
    saveCurrentTestPlanSupplier = saveCurrentTestPlan;
  }

  public static void setRecordingFilePathSupplier(Supplier<String> filePathSupplier) {
    recordingFilePathSupplier = filePathSupplier;
  }

  @Override
  public String toString() {
    return "CorrelationHistory {"
        + ", filepath='" + FileManagementUtils.getHistoryPath(historyId) + '\''
        + ", totalSteps=" + steps.size()
        + ", steps=" + steps + '}';
  }

  public void setHistoryId(String historyId) {
    this.historyId = historyId;
  }

  public void deleteSteps(List<Step> steps) {
    this.steps.removeAll(steps);
    saveToFile();
  }

  public void addRestoredStep(Step step) {
    Step newStep = new Step(
            "Restored iteration with timestamp: %s", step.getTimestamp());
    newStep.setTestPlanFilepath(step.getTestPlanFilepath());
    newStep.setRecordingTraceFilepath(step.getRecordingTraceFilepath());
    newStep.setReplayTraceFilepath(step.getReplayTraceFilepath());
    this.steps.add(newStep);
    saveToFile();
  }

  private static void createFolder(String folderName, ZipOutputStream zipOutputStream)
          throws IOException {
    ZipEntry zipEntry = new ZipEntry(folderName + "/");
    zipOutputStream.putNextEntry(zipEntry);
    zipOutputStream.closeEntry();
  }

  public void addZipEntry(ZipOutputStream zipOutputStream, String folder,  Path file)
          throws IOException {
    // Create a new entry in the zip file
    ZipEntry zipEntry = new ZipEntry(folder + "/" + file.getFileName().toString());
    zipOutputStream.putNextEntry(zipEntry);
    // Read the file content and write it to the zip output stream
    Files.copy(file, zipOutputStream);
    // Close entry
    zipOutputStream.closeEntry();
  }

  public String zipHistory() {
    String zipLocation = FileManagementUtils.getHistoryZipFilePath(historyId);
    try {
      ZipOutputStream zipOutputStream = new ZipOutputStream(
              new BufferedOutputStream(
                      Files.newOutputStream(
                              Paths.get(zipLocation))));

      createFolder(HISTORY_FOLDER, zipOutputStream);
      createFolder(RECORDING_FOLDER, zipOutputStream);
      createFolder(REPLAY_FOLDER, zipOutputStream);

      Set<String> savedFiles = new HashSet<>();

      addZipEntry(zipOutputStream, HISTORY_FOLDER,
              Paths.get(FileManagementUtils.getHistoryPath(historyId)));

      for (Step step : getSteps()) {
        if ((step.getTestPlanFilepath() != null)
                && !savedFiles.contains(step.getTestPlanFilepath())) {
          String testPlanFilepath = step.getTestPlanFilepath();
          addZipEntry(zipOutputStream, FileManagementUtils.getParentFolderInBin(testPlanFilepath),
                  Paths.get(FileManagementUtils.getBinFilePath(testPlanFilepath)));
          savedFiles.add(testPlanFilepath);
        }
        if ((step.getRecordingTraceFilepath() != null)
                && !savedFiles.contains(step.getRecordingTraceFilepath())) {
          String recordingTraceFilepath = step.getRecordingTraceFilepath();
          addZipEntry(zipOutputStream,
                  FileManagementUtils.getParentFolderInBin(recordingTraceFilepath),
                  Paths.get(FileManagementUtils.getBinFilePath(recordingTraceFilepath)));
          savedFiles.add(recordingTraceFilepath);
        }
        if ((step.getReplayTraceFilepath() != null)
                && !savedFiles.contains(step.getReplayTraceFilepath())) {
          String replyTraceFilepath = step.getReplayTraceFilepath();
          addZipEntry(zipOutputStream, FileManagementUtils.getParentFolderInBin(replyTraceFilepath),
                  Paths.get(FileManagementUtils.getBinFilePath(replyTraceFilepath)));
          savedFiles.add(replyTraceFilepath);
        }
      }
      zipOutputStream.close();
      LOG.info("History zipped at: " + zipLocation);
      return zipLocation;
    } catch (IOException e) {
      LOG.error("Problem creating history zip file", e);
    }
    return "";
  }

  public static class Step {
    private String stepMessage; //For now, only used for logging and debugging
    private String testPlanFilepath;
    private String recordingTraceFilepath;
    private String replayTraceFilepath;
    private String timestamp;
    private String fatherTimeStamp = null;

    @JsonIgnore
    private Supplier<String> saveCurrentTestPlanSupplier = saveCurrentTestPlan();

    public Step() {
    }

    public Step(String stepMessage) {
      this.stepMessage = stepMessage;
      this.timestamp = Instant.now().toString();
      this.fatherTimeStamp = null;
    }

    public Step(String stepMessage, String father) {
      this.stepMessage = stepMessage;
      this.timestamp = Instant.now().toString();
      this.fatherTimeStamp = father;
    }

    public String getStepMessage() {
      String msgEnd = fatherTimeStamp;
      if (msgEnd != null) {
        Instant date = Instant.parse(fatherTimeStamp);
        DateTimeFormatter outFormatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd' - 'HH:mm:ss")
                        .withZone(ZoneId.systemDefault());
        msgEnd = outFormatter.format(date);
      }
      return String.format(stepMessage, msgEnd);
    }

    @VisibleForTesting
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

    public String getFatherTimeStamp() {
      return fatherTimeStamp;
    }

    public void setReplayTraceFilepath(String replayTraceFilepath) {
      this.replayTraceFilepath = replayTraceFilepath;
    }

    public void addCurrentTestPlan() {
      setTestPlanFilepath(saveCurrentTestPlanSupplier.get());
    }

    public String getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(String tmp) {
      this.timestamp = tmp;
    }

    public void setFatherTimeStamp(String fts) {
      this.fatherTimeStamp = fts;
    }

    @Override
    public String toString() {
      return "Step {" +
          "message='" + stepMessage + '\'' +
          ", testPlanFilepath='" + testPlanFilepath + '\'' +
          ", recordingTraceFilepath='" + recordingTraceFilepath + '\'' +
          ", replayTraceFilepath='" + replayTraceFilepath + '\'' +
          ", time='" + timestamp + '\'' +
          ", fatherTimestamp='" + fatherTimeStamp + '\'' +
          '}';
    }
  }
}
