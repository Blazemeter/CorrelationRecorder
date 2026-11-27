package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import java.util.regex.Pattern;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CookieExtractionStrategy extends IndexedExtractionStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(CookieExtractionStrategy.class);
  private static final Pattern SET_COOKIE_LINES = Pattern.compile("(?m)^Set-Cookie.*");

  @Override
  protected String searchableContent(SampleResult response) {
    // We get the cookies from the response
    return getCleanedResponseHeaders(response.getResponseHeaders());
  }

  @Override
  protected LocationType locationType() {
    return LocationType.COOKIE;
  }

  private static String getCleanedResponseHeaders(String responseHeadersRaw) {
    return SET_COOKIE_LINES.matcher(responseHeadersRaw).replaceAll("");
  }
}
