package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.ArrayList;
import java.util.List;
import org.apache.jmeter.samplers.SampleResult;

/**
 * This class is used to store the results of a replay of a Test Plan,
 * along with the ResultCollector and the total amount of new errors
 * of such replay.
 */
public class ReplayReport {
  private CustomResultCollector collector;
  private boolean successful;
  private List<SampleResult> replayNewErrors = new ArrayList<>();

  public ReplayReport() {
  }

  public void setCollector(CustomResultCollector collector) {
    this.collector = collector;
  }

  public void setSuccessful(boolean successful) {
    this.successful = successful;
  }

  public CustomResultCollector getCollector() {
    return collector;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public void setReplayNewErrors(List<SampleResult> replayNewErrors) {
    this.replayNewErrors = replayNewErrors;
  }

  public int getTotalNewErrors() {
    return replayNewErrors.size();
  }
}
