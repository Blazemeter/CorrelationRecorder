package com.blazemeter.jmeter.correlation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.FunctionCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate.Builder;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistry;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowParamsCorrelationReplacement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationProxyControlTest {

  private static final String FIRST_RULE_REF_VAR_NAME = "firstRule";
  private static final String SECOND_RULE_REF_VAR_NAME = "secondRule";
  private static final String DEFAULT_RESPONSE_FILTER = "";
  private static final String TEMPLATE_ID = "repository1";
  private static final String TEMPLATE_VERSION = "2.3";
  private static final String LOCAL_REPOSITORY_ID = "local";
  private static final Builder BASE_TEMPLATE_BUILDER = new Builder()
      .withRepositoryId(LOCAL_REPOSITORY_ID)
      .withId("TestTemplateID")
      .withDescription("TestTemplateDescription");
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private CorrelationProxyControl correlationProxyControl;
  @Mock
  private CorrelationEngine correlationEngine;
  @Mock
  private CorrelationTemplatesRegistry correlationComponentsRegistry;
  @Mock
  private CorrelationTemplatesRepositoriesConfiguration configuration;
  @Mock
  private CorrelationTemplate testTemplate;
  private TestElement[] testElements = new TestElement[0];
  private SampleResult sampleResult = new SampleResult();
  private HTTPSampler sampler = new HTTPSampler();

  @Before
  public void setup() {
    correlationProxyControl = new CorrelationProxyControl(correlationComponentsRegistry,
        configuration);
  }

  @Test
  public void shouldNotInvokeCorrelationEngineProcessWhenSamplerIsNull() {
    GuiPackage.initInstance(null, Mockito.mock(JMeterTreeModel.class));
    correlationProxyControl.deliverSampler(null, testElements, sampleResult);
    verify(correlationEngine, never()).process(any(), any(), any(), any());
  }

  @Test
  public void shouldInvokeCorrelationEngineProcessWhenSamplerIsNotNull() {
    correlationProxyControl.setCorrelationEngine(correlationEngine);

    GuiPackage.initInstance(null, Mockito.mock(JMeterTreeModel.class));
    correlationProxyControl.deliverSampler(sampler, testElements, sampleResult);
    List<TestElement> children = new ArrayList<>();
    verify(correlationEngine, times(1))
        .process(sampler, children, sampleResult, DEFAULT_RESPONSE_FILTER);
  }

  @Test
  public void shouldBuildCorrelationRulesWhenOnSaveTemplateWithRulesWithoutCorrelationExtractor()
      throws IOException, ConfigurationException {
    List<CorrelationRule> rules = prepareRulesWithoutCorrelationExtractors();

    correlationProxyControl.setCorrelationRules(rules);
    correlationProxyControl.onSaveTemplate(BASE_TEMPLATE_BUILDER);

    //Its been called once since only save the CorrelationTemplate once after building it
    verify(correlationComponentsRegistry, only()).save(prepareExpectedTemplate(rules));
  }

  private List<CorrelationRule> prepareRulesWithoutCorrelationExtractors() {
    RegexCorrelationReplacement regexReplacement = new RegexCorrelationReplacement();
    return Arrays.asList(new CorrelationRule(FIRST_RULE_REF_VAR_NAME, null, regexReplacement),
        new CorrelationRule(SECOND_RULE_REF_VAR_NAME, null, regexReplacement));
  }

  private CorrelationTemplate prepareExpectedTemplate(List<CorrelationRule> rules) {
    return BASE_TEMPLATE_BUILDER
        .withRules(rules)
        .build();
  }

  @Test
  public void shouldBuildCorrelationRulesWhenOnSaveTemplateWithRulesWithoutCorrelationReplacement()
      throws IOException, ConfigurationException {
    List<CorrelationRule> rules = prepareRulesWithoutCorrelationReplacements();
    correlationProxyControl.setCorrelationRules(rules);
    correlationProxyControl.onSaveTemplate(BASE_TEMPLATE_BUILDER);

    //Its been called once since only save the CorrelationTemplate once after building it
    verify(correlationComponentsRegistry, only()).save(prepareExpectedTemplate(rules));
  }

  private List<CorrelationRule> prepareRulesWithoutCorrelationReplacements() {
    RegexCorrelationExtractor regexExtractor = new RegexCorrelationExtractor();
    return Arrays.asList(new CorrelationRule(FIRST_RULE_REF_VAR_NAME, regexExtractor, null),
        new CorrelationRule(SECOND_RULE_REF_VAR_NAME, regexExtractor, null));
  }

  @Test
  public void shouldAppendLoadedTemplateWhenOnLoadTemplate() throws IOException {
    setInitialValues();
    when(correlationComponentsRegistry.findByID(LOCAL_REPOSITORY_ID, TEMPLATE_ID, TEMPLATE_VERSION))
        .thenReturn(Optional.of(testTemplate));

    when(testTemplate.getResponseFilters()).thenReturn(buildArrayFromListOfStrings("Filter 1", "Filter 2"));

    String expectedComponents = buildArrayFromListOfStrings(
        RegexCorrelationExtractor.class.getName(),
        FunctionCorrelationReplacement.class.getName());

    when(testTemplate.getComponents()).thenReturn(expectedComponents);

    List<CorrelationRule> expectedCorrelationRules = prepareRulesWithoutCorrelationReplacements();
    when(testTemplate.getRules()).thenReturn(expectedCorrelationRules);

    correlationProxyControl.onLoadTemplate(LOCAL_REPOSITORY_ID, TEMPLATE_ID, TEMPLATE_VERSION);

    softly.assertThat(correlationProxyControl.getResponseFilter()).isEqualTo("Filter 0, Filter 1, Filter 2");
    softly.assertThat(correlationProxyControl.getCorrelationComponents())
        .isEqualTo(expectedComponents);
    softly.assertThat(correlationProxyControl.getRules()).isEqualTo(expectedCorrelationRules);
  }

  private void setInitialValues() {
    correlationProxyControl.setResponseFilter("Filter 0");
    correlationProxyControl.setCorrelationComponents(SiebelRowParamsCorrelationReplacement.class.getName());
    correlationProxyControl.setCorrelationRules(
        Collections.singletonList(new CorrelationRule("RefVar0", "=(1)", "=(2)")));
  }

  private String buildArrayFromListOfStrings(String... strings) {
    return Strings.join(Arrays.asList(strings), ',');
  }

  @Test
  public void shouldRemoveRepeatedElementsWhenWhenOnLoadTemplate() throws IOException {
    when(correlationComponentsRegistry.findByID(LOCAL_REPOSITORY_ID, TEMPLATE_ID, TEMPLATE_VERSION))
        .thenReturn(Optional.of(testTemplate));

    when(testTemplate.getResponseFilters()).thenReturn(
        buildArrayFromListOfStrings("Filter 1", "Filter 2", "Filter 2", "Filter 2", "Filter 2"));
    when(testTemplate.getComponents()).thenReturn(
        buildArrayFromListOfStrings(RegexCorrelationExtractor.class.getName(),
            RegexCorrelationExtractor.class.getName(), RegexCorrelationExtractor.class.getName(),
            FunctionCorrelationReplacement.class.getName()));
    when(testTemplate.getRules()).thenReturn(prepareRepeatedRulesWithoutCorrelationReplacements());

    correlationProxyControl.onLoadTemplate(LOCAL_REPOSITORY_ID, TEMPLATE_ID, TEMPLATE_VERSION);

    String expectedFilters = buildArrayFromListOfStrings("Filter 1", "Filter 2");
    String expectedComponents = buildArrayFromListOfStrings(
        RegexCorrelationExtractor.class.getName(), FunctionCorrelationReplacement.class.getName());
    List<CorrelationRule> expectedCorrelationRules = prepareRulesWithoutCorrelationReplacements();

    softly.assertThat(correlationProxyControl.getResponseFilter()).isEqualTo(expectedFilters);
    softly.assertThat(correlationProxyControl.getCorrelationComponents())
        .isEqualTo(expectedComponents);
    softly.assertThat(correlationProxyControl.getRules()).isEqualTo(expectedCorrelationRules);
  }

  private List<CorrelationRule> prepareRepeatedRulesWithoutCorrelationReplacements() {
    RegexCorrelationExtractor regexExtractor = new RegexCorrelationExtractor();
    CorrelationRule oneRule = new CorrelationRule(FIRST_RULE_REF_VAR_NAME, regexExtractor, null);
    return Arrays.asList(oneRule, oneRule, oneRule,
        new CorrelationRule(SECOND_RULE_REF_VAR_NAME, regexExtractor, null));
  }
}
