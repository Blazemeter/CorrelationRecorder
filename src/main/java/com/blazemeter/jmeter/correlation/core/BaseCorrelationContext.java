package com.blazemeter.jmeter.correlation.core;

import java.util.HashMap;
import java.util.Map;
import org.apache.jmeter.samplers.SampleResult;

public class BaseCorrelationContext implements CorrelationContext {

  private final Map<String, Integer> variablesCount = new HashMap<>();

  public Integer getNextVariableNr(String variableName) {
    return variablesCount.compute(variableName, (k, v) -> v != null ? v + 1 : 1);
  }

  public Integer getVariableCount(String variableName) {
    return variablesCount.getOrDefault(variableName, 0);
  }

  @Override
  public void reset() {
    variablesCount.clear();
  }

  @Override
  public void update(SampleResult sampleResult) {

  }
}
