package com.blazemeter.jmeter.correlation.core.analysis;

import static com.blazemeter.jmeter.correlation.TestUtils.findTestFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.io.TextFile;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnalysisIT extends AnalysisTest {

  @Mock
  private CorrelationComponentsRegistry registry;
  private CorrelationEngine engine;
  private final List<TestElement> childrenElements = new ArrayList<>();
  private CorrelationRule rule;
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();

  @Before
  public void setup() {
    JMeterTestUtils.setupJmeterEnv();
    engine = new CorrelationEngine();
    engine.setEnabled(true);
    when(registry.getContext(BaseCorrelationContext.class))
        .thenReturn(new BaseCorrelationContext());
    rule = new CorrelationRule(VALUE_NAME, extractor, replacement);

    List<RulesGroup> groups = Collections.singletonList(new RulesGroup.Builder()
        .withId("simpleGroup")
        .withRules(Collections.singletonList(rule))
        .build());

    engine.setCorrelationRules(groups, registry);
    AnalysisReporter.startCollecting();
  }

  @After
  public void tearDown() {
    stopAnalysis();
  }

  @Test
  public void shouldAddExtractionReportWhenRegexExtractorIsApplied() throws IOException {
    engineProcess(createLoggedUserRequest(), createLoggedUserResponse());
    CorrelationRuleReport reportForRule = getRuleReport();
    softly.assertThat(reportForRule).isNotNull();
    softly.assertThat(reportForRule.getExtractorReport()).isNotNull();
  }

  private CorrelationRuleReport getRuleReport() {
    return AnalysisReporter.getReporter().getRuleReport(rule);
  }

  private void engineProcess(HTTPSamplerBase request, SampleResult response) {
    engine.process(request, childrenElements, response, "");
  }

  private SampleResult createLoggedUserResponse() throws IOException {
    return buildSampleResult("loggedUserResponse");
  }

  private SampleResult buildSampleResult(String responseFilename) throws IOException {
    HTTPSampleResult result = new HTTPSampleResult();
    result.setResponseData(getResponseString(responseFilename), null);
    result.setSampleLabel("1");
    result.setSamplerData("2");
    result.setURL(new URL("http://bz.apache.org/fakepage.html"));
    return result;
  }

  private String getResponseString(String responseFilename) {
    if (responseFilename.isEmpty()) {
      return "";
    }
    return new TextFile(findTestFile(responseFilename + ".html")).getText();
  }



  @Test
  public void shouldAddReplacementReportWhenRegexReplacementIsApplied() throws IOException {
    engineProcess(createLoggedUserRequest(), createLoggedUserResponse());
    engineProcess(createCallToActionRequest(), createCallToActionResponse());

    CorrelationRuleReport ruleReport = getRuleReport();
    softly.assertThat(ruleReport).isNotNull();
    softly.assertThat(ruleReport.getReplacementReport()).isNotNull();
  }

  private HTTPSamplerBase createCallToActionRequest() throws IOException {
    return createSampler("callToActionRequest.jmx");
  }

  private SampleResult createCallToActionResponse() throws IOException {
    return buildSampleResult("");
  }

  @Test
  public void shouldAllowExtractorApplianceWhenProcessWithAnalysisModeOff() throws IOException {
    stopAnalysis();
    engineProcess(createLoggedUserRequest(), createLoggedUserResponse());
    assertThat(childrenElements).hasSize(1);
  }

  private static void stopAnalysis() {
    AnalysisReporter.stopCollecting();
  }

  @Test
  public void shouldAllowReplacementApplianceWhenProcessWithAnalysisModeOff() throws IOException {
    stopAnalysis();
    engineProcess(createLoggedUserRequest(), createLoggedUserResponse());
    HTTPSamplerBase callToActionRequest = createCallToActionRequest();
    callToActionRequest.setRunningVersion(true);
    String prev = callToActionRequest.toString();
    engineProcess(callToActionRequest, createCallToActionResponse());
    callToActionRequest.setRunningVersion(true);
    String expectedReplace = prev.replace(TOKEN_VALUE, "${" + VALUE_NAME + "}");
    assertThat(callToActionRequest.toString()).isEqualTo(expectedReplace);
  }

  @Test
  public void shouldReturnRulePartReportWhenGetRuleReport() throws IOException {
    engineProcess(createLoggedUserRequest(), createLoggedUserResponse());
    CorrelationRuleReport ruleReport = AnalysisReporter.getReporter()
        .getRuleReport(getTokenRule());
    assertThat(ruleReport.didApply()).isTrue();
  }

  private CorrelationRule getTokenRule() {
    //noinspection OptionalGetWithoutIsPresent
    return getRuleByVariableName(VALUE_NAME).get();
  }

  private Optional<CorrelationRule> getRuleByVariableName(String varName) {
    return engine.getCorrelationRules().stream()
        .filter(rule -> rule.getReferenceName().equals(varName))
        .findFirst();
  }

  @Test
  public void shouldReportAffectedRequestWhenProcessWithAppliedExtractor() throws IOException {
    engineProcess(createLoggedUserRequest(), createLoggedUserResponse());
    CorrelationRuleReport ruleReport = AnalysisReporter.getReporter()
        .getRuleReport(getTokenRule());

    softly.assertThat(ruleReport.getExtractorReport())
        .isNotNull();
  }
}
