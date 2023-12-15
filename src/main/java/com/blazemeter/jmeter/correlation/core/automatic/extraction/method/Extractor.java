package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import java.util.List;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;

/**
 * The Extractor abstract class provides a blueprint for creating different types of extractors.
 * Each extractor is responsible for extracting values from a SampleResult, creating
 * PostProcessors, and creating CorrelationExtractors.
 */
public abstract class Extractor {

  /**
   * This method is responsible for extracting a value from a SampleResult.
   * The specific implementation of this method depends on the concrete subclass of Extractor.
   *
   * @param response the SampleResult from which to extract the value.
   * @param value    the value to extract from the SampleResult.
   * @return a list of extracted values.
   */
  public abstract List<String> extractValue(SampleResult response, String value);

  /**
   * This method is responsible for creating a list of PostProcessors based on a SampleResult.
   * The specific implementation of this method depends on the concrete subclass of Extractor.
   *
   * @param response the SampleResult from which to create the PostProcessors.
   * @param value    the value to use for creating the PostProcessors.
   * @param name     the name to use for creating the PostProcessors.
   * @return a list of created PostProcessors.
   */
  public abstract List<PostProcessor> getPostProcessors(SampleResult response, String value,
                                                        String name);

  /**
   * This method is responsible for creating a list of CorrelationExtractors based on a
   * SampleResult.
   * The specific implementation of this method depends on the concrete subclass of Extractor.
   *
   * @param response the SampleResult from which to create the CorrelationExtractors.
   * @param value    the value to use for creating the CorrelationExtractors.
   * @param name     the name to use for creating the CorrelationExtractors.
   * @return a list of created CorrelationExtractors.
   */
  public abstract List<CorrelationExtractor<?>> getCorrelationExtractors(SampleResult response,
                                                                         String value, String name);
}
