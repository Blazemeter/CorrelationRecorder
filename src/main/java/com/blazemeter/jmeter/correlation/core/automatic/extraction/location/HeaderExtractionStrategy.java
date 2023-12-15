package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import org.apache.jmeter.samplers.SampleResult;

public class HeaderExtractionStrategy implements ExtractionStrategy {
  @Override
  public LocationType identifyLocationInResponse(SampleResult response, String value) {
    // Logic to determine if the argument is in the headers
    String headersWithoutSetCookies =
        getCleanedResponseHeaders(String.valueOf(response.getResponseHeaders()));
    boolean isInRawHeaders = headersWithoutSetCookies.contains(value);
    String encodedValue = ExtractorGenerator.encodeValue(value);
    boolean isInEncodedHeaders = !isInRawHeaders && headersWithoutSetCookies.contains(encodedValue);
    // Return LocationType.HEADER if found, otherwise return LocationType.UNKNOWN
    if (isInRawHeaders || isInEncodedHeaders) {
      return LocationType.HEADER;
    }
    return LocationType.UNKNOWN; // Placeholder
  }

  private static String getCleanedResponseHeaders(String responseHeadersRaw) {
    return responseHeadersRaw.replaceAll("(?m)^Set-Cookie.*", "");
  }
}
