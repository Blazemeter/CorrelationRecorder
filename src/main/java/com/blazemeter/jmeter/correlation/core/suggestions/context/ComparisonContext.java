package com.blazemeter.jmeter.correlation.core.suggestions.context;

import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.automatic.ResultsExtraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
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
    return JMeterElementUtils.getSampleResultsFiltered(recordingTraceFilePath);
  }

  /**
   * This method retrieves a list of HTTPSamplerBase objects from the recording.
   * @return a list of HTTPSamplerBase objects.
   */
  @Override
  public List<HTTPSamplerProxy> getRecordingSamplers() {
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
    return new ResultsExtraction(configuration)
        .extractAppearanceMap(recordingTraceFilePath);
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
