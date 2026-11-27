package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import java.util.regex.Pattern;
import org.apache.jmeter.samplers.SampleResult;

public class HeaderExtractionStrategy extends IndexedExtractionStrategy {

  private static final Pattern SET_COOKIE_LINES = Pattern.compile("(?m)^Set-Cookie.*");

  @Override
  protected String searchableContent(SampleResult response) {
    // Logic to determine if the argument is in the headers
    return getCleanedResponseHeaders(String.valueOf(response.getResponseHeaders()));
  }

  @Override
  protected LocationType locationType() {
    return LocationType.HEADER;
  }

  private static String getCleanedResponseHeaders(String responseHeadersRaw) {
    return SET_COOKIE_LINES.matcher(responseHeadersRaw).replaceAll("");
  }
}
