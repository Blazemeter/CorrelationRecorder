package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import java.util.ArrayList;
import java.util.List;
import org.apache.jmeter.samplers.SampleResult;

public class RawTextBodyExtractor extends RegexExtractor {

  public RawTextBodyExtractor(Configuration configuration) {
    super(configuration);
  }

  @Override
  public List<String> extractValue(SampleResult response, String value) {
    throw new UnsupportedOperationException(
        "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<CorrelationExtractor<?>> getCorrelationExtractors(SampleResult response, String value,
      String name) {
    String responseDataAsString = response.getResponseDataAsString();
    List<Integer> indexes = ExtractorGenerator.getIndexes(value, responseDataAsString);
    List<CorrelationExtractor<?>> extractors = new ArrayList<>();
    String context;
    for (int index : indexes) {
      context = getContextString(responseDataAsString, value, index);
      RegexCorrelationExtractor<?> extractor =
          generateExtractor(name, value, context, ResultField.BODY);
      extractor.setMultiValued(true);
      extractors.add(extractor);
    }
    return extractors;
  }
}
