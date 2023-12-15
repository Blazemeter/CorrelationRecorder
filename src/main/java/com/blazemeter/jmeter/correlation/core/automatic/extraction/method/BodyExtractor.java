package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.automatic.RegexCommons;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BodyExtractor extends Extractor {
  private static final Logger LOG = LoggerFactory.getLogger(BodyExtractor.class);

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
    String responseDataAsString = response.getResponseDataAsString();
    List<Integer> indexes = ExtractorGenerator.getIndexes(value, responseDataAsString);
    List<CorrelationExtractor<?>> extractors = new ArrayList<>();
    for (int i = 0; i < indexes.size(); i++) {
      int index = indexes.get(i);
      String context = getContextString(responseDataAsString, value, index);
      RegexCorrelationExtractor<?> extractor =
          generateExtractor(name, value, context, ResultField.BODY);
      extractor.setMultiValued(true);
      extractors.add(extractor);
    }
    return extractors;
  }

  public String getContextString(String response, String value, int location) {
    int contextLength = 10;
    int indexStart = Math.max(location - contextLength, 0);
    // +2 for the 0-based index and the length of the value
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
