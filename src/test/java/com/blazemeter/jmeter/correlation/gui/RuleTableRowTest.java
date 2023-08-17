package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RuleTableRowTest {

  private static final CorrelationRulePartTestElement<?> DEFAULT_REPLACEMENT =
      new RegexCorrelationReplacement<>();
  private static final CorrelationRulePartTestElement<?> DEFAULT_EXTRACTOR =
      new RegexCorrelationExtractor<>();

  @Mock
  public CorrelationComponentsRegistry registry;
  @Mock
  private Runnable update;
  @Mock
  private Consumer<CorrelationRulePartTestElement<?>> displayExtensions;
  private RuleTableRow rule;

  @Before
  public void setup() {
    prepareRegistry();
    rule = new RuleTableRow(0, update, displayExtensions, registry);
  }

  private void prepareRegistry() {
    when(registry.buildActiveExtractorRulePart())
        .thenReturn(Arrays.asList(CorrelationComponentsRegistry.NONE_EXTRACTOR, DEFAULT_EXTRACTOR));
    when(registry.buildActiveReplacementRulePart()).thenReturn(
        Arrays.asList(CorrelationComponentsRegistry.NONE_REPLACEMENT, DEFAULT_REPLACEMENT));
  }

  @Test
  public void shouldUpdateExtractorValuesWhenSetExtractorFromRulePart() {
    List<String> originalValues = getExtractorValues();
    rule.setExtractorFromRulePart(DEFAULT_EXTRACTOR);
    assertThat(getExtractorValues())
        .isNotEqualTo(originalValues)
        .isEqualTo(Arrays.asList("param=\"(.+?)\"", "1", "1", "BODY", "false"));
  }

  private List<String> getExtractorValues() {
    return rule.getExtractorConfigurationPanel().getComponentsValues();
  }

  @Test
  public void shouldUpdateReplacementValuesWhenSetReplacementFromRulePart() {
    List<String> originalValues = getReplacementValues();
    rule.setReplacementFromRulePart(DEFAULT_REPLACEMENT);
    assertThat(getReplacementValues())
        .isNotEqualTo(originalValues)
        .isEqualTo(Arrays.asList("param=\"(.+?)\"", "", "false"));
  }

  private List<String> getReplacementValues() {
    return rule.getReplacementConfigurationPanel().getComponentsValues();
  }

  @Test
  public void shouldNotUpdateExtractorValuesWhenSetExtractorFromRulePartOfNull() {
    List<String> originalValues = getExtractorValues();
    rule.setExtractorFromRulePart(null);
    assertThat(getExtractorValues())
        .isEqualTo(originalValues);
  }

  @Test
  public void shouldBeEnabledWhenCreated() {
    assertThat(rule.isEnabled()).isTrue();
  }

  @Test
  public void shouldGetConfiguredRuleWhenGetCorrelationRule() {
    String referenceVariable = "refVar";
    rule.setVariableName(referenceVariable);
    rule.setExtractorFromRulePart(DEFAULT_EXTRACTOR);
    rule.setReplacementFromRulePart(DEFAULT_REPLACEMENT);

    assertThat(rule.getCorrelationRule()).isEqualTo(
        new CorrelationRule(referenceVariable, (CorrelationExtractor<?>) DEFAULT_EXTRACTOR,
            (CorrelationReplacement<?>) DEFAULT_REPLACEMENT));
  }

  @Test
  public void shouldGetEmptyRuleWhenGetCorrelationRuleWithoutConfiguration() {
    String referenceVariable = "refVar";
    rule.setVariableName(referenceVariable);
    assertThat(rule.getCorrelationRule()).isEqualTo(
        new CorrelationRule(referenceVariable, null, null));
  }
}
