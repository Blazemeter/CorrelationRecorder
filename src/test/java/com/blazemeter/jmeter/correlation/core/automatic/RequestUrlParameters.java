package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.Arrays;
import java.util.Collection;

public class RequestUrlParameters {

  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"http://example.com/path?Name=value&other=123", "value"},
        {"https://api.example.org/endpoint?Name=abc123&param=456", "abc123"},
        {"https://www.example.net/query?param1=123&Name=test", "test"},
        {"https://example.org/single?Name=only", "only"},
        {null, null},
        {"https://example.com/path?param=value&other=123", null}
    });
  }
}

