package com.blazemeter.jmeter.correlation.siebel;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.ResultField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.jmeter.extractor.JSR223PostProcessor;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class SiebelRowCorrelationExtractorTest {

  private static final String REGEX_ONE = "`ValueArray`(.*?)`";
  private static final String REGEX_TWO = "`v`(.*?)`";
  private static final String RESPONSE_DATA_ONE =
      "`ValueArray`8*testUser12*testPassword6*VRId-0`";
  private static final String RESPONSE_DATA_TWO =
      "`v`8*testUser12*testPassword6*VRId-0`";
  private static final String REFERENCE_NAME = "TEST_REFERENCE_NAME";
  private SiebelRowCorrelationExtractor siebelRowExtractor;
  private HTTPSampler sampler;
  private SampleResult sampleResult;
  private JMeterVariables vars;

  @BeforeClass
  public static void setupClass() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  @Before
  public void setup() {
    SiebelContext context = new SiebelContext();
    sampler = new HTTPSampler();
    sampleResult = new SampleResult();
    vars = new JMeterVariables();
    siebelRowExtractor = new SiebelRowCorrelationExtractor();
    siebelRowExtractor.setParams(Collections.singletonList(REGEX_ONE));
    siebelRowExtractor.setContext(context);
  }

  @Test
  public void shouldAddRegexExtractorAndPostProcessorWhenRegexOneMatches() {
    List<TestElement> children = new ArrayList<>();
    List<TestElement> extractors = new ArrayList<>();
    sampleResult.setResponseData(RESPONSE_DATA_ONE, SampleResult.DEFAULT_HTTP_ENCODING);
    siebelRowExtractor.setVariableName(REFERENCE_NAME);
    siebelRowExtractor.process(sampler, children, sampleResult, vars);
    RegexExtractor regexExtractor = createRegexExtractor(REGEX_ONE, ResultField.BODY);
    JSR223PostProcessor jSR223PostProcessor = createPostProcessor();
    extractors.add(regexExtractor);
    extractors.add(jSR223PostProcessor);
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(TestUtils.comparableFrom(extractors));
  }

  @Test
  public void shouldAddRegexExtractorAndPostProcessorWhenRegexTwoMatches() {
    List<TestElement> children = new ArrayList<>();
    List<TestElement> extractors = new ArrayList<>();
    sampleResult.setResponseData(RESPONSE_DATA_TWO, SampleResult.DEFAULT_HTTP_ENCODING);
    siebelRowExtractor.setRegex(REGEX_TWO);
    siebelRowExtractor.setVariableName(REFERENCE_NAME);
    siebelRowExtractor.process(sampler, children, sampleResult, vars);
    RegexExtractor regexExtractor = createRegexExtractor(REGEX_TWO, ResultField.BODY);
    JSR223PostProcessor jSR223PostProcessor = createPostProcessor();
    extractors.add(regexExtractor);
    extractors.add(jSR223PostProcessor);
    assertThat(TestUtils.comparableFrom(children)).isEqualTo(TestUtils.comparableFrom(extractors));
  }

  @Test
  public void shouldNotAddRegexExtractorAndPostProcessorWhenRegexDoesNotMatch() {
    List<TestElement> children = new ArrayList<>();
    sampleResult.setResponseData(RESPONSE_DATA_TWO, SampleResult.DEFAULT_HTTP_ENCODING);
    siebelRowExtractor = new SiebelRowCorrelationExtractor();
    siebelRowExtractor.setParams(Collections.singletonList(REGEX_ONE));
    siebelRowExtractor.setVariableName(REFERENCE_NAME);
    siebelRowExtractor.process(sampler, children, sampleResult, vars);
    assertThat(children).isEmpty();
  }

  private JSR223PostProcessor createPostProcessor() {
    StringBuilder script = new StringBuilder();
    JSR223PostProcessor jSR223PostProcessor = new JSR223PostProcessor();
    jSR223PostProcessor.setProperty(JSR223PostProcessor.GUI_CLASS, TestBeanGUI.class.getName());
    jSR223PostProcessor.setName("Parse Array Values");
    script.append("import com.blazemeter.jmeter.correlation.siebel.SiebelArrayFunction;\n\n");
    script.append("String stringToSplit = \"\";\n");
    script.append("String rowId = \"\";");
    String varNamePrefix = SiebelRowCorrelationExtractorTest.REFERENCE_NAME + "0";
    script.append(String.format("\n\n// Parsing Star Array parameter(s) using match number %1$d\n"
            + "stringToSplit = vars.get(\"%2$s_%1$d\");\n"
            + "if (stringToSplit != null) {\n"
            + "\tSiebelArrayFunction.split(stringToSplit, \"%3$s\", vars);\n"
            + "\trowIdValue = vars.get(\"%3$s_%4$d\");\n"
            + "\tvars.put(\"%3$s_rowId\", rowIdValue);"
            + "\n}", 1, SiebelRowCorrelationExtractorTest.REFERENCE_NAME, varNamePrefix,
        3));
    jSR223PostProcessor.setProperty("script", script.toString());
    return jSR223PostProcessor;
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
    return regex;
  }

}
