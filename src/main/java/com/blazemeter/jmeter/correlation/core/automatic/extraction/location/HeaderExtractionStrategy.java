package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.samplers.SampleResult;

public class HeaderExtractionStrategy implements ExtractionStrategy {

  @Override
  public Pair<LocationType, String> identifyLocationInResponse(SampleResult response,
      String value) {
    // Logic to determine if the argument is in the headers
    String headersWithoutSetCookies =
        getCleanedResponseHeaders(String.valueOf(response.getResponseHeaders()));
    return ExtractionStrategy.getLocationAndValue(headersWithoutSetCookies, value,
        LocationType.HEADER);
  }

  private static String getCleanedResponseHeaders(String responseHeadersRaw) {
    return responseHeadersRaw.replaceAll("(?m)^Set-Cookie.*", "");
  }
}
