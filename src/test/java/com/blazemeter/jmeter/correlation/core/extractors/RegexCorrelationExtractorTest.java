package com.blazemeter.jmeter.correlation.core.extractors;

import static com.blazemeter.jmeter.correlation.TestUtils.getFileContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.method.ComparableJMeterVariables;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.assertions.JSR223Assertion;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.bridge.SLF4JBridgeHandler;

@RunWith(MockitoJUnitRunner.class)
public class RegexCorrelationExtractorTest {

  private static final String REFERENCE_NAME = "Reference Name";
  private static final String RESPONSE_BODY_REGEX = "Test_SWEACn=(.*?)&";
  private static final List<String> PARAMS = Arrays
      .asList(RESPONSE_BODY_REGEX, "1", "1", ResultField.BODY.name()
          , "false");
  private static final String URL_RESPONSE_REGEX = "jmeter\\.(.*?)\\.org";
  private static final String TEST_URL = "https://jmeter.apache.org/";
  private static final String SUCCESS_RESPONSE_MESSAGE = HttpStatus
      .getStatusText(HttpStatus.SC_OK);
  @Mock
  private BaseCorrelationContext baseCorrelationContext;

  // we need this to avoid nasty logs about pdfbox
  @BeforeClass
  public static void setupClass() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  protected static SampleResult createSampleResultWithResponseBody(String responseBody)
      throws MalformedURLException {
    SampleResult sampleResult = new SampleResult();
    URL testUrl = new URL(TEST_URL);
    sampleResult.setURL(testUrl);
    sampleResult.setSamplerData("Test_SWEACn=123&Test_Path=1");
    sampleResult.setResponseCode(String.valueOf(HttpStatus.SC_OK));
    sampleResult.setResponseMessage(SUCCESS_RESPONSE_MESSAGE);
    sampleResult.setResponseHeaders(TEST_URL);
    sampleResult.setRequestHeaders(TEST_URL);
    sampleResult.setResponseData(responseBody, SampleResult.DEFAULT_HTTP_ENCODING);
    sampleResult.setContentType(ContentType.TEXT_HTML.toString());
    return sampleResult;
  }

  @Test
  public void shouldNotAddChildRegexExtractorWhenDoesNotMatch()
      throws MalformedURLException {
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>();
    regexExtractor.setParams(PARAMS);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children, createSampleResultWithResponseBody("Other Body"),
        new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(Collections.emptyList());
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesAgainstSampleResultBody()
      throws MalformedURLException {
    assertAddChildRegex(RESPONSE_BODY_REGEX, ResultField.BODY);
  }

  private void assertAddChildRegex(String responseRegex, ResultField fieldToCheck)
      throws MalformedURLException {
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>(responseRegex, 1,
        fieldToCheck);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children, createMatchSampleResult(), new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(createRegexExtractor(responseRegex, fieldToCheck))));
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesOnceAndMatchNumberIsLessThanZero()
      throws MalformedURLException {
    List<TestElement> children = addChildRegex(RESPONSE_BODY_REGEX, ResultField.BODY, false);
    RegexExtractor regexExtractor = createRegexExtractor(RESPONSE_BODY_REGEX, ResultField.BODY);
    regexExtractor.setMatchNumber(1);
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(regexExtractor)));
  }

  private List<TestElement> addChildRegex(String responseRegex, ResultField fieldToCheck,
                                          boolean isMultipleMatch)
      throws MalformedURLException {
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>(responseRegex, -1,
        fieldToCheck);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children, isMultipleMatch ? createMultipleMatchSampleResult() :
        createMatchSampleResult(), new JMeterVariables());
    return children;
  }

  private SampleResult createMultipleMatchSampleResult() throws MalformedURLException {
    return RegexCorrelationExtractorTest
        .createSampleResultWithResponseBody(
            "Test_SWEACn=123&Test_Path=1&Test_SWEACn=456&Test Body \t");
  }


  private SampleResult createMatchSampleResult() throws MalformedURLException {
    return createSampleResultWithResponseBody("Test_SWEACn=123&Test_Path=1&Test Body \t");
  }

  private RegexExtractor createRegexExtractor(String responseRegex, ResultField fieldToCheck) {
    RegexExtractor regex = new RegexExtractor();
    regex.setProperty(TestElement.GUI_CLASS, RegexExtractorGui.class.getName());
    regex.setName("RegExp - " + REFERENCE_NAME);
    regex.setRefName(REFERENCE_NAME);
    regex.setTemplate("$1$");
    regex.setMatchNumber(1);
    regex.setDefaultValue(REFERENCE_NAME + "_NOT_FOUND");
    regex.setRegex(responseRegex);
    regex.setUseField(fieldToCheck.getCode());
    regex.setScopeAll();
    return regex;
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesTwiceAndMatchNumberIsLowerThanZero()
      throws MalformedURLException {
    List<TestElement> children = addChildRegex(RESPONSE_BODY_REGEX, ResultField.BODY, true);
    RegexExtractor regexExtractor = createRegexExtractor(RESPONSE_BODY_REGEX, ResultField.BODY);
    regexExtractor.setMatchNumber(-1);
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(regexExtractor)));
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesAgainstSampleResultBodyUnescaped()
      throws MalformedURLException {
    String responseRegex = "(Test Body \t)";
    ResultField fieldToCheck = ResultField.BODY_UNESCAPED;
    assertAddChildRegex(responseRegex, fieldToCheck);
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesAgainstSampleResultRequestHeaders()
      throws MalformedURLException {
    assertAddChildRegex(URL_RESPONSE_REGEX, ResultField.REQUEST_HEADERS);
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesAgainstSampleResultResponseHeaders()
      throws MalformedURLException {
    assertAddChildRegex(URL_RESPONSE_REGEX, ResultField.RESPONSE_HEADERS);
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesAgainstSampleResultResponseMessage()
      throws MalformedURLException {
    assertAddChildRegex("(OK)", ResultField.RESPONSE_MESSAGE);
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesAgainstSampleResultResponseCode()
      throws MalformedURLException {
    assertAddChildRegex("(\\d+)", ResultField.RESPONSE_CODE);
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesAgainstSampleResultURL()
      throws MalformedURLException {
    assertAddChildRegex(URL_RESPONSE_REGEX, ResultField.URL);
  }

  @Test
  public void shouldSetMatchNumberToDefaultValueWhenItIsEmpty() throws MalformedURLException {
    ResultField fieldToCheck = ResultField.BODY;
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>();
    regexExtractor.setParams(PARAMS);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children, createMatchSampleResult(), new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children))
        .isEqualTo(TestUtils.comparableFrom(Collections.singletonList(createRegexExtractor(
            RESPONSE_BODY_REGEX, fieldToCheck))));
  }

  @Test
  public void shouldSetMatchNumberToDefaultValueWhenItIsNotAValidNumber()
      throws MalformedURLException {
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>(
        RESPONSE_BODY_REGEX,
        "invalid_number", "invalid_number", ResultField.BODY.name(), "false");
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children, createMatchSampleResult(), new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children))
        .isEqualTo(TestUtils.comparableFrom(Collections.singletonList(createRegexExtractor(
            RESPONSE_BODY_REGEX, ResultField.BODY))));
  }

  @Test
  public void shouldNotAddRegexExtractorWhenMatchedValueIsAlreadyExtracted()
      throws MalformedURLException {
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>();
    regexExtractor.setParams(PARAMS);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    SampleResult firstSampleResults = createMatchSampleResult();
    JMeterVariables vars = new JMeterVariables();
    regexExtractor.process(null, children, firstSampleResults, vars);
    regexExtractor.process(null, children, firstSampleResults, vars);
    assertThat(TestUtils.comparableFrom(children))
        .isEqualTo(TestUtils.comparableFrom(Collections.singletonList(createRegexExtractor(
            RESPONSE_BODY_REGEX, ResultField.BODY))));
  }

  @Test
  public void shouldFailSampleWhenProcessWithFailingExtractingVariable() throws IOException {
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>(
        RESPONSE_BODY_REGEX);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();

    JSR223Assertion assertion = buildExtractionAssertion();
    assertion.setScript(StringEscapeUtils
        .unescapeXml(getFileContent("/templates/components/ExtractingVariableAssertion.xml",
            regexExtractor.getClass())));
    children.add(assertion);

    JMeterVariables vars = new JMeterVariables();
    SampleResult firstSampleResults = createMatchSampleResult();
    regexExtractor.process(null, children, firstSampleResults, vars);

    SampleResult sampleResult = createSampleResultWithResponseBody("Other Body");
    sampleResult.setSuccessful(true);
    AssertionResult result = assertion.getResult(sampleResult);
    assertThat(result.isFailure());
  }

  private JSR223Assertion buildExtractionAssertion() {
    JSR223Assertion assertion = new JSR223Assertion();
    assertion.setProperty(JSR223Assertion.GUI_CLASS, TestBeanGUI.class.getName());
    assertion.setName("Extraction assertion");
    assertion.setProperty("cacheKey", UUID.randomUUID().toString());
    assertion.setProperty("language", "groovy");

    return assertion;
  }

  @Test
  public void shouldSampleSucceedWhenVariableIsExtractedWithAssertion() throws IOException {
    RegexCorrelationExtractor<?> regexExtractor = new RegexCorrelationExtractor<>(
        RESPONSE_BODY_REGEX);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();

    JSR223Assertion assertion = buildExtractionAssertion();
    assertion.setScript(StringEscapeUtils
        .unescapeXml(getFileContent("/templates/components/ExtractingVariableAssertion.xml",
            regexExtractor.getClass())));
    children.add(assertion);

    JMeterVariables vars = new JMeterVariables();
    SampleResult firstSampleResults = createMatchSampleResult();
    regexExtractor.process(null, children, firstSampleResults, vars);

    SampleResult sampleResult = createSampleResultWithResponseBody("Test_SWEACn=TestBodyInfo&");
    sampleResult.setSuccessful(true);
    AssertionResult result = assertion.getResult(sampleResult);
    assertThat(!result.isFailure());
  }

  @Test
  public void shouldNotAddChildRegexExtractorWhenDoesNotMatchAndMultiValued()
      throws MalformedURLException {
    setupContextForMultiValue();
    RegexCorrelationExtractor<BaseCorrelationContext> regexExtractor =
        new RegexCorrelationExtractor<>(RESPONSE_BODY_REGEX, "-1", "1",
            ResultField.BODY.name(), "true");
    regexExtractor.setVariableName(REFERENCE_NAME);
    regexExtractor.setContext(baseCorrelationContext);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children,
        RegexCorrelationExtractorTest.createSampleResultWithResponseBody("Other Body"),
        new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(Collections.emptyList());
  }

  private void setupContextForMultiValue() {
    when(baseCorrelationContext.getNextVariableNr(REFERENCE_NAME)).thenReturn(1).thenReturn(2);
  }

  @Test
  public void shouldAddChildRegexExtractorWithOneInMatchNrWhenMatchesAgainstSampleResultOnlyOnceBody()
      throws MalformedURLException {
    setupContextForMultiValue();
    List<TestElement> children = new ArrayList<>();
    addChildRegex(RESPONSE_BODY_REGEX, createMatchSampleResult(), children, new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(createRegexExtractor(RESPONSE_BODY_REGEX, 1, 1))));
  }

  @Test
  public void shouldAddMatchNrVarWhenMatchesAgainstSampleResultBodyMultipleTimes()
      throws MalformedURLException {
    setupContextForMultiValue();
    List<TestElement> children = new ArrayList<>();
    JMeterVariables vars = new JMeterVariables();
    addChildRegex(RESPONSE_BODY_REGEX, createMultipleMatchSampleResult(), children, vars);
    assertThat(vars.get(REFERENCE_NAME + "#1_matchNr")).isNotNull();
  }

  @Test
  public void shouldAddChildWithDifferentIndexWhenMatchesAgainstMultipleSampleResultBody()
      throws MalformedURLException {
    setupContextForMultiValue();
    List<TestElement> children = new ArrayList<>();
    JMeterVariables vars = new JMeterVariables();
    addChildRegex(RESPONSE_BODY_REGEX, createMultipleMatchSampleResult(), children, vars);
    addChildRegex(RESPONSE_BODY_REGEX, createMultipleMatchSampleResult(), children, vars);
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Arrays.asList(createRegexExtractor(RESPONSE_BODY_REGEX, 1, -1),
                createRegexExtractor(RESPONSE_BODY_REGEX, 2, -1))));
    assertThat(vars.get(REFERENCE_NAME + "#1_matchNr")).isNotNull();
  }

  private void addChildRegex(String responseRegex, SampleResult sampleResult,
                             List<TestElement> children, JMeterVariables vars) {
    RegexCorrelationExtractor<BaseCorrelationContext> regexExtractor =
        new RegexCorrelationExtractor<>(RESPONSE_BODY_REGEX, "-1", "1", ResultField.BODY.name(),
            "true");

    regexExtractor.setContext(baseCorrelationContext);
    regexExtractor.setVariableName(REFERENCE_NAME);
    regexExtractor.process(null, children, sampleResult, vars);
  }


  private RegexExtractor createRegexExtractor(String responseRegex, int varNr, int matchNr) {
    RegexExtractor regex = new RegexExtractor();
    regex.setProperty(TestElement.GUI_CLASS, RegexExtractorGui.class.getName());
    regex.setName("RegExp - " + REFERENCE_NAME + "#" + varNr);
    regex.setRefName(REFERENCE_NAME + "#" + varNr);
    regex.setTemplate("$1$");
    regex.setMatchNumber(matchNr);
    regex.setDefaultValue(REFERENCE_NAME + "#" + varNr + "_NOT_FOUND");
    regex.setRegex(responseRegex);
    regex.setUseField(ResultField.BODY.getCode());
    regex.setScopeAll();
    return regex;
  }

  @Test
  public void shouldRemoveLeftOverVariablesWhenMultipleMatchesAndMatchNrLowerThanZero()
      throws Exception {
    ComparableJMeterVariables vars = new ComparableJMeterVariables();
    vars.put(REFERENCE_NAME + "_1", "value1");
    vars.put(REFERENCE_NAME + "_2", "value2");
    vars.put(REFERENCE_NAME + "_3", "value3");
    vars.put(REFERENCE_NAME + "_matchNr", "3");
    RegexCorrelationExtractor<BaseCorrelationContext> regexExtractor =
        new RegexCorrelationExtractor<>(RESPONSE_BODY_REGEX, "-1", "1", ResultField.BODY.name(),
            "false");
    regexExtractor.setContext(baseCorrelationContext);
    regexExtractor.setVariableName(REFERENCE_NAME);
    regexExtractor.process(null, new ArrayList<>(), createMultipleMatchSampleResult(),
        vars);
    assertThat(vars).isEqualTo(buildExpectedVariable());
  }

  private JMeterVariables buildExpectedVariable() {
    ComparableJMeterVariables vars = new ComparableJMeterVariables();
    vars.put(REFERENCE_NAME + "_1", "123");
    vars.put(REFERENCE_NAME + "_2", "456");
    vars.put(REFERENCE_NAME + "_matchNr", "2");
    return vars;
  }

  @Mock
  private CorrelationComponentsRegistry registry;

  @Test
  public void shouldCatchAllMatchedValues() throws MalformedURLException {
    ComparableJMeterVariables variables = new ComparableJMeterVariables();

    RegexCorrelationExtractor<BaseCorrelationContext> regexExtractor =
        new RegexCorrelationExtractor<>("wpnonce=(.+?)'> ", "-1", "1",
            ResultField.BODY.name(), "true");
    regexExtractor.setVariableName("nonce");
    regexExtractor.setMultiValued(true);
    regexExtractor.setContext(new BaseCorrelationContext());

    String responseWithNonces =
        "<a href='_wpnonce=123'> Login</a> <a href='_wpnonce=345'> Login</a> ";


    regexExtractor.process(null, new ArrayList<>(),
        createSampleResultWithResponseBody(responseWithNonces), variables);
    assertThat(variables.entrySet().size()).isEqualTo(3);
  }

}
