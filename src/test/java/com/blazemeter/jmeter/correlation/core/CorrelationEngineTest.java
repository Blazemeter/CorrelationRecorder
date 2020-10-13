package com.blazemeter.jmeter.correlation.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.siebel.SiebelContext;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowIdCorrelationReplacement;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationEngineTest {

  private static final String REGEX = "Test_SWEACn=(.*?)&";

  @Mock
  private CorrelationComponentsRegistry registry;
  private CorrelationEngine engine;

  private static SampleResult buildSampleResult() throws IOException {
    SampleResult result = new SampleResult();
    URL testUrl = new URL("https://test.com/");
    result.setURL(testUrl);
    result.setSamplerData("");
    result.setResponseCode(HttpStatus.getStatusText(HttpStatus.SC_OK));
    result.setResponseMessage(HttpStatus.getStatusText(HttpStatus.SC_OK));
    result.setResponseHeaders(testUrl.toString());
    result.setRequestHeaders(testUrl.toString());
    result.setResponseData(
        TestUtils.getFileContent("/validResponseData.txt", CorrelationEngineTest.class));
    result.setContentType(ContentType.TEXT_HTML.toString());
    return result;
  }

  private static HTTPSampler createSampler() {
    HTTPSampler sampler = new HTTPSampler();
    sampler.setMethod("GET");
    sampler.setPath("Test_SWEACn=123&Test_Path=1");
    return sampler;
  }

  @Before
  public void prepare() {
    engine = new CorrelationEngine();
    when(registry.getContext(SiebelContext.class)).thenReturn(new SiebelContext());
    when(registry.getContext(BaseCorrelationContext.class))
        .thenReturn(new BaseCorrelationContext());
  }

  @Test
  public void shouldUpdateContextWhenSetCorrelationRules() {
    engine.setCorrelationRules(
        Arrays.asList(buildRuleWithEnable(true), buildRuleWithSiebelReplacement("variable2")),
        registry);

    assertThat(engine.getCorrelationRules().stream()
        .noneMatch(c -> c.getCorrelationReplacement().getSupportedContext() != null
            && c.getCorrelationReplacement().getContext() == null)).isTrue();
  }

  private CorrelationRule buildRuleWithEnable(boolean enable) {
    CorrelationRule correlationRule = new CorrelationRule("variable",
        new RegexCorrelationExtractor<>(REGEX, "1", "1", ResultField.BODY.name(),
            "false"),
        new RegexCorrelationReplacement<>(REGEX));
    correlationRule.setEnabled(enable);
    return correlationRule;
  }

  private CorrelationRule buildRuleWithSiebelReplacement(String referenceName) {
    return new CorrelationRule(referenceName,
        new RegexCorrelationExtractor<>(REGEX, "1", "1", ResultField.BODY.name(), "false"),
        createSiebelRowIdReplacement());
  }

  private RegexCorrelationExtractor<?> createRegexExtractor(String regex) {
    RegexCorrelationExtractor<?> regexExtractorWithoutRegex = new RegexCorrelationExtractor<>();
    regexExtractorWithoutRegex.setParams(Collections.singletonList(regex));
    return regexExtractorWithoutRegex;
  }

  private SiebelRowIdCorrelationReplacement createSiebelRowIdReplacement() {
    SiebelRowIdCorrelationReplacement siebelRowIdReplacement =
        new SiebelRowIdCorrelationReplacement();
    siebelRowIdReplacement.setParams(Collections.singletonList(""));
    return siebelRowIdReplacement;
  }

  @Test
  public void shouldResetContextWhenReset() throws IOException {
    engine.setCorrelationRules(Collections
            .singletonList(buildRuleWithSiebelReplacement("var")), registry);
    engine.updateContexts(buildSampleResult());
    String updatedContext = getContextsToString();
    engine.reset();

    assertThat(getContextsToString()).isNotEqualTo(updatedContext);
  }

  private String getContextsToString() {
    return engine.getInitializedContexts().stream()
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }

  @Test
  public void shouldApplyExtractorWhenProcess() throws IOException {
    engine.setCorrelationRules(Collections.singletonList(buildRuleWithEnable(true)), registry);
    List<TestElement> children = new ArrayList<>();
    engine.process(createSampler(), children, buildSampleResult(), "");
    assertThat(children).isNotEmpty();
  }

  @Test
  public void shouldNotApplyExtractorWhenProcessWithDisabledRule() throws IOException {
    engine.setCorrelationRules(Collections.singletonList(buildRuleWithEnable(false)), registry);
    List<TestElement> children = new ArrayList<>();
    engine.process(createSampler(), children, buildSampleResult(), "");
    assertThat(children).isEmpty();
  }

  @Test
  public void shouldApplyReplacementWhenProcess() throws IOException{
    engine.setCorrelationRules(Collections.singletonList(buildRuleWithEnable(true)), registry);
    HTTPSampler sampler = createSampler();
    JMeterVariables vars = new JMeterVariables();
    vars.put("variable", "123");
    engine.setVars(vars);
    engine.process(sampler, new ArrayList<>(), buildSampleResult(), "");
    assertThat(sampler.getPath()).isEqualTo("Test_SWEACn=${variable}&Test_Path=1");
  }
  
  @Test
  public void shouldNotApplyReplacementWhenProcessWithDisabledRule() {
    engine.setCorrelationRules(Collections.singletonList(buildRuleWithEnable(false)), registry);
    HTTPSampler sampler = createSampler();
    JMeterVariables vars = new JMeterVariables();
    vars.put("variable", "123");
    engine.setVars(vars);
    engine.process(sampler, new ArrayList<>(), null, "");
    assertThat(sampler.getPath()).isEqualTo("Test_SWEACn=123&Test_Path=1");
  }

  @Test
  public void shouldApplyExtractorWhenProcessWithAllowedContentType() throws IOException {
    engine.setCorrelationRules(Collections.singletonList(buildRuleWithEnable(true)), registry);
    List<TestElement> children = new ArrayList<>();
    engine.process(createSampler(), children, buildSampleResult(), "text/*");
    assertThat(children).isNotEmpty();
  }
  
  @Test
  public void shouldNotApplyExtractorWhenProcessWithNotAllowedContentType() throws IOException {
    engine.setCorrelationRules(Collections.singletonList(buildRuleWithEnable(true)), registry);
    List<TestElement> children = new ArrayList<>();
    engine.process(createSampler(), children, buildSampleResult(), "*/xml");
    assertThat(children).isEmpty();
  }
}
