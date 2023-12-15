package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import org.apache.jmeter.samplers.SampleResult;

public interface ExtractionStrategy {
  LocationType identifyLocationInResponse(SampleResult response, String value);
}
