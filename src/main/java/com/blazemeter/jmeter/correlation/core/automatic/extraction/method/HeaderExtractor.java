package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import java.util.ArrayList;
import java.util.List;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HeaderExtractor class extends the Extractor abstract class. It provides methods for
 * extracting values from the headers of a SampleResult, creating PostProcessors, and creating
 * CorrelationExtractors.
 */
public class HeaderExtractor extends RegexExtractor {

  private static final Logger LOG = LoggerFactory.getLogger(HeaderExtractor.class);

  public HeaderExtractor(Configuration configuration) {
    super(configuration);
  }

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
   * This method is responsible for extracting a value from the headers of a SampleResult. The
   * specific implementation of this method is not provided in this class.
   *
   * @param response the SampleResult from which to extract the value.
   * @param value the value to extract from the SampleResult.
   * @return a list of extracted values.
   */
  @Override
  public List<String> extractValue(SampleResult response, String value) {
    // Logic to extract value from the header
    return null; // Placeholder
  }

  /**
   * This method is responsible for creating a list of PostProcessors based on a SampleResult. The
   * specific implementation of this method is not provided in this class.
   *
   * @param response the SampleResult from which to create the PostProcessors.
   * @param value the value to use for creating the PostProcessors.
   * @param name the name to use for creating the PostProcessors.
   * @return a list of created PostProcessors.
   */
  @Override
  public List<PostProcessor> getPostProcessors(SampleResult response, String value, String name) {
    return new ArrayList<>();
  }

  /**
   * This method is responsible for creating a list of CorrelationExtractors based on a
   * SampleResult. It first cleans the response headers and gets the indexes of the value in the
   * headers. Then, it generates a CorrelationExtractor for each index and adds it to the list of
   * extractors.
   *
   * @param response the SampleResult from which to create the CorrelationExtractors.
   * @param value the value to use for creating the CorrelationExtractors.
   * @param name the name to use for creating the CorrelationExtractors.
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
}
