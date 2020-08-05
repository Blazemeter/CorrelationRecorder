package com.blazemeter.jmeter.correlation.core;

import org.apache.jmeter.samplers.SampleResult;

public interface CorrelationContext {

  void reset();

  void update(SampleResult sampleResult);
}
