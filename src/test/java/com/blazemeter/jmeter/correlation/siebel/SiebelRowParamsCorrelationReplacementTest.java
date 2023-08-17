package com.blazemeter.jmeter.correlation.siebel;

import static org.assertj.core.api.Assertions.assertThat;
import com.blazemeter.jmeter.correlation.TestUtils;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.oro.text.regex.MalformedPatternException;
import org.junit.Before;
import org.junit.Test;

public class SiebelRowParamsCorrelationReplacementTest {

  private static final String VARIABLE_PREFIX_ONE = "Siebel_Star_Array41";
  private static final String VARIABLE_PREFIX_TWO = "Siebel_Star_Array15";
  private static final String VARIABLE_NAME_ONE = "Siebel_Star_Array41_1";
  private static final String VARIABLE_NAME_TWO = "Siebel_Star_Array15_1";
  private static final String ROW_ID_ONE = "1-639";
  private static final String REGEX_THAT_MATCHES = "SWERowId=([^&\\n]+)";
  private static final String PARAM_NAME_ONE = "s_1_2_20_1";
  private static final String PARAM_VALUE_ONE = "3 CommmmT";
  private static final String PARAM_VALUE_TWO = "3 CommmmT Does Not Match";
  private static final String INITIAL_RESPONSE_DATA_PATH = "src/test/resources/initialResponse.txt";
  private static SampleResult initialSampleResult;
  private static final List<TestElement> children = new ArrayList<>();

  private SiebelRowParamsCorrelationReplacement siebelRowParamsReplacement;
  private final SiebelContext siebelContext = new SiebelContext();
  private HTTPSampler sampler;
  private SampleResult sampleResult;
  private JMeterVariables vars;


  @Before
  public void setup() {
    sampler = new HTTPSampler();
    sampler.setMethod("GET");
    sampler.setPath("/" + PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE + "&Test_Path=1");
    sampleResult = new SampleResult();
    sampleResult.setSamplerData("SWERowId=" + ROW_ID_ONE + "&Test_Path=1");
    vars = new JMeterVariables();
    siebelRowParamsReplacement = new SiebelRowParamsCorrelationReplacement();
    siebelRowParamsReplacement.setParams(Collections.singletonList(REGEX_THAT_MATCHES));
    siebelRowParamsReplacement.setContext(siebelContext);
    initialSampleResult = new SampleResult();
    initialSampleResult.setResponseData(TestUtils
        .readFile(INITIAL_RESPONSE_DATA_PATH, Charset.defaultCharset()));
  }

  @Test
  public void shouldReplaceInputStringWhenRegexMatches() throws MalformedPatternException {
    siebelContext.update(initialSampleResult);
    siebelContext.addRowVar(ROW_ID_ONE, VARIABLE_PREFIX_ONE);
    vars.put(VARIABLE_NAME_ONE, PARAM_VALUE_ONE);
    siebelRowParamsReplacement.process(sampler, children, sampleResult, vars);
    String expectedString = PARAM_NAME_ONE + "=${" + VARIABLE_NAME_ONE + "}";
    String replacedString = siebelRowParamsReplacement
        .replaceWithRegex(PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE, "", "", vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }

  @Test
  public void shouldReplaceInputStringWhenRegexMatchesAndParamValueIsEmpty()
      throws MalformedPatternException {
    siebelContext.update(initialSampleResult);
    siebelContext.addRowVar(ROW_ID_ONE, VARIABLE_PREFIX_ONE);
    vars.put(VARIABLE_NAME_ONE, "");
    siebelRowParamsReplacement.process(sampler, children, sampleResult, vars);
    String expectedString = PARAM_NAME_ONE + "=${" + VARIABLE_NAME_ONE + "}";
    String replacedString = siebelRowParamsReplacement
        .replaceWithRegex(PARAM_NAME_ONE + "=" + "", "", "", vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }

  @Test
  public void shouldNotReplaceInputStringWhenRegexMatchesButVarNamePrefixDoesNotMatch()
      throws MalformedPatternException {
    siebelContext.update(initialSampleResult);
    siebelContext.addRowVar(ROW_ID_ONE, VARIABLE_PREFIX_TWO);
    vars.put(VARIABLE_NAME_ONE, PARAM_VALUE_ONE);
    siebelRowParamsReplacement.process(sampler, children, sampleResult, vars);
    String expectedString = PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE;
    String replacedString = siebelRowParamsReplacement
        .replaceWithRegex(PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE, "", "", vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }

  @Test
  public void shouldNotReplaceInputStringWhenRegexMatchesButParamValueDoesNotMatch()
      throws MalformedPatternException {
    siebelContext.update(initialSampleResult);
    siebelContext.addRowVar(ROW_ID_ONE, VARIABLE_PREFIX_ONE);
    vars.put(VARIABLE_NAME_ONE, PARAM_VALUE_TWO);
    siebelRowParamsReplacement.process(sampler, children, sampleResult, vars);
    String expectedString = PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE;
    String replacedString = siebelRowParamsReplacement
        .replaceWithRegex(PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE, "", "", vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }

  @Test
  public void shouldNotReplaceInputStringWhenRegexAndParamValueButParamNameDoesNotMatch()
      throws MalformedPatternException {
    siebelContext.update(initialSampleResult);
    siebelContext.addRowVar(ROW_ID_ONE, VARIABLE_PREFIX_ONE);
    vars.put(VARIABLE_NAME_TWO, PARAM_VALUE_ONE);
    siebelRowParamsReplacement.process(sampler, children, sampleResult, vars);
    String expectedString = PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE;
    String replacedString = siebelRowParamsReplacement
        .replaceWithRegex(PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE, "", "", vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }
}
