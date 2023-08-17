package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;

public class ExtractionSuggestion {
  private RegexCorrelationExtractor<?> extractor;
  private SampleResult sampleResult;
  private String value;
  private String name;
  private String source;
  // This suggestion comes from the analysis of a sample result?
  private boolean comesFromSampleResult = true;
  private HTTPSamplerBase sampler;

  public ExtractionSuggestion(RegexCorrelationExtractor<?> extractor, SampleResult sampleResult) {
    this.extractor = extractor;
    this.sampleResult = sampleResult;
  }

  public ExtractionSuggestion(RegexCorrelationExtractor<?> extractor, HTTPSamplerBase sampler) {
    this.extractor = extractor;
    this.sampler = sampler;
    this.comesFromSampleResult = false;
  }

  public RegexCorrelationExtractor<?> getExtractor() {
    return extractor;
  }

  public void setExtractor(RegexCorrelationExtractor<?> extractor) {
    this.extractor = extractor;
  }

  public SampleResult getSampleResult() {
    return sampleResult;
  }

  public void setSampleResult(SampleResult sampleResult) {
    this.sampleResult = sampleResult;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public boolean comesFromASampleResult() {
    return comesFromSampleResult;
  }

  public HTTPSamplerBase getSampler() {
    return sampler;
  }

  @Override
  public String toString() {
    return "ExtractionSuggestion {"
        + "name='" + name + '\''
        + ", value='" + value + '\''
        + ", source='" + source + '\''
        + (comesFromSampleResult ? ", sampleResult=" + sampleResult.getSampleLabel()
        : ", httpSamplerBase=" + sampler.getName())
        + ", \nextractor=" + extractor + '}';
  }
}
