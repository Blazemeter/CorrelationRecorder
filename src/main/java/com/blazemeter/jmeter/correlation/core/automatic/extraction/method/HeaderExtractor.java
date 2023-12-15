package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.automatic.RegexCommons;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HeaderExtractor class extends the Extractor abstract class.
 * It provides methods for extracting values from the headers of a SampleResult,
 * creating PostProcessors, and creating CorrelationExtractors.
 */
public class HeaderExtractor extends Extractor {

  private static final Logger LOG = LoggerFactory.getLogger(HeaderExtractor.class);

  /**
   * This method removes all Set-Cookie headers from the response headers.
   *
   * @param responseHeadersRaw the raw response headers.
   * @return the cleaned response headers.
   */
  private static String getCleanedResponseHeaders(String responseHeadersRaw) {
    return responseHeadersRaw.replaceAll("(?m)^Set-Cookie.*", "");
  }

  /**
   * This method is responsible for extracting a value from the headers of a SampleResult.
   * The specific implementation of this method is not provided in this class.
   *
   * @param response the SampleResult from which to extract the value.
   * @param value    the value to extract from the SampleResult.
   * @return a list of extracted values.
   */
  @Override
  public List<String> extractValue(SampleResult response, String value) {
    // Logic to extract value from the header
    return null; // Placeholder
  }

  /**
   * This method is responsible for creating a list of PostProcessors based on a SampleResult.
   * The specific implementation of this method is not provided in this class.
   *
   * @param response the SampleResult from which to create the PostProcessors.
   * @param value    the value to use for creating the PostProcessors.
   * @param name     the name to use for creating the PostProcessors.
   * @return a list of created PostProcessors.
   */
  @Override
  public List<PostProcessor> getPostProcessors(SampleResult response, String value, String name) {
    return new ArrayList<>();
  }

  /**
   * This method is responsible for creating a list of CorrelationExtractors based on a
   * SampleResult.
   * It first cleans the response headers and gets the indexes of the value in the headers.
   * Then, it generates a CorrelationExtractor for each index and adds it to the list of
   * extractors.
   *
   * @param response the SampleResult from which to create the CorrelationExtractors.
   * @param value    the value to use for creating the CorrelationExtractors.
   * @param name     the name to use for creating the CorrelationExtractors.
   * @return a list of created CorrelationExtractors.
   */
  @Override
  public List<CorrelationExtractor<?>> getCorrelationExtractors(SampleResult response, String value,
                                                                String name) {
    String responseDataAsString = response.getResponseHeaders();
    String headers = getCleanedResponseHeaders(responseDataAsString);
    List<Integer> indexes = ExtractorGenerator.getIndexes(value, headers);
    List<CorrelationExtractor<?>> extractors = new ArrayList<>();
    for (int i = 0; i < indexes.size(); i++) {
      String source = "Response Header Set-Cookie ('" + ExtractionConstants.TEXT_NOT_ENCODED + "')";

      String possibleValue = "X-Vcap-Request-Id: " + value;
      boolean appearsNaturally = headers.contains(possibleValue);
      if (appearsNaturally) {
        boolean singleLineExtraction =
            headers.substring(indexes.get(0) + value.length()).substring(0, 1)
                .equals(System.lineSeparator());
      }

      String context = getContextString(headers, value, indexes.get(i));
      RegexCorrelationExtractor<?> extractor =
          generateExtractor(name, value, context, ResultField.RESPONSE_HEADERS);
      extractors.add(extractor);
    }
    return extractors;
  }

  /**
   * This method retrieves a substring of the response that surrounds the location of a value.
   * The substring starts from contextLength characters before the location and ends at
   * contextLength characters after the location.
   * If the location is near the start or end of the response, the substring starts or
   * ends at the start or end of the response, respectively.
   *
   * @param response the response from which to retrieve the substring.
   * @param value    the value whose location is used to determine the start and end of
   *                 the substring.
   * @param location the location of the value in the response.
   * @return the substring of the response that surrounds the location of the value.
   */
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

  /**
   * This method generates a RegexCorrelationExtractor based on a value, its context string,
   * and the target field.
   * It sets the variable name of the extractor to the value name and generates a regex for
   * extracting the value from the context string.
   * The regex, the index of the match to use, the group of the match to use, the name of
   * the target field, and a flag indicating whether to use the regex are set as the
   * parameters of the extractor.
   *
   * @param valueName     the name of the value to extract.
   * @param originalValue the original value to extract.
   * @param contextString the context string from which to extract the value.
   * @param targetField   the target field from which to extract the value.
   * @return the generated RegexCorrelationExtractor.
   */
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
