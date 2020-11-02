package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RuleConfigurationTest {

  private static final CorrelationRulePartTestElement<?> NONE =
      CorrelationComponentsRegistry.NONE_EXTRACTOR;
  private static final CorrelationRulePartTestElement<?> DEFAULT_REPLACEMENT =
      new RegexCorrelationReplacement<>();
  private static final CorrelationRulePartTestElement<?> DEFAULT_EXTRACTOR =
      new RegexCorrelationExtractor<>();

  @Mock
  public CorrelationComponentsRegistry registry;
  @Mock
  private Runnable update;
  private RuleConfiguration ruleConfiguration;

  @Before
  public void setup() {
    prepareRegistry();
    ruleConfiguration = new RuleConfiguration(0, update, registry);
  }

  private void prepareRegistry() {
    when(registry.buildActiveExtractors()).thenReturn(Arrays.asList(NONE, DEFAULT_EXTRACTOR));
    when(registry.buildActiveReplacements()).thenReturn(Arrays.asList(NONE, DEFAULT_REPLACEMENT));
  }

  @Test
  public void shouldUpdateExtractorValuesWhenSetExtractorFromRulePart() {
    List<String> originalValues = getExtractorValues();
    ruleConfiguration.setExtractorFromRulePart(DEFAULT_EXTRACTOR);
    assertThat(getExtractorValues())
        .isNotEqualTo(originalValues)
        .isEqualTo(Arrays.asList("param=\"(.+?)\"", "1", "1", "BODY", "false"));
  }

  private List<String> getExtractorValues() {
    return ruleConfiguration.getExtractorConfigurationPanel().getValues();
  }

  @Test
  public void shouldUpdateReplacementValuesWhenSetReplacementFromRulePart() {
    List<String> originalValues = getReplacementValues();
    ruleConfiguration.setReplacementFromRulePart(DEFAULT_REPLACEMENT);
    assertThat(getReplacementValues())
        .isNotEqualTo(originalValues)
        .isEqualTo(Arrays.asList("param=\"(.+?)\"", "", "false"));
  }

  private List<String> getReplacementValues() {
    return ruleConfiguration.getReplacementConfigurationPanel().getValues();
  }

  @Test
  public void shouldNotUpdateExtractorValuesWhenSetExtractorFromRulePartOfNull() {
    List<String> originalValues = getExtractorValues();
    ruleConfiguration.setExtractorFromRulePart(null);
    assertThat(getExtractorValues())
        .isEqualTo(originalValues);
  }

  @Test
  public void shouldBeEnabledWhenCreated() {
    assertThat(ruleConfiguration.isEnable()).isTrue();
  }

  @Test
  public void shouldGetConfiguredRuleWhenGetCorrelationRule() {
    String referenceVariable = "refVar";
    ruleConfiguration.setVariableName(referenceVariable);
    ruleConfiguration.setExtractorFromRulePart(DEFAULT_EXTRACTOR);
    ruleConfiguration.setReplacementFromRulePart(DEFAULT_REPLACEMENT);

    assertThat(ruleConfiguration.getCorrelationRule()).isEqualTo(
        new CorrelationRule(referenceVariable, (CorrelationExtractor<?>) DEFAULT_EXTRACTOR,
            (CorrelationReplacement<?>) DEFAULT_REPLACEMENT));
  }

  @Test
  public void shouldGetEmptyRuleWhenGetCorrelationRuleWithoutConfiguration() {
    String referenceVariable = "refVar";
    ruleConfiguration.setVariableName(referenceVariable);
    assertThat(ruleConfiguration.getCorrelationRule()).isEqualTo(
        new CorrelationRule(referenceVariable, null, null));
  }
}
