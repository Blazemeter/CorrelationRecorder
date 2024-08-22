package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.samplers.SampleResult;

public class BodyExtractionStrategy implements ExtractionStrategy {

  @Override
  public Pair<LocationType, String> identifyLocationInResponse(SampleResult response,
      String value) {
    String bodyResponse =
        response.getDataType().equals("bin") ? "" : response.getResponseDataAsString();
    return ExtractionStrategy.getLocationAndValue(bodyResponse, value,
        LocationType.BODY);
  }
}
