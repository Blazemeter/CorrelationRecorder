package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.RegexCommons;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.Arrays;
import java.util.List;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexExtractor extends Extractor {

  private static final Logger LOG = LoggerFactory.getLogger(RegexExtractor.class);
  private final Configuration configuration;

  public RegexExtractor(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public List<String> extractValue(SampleResult response, String value) {
    // Logic to extract value from the body
    return null; // Placeholder
  }

  @Override
  public List<PostProcessor> getPostProcessors(SampleResult response, String value, String name) {
    return null; // Placeholder
  }

  @Override
  public List<CorrelationExtractor<?>> getCorrelationExtractors(SampleResult response, String value,
      String name) {
    return null;
  }

  /**
   * This method retrieves a substring of the response that surrounds the location of a value. The
   * substring starts from contextLength characters before the location and ends at contextLength
   * characters after the location. If the location is near the start or end of the response, the
   * substring starts or ends at the start or end of the response, respectively.
   *
   * @param response the response from which to retrieve the substring.
   * @param value the value whose location is used to determine the start and end of the substring.
   * @param location the location of the value in the response.
   * @return the substring of the response that surrounds the location of the value.
   */
  public String getContextString(String response, String value, int location) {
    int contextLength = configuration.getContextLength();
    int indexStart = Math.max(location - contextLength, 0);
    int indexEnd = location + value.length() + contextLength;

    if (response.length() < indexEnd) {
      indexEnd = response.length();
    }

    try {
      return response.substring(indexStart, indexEnd);
    } catch (StringIndexOutOfBoundsException e) {
      LOG.error("Error getting context string for value '{}' in response '{}'", value, response);
      return "";
    }
  }

  /**
   * This method generates a RegexCorrelationExtractor based on a value, its context string, and the
   * target field. It sets the variable name of the extractor to the value name and generates a
   * regex for extracting the value from the context string. The regex, the index of the match to
   * use, the group of the match to use, the name of the target field, and a flag indicating whether
   * to use the regex are set as the parameters of the extractor.
   *
   * @param valueName the name of the value to extract.
   * @param originalValue the original value to extract.
   * @param contextString the context string from which to extract the value.
   * @param targetField the target field from which to extract the value.
   * @return the generated RegexCorrelationExtractor.
   */
  @VisibleForTesting
  public RegexCorrelationExtractor<?> generateExtractor(String valueName, String originalValue,
      String contextString,
      ResultField targetField) {
    RegexCorrelationExtractor<?> extractor = new RegexCorrelationExtractor<>();
    extractor.setVariableName(valueName);
    String regex = RegexCommons.dynamicGenerateExtractorRegex(originalValue, contextString);
    extractor.setParams(Arrays.asList(regex, "-1", "1", targetField.name(), "true"));
    return extractor;
  }
}
