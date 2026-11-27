package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import org.apache.jmeter.samplers.SampleResult;

public class BodyExtractionStrategy extends IndexedExtractionStrategy {

  @Override
  protected String searchableContent(SampleResult response) {
    return response.getDataType().equals("bin") ? "" : response.getResponseDataAsString();
  }

  @Override
  protected LocationType locationType() {
    return LocationType.BODY;
  }
}
