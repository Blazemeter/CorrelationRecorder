package com.blazemeter.jmeter.correlation.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import com.google.common.base.Charsets;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class
ResultFieldTest {

  private static final String URL_TEST_VALUE = "https://jmeter.apache.org/";
  private static final String RESPONSE_CODE_TEST_VALUE = "200";
  private static final String RESPONSE_MESSAGE_TEST_VALUE = "Test Response Message";
  private static final String RESPONSE_HEADERS_TEST_VALUE = "Test Response Headers";
  private static final String REQUEST_HEADERS_TEST_VALUE = "Test Request Headers";
  private static final String RESPONSE_DATA =
      "<?xml version=\"1.0\"?><test>Test &quot;body&quot;</test>";

  private final SampleResult sampleResult = new SampleResult();

  // we need this to avoid nasty logs about pdfbox
  @BeforeClass
  public static void setupClass() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  @Before
  public void setup() throws MalformedURLException {
    URL testUrl = new URL(URL_TEST_VALUE);
    sampleResult.setURL(testUrl);
    sampleResult.setResponseCode(RESPONSE_CODE_TEST_VALUE);
    sampleResult.setResponseMessage(RESPONSE_MESSAGE_TEST_VALUE);
    sampleResult.setResponseHeaders(RESPONSE_HEADERS_TEST_VALUE);
    sampleResult.setRequestHeaders(REQUEST_HEADERS_TEST_VALUE);
    sampleResult.setResponseData(RESPONSE_DATA, SampleResult.DEFAULT_HTTP_ENCODING);
  }

  @Test
  public void shouldReturnSampleResultUrl() {
    ResultField resultField = ResultField.URL;
    String inputString = resultField.getField(sampleResult);
    assertThat(inputString).isEqualTo(URL_TEST_VALUE);
  }

  @Test
  public void shouldReturnSampleResultResponseCode() {
    ResultField resultField = ResultField.RESPONSE_CODE;
    String fieldToCheck = resultField.getField(sampleResult);
    assertThat(fieldToCheck).isEqualTo(RESPONSE_CODE_TEST_VALUE);
  }

  @Test
  public void shouldReturnSampleResultResponseMessage() {
    ResultField resultField = ResultField.RESPONSE_MESSAGE;
    String fieldToCheck = resultField.getField(sampleResult);
    assertThat(fieldToCheck).isEqualTo(RESPONSE_MESSAGE_TEST_VALUE);
  }

  @Test
  public void shouldReturnSampleResultResponseHeaders() {
    ResultField resultField = ResultField.RESPONSE_HEADERS;
    String fieldToCheck = resultField.getField(sampleResult);
    assertThat(fieldToCheck).isEqualTo(RESPONSE_HEADERS_TEST_VALUE);
  }

  @Test
  public void shouldReturnSampleResultRequestHeaders() {
    ResultField resultField = ResultField.REQUEST_HEADERS;
    String fieldToCheck = resultField.getField(sampleResult);
    assertThat(fieldToCheck).isEqualTo(REQUEST_HEADERS_TEST_VALUE);
  }

  @Test
  public void shouldReturnSampleResultResponseBody() {
    sampleResult.setResponseData(RESPONSE_DATA, Charsets.UTF_8.name());
    ResultField resultField = ResultField.BODY;
    String fieldToCheck = resultField.getField(sampleResult);
    assertThat(fieldToCheck).isEqualTo(RESPONSE_DATA);
  }

  @Test
  public void shouldReturnSampleResultResponseBodyUnescaped() {
    sampleResult.setResponseData(RESPONSE_DATA, Charsets.UTF_8.name());
    ResultField resultField = ResultField.BODY_UNESCAPED;
    String fieldToCheck = resultField.getField(sampleResult);
    assertThat(fieldToCheck)
        .isEqualTo("<?xml version=\"1.0\"?><test>Test \"body\"</test>");
  }

  @Test
  public void shouldReturnSampleResultResponseBodyAsADocument() {
    sampleResult.setResponseData(RESPONSE_DATA, Charsets.UTF_8.name());
    ResultField resultField = ResultField.BODY_AS_A_DOCUMENT;
    String fieldToCheck = resultField.getField(sampleResult);
    assertThat(fieldToCheck).isEqualTo(" Test \"body\"\n");
  }
}
