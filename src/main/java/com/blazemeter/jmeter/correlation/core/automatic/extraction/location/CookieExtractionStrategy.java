package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CookieExtractionStrategy implements ExtractionStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(CookieExtractionStrategy.class);

  @Override
  public Pair<LocationType, String> identifyLocationInResponse(SampleResult response,
      String value) {
    String setCookiesFromHeaders = getCleanedResponseHeaders(response.getResponseHeaders());
    // We get the cookies from the response
    return ExtractionStrategy.getLocationAndValue(setCookiesFromHeaders, value,
        LocationType.COOKIE);
  }

  private static String getCleanedResponseHeaders(String responseHeadersRaw) {
    return responseHeadersRaw.replaceAll("(?m)^Set-Cookie.*", "");
  }
}
