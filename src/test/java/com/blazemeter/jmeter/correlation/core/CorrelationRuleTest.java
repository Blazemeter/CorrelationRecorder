package com.blazemeter.jmeter.correlation.core;

import static org.junit.Assert.assertEquals;

import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationRuleTest {

  private static final String VARIABLE_NAME = "variable";

  private static final String EXPECTED_REGEX = "Test_SWEACn=(.*?)&";
  private static final String EXPECTED_GROUP_NUMBER = "1";
  private static final String EXPECTED_MATCH_NUMBER = "1";
  private static final String EXPECTED_TARGET = ResultField.BODY.name();

  private RegexCorrelationExtractor<?> correlationExtractor;
  private RegexCorrelationReplacement<?> correlationReplacement;
  private CorrelationRule correlationRule;

  @Before
  public void setup() {
    correlationExtractor = new RegexCorrelationExtractor<>(EXPECTED_REGEX, EXPECTED_GROUP_NUMBER,
        EXPECTED_MATCH_NUMBER, EXPECTED_TARGET, "false");
    correlationReplacement = new RegexCorrelationReplacement<>(EXPECTED_REGEX);

    correlationRule = new CorrelationRule(VARIABLE_NAME, correlationExtractor,
        correlationReplacement);
  }

  @Test
  public void shouldReturnACorrelationRuleTestElement() {
    CorrelationRuleTestElement correlationRuleTestElementResult = correlationRule
        .buildTestElement();

    CorrelationRuleTestElement expectedCorrelationRuleTestElement = buildExpectedCorrelationRule();

    assertEquals(expectedCorrelationRuleTestElement.toString().trim(),
        correlationRuleTestElementResult.toString().trim());
  }

  private CorrelationRuleTestElement buildExpectedCorrelationRule() {
    return new CorrelationRuleTestElement(VARIABLE_NAME, correlationExtractor,
        correlationReplacement, (p) -> p);
  }
}
