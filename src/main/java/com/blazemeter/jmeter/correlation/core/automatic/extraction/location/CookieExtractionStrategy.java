package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CookieExtractionStrategy implements ExtractionStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(CookieExtractionStrategy.class);

  @Override
  public LocationType identifyLocationInResponse(SampleResult response, String value) {
    String setCookiesFromHeaders = getCleanedResponseHeaders(response.getResponseHeaders());
    // We get the cookies from the response
    boolean isInSetCookie = setCookiesFromHeaders.contains(value);
    String encodedValue = ExtractorGenerator.encodeValue(value);
    boolean isInSetCookieEncoded = !isInSetCookie && setCookiesFromHeaders.contains(encodedValue);

    if (isInSetCookie || isInSetCookieEncoded) {
      return LocationType.COOKIE;
    }

    return LocationType.UNKNOWN; // Placeholder
  }

  private static String getCleanedResponseHeaders(String responseHeadersRaw) {
    return responseHeadersRaw.replaceAll("(?m)^Set-Cookie.*", "");
  }
}
