package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.core.automatic.extraction.StructureType;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.BodyExtractionStrategy;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.CookieExtractionStrategy;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.ExtractionStrategy;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.HeaderExtractionStrategy;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.LocationType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.jmeter.samplers.SampleResult;

/**
 * Class that identifies the location of an argument in a response.
 * Uses a list of {@link ExtractionStrategy} to identify the location.
 */
public class ResponseAnalyzer {

  private final List<ExtractionStrategy> strategies = Arrays.asList(
      new HeaderExtractionStrategy(),
      new CookieExtractionStrategy(),
      new BodyExtractionStrategy()
  );

  private HashMap<String, StructureType> resposeStructureTypeCache =
      new HashMap<String, StructureType>();

  /**
   * Identifies the location of an argument in a response.
   * Uses a list of {@link ExtractionStrategy} to identify the location.
   *
   * @param response The response to analyze
   * @param value    The value to search in the response
   * @return The location of the argument in the response
   * @see ExtractionStrategy
   */
  public LocationType identifyArgumentLocation(SampleResult response, String value) {
    for (ExtractionStrategy strategy : strategies) {
      LocationType location = strategy.identifyLocationInResponse(response, value);
      if (location != LocationType.UNKNOWN) {
        return location;
      }
    }
    return LocationType.UNKNOWN;
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
    StructureType structureType = StructureType.UNKNOWN;
    String responseKey = response.getSampleLabel() + ":" + locationType;
    if (resposeStructureTypeCache.containsKey(responseKey)) {
      return resposeStructureTypeCache.get(responseKey);
    } else {
      if (locationType == LocationType.HEADER || locationType == LocationType.COOKIE) {
        return StructureType.RAW_TEXT;
      } else if (locationType == LocationType.BODY) {
        if (JMeterElementUtils.isJson(response.getResponseDataAsString())) {
          structureType = StructureType.JSON;
        } else {
          structureType = StructureType.RAW_TEXT;
        }
      }
    }
    resposeStructureTypeCache.put(responseKey, structureType);
    return structureType;
  }

}
