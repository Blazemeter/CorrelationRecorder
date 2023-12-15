package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import java.util.List;
import org.apache.jmeter.samplers.SampleResult;

public class RawTextBodyExtractor extends BodyExtractor {
  @Override
  public List<String> extractValue(SampleResult response, String value) {
    throw new UnsupportedOperationException(
        "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
