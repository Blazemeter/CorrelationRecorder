package com.blazemeter.jmeter.correlation.core.suggestions.context;

import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.automatic.ResultsExtraction;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;


/**
 * The ComparisonContext class implements the CorrelationContext interface.
 * It is used to store and manage the context for generating correlation suggestions.
 * The context includes the file paths of the recording and replay traces,
 * the recording test plan, and the configuration.
 */
public class ComparisonContext implements CorrelationContext {
  private String recordingTraceFilePath;
  private String replayTraceFilePath;
  private String recordingTestPlanPath;
  private Configuration configuration;
  // The recording as getRecordingSampleResults last loaded it: the samples, the trace they came
  // from (path, size and modification time) and the filter applied to them
  private List<SampleResult> loadedRecording;
  private String loadedRecordingKey;
  private List<List<String>> loadedRecordingFilter;

  /**
   * Constructor for the ComparisonContext class that takes a Configuration object as
   * a parameter.
   * @param configuration the Configuration object to set.
   */
  public ComparisonContext(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Default constructor for the ComparisonContext class.
   */
  public ComparisonContext() {
    this.configuration = new Configuration();
  }

  /**
   * This method retrieves a list of SampleResult objects from the recording trace file.
   * @return a list of SampleResult objects.
   */
  @Override
  public List<SampleResult> getRecordingSampleResults() {
    Configuration loadedWith = new Configuration();
    List<SampleResult> results =
        JMeterElementUtils.getSampleResultsFiltered(recordingTraceFilePath, loadedWith);
    loadedRecording = results;
    loadedRecordingKey = traceKey(recordingTraceFilePath);
    loadedRecordingFilter = sampleFilter(loadedWith);
    return results;
  }

  private static String traceKey(String filePath) {
    if (filePath == null) {
      return "";
    }
    File file = new File(filePath);
    return file.getAbsolutePath() + "|" + file.length() + "|" + file.lastModified();
  }

  // What decides which samples a trace load keeps (see JMeterElementUtils.canBeFiltered)
  private static List<List<String>> sampleFilter(Configuration configuration) {
    return Arrays.asList(configuration.getIgnoredDomains(), configuration.getIgnoredFiles());
  }

  /**
   * This method retrieves a list of HTTPSamplerBase objects from the recording.
   * @return a list of HTTPSamplerBase objects.
   */
  @Override
  public List<HTTPSamplerBase> getRecordingSamplers() {
    return new ArrayList<>();
  }

  /**
   * This method sets the file path of the recording trace.
   * @param recordingTraceFilePath the file path to set.
   */
  public void setRecordingTraceFilePath(String recordingTraceFilePath) {
    this.recordingTraceFilePath = recordingTraceFilePath;
  }

  /**
   * This method sets the file path of the replay trace.
   * @param replayTraceFilePath the file path to set.
   */
  public void setReplayTraceFilePath(String replayTraceFilePath) {
    this.replayTraceFilePath = replayTraceFilePath;
  }

  /**
   * This method sets the file path of the recording test plan.
   * @param recordingTestPlanPath the file path to set.
   */
  public void setRecordingTestPlanPath(String recordingTestPlanPath) {
    this.recordingTestPlanPath = recordingTestPlanPath;
  }

  /**
   * This method retrieves the Configuration object.
   * @return the Configuration object.
   */
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * This method sets the Configuration object.
   * @param configuration the Configuration object to set.
   */
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * This method retrieves a map of parameter names to their appearances
   * from the recording
   * trace file.
   * @return a map of parameter names to their appearances.
   */
  public Map<String, List<Appearances>> getRecordingMap() {
    ResultsExtraction extraction = new ResultsExtraction(configuration);
    // The samples getRecordingSampleResults loaded are the ones this map needs when the trace has
    // not changed and both filter samples alike: reusing them reads the trace once, not twice
    if (loadedRecording != null && traceKey(recordingTraceFilePath).equals(loadedRecordingKey)
        && loadedRecordingFilter.equals(sampleFilter(configuration))) {
      return extraction.extractAppearanceMap(loadedRecording);
    }
    return extraction.extractAppearanceMap(recordingTraceFilePath);
  }

  /**
   * This method retrieves a map of parameter names to their appearances from the replay
   * trace file.
   * @return a map of parameter names to their appearances.
   */
  public Map<String, List<Appearances>> getReplayMap() {
    return new ResultsExtraction(configuration)
        .extractAppearanceMap(replayTraceFilePath);
  }
}
