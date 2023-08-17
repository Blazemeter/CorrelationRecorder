package com.blazemeter.jmeter.correlation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.proxy.ComparableCookie;
import com.blazemeter.jmeter.correlation.core.proxy.PendingProxy;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistry;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.Template.Builder;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowParamsCorrelationReplacement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
  private final TestElement[] testElements = new TestElement[0];
  private final HTTPSampler sampler = buildSampler();
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private CorrelationProxyControlBuilder builder;
  private SampleResult sampleResult = new SampleResult();

  @Mock
  private CorrelationEngine correlationEngine;
  @Mock
  private CorrelationTemplatesRegistry correlationComponentsRegistry;
  @Mock
  private CorrelationTemplatesRepositoriesConfiguration configuration;
  @Mock
  private LocalConfiguration localConfiguration;
  @Mock
  private Template testTemplate;
  @Mock
  private JMeterTreeNode target;

  private CorrelationProxyControl model;

  private HTTPSampler buildSampler() {
    HTTPSampler ret = new HTTPSampler();
    // we need to set domain to avoid filters to filter it out
    ret.setDomain("localhost");
    ret.setHeaderManager(new HeaderManager());

    return ret;
  }

  @BeforeClass
  public static void setupClass() {
    JMeterTestUtils.setupJmeterEnv();
  }

  @Before
  public void setup() throws IOException {

    builder = new CorrelationProxyControlBuilder()
        .withCorrelationTemplatesRegistry(correlationComponentsRegistry)
        .withCorrelationTemplatesRepositoriesConfiguration(configuration)
        .withLocalConfiguration(localConfiguration)
        .withTarget(target);
    GuiPackage.initInstance(null, Mockito.mock(JMeterTreeModel.class));
    Mockito.when(target.children()).thenReturn(Mockito.mock(Enumeration.class));
  }

  @After
  public void tearDown() {
    if (model != null) {
      model.stopProxy();
    }
  }

  @Test
  public void shouldNotInvokeCorrelationEngineProcessWhenSamplerIsNull() {
    CorrelationProxyControl build = builder.build();
    build.startedProxy(Thread.currentThread());
    build.deliverSampler(null, testElements, sampleResult);
    verify(correlationEngine, never()).process(any(), any(), any(), any());
  }

  @Test
  public void shouldInvokeCorrelationEngineProcessWhenSamplerIsNotNull() {
    CorrelationProxyControl proxyControl = builder.withCorrelationEngine(correlationEngine)
        .build();
    sampleResult = new HTTPSampleResult();
    proxyControl.startedProxy(Thread.currentThread());
    proxyControl.deliverSampler(sampler, testElements, sampleResult);
    proxyControl.endedProxy(Thread.currentThread());
    List<TestElement> children = new ArrayList<>();
    verify(correlationEngine, times(1))
        .process(sampler, children, sampleResult, DEFAULT_RESPONSE_FILTER);
  }

  @Test
  public void shouldBuildCorrelationRulesWhenOnSaveTemplateWithRulesWithoutCorrelationExtractor()
      throws IOException, ConfigurationException {
    List<CorrelationRule> rules = prepareRulesWithoutCorrelationExtractors();
    RulesGroup.Builder groupsBuilder = new RulesGroup.Builder();
    groupsBuilder.withRules(rules);
    List<RulesGroup> groups = Collections.singletonList(groupsBuilder.build());
    model = builder
        .withCorrelationEngine(correlationEngine)
        .build();

    model.setCorrelationGroups(groups);
    model.onSaveTemplate(BASE_TEMPLATE_BUILDER);

    //Its been called once since only save the CorrelationTemplate once after building it
    verify(correlationComponentsRegistry, only()).save(prepareExpectedTemplate(groups));
  }

  private List<CorrelationRule> prepareRulesWithoutCorrelationExtractors() {
    RegexCorrelationReplacement<?> regexReplacement = new RegexCorrelationReplacement<>();
    return Arrays.asList(new CorrelationRule(FIRST_RULE_REF_VAR_NAME, null, regexReplacement),
        new CorrelationRule(SECOND_RULE_REF_VAR_NAME, null, regexReplacement));
  }

  private Template prepareExpectedTemplate(List<RulesGroup> groups) {
    return BASE_TEMPLATE_BUILDER
        .withGroups(groups)
        .build();
  }

  @Test
  public void shouldBuildCorrelationRulesWhenOnSaveTemplateWithRulesWithoutCorrelationReplacement()
      throws IOException, ConfigurationException {
    List<RulesGroup> groups = prepareGroupOfRulesWithoutCorrelationReplacements();
    model = builder.build();
    model.setCorrelationGroups(groups);
    model.onSaveTemplate(BASE_TEMPLATE_BUILDER);

    //Its been called once since only save the CorrelationTemplate once after building it
    verify(correlationComponentsRegistry, only()).save(prepareExpectedTemplate(groups));
  }

  private List<RulesGroup> prepareGroupOfRulesWithoutCorrelationReplacements() {
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>();
    List<CorrelationRule> rules = Arrays
        .asList(new CorrelationRule(FIRST_RULE_REF_VAR_NAME, regexExtractor, null),
            new CorrelationRule(SECOND_RULE_REF_VAR_NAME, regexExtractor, null));
    RulesGroup.Builder builder = new RulesGroup.Builder();
    builder.withRules(rules);
    return Collections.singletonList(builder.build());
  }

  @Test
  public void shouldAppendLoadedTemplateWhenOnLoadTemplate() throws IOException {
    when(correlationComponentsRegistry.findByID(LOCAL_REPOSITORY_ID, TEMPLATE_ID, TEMPLATE_VERSION))
        .thenReturn(Optional.of(testTemplate));

    when(testTemplate.getResponseFilters())
        .thenReturn(buildArrayFromListOfStrings("Filter 1", "Filter 2"));

    String expectedComponents = buildArrayFromListOfStrings(
        RegexCorrelationExtractor.class.getName());

    when(testTemplate.getComponents()).thenReturn(expectedComponents);

    List<RulesGroup> expectedCorrelationGroup = prepareGroupOfRulesWithoutCorrelationReplacements();
    when(testTemplate.getGroups()).thenReturn(expectedCorrelationGroup);

    model = builder.build();
    setInitialValues(model);
    model.onLoadTemplate(LOCAL_REPOSITORY_ID, TEMPLATE_ID, TEMPLATE_VERSION);

    assertModelInfo(expectedComponents, expectedCorrelationGroup, model,
        "Filter 0, Filter 1, Filter 2");
  }

  private void assertModelInfo(String expectedComponents, List<RulesGroup> expectedGroups,
                               CorrelationProxyControl model,
                               String expectedFilters) {
    softly.assertThat(model.getResponseFilter()).isEqualTo(expectedFilters);
    softly.assertThat(model.getCorrelationComponents()).isEqualTo(expectedComponents);
    softly.assertThat(model.getGroups()).isEqualTo(expectedGroups);
  }

  private void setInitialValues(CorrelationProxyControl model) {
    model.setResponseFilter("Filter 0");
    model.setCorrelationComponents(SiebelRowParamsCorrelationReplacement.class.getName());

    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>();
    regexExtractor.setParams(Collections.singletonList("=(1)"));

    RegexCorrelationReplacement<?> regexReplacement = new RegexCorrelationReplacement<>();
    regexReplacement.setParams(Collections.singletonList("=(2)"));

    List<CorrelationRule> rules = Collections
        .singletonList(new CorrelationRule("RefVar0", regexExtractor, regexReplacement));
    RulesGroup.Builder builder = new RulesGroup.Builder();
    builder.withId("Rules");
    builder.withRules(rules);
    model.setCorrelationGroups(Collections.singletonList(builder.build()));
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
            RegexCorrelationExtractor.class.getName(), RegexCorrelationExtractor.class.getName()));
    when(testTemplate.getGroups())
        .thenReturn(prepareRepeatedGroupOfRulesWithoutCorrelationReplacements());

    CorrelationProxyControl model = builder.build();
    model.onLoadTemplate(LOCAL_REPOSITORY_ID, TEMPLATE_ID, TEMPLATE_VERSION);

    String expectedFilters = buildArrayFromListOfStrings("Filter 1", "Filter 2");
    String expectedComponents = buildArrayFromListOfStrings(
        RegexCorrelationExtractor.class.getName());
    List<RulesGroup> expectedCorrelationRules =
        prepareRepeatedGroupOfRulesWithoutCorrelationReplacements();

    assertModelInfo(expectedComponents, expectedCorrelationRules, model, expectedFilters);
  }

  private List<RulesGroup> prepareRepeatedGroupOfRulesWithoutCorrelationReplacements() {
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>();
    CorrelationRule oneRule = new CorrelationRule(FIRST_RULE_REF_VAR_NAME, regexExtractor, null);
    List<CorrelationRule> rules = Arrays.asList(oneRule, oneRule, oneRule,
        new CorrelationRule(SECOND_RULE_REF_VAR_NAME, regexExtractor, null));
    return Collections.singletonList(new RulesGroup.Builder().withRules(rules).build());
  }

  @Test
  public void shouldClearCustomCookiesWhenStartProxy() {
    model = builder.build();
    ComparableCookie comparableCookie = new ComparableCookie("header", "headerValue", "localhost");
    model.addCookie(comparableCookie);
    model.setPort(8898);
    try {
      model.startProxy();
    } catch (IOException e) {
      //Is expected to throw an exception since 'keytool' command isn't allowed in this environment
    }
    softly.assertThat(model.getLastCookies()).isEmpty();
  }

  @Test
  public void shouldAddNewPendingProxyWhenStartedProxy() {
    model = builder.withLocalConfiguration(localConfiguration).build();
    Thread proxy = Thread.currentThread();
    model.startedProxy(proxy);
    LinkedHashMap<Object, PendingProxy> actualPending = model.getPendingProxies();
    softly.assertThat(actualPending).isNotEmpty();
    softly.assertThat(actualPending).isEqualTo(proxy);
  }
}
