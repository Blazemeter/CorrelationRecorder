package com.blazemeter.jmeter.correlation.core;

import static junit.framework.TestCase.assertEquals;

import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.FunctionCorrelationReplacement;
import com.blazemeter.jmeter.correlation.siebel.SiebelContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CorrelationComponentsRegistryTest {

  private static final String VALID_EXTRACTOR_COMPONENT = RegexCorrelationExtractor.class
      .getCanonicalName();
  private static final String VALID_REPLACEMENT_COMPONENT = FunctionCorrelationReplacement.class
      .getCanonicalName();
  private static final String VALID_EXTRACTOR_TYPE = "Regex";
  private static final String VALID_REPLACEMENT_TYPE = "Function";
  private static final String INVALID_COMPONENT_STRING = "invalid.component.string.java";
  private static final String CONTEXT_COMPONENT = SiebelContext.class.getCanonicalName();
  private static final String DEFAULT_REGEX = "regex";
  private static final String DEFAULT_GROUP_NUMBER = "2";
  private static final String DEFAULT_GROUP_MATCH = "3";
  private static final String DEFAULT_TARGET = ResultField.RESPONSE_HEADERS.name();
  private static final List<String> EXTRACTOR_PARAMS = Arrays.asList(DEFAULT_REGEX,
      DEFAULT_GROUP_NUMBER, DEFAULT_GROUP_MATCH, DEFAULT_TARGET);
  private CorrelationComponentsRegistry correlationComponentsRegistry;

  @Before
  public void setup() {
    correlationComponentsRegistry = new CorrelationComponentsRegistry();
  }

  @Test
  public void shouldNotReturnErrorsWhenComponentStringValid() throws ClassNotFoundException {
    correlationComponentsRegistry.addComponent(VALID_EXTRACTOR_COMPONENT);
  }

  @Test(expected = ClassNotFoundException.class)
  public void shouldThrowErrorWhenComponentStringNotValid() throws ClassNotFoundException {
    correlationComponentsRegistry.addComponent(INVALID_COMPONENT_STRING);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowErrorWhenComponentStringContainsContexts() throws ClassNotFoundException {
    correlationComponentsRegistry.addComponent(CONTEXT_COMPONENT);
  }

  @Test
  public void shouldReturnTrueWhenContainsExtractorWithAddedExtractorClass()
      throws ClassNotFoundException {
    correlationComponentsRegistry.addComponent(VALID_EXTRACTOR_COMPONENT);
    assert (correlationComponentsRegistry.containsExtractor(VALID_EXTRACTOR_TYPE));
  }

  @Test
  public void shouldReturnTrueWhenContainsReplacementWithAddedReplacementClass()
      throws ClassNotFoundException {
    correlationComponentsRegistry.addComponent(VALID_REPLACEMENT_COMPONENT);
    assert (correlationComponentsRegistry.containsReplacement(VALID_REPLACEMENT_TYPE));
  }

  @Test
  public void shouldCreateExtractorWhenGetCorrelationExtractorWithParams() {
    RegexCorrelationExtractor expectedExtractor = new RegexCorrelationExtractor(DEFAULT_REGEX,
        DEFAULT_GROUP_NUMBER, DEFAULT_GROUP_MATCH, DEFAULT_TARGET);
    CorrelationExtractor correlationExtractor = correlationComponentsRegistry
        .getCorrelationExtractor(VALID_EXTRACTOR_COMPONENT, EXTRACTOR_PARAMS);
    assertEquals(expectedExtractor.toString(), correlationExtractor.toString());
  }

  @Test
  public void shouldRulePartWhenGetCorrelationRulePartTestElement() {
    assertEquals(new RegexCorrelationExtractor().toString(), correlationComponentsRegistry
        .getCorrelationRulePartTestElement(RegexCorrelationExtractor.class.getCanonicalName())
        .toString());
  }

  @Test
  public void shouldReturnExtractorWhenGetCorrelationExtractorWithType() {
    assertEquals(new RegexCorrelationExtractor().toString(), correlationComponentsRegistry
        .getCorrelationExtractor(VALID_EXTRACTOR_TYPE, new ArrayList<>()).toString());
  }

  @Test
  public void shouldReturnReplacementWhenGetCorrelationReplacementWithType() {
    assertEquals(new FunctionCorrelationReplacement().toString(), correlationComponentsRegistry
        .getCorrelationReplacement(VALID_REPLACEMENT_TYPE, new ArrayList<>()).toString());
  }
}