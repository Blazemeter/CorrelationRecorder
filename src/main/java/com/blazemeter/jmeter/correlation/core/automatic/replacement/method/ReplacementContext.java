package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import com.blazemeter.jmeter.correlation.core.automatic.Sources;

public class ReplacementContext {

  public static ReplacementStrategy getStrategy(String source) {
    if (source.contains(Sources.REQUEST_BODY_JSON) || source.contains(
        Sources.REQUEST_BODY_JSON_NUMERIC)) {
      return new ReplacementJsonStrategy();
    } else {
      return new ReplacementRegexStrategy();
    }
  }

}
