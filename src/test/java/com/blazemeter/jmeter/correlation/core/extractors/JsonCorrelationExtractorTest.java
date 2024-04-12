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
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
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
public class JsonCorrelationExtractorTest {


  private static final String REFERENCE_NAME = "TEST_SWEACN";
  private static final String RESPONSE_BODY_JSONPATH = "$.arg1";
  private static final List<String> PARAMS = Arrays
      .asList(RESPONSE_BODY_JSONPATH, "1", ResultField.BODY.name()
          , "false"); // JSONPAth, Match 1, BODY, Multivalue False
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
  public void shouldNotAddChildJsonExtractorWhenDoesNotMatch()
      throws MalformedURLException {
    JsonCorrelationExtractor<?> jsonExtractor = new JsonCorrelationExtractor<>();
    jsonExtractor.setParams(PARAMS);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    jsonExtractor.process(null, children,
        createSampleResultWithResponseBody("{\"arg2\":\"123\"}"), // arg2 not in jsonpath
        new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(Collections.emptyList());
  }

  @Test
  public void shouldAddChildJsonExtractorWhenMatchesAgainstSampleResultBody()
      throws MalformedURLException {
    assertAddChildJson(RESPONSE_BODY_JSONPATH, ResultField.BODY);
  }

  private void assertAddChildJson(String responseJson, ResultField fieldToCheck)
      throws MalformedURLException {
    JsonCorrelationExtractor<?> jsonExtractor = new JsonCorrelationExtractor<>(responseJson, 1,
        fieldToCheck);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    jsonExtractor.process(null, children, createMatchSampleResult(), new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(createJsonExtractor(responseJson, fieldToCheck))));
  }


  @Test
  public void shouldAddChildJsonExtractorWhenMatchesOnceAndMatchNumberIsLessThanZero()
      throws MalformedURLException {
    List<TestElement> children = addChildJson(RESPONSE_BODY_JSONPATH, ResultField.BODY, false);
    JSONPostProcessor jsonExtractor =
        createJsonExtractor(RESPONSE_BODY_JSONPATH, ResultField.BODY);
    jsonExtractor.setMatchNumbers("1");
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(jsonExtractor)));
  }

  private List<TestElement> addChildJson(String responseJson, ResultField fieldToCheck,
                                         boolean isMultipleMatch)
      throws MalformedURLException {
    JsonCorrelationExtractor<?> jsonExtractor = new JsonCorrelationExtractor<>(responseJson, -1,
        fieldToCheck);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    jsonExtractor.process(null, children, isMultipleMatch ? createMultipleMatchSampleResult() :
        createMatchSampleResult(), new JMeterVariables());
    return children;
  }

  private SampleResult createMultipleMatchSampleResult() throws MalformedURLException {
    return JsonCorrelationExtractorTest
        .createSampleResultWithResponseBody(
            "{\"arg1\":[\"123\", \"321\"]}");
  }

  private SampleResult createEmptySampleResult() throws MalformedURLException {
    return JsonCorrelationExtractorTest
        .createSampleResultWithResponseBody("");
  }

  private SampleResult createMatchSampleResult() throws MalformedURLException {
    return createSampleResultWithResponseBody("{\"arg1\":\"123\"}");
  }

  private JSONPostProcessor createJsonExtractor(String responseJsonPath, ResultField fieldToCheck) {
    JSONPostProcessor processor = new JSONPostProcessor();
    processor.setProperty(TestElement.GUI_CLASS,
        org.apache.jmeter.extractor.json.jsonpath.gui.JSONPostProcessorGui.class.getName());
    processor.setName("JSON Path - " + REFERENCE_NAME);
    processor.setRefNames(REFERENCE_NAME);
    processor.setMatchNumbers("1");
    processor.setDefaultValues(REFERENCE_NAME + "_NOT_FOUND");
    processor.setComputeConcatenation(true);
    processor.setJsonPathExpressions(responseJsonPath);
    processor.setScopeAll();
    return processor;
  }

  @Test
  public void shouldAddChildJsonExtractorWhenMatchesTwiceAndMatchNumberIsLowerThanZero()
      throws MalformedURLException {
    List<TestElement> children = addChildJson(RESPONSE_BODY_JSONPATH, ResultField.BODY, true);
    JSONPostProcessor jsonExtractor = createJsonExtractor(RESPONSE_BODY_JSONPATH, ResultField.BODY);
    jsonExtractor.setMatchNumbers("-1");
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(jsonExtractor)));
  }


  @Test
  public void shouldSetMatchNumberToDefaultValueWhenItIsEmpty() throws MalformedURLException {
    ResultField fieldToCheck = ResultField.BODY;
    JsonCorrelationExtractor<?> jsonExtractor = new JsonCorrelationExtractor<>();
    jsonExtractor.setParams(PARAMS);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    jsonExtractor.process(null, children, createMatchSampleResult(), new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children))
        .isEqualTo(TestUtils.comparableFrom(Collections.singletonList(createJsonExtractor(
            RESPONSE_BODY_JSONPATH, fieldToCheck))));
  }

  @Test
  public void shouldNotAddJsonExtractorWhenMatchedValueIsAlreadyExtracted()
      throws MalformedURLException {
    JsonCorrelationExtractor<?> jsonExtractor = new JsonCorrelationExtractor<>();
    jsonExtractor.setParams(PARAMS);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    SampleResult firstSampleResults = createMatchSampleResult();
    JMeterVariables vars = new JMeterVariables();
    jsonExtractor.process(null, children, firstSampleResults, vars);
    jsonExtractor.process(null, children, firstSampleResults, vars);
    assertThat(TestUtils.comparableFrom(children))
        .isEqualTo(TestUtils.comparableFrom(Collections.singletonList(createJsonExtractor(
            RESPONSE_BODY_JSONPATH, ResultField.BODY))));
  }

  @Test
  public void shouldFailSampleWhenProcessWithFailingExtractingVariable() throws IOException {
    JsonCorrelationExtractor<?> jsonExtractor = new JsonCorrelationExtractor<>(
        RESPONSE_BODY_JSONPATH);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();

    JSR223Assertion assertion = buildExtractionAssertion();
    assertion.setScript(StringEscapeUtils
        .unescapeXml(getFileContent("/templates/components/ExtractingVariableAssertion.xml",
            jsonExtractor.getClass())));
    children.add(assertion);

    JMeterVariables vars = new JMeterVariables();
    SampleResult firstSampleResults = createMatchSampleResult();
    jsonExtractor.process(null, children, firstSampleResults, vars);

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
    JsonCorrelationExtractor<?> jsonExtractor = new JsonCorrelationExtractor<>(
        RESPONSE_BODY_JSONPATH);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();

    JSR223Assertion assertion = buildExtractionAssertion();
    assertion.setScript(StringEscapeUtils
        .unescapeXml(getFileContent("/templates/components/ExtractingVariableAssertion.xml",
            jsonExtractor.getClass())));
    children.add(assertion);

    JMeterVariables vars = new JMeterVariables();
    SampleResult firstSampleResults = createMatchSampleResult();
    jsonExtractor.process(null, children, firstSampleResults, vars);

    SampleResult sampleResult = createSampleResultWithResponseBody("Test_SWEACn=TestBodyInfo&");
    sampleResult.setSuccessful(true);
    AssertionResult result = assertion.getResult(sampleResult);
    assertThat(!result.isFailure());
  }

  @Test
  public void shouldNotAddChildJsonExtractorWhenDoesNotMatchAndMultiValued()
      throws MalformedURLException {
    setupContextForMultiValue();
    JsonCorrelationExtractor<BaseCorrelationContext> jsonExtractor =
        new JsonCorrelationExtractor<>(RESPONSE_BODY_JSONPATH, -1,
            ResultField.BODY.name(), "true");
    jsonExtractor.setVariableName(REFERENCE_NAME);
    jsonExtractor.setContext(baseCorrelationContext);
    List<TestElement> children = new ArrayList<>();
    jsonExtractor.process(null, children,
        JsonCorrelationExtractorTest.createSampleResultWithResponseBody("Other Body"),
        new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(Collections.emptyList());
  }

  private void setupContextForMultiValue() {
    when(baseCorrelationContext.getNextVariableNr(REFERENCE_NAME)).thenReturn(1).thenReturn(2);
  }

  @Test
  public void shouldAddChildJsonExtractorWithOneInMatchNrWhenMatchesAgainstSampleResultOnlyOnceBody()
      throws MalformedURLException {
    setupContextForMultiValue();
    List<TestElement> children = new ArrayList<>();
    addChildJson(RESPONSE_BODY_JSONPATH, createMatchSampleResult(), children,
        new JMeterVariables());
    JSONPostProcessor extractor = createJsonExtractor(RESPONSE_BODY_JSONPATH, 1);
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(extractor)));
  }

  @Test
  public void shouldAddMatchNrVarWhenMatchesAgainstSampleResultBodyMultipleTimes()
      throws MalformedURLException {
    setupContextForMultiValue();
    List<TestElement> children = new ArrayList<>();
    JMeterVariables vars = new JMeterVariables();
    addChildJson(RESPONSE_BODY_JSONPATH, createMultipleMatchSampleResult(), children, vars);
    assertThat(vars.get(REFERENCE_NAME + "#1_matchNr")).isNotNull();
  }

  @Test
  public void shouldAddChildWithDifferentIndexWhenMatchesAgainstMultipleSampleResultBody()
      throws MalformedURLException {
    setupContextForMultiValue();
    List<TestElement> children = new ArrayList<>();
    JMeterVariables vars = new JMeterVariables();
    addChildJson(RESPONSE_BODY_JSONPATH, createMultipleMatchSampleResult(), children, vars);
    addChildJson(RESPONSE_BODY_JSONPATH, createMultipleMatchSampleResult(), children, vars);
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Arrays.asList(createJsonExtractor(RESPONSE_BODY_JSONPATH, 1, -1),
                createJsonExtractor(RESPONSE_BODY_JSONPATH, 2, -1))));
    assertThat(vars.get(REFERENCE_NAME + "#1_matchNr")).isNotNull();
  }

  private void addChildJson(String responseJson, SampleResult sampleResult,
                            List<TestElement> children, JMeterVariables vars) {
    JsonCorrelationExtractor<BaseCorrelationContext> jsonExtractor =
        new JsonCorrelationExtractor<>(RESPONSE_BODY_JSONPATH, -1, ResultField.BODY.name(),
            "true");

    jsonExtractor.setContext(baseCorrelationContext);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    jsonExtractor.process(null, children, sampleResult, vars);

  }

  private JSONPostProcessor createJsonExtractor(String responseJson, int varNr) {
    JSONPostProcessor processor = new JSONPostProcessor();
    processor.setProperty(TestElement.GUI_CLASS,
        org.apache.jmeter.extractor.json.jsonpath.gui.JSONPostProcessorGui.class.getName());
    processor.setName("JSON Path - " + REFERENCE_NAME + "#" + varNr);
    processor.setRefNames(REFERENCE_NAME + "#" + varNr);
    processor.setMatchNumbers(String.valueOf(varNr));
    processor.setDefaultValues(REFERENCE_NAME + "#" + varNr + "_NOT_FOUND");
    processor.setJsonPathExpressions(responseJson);
    processor.setComputeConcatenation(true);
    processor.setScopeAll();
    return processor;
  }

  private JSONPostProcessor createJsonExtractor(String responseJson, int varNr, int matchNr) {
    JSONPostProcessor processor = createJsonExtractor(responseJson, varNr);
    processor.setMatchNumbers(String.valueOf(matchNr));
    return processor;
  }

  @Test
  public void shouldNotFailWhenSampleResultBodyIsEmpty()
      throws MalformedURLException {
    setupContextForMultiValue();
    List<TestElement> children = new ArrayList<>();
    JMeterVariables vars = new JMeterVariables();
    addChildJson(RESPONSE_BODY_JSONPATH, createEmptySampleResult(), children, vars);
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.emptyList()));
  }


  @Test
  public void shouldRemoveLeftOverVariablesWhenMultipleMatchesAndMatchNrLowerThanZero()
      throws Exception {
    ComparableJMeterVariables vars =
        new ComparableJMeterVariables();
    vars.put(REFERENCE_NAME + "_1", "value1");
    vars.put(REFERENCE_NAME + "_2", "value2");
    vars.put(REFERENCE_NAME + "_3", "value3");
    vars.put(REFERENCE_NAME + "_matchNr", "3");
    JsonCorrelationExtractor<BaseCorrelationContext> jsonExtractor =
        new JsonCorrelationExtractor<>(RESPONSE_BODY_JSONPATH, -1, ResultField.BODY.name(),
            "false");
    jsonExtractor.setContext(baseCorrelationContext);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    jsonExtractor.process(null, new ArrayList<>(), createMultipleMatchSampleResult(),
        vars);
    assertThat(vars).isEqualTo(buildExpectedVariable());
  }

  private JMeterVariables buildExpectedVariable() {
    ComparableJMeterVariables vars =
        new ComparableJMeterVariables();
    vars.put(REFERENCE_NAME + "_1", "123");
    vars.put(REFERENCE_NAME + "_2", "321");
    vars.put(REFERENCE_NAME + "_matchNr", "2");
    return vars;
  }

  @Mock
  private CorrelationComponentsRegistry registry;

  @Test
  public void shouldCatchAllMatchedValues() throws MalformedURLException {
    ComparableJMeterVariables variables =
        new ComparableJMeterVariables();

    JsonCorrelationExtractor<BaseCorrelationContext> jsonExtractor =
        new JsonCorrelationExtractor<>("$..arg1", -1,
            ResultField.BODY.name(), "true");
    jsonExtractor.setVariableName("args");
    jsonExtractor.setMultiValued(true);
    jsonExtractor.setContext(new BaseCorrelationContext());

    String responseWithArgs = "[{\"arg1\":\"123\"},{\"arg1\":\"321\"}]";

    jsonExtractor.process(null, new ArrayList<>(),
        createSampleResultWithResponseBody(responseWithArgs), variables);
    assertThat(variables.entrySet().size()).isEqualTo(3);
  }

}
