package com.blazemeter.jmeter.correlation.core.replacements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import java.util.Collections;
import java.util.function.Function;
import org.apache.jmeter.config.Arguments;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.bridge.SLF4JBridgeHandler;

@RunWith(MockitoJUnitRunner.class)
public class RegexCorrelationReplacementTest {

  private static final String XPATH_EXTRACTOR_GUI_CLASS = XPathExtractorGui.class.getName();
  private static final String REFERENCE_NAME = "TEST_SWEACN";
  private static final String REQUEST_REGEX = "=([^&]+)";
  private static final String REQUEST_REGEX_WITHOUT_GROUP = "=[^&]+";
  private static final String PARAM_NAME = "Test_SWEACn";
  private static final String PARAM_VALUE = "123";
  private RegexCorrelationReplacement<BaseCorrelationContext> replacer;
  private JMeterVariables vars;
  private HTTPSampler sampler;

  @Mock
  private BaseCorrelationContext context;

  @Mock
  private Function<String, String> expressionEvaluation;

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
    replacer = new RegexCorrelationReplacement<>(REQUEST_REGEX);
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(context);
    doReturn(0).when(context).getVariableCount(anyString());
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
    replacer = new RegexCorrelationReplacement<>("(" + PARAM_VALUE + ")");
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(context);
    HeaderManager headerManager = new HeaderManager();
    headerManager.add(new Header(PARAM_NAME, PARAM_VALUE));
    replacer.process(sampler, Collections.singletonList(headerManager), null, vars);
    assertThat(headerManager.getHeader(0))
        .isEqualTo(new Header(PARAM_NAME, "${" + REFERENCE_NAME + "}"));
  }

  @Test
  public void shouldReplaceHeaderManagerValueWhenRegexMatchesWithFullHeader() {
    replacer = new RegexCorrelationReplacement<>(PARAM_NAME + ": (" + PARAM_VALUE + ")");
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(context);
    HeaderManager headerManager = new HeaderManager();
    headerManager.add(new Header(PARAM_NAME, PARAM_VALUE));
    replacer.process(sampler, Collections.singletonList(headerManager), null, vars);
    assertThat(headerManager.getHeader(0))
        .isEqualTo(new Header(PARAM_NAME, "${" + REFERENCE_NAME + "}"));
  }

  @Test
  public void shouldNotReplaceHeaderManagerValueWhenRegexDoesNotMatch() {
    replacer = new RegexCorrelationReplacement<>("(" + PARAM_VALUE + ")");
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(context);
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
  public void shouldNotReplaceValueInRequestPathWhenRegexIsEmpty() {
    replacer = new RegexCorrelationReplacement<>("");
    replacer.setVariableName(REFERENCE_NAME);
    String originalPath = sampler.getPath();
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(sampler.getPath())
        .isEqualTo(originalPath);
  }

  @Test
  public void shouldNotReplaceValueInRequestPathWhenRegexDoesNotHaveAnyGroup() {
    replacer = new RegexCorrelationReplacement<>(REQUEST_REGEX_WITHOUT_GROUP);
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(context);
    String originalPath = sampler.getPath();
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(sampler.getPath())
        .isEqualTo(originalPath);
  }

  @Test
  public void shouldNotReplaceValueInRequestPathWhenRegexMatchesButVariableValueIsDifferent() {
    vars.put(REFERENCE_NAME, "Other");
    String originalPath = sampler.getPath();
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(sampler.getPath()).isEqualTo(originalPath);
  }

  @Test
  public void shouldReplaceValueInArgumentWhenRegexMatches() {
    sampler.addArgument(PARAM_NAME, PARAM_VALUE);
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(getFirstArgumentValue())
        .isEqualTo("${" + REFERENCE_NAME + "}");
  }

  @Test
  public void shouldReplaceValueInArgumentWhenRegexMatchesOneTime() {
    vars.put(REFERENCE_NAME, PARAM_VALUE);
    when(context.getVariableCount(REFERENCE_NAME)).thenReturn(0);
    validateReplacement("${" + REFERENCE_NAME + "}");
  }

  private void validateReplacement(String refName) {
    sampler.addArgument(PARAM_NAME, PARAM_VALUE);
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(getFirstArgumentValue())
        .isEqualTo(refName);
  }

  @Test
  public void shouldReplaceValueInArgumentWhenRegexMatchesMultipleTimesOnePerRequest() {
    vars = new JMeterVariables();
    vars.put(REFERENCE_NAME + "#1", "Other");
    vars.put(REFERENCE_NAME + "#2", PARAM_VALUE);
    vars.put(REFERENCE_NAME + "#3", "Other");
    when(context.getVariableCount(REFERENCE_NAME)).thenReturn(3);
    validateReplacement("${" + REFERENCE_NAME + "#2}");
  }

  @Test
  public void shouldReplaceValueInArgumentWhenRegexMatchesMultipleTimesPerRequest() {
    vars = new JMeterVariables();
    vars.put(REFERENCE_NAME + "#1_1", "Other");
    vars.put(REFERENCE_NAME + "#1_2", "Other");
    vars.put(REFERENCE_NAME + "#1_matchNr", "2");
    vars.put(REFERENCE_NAME + "#2_1", "Other");
    vars.put(REFERENCE_NAME + "#2_2", PARAM_VALUE);
    vars.put(REFERENCE_NAME + "#2_matchNr", "2");
    vars.put(REFERENCE_NAME + "#3", "Other");
    when(context.getVariableCount(REFERENCE_NAME)).thenReturn(3);
    validateReplacement("${" + REFERENCE_NAME + "#2_2}");
  }

  @Test
  public void shouldNotReplaceValueInArgumentWhenRegexMatchesButVariableValueIsDifferent() {
    vars.put(REFERENCE_NAME, "Other");
    when(context.getVariableCount(REFERENCE_NAME)).thenReturn(0);
    validateReplacement(PARAM_VALUE);
  }

  @Test
  public void shouldReplaceMatchForReplacementStringWhenIgnoreValue() {
    String replacementString = "${__changeCase(\"test\", UPPER)}";
    replacer =
        buildFunctionReplacement(replacementString, true);
    replacer.process(sampler, Collections.emptyList(), null, new JMeterVariables());
    assertThat(getFirstArgumentValue()).isEqualTo(replacementString);
  }

  private RegexCorrelationReplacement<BaseCorrelationContext> buildFunctionReplacement(
      String replacementString, boolean ignoreValue) {
    RegexCorrelationReplacement<BaseCorrelationContext> replacement =
        new RegexCorrelationReplacement<>(REQUEST_REGEX,
            replacementString, Boolean.toString(ignoreValue));
    replacement.setContext(context);
    replacement.setVariableName(REFERENCE_NAME);
    sampler.addArgument(PARAM_NAME, PARAM_VALUE);
    replacement.setExpressionEvaluator(expressionEvaluation);
    return replacement;
  }

  @Test
  public void shouldNotReplaceMatchForReplacementStringWhenNotIgnoreValueAndEvaluationNotMeet() {
    String replacementString = "${__RandomString(5)}";
    replacer = buildFunctionReplacement(replacementString, false);
    when(expressionEvaluation.apply(replacementString)).thenReturn("4");
    replacer.process(sampler, Collections.emptyList(), null, new JMeterVariables());
    assertThat(getFirstArgumentValue()).isEqualTo(PARAM_VALUE);
  }

  private String getFirstArgumentValue() {
    return sampler.getArguments().getArgument(0).getValue();
  }

  @Test
  public void shouldReplaceMatchWhenNotIgnoreValueAndEvaluationMeets() {
    String replacementString = "${__javaScript('1' + '2' + '3')}";
    replacer = buildFunctionReplacement(replacementString, false);
    when(expressionEvaluation.apply(replacementString)).thenReturn("123");
    replacer.process(sampler, Collections.emptyList(), null, new JMeterVariables());
    assertThat(getFirstArgumentValue()).isEqualTo(replacementString);
  }

  @Test
  public void shouldReplaceMatchWhenNotIgnoreValueWithInnerVarsAndEvaluationMeets() {
    String replacementString = "${__javaScript(${" + REFERENCE_NAME + "} + '3')}";
    vars = new JMeterVariables();
    vars.put(REFERENCE_NAME, "12");
    replacer = buildFunctionReplacement(replacementString, false);
    when(expressionEvaluation.apply(replacementString)).thenReturn("123");
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(getFirstArgumentValue()).isEqualTo(replacementString);
  }

  @Test
  public void shouldReplaceMatchWhenNotIgnoreValueWithMultiInnerVarAndEvaluationMeets() {
    vars = new JMeterVariables();
    vars.put(REFERENCE_NAME, "\"NOT_FOUND\"");
    vars.put(REFERENCE_NAME + "#1_1", "213");
    vars.put(REFERENCE_NAME + "#1_2", "12");
    vars.put(REFERENCE_NAME + "#1_matchNr", "2");
    String replacementString = "${__javaScript(${" + REFERENCE_NAME + "} + '3')}";
    replacer = buildFunctionReplacement(replacementString, false);
    doReturn(1).when(context).getVariableCount(anyString());
    when(expressionEvaluation.apply(anyString()))
        .thenReturn("${__javaScript('NOT_FOUND3')}")
        .thenReturn("2133");
    when(expressionEvaluation.apply("${__javaScript(${" + REFERENCE_NAME + "#1_2} + '3')}"))
        .thenReturn("123");
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(getFirstArgumentValue())
        .isEqualTo("${__javaScript(${TEST_SWEACN#1_2} + '3')}");
  }

  @Test
  public void shouldNotRemoveEqualSignsFromArgumentsValue() {
    Arguments arguments = new Arguments();
    String argumentValue = "{\"arg1\":\"value = 1\"}";
    arguments.addArgument("", argumentValue);

    sampler.setArguments(arguments);
    replacer.process(sampler, Collections.singletonList(
        createRegexExtractor(PARAM_NAME + "=" + PARAM_VALUE + "&")), null, vars);
    assertThat(sampler.getArguments().getArgument(0).getValue()).isEqualTo(argumentValue);
  }
}
