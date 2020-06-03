package com.blazemeter.jmeter.correlation.core.replacements;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.core.ResultField;
import java.util.Collections;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.XPathExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.extractor.gui.XPathExtractorGui;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class FunctionCorrelationReplacementTest {

  private static final String XPATH_EXTRACTOR_GUI_CLASS = XPathExtractorGui.class.getName();
  private static final String REFERENCE_NAME = "__time()";
  private static final String REQUEST_REGEX = "Test_Timestamp=([^&]+)";
  private static final String PARAM_NAME = "Test_Timestamp";
  private static final String PARAM_VALUE = "1234567891234";

  private FunctionCorrelationReplacement replacer;
  private JMeterVariables vars;
  private HTTPSampler sampler;

  // we need this to avoid nasty logs about pdfbox
  @BeforeClass
  public static void setupClass() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  @Before
  public void setup() {
    sampler = new HTTPSampler();
    sampler.setMethod("GET");
    sampler.setPath("/" + PARAM_NAME + "=" + PARAM_VALUE + "&Test_Path=1");
    vars = new JMeterVariables();
    vars.put(REFERENCE_NAME, PARAM_VALUE);
    replacer = new FunctionCorrelationReplacement(REQUEST_REGEX);
    replacer.setVariableName(REFERENCE_NAME);
  }

  @Test
  public void shouldNotReplaceRegexExtractorValues() {
    String responseRegex = PARAM_NAME + "=" + PARAM_VALUE + "&";
    RegexExtractor extractor = createRegexExtractor(responseRegex);
    replacer.process(sampler, Collections.singletonList(extractor), null, vars);
    assertThat(extractor.getRegex()).isEqualTo(responseRegex);
  }

  private RegexExtractor createRegexExtractor(String responseRegex) {
    RegexExtractor regex = new RegexExtractor();
    regex.setProperty(TestElement.GUI_CLASS, RegexExtractorGui.class.getName());
    regex.setName("RegExp - " + REFERENCE_NAME);
    regex.setRefName(REFERENCE_NAME);
    regex.setTemplate("$1$");
    regex.setMatchNumber(1);
    regex.setDefaultValue("NOT_FOUND");
    regex.setRegex(responseRegex);
    regex.setUseField(ResultField.BODY.getCode());
    return regex;
  }

  @Test
  public void shouldNotReplaceClassNameIfItMatchesRegex() {
    XPathExtractor xPathExtractor = new XPathExtractor();
    xPathExtractor.setProperty(TestElement.GUI_CLASS, XPATH_EXTRACTOR_GUI_CLASS);
    replacer.process(sampler, Collections.singletonList(xPathExtractor), null, vars);
    assertThat(xPathExtractor.getPropertyAsString(TestElement.GUI_CLASS))
        .isEqualTo(XPATH_EXTRACTOR_GUI_CLASS);
  }

  @Test
  public void shouldReplaceHeaderManagerValueWhenRegexMatches() {
    replacer = new FunctionCorrelationReplacement("(" + PARAM_VALUE + ")");
    replacer.setVariableName(REFERENCE_NAME);
    HeaderManager headerManager = new HeaderManager();
    headerManager.add(new Header(PARAM_NAME, PARAM_VALUE));
    replacer.process(sampler, Collections.singletonList(headerManager), null, vars);
    assertThat(headerManager.getHeader(0))
        .isEqualTo(new Header(PARAM_NAME, "${" + REFERENCE_NAME + "}"));
  }

  @Test
  public void shouldNotReplaceHeaderManagerValueWhenRegexDoesNotMatch() {
    replacer = new FunctionCorrelationReplacement("(" + PARAM_VALUE + ")");
    replacer.setVariableName(REFERENCE_NAME);
    HeaderManager headerManager = new HeaderManager();
    String headerValue = "OtherValue";
    headerManager.add(new Header(PARAM_NAME, headerValue));
    replacer.process(sampler, Collections.singletonList(headerManager), null, vars);
    assertThat(headerManager.getHeader(0)).isEqualTo(new Header(PARAM_NAME, headerValue));
  }

  @Test
  public void shouldReplaceValueInRequestPathWhenRegexMatches() {
    String originalPath = sampler.getPath();
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(sampler.getPath())
        .isEqualTo(originalPath.replace(PARAM_VALUE, "${" + REFERENCE_NAME + "}"));
  }

  @Test
  public void shouldReplaceValueInRequestPathEvenWhenRegexMatchesButVariableValueIsDifferent() {
    vars.put(REFERENCE_NAME, "Other");
    String originalPath = sampler.getPath();
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(sampler.getPath())
        .isEqualTo(originalPath.replace(PARAM_VALUE, "${" + REFERENCE_NAME + "}"));
  }

  @Test
  public void shouldReplaceValueInArgumentWhenRegexMatches() {
    sampler.addArgument(PARAM_NAME, PARAM_VALUE);
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(sampler.getArguments().getArgument(0).getValue())
        .isEqualTo("${" + REFERENCE_NAME + "}");
  }

  @Test
  public void shouldReplaceValueInArgumentEvenWhenRegexMatchesButVariableValueIsDifferent() {
    vars.put(REFERENCE_NAME, "Other");
    sampler.addArgument(PARAM_NAME, PARAM_VALUE);
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(sampler.getArguments().getArgument(0).getValue())
        .isEqualTo("${" + REFERENCE_NAME + "}");
  }
}
