package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import com.blazemeter.jmeter.correlation.core.automatic.replacement.method.ReplacementString;
import java.util.Arrays;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.samplers.SampleResult;

public interface ExtractionStrategy {

  Pair<LocationType, String> identifyLocationInResponse(SampleResult response, String value);

  static Pair<LocationType, String> getLocationAndValue(String content, String value,
                                                        LocationType location) {
    // Lambda need a final mutable variable, we use the string array for that
    final String[] appliedValue = new String[1];
    return Arrays.stream(ReplacementString.values())
        .filter(r -> {
          appliedValue[0] = r.applyFunction(value);
          return content.contains(appliedValue[0]);
        }).findFirst()
        .map(string -> Pair.of(location, appliedValue[0]))
        .orElseGet(() -> Pair.of(LocationType.UNKNOWN, ""));
  }
}
