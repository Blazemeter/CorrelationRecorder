package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import org.apache.jmeter.samplers.SampleResult;

public class BodyExtractionStrategy implements ExtractionStrategy {
  @Override
  public LocationType identifyLocationInResponse(SampleResult response, String value) {
    String bodyResponse =
        response.getDataType().equals("bin") ? "" : response.getResponseDataAsString();
    boolean isInBody = bodyResponse.contains(value);
    String encodedValue = ExtractorGenerator.encodeValue(value);
    boolean isInBodyEncoded = !isInBody && bodyResponse.contains(encodedValue);
    if (isInBody || isInBodyEncoded) {
      return LocationType.BODY;
    }
    return LocationType.UNKNOWN;
  }
}
