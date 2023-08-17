package com.blazemeter.jmeter.correlation.siebel;

import static org.assertj.core.api.Assertions.assertThat;
import com.blazemeter.jmeter.correlation.TestUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.modifiers.JSR223PreProcessor;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.Before;
import org.junit.Test;

public class SiebelCounterCorrelationReplacementTest {

  private static final String TEST_URL = "https://jmeter.apache.org/";
  private static final String SUCCESS_RESPONSE_MESSAGE = HttpStatus.getStatusText(HttpStatus.SC_OK);
  private static final String VARIABLE_NAME = "Siebel_SWEC";
  private static final String REGEX_THAT_MATCHES = "SWEC=([^&\n]+)";
  private static final String PARAM_NAME_ONE = "s_1_2_20_1";
  private static final String PARAM_VALUE_ONE = "3 CommmmT";
  private static final int SIEBEL_COUNT = 5;
  private static final Integer SIEBEL_COUNT_GREATER = 6;
  private static final Integer SIEBEL_COUNT_LESS = 4;
  private static final JMeterVariables EMPTY_VARS = new JMeterVariables();

  private SiebelCounterCorrelationReplacement siebelCounterReplacement;
  private final SiebelContext siebelContext = new SiebelContext();
  private HTTPSampler sampler;
  private SampleResult sampleResult;
  private JMeterVariables vars;
  private final JMeterVariables expectedVariables = new JMeterVariables();

  @Before
  public void setup() {
    sampler = new HTTPSampler();
    sampler.setMethod("GET");
    sampler.setPath("/" + PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE + "&Test_Path=1");
    sampleResult = new SampleResult();
    vars = new JMeterVariables();
    siebelCounterReplacement = new SiebelCounterCorrelationReplacement();
    siebelCounterReplacement.setParams(Collections.singletonList(REGEX_THAT_MATCHES));
    siebelCounterReplacement.setContext(siebelContext);
    siebelCounterReplacement.setVariableName(VARIABLE_NAME);
  }

  private SampleResult createMatchSampleResult() throws MalformedURLException {
    return createSampleResultWithResponseBody("Test_SWEACn=123&Test_Path=1&Test Body \t");
  }

  private SampleResult createNotMatchSampleResult() throws MalformedURLException {
    SampleResult sampleResult = createSampleResultWithResponseBody(
        "Test_SWEACn=123&Test_Path=1&Test Body \t");
    sampleResult.setSamplerData(
        "/siebel/app/salesm/enu?SWECmd=GetCachedFrame&SWEACn=1043490673&SWEFrame=top._swe&SRN=");
    return sampleResult;
  }

  private SampleResult createSampleResultWithResponseBody(String responseBody)
      throws MalformedURLException {
    sampleResult = new SampleResult();
    URL testUrl = new URL(TEST_URL);
    sampleResult.setURL(testUrl);
    sampleResult.setSamplerData(
        "/siebel/app/salesm/enu?SWECmd=GetCachedFrame&SWEACn=1043490673&SWEC=" + SIEBEL_COUNT
            + "&SWEFrame=top._swe&SRN=");
    sampleResult.setResponseCode(String.valueOf(HttpStatus.SC_OK));
    sampleResult.setResponseMessage(SUCCESS_RESPONSE_MESSAGE);
    sampleResult.setResponseHeaders(TEST_URL);
    sampleResult.setRequestHeaders(TEST_URL);
    sampleResult.setResponseData(responseBody, SampleResult.DEFAULT_HTTP_ENCODING);
    sampleResult.setContentType(ContentType.TEXT_HTML.toString());
    return sampleResult;
  }

  private JSR223PreProcessor createPreProcessor(Integer counter, int count) {
    JSR223PreProcessor jSR223PreProcessor = new JSR223PreProcessor();
    jSR223PreProcessor.setProperty(JSR223PreProcessor.GUI_CLASS, TestBeanGUI.class.getName());
    jSR223PreProcessor.setName(String.format("Calculate %s", VARIABLE_NAME));
    StringBuilder script = new StringBuilder();
    if (counter == null) {
      script.append(String.format("int %s_var = %d;\n", VARIABLE_NAME, count));
    } else if (counter < count) {
      script.append(String
          .format("int %1$s_var = Integer.valueOf(vars.get(\"%1$s\")) + %2$d;\n", VARIABLE_NAME,
              count - counter));
    } else if (counter > count) {
      script.append(String
          .format("int %1$s_var = Integer.valueOf(vars.get(\"%1$s\")) - %2$d;\n", VARIABLE_NAME,
              counter - count));
    }
    siebelContext.setCounter(count);
    script.append(String.format("vars.put(\"%1$s\", String.valueOf(%1$s_var));\n", VARIABLE_NAME));
    jSR223PreProcessor.setProperty("script", script.toString());
    return jSR223PreProcessor;
  }

  @Test
  public void shouldAddTheExpectedJSR223PreProcessorWithCounterCalculationWhenCounterIsNull()
      throws MalformedURLException {
    List<TestElement> children = new ArrayList<>();
    siebelCounterReplacement
        .process(sampler, children, createMatchSampleResult(), new JMeterVariables());
    JSR223PreProcessor jSR223PreProcessor = createPreProcessor(null, SIEBEL_COUNT);
    assertThat(TestUtils.comparableFrom(children))
        .isEqualTo(TestUtils.comparableFrom(Collections.singletonList(jSR223PreProcessor)));
  }

  @Test
  public void shouldAddTheExpectedJSR223PreProcessorWithCounterCalculationWhenCountIsGreater()
      throws MalformedURLException {
    List<TestElement> children = new ArrayList<>();
    siebelContext.setCounter(SIEBEL_COUNT_GREATER);
    siebelCounterReplacement
        .process(sampler, children, createMatchSampleResult(), new JMeterVariables());
    JSR223PreProcessor jSR223PreProcessor = createPreProcessor(SIEBEL_COUNT_GREATER, SIEBEL_COUNT);
    assertThat(TestUtils.comparableFrom(children))
        .isEqualTo(TestUtils.comparableFrom(Collections.singletonList(jSR223PreProcessor)));
  }

  @Test
  public void shouldAddTheExpectedJSR223PreProcessorWithCounterCalculationWhenCountIsLess()
      throws MalformedURLException {
    List<TestElement> children = new ArrayList<>();
    siebelContext.setCounter(SIEBEL_COUNT_LESS);
    siebelCounterReplacement
        .process(sampler, children, createMatchSampleResult(), new JMeterVariables());
    JSR223PreProcessor jSR223PreProcessor = createPreProcessor(SIEBEL_COUNT_LESS, SIEBEL_COUNT);
    assertThat(TestUtils.comparableFrom(children))
        .isEqualTo(TestUtils.comparableFrom(Collections.singletonList(jSR223PreProcessor)));
  }

  @Test
  public void shouldAddTheSiebelCountToJMeterVarsWhenRegexMatches() throws MalformedURLException {
    List<TestElement> children = new ArrayList<>();
    siebelCounterReplacement.process(sampler, children, createMatchSampleResult(), vars);
    expectedVariables.put(VARIABLE_NAME, String.valueOf(SIEBEL_COUNT));
    assertThat(vars.entrySet()).isEqualTo(expectedVariables.entrySet());
  }

  @Test
  public void shouldNotAddTheSiebelCountToJMeterVarsWhenRegexDoesNotMatch()
      throws MalformedURLException {
    List<TestElement> children = new ArrayList<>();
    siebelCounterReplacement.process(sampler, children, createNotMatchSampleResult(), vars);
    expectedVariables.put(VARIABLE_NAME, String.valueOf(SIEBEL_COUNT));
    assertThat(vars.entrySet()).isEqualTo(EMPTY_VARS.entrySet());
  }
}
