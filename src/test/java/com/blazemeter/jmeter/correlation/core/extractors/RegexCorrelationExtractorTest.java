package com.blazemeter.jmeter.correlation.core.extractors;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.ResultField;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class RegexCorrelationExtractorTest {

  private static final String REFERENCE_NAME = "Reference Name";
  private static final String RESPONSE_BODY_REGEX = "Test_SWEACn=(.*?)&";
  private static final String URL_RESPONSE_REGEX = "jmeter\\.(.*?)\\.org";
  private static final String TEST_URL = "https://jmeter.apache.org/";
  private static final String SUCCESS_RESPONSE_MESSAGE = HttpStatus.getStatusText(HttpStatus.SC_OK);
  private static final String DEFAULT_REGEX_VALUE = "";

  // we need this to avoid nasty logs about pdfbox
  @BeforeClass
  public static void setupClass() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  @Test
  public void shouldNotAddChildRegexExtractorWhenDoesNotMatch()
      throws MalformedURLException {
    RegexCorrelationExtractor regexExtractor = new RegexCorrelationExtractor(RESPONSE_BODY_REGEX);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children, createSampleResultWithResponseBody("Other Body"),
        new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(Collections.emptyList());
  }

  private SampleResult createSampleResultWithResponseBody(String responseBody)
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
  public void shouldAddChildRegexExtractorWhenMatchesAgainstSampleResultBody()
      throws MalformedURLException {
    assertAddChildRegex(RESPONSE_BODY_REGEX, ResultField.BODY);
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesAndMatchNumberIsLessThanZero()
      throws MalformedURLException {
    List<TestElement> children = addChildRegex(RESPONSE_BODY_REGEX, ResultField.BODY);
    RegexExtractor regexExtractor = createRegexExtractor(RESPONSE_BODY_REGEX, ResultField.BODY);
    regexExtractor.setMatchNumber(-1);
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(regexExtractor)));
  }

  private List<TestElement> addChildRegex(String responseRegex, ResultField fieldToCheck)
      throws MalformedURLException {
    RegexCorrelationExtractor regexExtractor = new RegexCorrelationExtractor(responseRegex, -1,
        fieldToCheck);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children, createMatchSampleResult(), new JMeterVariables());
    return children;
  }

  private void assertAddChildRegex(String responseRegex, ResultField fieldToCheck)
      throws MalformedURLException {
    RegexCorrelationExtractor regexExtractor = new RegexCorrelationExtractor(responseRegex, 1,
        fieldToCheck);
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children, createMatchSampleResult(), new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(
        TestUtils.comparableFrom(
            Collections.singletonList(createRegexExtractor(responseRegex, fieldToCheck))));
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
    regex.setDefaultValue(DEFAULT_REGEX_VALUE);
    regex.setRegex(responseRegex);
    regex.setUseField(fieldToCheck.getCode());
    return regex;
  }

  @Test
  public void shouldAddChildRegexExtractorWhenMatchesAgainstSampleResultBodyAsADocument()
      throws MalformedURLException {
    assertAddChildRegex("(Test Body \t\n)", ResultField.BODY_AS_A_DOCUMENT);
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
    RegexCorrelationExtractor regexExtractor = new RegexCorrelationExtractor(RESPONSE_BODY_REGEX);
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
    RegexCorrelationExtractor regexExtractor = new RegexCorrelationExtractor(RESPONSE_BODY_REGEX,
        "invalid_number", "invalid_number", ResultField.BODY.name());
    regexExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    regexExtractor.process(null, children, createMatchSampleResult(), new JMeterVariables());
    assertThat(TestUtils.comparableFrom(children))
        .isEqualTo(TestUtils.comparableFrom(Collections.singletonList(createRegexExtractor(
            RESPONSE_BODY_REGEX, ResultField.BODY))));
  }
}
