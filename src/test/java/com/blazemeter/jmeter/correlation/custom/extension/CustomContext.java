package com.blazemeter.jmeter.correlation.custom.extension;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.Map;
import org.apache.jmeter.samplers.SampleResult;


/**
 * Dummy class to be used only for testing proposes
 */
@VisibleForTesting
public class CustomContext extends BaseCorrelationContext implements CorrelationContext {

  private final Map<String, String> sharedFieldOne = new HashMap<>();
  private int sharedFieldTwo = 0;
  private Integer counter = 0;

  @Override
  public void reset() {
    sharedFieldOne.clear();
    sharedFieldTwo = 0;
    counter = 0;
  }

  @Override
  public void update(SampleResult sampleResult) {
    String responseAsString = sampleResult.getResponseDataAsString();
    if (responseAsString.contains("<script>")) {
      counter++;
    } else if (responseAsString.contains("NewLayout")) {
      counter++;
    } else {
      counter = sampleResult.getResponseDataAsString().length();
    }
  }

  @Override
  public String toString() {
    return String.format("Current counter is %d ", counter);
  }
}




