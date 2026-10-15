package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.core.automatic.extraction.StructureType;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.BodyExtractionStrategy;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.CookieExtractionStrategy;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.ExtractionStrategy;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.HeaderExtractionStrategy;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.IndexedExtractionStrategy;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.LocationType;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.MultiPatternFinder;
import com.blazemeter.jmeter.correlation.core.automatic.replacement.method.ReplacementString;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.samplers.SampleResult;

/**
 * Class that identifies the location of an argument in a response.
 * Uses a list of {@link ExtractionStrategy} to identify the location.
 */
public class ResponseAnalyzer {

  private final List<IndexedExtractionStrategy> strategies = Arrays.asList(
      new HeaderExtractionStrategy(),
      new CookieExtractionStrategy(),
      new BodyExtractionStrategy()
  );

  // Keyed by the response itself, not by its label: two responses can share a label
  private final Map<SampleResult, StructureType> bodyStructureTypes = new IdentityHashMap<>();

  /**
   * Gives the analyzer the values it is going to be asked about, so each part of a response is
   * scanned once for all of them and their encoding variants, instead of once per value. The
   * locations found are the same; a value it is asked about and was not given is searched
   * directly.
   *
   * @param values the values the following calls to {@link #identifyArgumentLocation} will use
   */
  public void setCandidateValues(Collection<String> values) {
    Set<String> candidates = new HashSet<>(values);
    Set<String> variants = new HashSet<>();
    for (String value : candidates) {
      for (ReplacementString variant : ReplacementString.values()) {
        variants.add(variant.applyFunction(value));
      }
    }
    MultiPatternFinder finder = new MultiPatternFinder(variants);
    for (IndexedExtractionStrategy strategy : strategies) {
      strategy.setCandidates(candidates, finder);
    }
  }

  /**
   * Identifies the location of an argument in a response.
   * Uses a list of {@link ExtractionStrategy} to identify the location.
   *
   * @param response The response to analyze
   * @param value    The value to search in the response
   * @return The location of the argument in the response
   * @see ExtractionStrategy
   */
  public Pair<LocationType, String> identifyArgumentLocation(SampleResult response, String value) {
    for (ExtractionStrategy strategy : strategies) {
      Pair<LocationType, String> location = strategy.identifyLocationInResponse(response, value);
      if (location.getLeft() != LocationType.UNKNOWN) {
        return location;
      }
    }
    return Pair.of(LocationType.UNKNOWN, "");
  }

  /**
   * Given a {@link SampleResult} and a {@link LocationType}, identifies the structure of the
   * value in the response. The structure can be {@link StructureType#JSON}, {@link
   * StructureType#XML}, {@link StructureType#RAW_TEXT} or {@link StructureType#UNKNOWN}.
   * This is particularly useful when the argument is in the body of the response and we need to
   * know the structure of the body to generate the extractor.
   *
   * @param response     The response to analyze
   * @param locationType The location of the argument in the response
   * @return The structure of the argument in the response
   * @see StructureType
   */
  public StructureType identifyStructureType(SampleResult response, LocationType locationType) {
    if (locationType == LocationType.HEADER || locationType == LocationType.COOKIE) {
      return StructureType.RAW_TEXT;
    } else if (locationType != LocationType.BODY) {
      return StructureType.UNKNOWN;
    }
    // Parsing the body to tell JSON apart is costly, so it is done once per response
    return bodyStructureTypes.computeIfAbsent(response,
        r -> JMeterElementUtils.isJson(r.getResponseDataAsString()) ? StructureType.JSON
            : StructureType.RAW_TEXT);
  }

}
