package com.blazemeter.jmeter.correlation.gui;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotSame;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ResultField;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowIdCorrelationReplacement;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class RulePanelTestIT {

  private static final String CORRELATION_PREFIX = "CorrelationRule";
  private static final String EXTRACTOR_PREFIX = CORRELATION_PREFIX + ".CorrelationExtractor";
  private static final String REPLACEMENT_PREFIX = CORRELATION_PREFIX + ".CorrelationReplacement";
  private static final String GROUP_NUMBER_NAME = "groupNumber";
  private static final String MATCH_NUMBER_NAME = "matchNumber";
  private static final String EXTRACTOR_REGEX_NAME = "regex";
  private static final String EMPTY_DESCRIPTION = "";

  private static final String EXTRACTOR_COMPONENTS_NAME = "extractor";
  private static final String REPLACEMENT_COMPONENTS_NAME = "replacement";

  private static final int DEFAULT_MATCH_GROUP = 1;
  private static final String TARGET_FIELD_NAME = "target";
  private static final String REGEX_DEFAULT_VALUE = "";
  private static final String JTEXTFIELD_NAME = "text";
  private static final String JCOMBOBOX_NAME = "combo";

  private static final String REFERENCE_VARIABLE_SUFFIX = "-referenceVariable";
  private static final String CORRELATION_REGEX_COMBO_NAME = "Regex";
  private static final String CORRELATION_SIEBELROW_COMBO_NAME = "SiebelRowId";
  private static final String CORRELATION_EXTRACTOR_COMBO_BOX_NAME_SUFFIX = "-CorrelationExtractor-comboBox";
  private static final String CORRELATION_REPLACEMENT_COMBO_BOX_NAME_SUFFIX = "-CorrelationReplacement-comboBox";
  private static final String EXPECTED_REGEX = "(regex)";
  private static final String EXPECTED_MATCH_NUMBER = "1";
  private static final String EXPECTED_GROUP_NUMBER = "2";
  private static final String EXPECTED_TARGET = ResultField.URL.name();
  private static final List<String> EXPECTED_PARAMS = Arrays
      .asList(EXPECTED_REGEX, EXPECTED_MATCH_NUMBER, EXPECTED_GROUP_NUMBER, EXPECTED_TARGET);
  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Mock
  RulesContainer rulesContainer;
  private RulePanel rulePanel;
  private FrameFixture frame;

  @Before
  public void prepare() {
    prepareRulesContainer();
    rulePanel = new RulePanel(rulesContainer);
    frame = showInFrame(rulePanel);
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  private void prepareRulesContainer() {
    when(rulesContainer.getRules()).thenReturn(new ArrayList<>());
    when(rulesContainer.getReplacements())
        .thenReturn(Arrays.asList(RegexCorrelationReplacement.class.getSimpleName(),
            SiebelRowIdCorrelationReplacement.class.getSimpleName()));
    when(rulesContainer.getExtractors())
        .thenReturn(Collections.singletonList(RegexCorrelationExtractor.class.getSimpleName()));
    when(rulesContainer.getExtractorParamsDefinition(CORRELATION_REGEX_COMBO_NAME))
        .thenReturn(getParametersDefinitionForRegex(EXTRACTOR_PREFIX));
    when(rulesContainer.getReplacementParamsDefinition(CORRELATION_REGEX_COMBO_NAME))
        .thenReturn(getParametersDefinitionForRegex(REPLACEMENT_PREFIX));
    when(rulesContainer.getReplacementParamsDefinition(CORRELATION_SIEBELROW_COMBO_NAME))
        .thenReturn(getParametersDefinitionForSiebelRowId(REPLACEMENT_PREFIX));
    when(rulesContainer.getSizePanelThatContainsRules()).thenReturn(new Dimension(900, 200));

    //We want the rulePanel to be redraw but, that's rulesContainer job.
    doAnswer((Answer<Void>) invocation -> {
      rulePanel.repaint();
      return null;
    }).when(rulesContainer).reDrawScroll();
  }

  private List<ParameterDefinition> getParametersDefinitionForRegex(String prefix) {
    return Arrays
        .asList(
            new ParameterDefinition(prefix + "." + EXTRACTOR_REGEX_NAME, EMPTY_DESCRIPTION,
                REGEX_DEFAULT_VALUE, null),
            new ParameterDefinition(prefix + "." + MATCH_NUMBER_NAME, EMPTY_DESCRIPTION,
                String.valueOf(DEFAULT_MATCH_GROUP),
                null),
            new ParameterDefinition(prefix + "." + GROUP_NUMBER_NAME, EMPTY_DESCRIPTION,
                String.valueOf(DEFAULT_MATCH_GROUP),
                null),
            new ParameterDefinition(prefix + "." + TARGET_FIELD_NAME, EMPTY_DESCRIPTION,
                ResultField.BODY.name(),
                ResultField.getNamesToCodesMapping()));
  }

  private List<ParameterDefinition> getParametersDefinitionForSiebelRowId(String prefix) {
    return Collections.singletonList(
        new ParameterDefinition(prefix + EXTRACTOR_REGEX_NAME, EMPTY_DESCRIPTION,
            REGEX_DEFAULT_VALUE, null));
  }

  @Test
  public void shouldDisplayComboBoxWithCorrelationsWhenCreated() {
    String ruleId = rulePanel.getName();
    softly.assertThat(frame.textBox(ruleId + REFERENCE_VARIABLE_SUFFIX)).isNotNull();
    softly.assertThat(frame.comboBox(ruleId + CORRELATION_EXTRACTOR_COMBO_BOX_NAME_SUFFIX))
        .isNotNull();
    softly.assertThat(frame.comboBox(ruleId + CORRELATION_REPLACEMENT_COMBO_BOX_NAME_SUFFIX))
        .isNotNull();
  }

  @Test
  public void shouldBuildComponentsWhenExtractorSelected() {
    selectExtractorCombo(CORRELATION_REGEX_COMBO_NAME);

    softly.assertThat(frame.textBox(buildExtractorFieldName(EXTRACTOR_REGEX_NAME))).isNotNull();
    softly.assertThat(frame.textBox(buildExtractorFieldName(MATCH_NUMBER_NAME))).isNotNull();
    softly.assertThat(frame.textBox(buildExtractorFieldName(GROUP_NUMBER_NAME))).isNotNull();
    softly.assertThat(frame.comboBox(buildExtractorComboName(TARGET_FIELD_NAME))).isNotNull();
  }

  private void selectExtractorCombo(String item) {
    selectItemCombo(false, item);
  }

  private void selectItemCombo(boolean isReplacement, String item) {
    frame.comboBox(
        rulePanel.getName() + (isReplacement ? CORRELATION_REPLACEMENT_COMBO_BOX_NAME_SUFFIX
            : CORRELATION_EXTRACTOR_COMBO_BOX_NAME_SUFFIX)).selectItem(item);
  }

  private String buildExtractorFieldName(String fieldName) {
    return buildName(EXTRACTOR_COMPONENTS_NAME, JTEXTFIELD_NAME, EXTRACTOR_PREFIX, fieldName);
  }

  private String buildName(String prefix, String type, String correlationPrefixName,
      String fieldName) {
    return rulePanel.getName() + "-" + prefix + "-" + type + "-" + correlationPrefixName + "."
        + fieldName;
  }

  private String buildExtractorComboName(String fieldName) {
    return buildName(EXTRACTOR_COMPONENTS_NAME, JCOMBOBOX_NAME, EXTRACTOR_PREFIX, fieldName);
  }

  @Test
  public void shouldBuildComponentsWhenReplacementSelected() {
    selectReplacementCombo(CORRELATION_REGEX_COMBO_NAME);

    softly.assertThat(frame.textBox(buildReplacementFieldName(EXTRACTOR_REGEX_NAME))).isNotNull();
    softly.assertThat(frame.textBox(buildReplacementFieldName(MATCH_NUMBER_NAME))).isNotNull();
    softly.assertThat(frame.textBox(buildReplacementFieldName(GROUP_NUMBER_NAME))).isNotNull();
    softly.assertThat(frame.comboBox(buildReplacementComboName(TARGET_FIELD_NAME))).isNotNull();
  }

  private void selectReplacementCombo(String item) {
    selectItemCombo(true, item);
  }

  private String buildReplacementFieldName(String fieldName) {
    return buildName(REPLACEMENT_COMPONENTS_NAME, JTEXTFIELD_NAME, REPLACEMENT_PREFIX, fieldName);
  }

  private String buildReplacementComboName(String fieldName) {
    return buildName(REPLACEMENT_COMPONENTS_NAME, JCOMBOBOX_NAME, REPLACEMENT_PREFIX, fieldName);
  }

  @Test
  public void shouldReBuildComponentsWhenCorrelationChanges() {
    selectReplacementCombo(CORRELATION_REGEX_COMBO_NAME);
    //We use this to avoid keeping the references and been able to compare
    List<Component> oldComponent = new ArrayList<>(rulePanel.getReplacementsComponents());

    selectReplacementCombo(CORRELATION_SIEBELROW_COMBO_NAME);
    List<Component> newComponents = new ArrayList<>(rulePanel.getReplacementsComponents());

    assertNotSame(oldComponent, newComponents);
  }

  @Test
  public void shouldReturnCompleteWhenRuleIsComplete() {
    selectExtractorCombo(CORRELATION_REGEX_COMBO_NAME);
    selectReplacementCombo(CORRELATION_REGEX_COMBO_NAME);
    assert (rulePanel.isComplete());
  }

  @Test
  public void shouldReturnIncompleteWhenRuleIncomplete() {
    assert (!rulePanel.isComplete());
  }

  @Test
  public void shouldSetValuesWhenSetValuesFromRulePart() {
    when(rulesContainer.getSizePanelThatContainsRules()).thenReturn(new Dimension(1000, 110));

    CorrelationRulePartTestElement extractor = prepareExpectedRulePart();
    rulePanel.setValuesFromRulePart(extractor, rulePanel.getExtractorHandler());

    assertEquals(EXPECTED_PARAMS, rulePanel.getExtractorHandler().getListComponents().stream()
        .map(c -> c instanceof JTextField ? ((JTextField) c).getText()
            : ((JComboBox) c).getSelectedItem().toString()).collect(Collectors.toList()));

  }

  private CorrelationRulePartTestElement prepareExpectedRulePart() {
    RegexCorrelationExtractor correlationExtractor = new RegexCorrelationExtractor();
    correlationExtractor.setParams(EXPECTED_PARAMS);

    return correlationExtractor;
  }
}
