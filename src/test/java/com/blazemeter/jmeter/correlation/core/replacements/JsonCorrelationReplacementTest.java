package com.blazemeter.jmeter.correlation.core.replacements;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.extractors.JsonCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.bridge.SLF4JBridgeHandler;

@RunWith(MockitoJUnitRunner.class)
public class JsonCorrelationReplacementTest {

  private static final String REFERENCE_NAME = "TEST_SWEACN";
  private static final String PARAM_VALUE = "123";
  private static final String REQUEST_JSONPATH = "$.arg1";
  private static final String DEFAULT_SAMPLER_JSON = "{\"arg1\":\"123\"}";
  private static final Object EXPECTED_REPLACED_JSON =
      "{\"arg1\":\"${" + REFERENCE_NAME + "}\"}";
  private JsonCorrelationReplacement<BaseCorrelationContext> replacer;
  private JMeterVariables vars;
  private HTTPSampler sampler;
  @Mock
  private BaseCorrelationContext replaceContext;
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
    sampler.setMethod("POST");
    sampler.setPath("/");
    sampler.setPostBodyRaw(true);

    Arguments arguments = new Arguments();
    arguments.addArgument("", DEFAULT_SAMPLER_JSON);
    sampler.setArguments(arguments);
    vars = new JMeterVariables();
    vars.put(REFERENCE_NAME, PARAM_VALUE);

  }

  @Test
  public void shouldReplaceJsonValueWithReferenceWhenJsonPathMatch() {

    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);

    // Execute the replacement with a valid JSON path
    processJonReplacement(extractor, REQUEST_JSONPATH);

    // Get if the value of the argument of the sample was updated wit the reference variable
    assertThat(getFirstArgumentValue()).isEqualTo(EXPECTED_REPLACED_JSON);
  }

  @Test
  public void shouldReplaceJsonValueWithReferenceWhenJsonPathMatchAndUseReplaceStringWithIgnoreValue() {

    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);

    String replacementString = "${__changeCase(\"test\", UPPER)}";

    // Execute the replacement with a valid JSON path
    processJonReplacement(extractor, REQUEST_JSONPATH, replacementString, true);

    // Get if the value of the argument of the sample was updated wit the reference variable
    assertThat(getFirstArgumentValue()).isEqualTo(
        "{\"arg1\":\"" + escapeJsonValue(replacementString) + "\"}");
  }

  @Test
  public void shouldNotReplaceMatchForReplacementStringWhenNotIgnoreValueAndEvaluationNotMeet() {
    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);

    String replacementString = "${__RandomString(5)}";

    Arguments arguments = new Arguments();
    String samplerJsonNotMatch = "{\"arg1\":\"321\"}";
    arguments.addArgument("", samplerJsonNotMatch);
    sampler.setArguments(arguments);

    // Execute the replacement, but extractor not match with replacement and ignore is false
    processJonReplacement(extractor, REQUEST_JSONPATH, replacementString, false);

    replacer.process(sampler, Collections.emptyList(), null, new JMeterVariables());
    Assertions.assertThat(getFirstArgumentValue()).isEqualTo(samplerJsonNotMatch);
  }

  @Test
  public void shouldReplaceMatchWhenNotIgnoreValueAndEvaluationMeets() {
    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);

    String replacementString = "${__javaScript('1' + '2' + '3')}";
    processJonReplacement(extractor, REQUEST_JSONPATH, replacementString, false);

    Assertions.assertThat(getFirstArgumentValue()).isEqualTo(
        "{\"arg1\":\"" + escapeJsonValue(replacementString) + "\"}");
  }

  @Test
  public void shouldReplaceMatchWhenNotIgnoreValueWithInnerVarsAndEvaluationMeets() {
    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);
    String replacementString = "${__javaScript(${" + REFERENCE_NAME + "} + '3')}";
    processJonReplacement(extractor, REQUEST_JSONPATH, replacementString, false);
    Assertions.assertThat(getFirstArgumentValue())
        .isEqualTo("{\"arg1\":\"" + escapeJsonValue(replacementString) + "\"}");
  }

  @Test
  public void shouldReplaceMatchWhenNotIgnoreValueWithMultiInnerVarAndEvaluationMeets() {
    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);
    String replacementString = "${__javaScript(${" + REFERENCE_NAME + "#1_2} + '3')}";
    // Simulate multi value variables, overwrite context counter to 1
    doReturn(1).when(replaceContext).getVariableCount(anyString());
    JMeterVariables ctx_vars = context.getVariables();
    vars.put(REFERENCE_NAME, "\"NOT_FOUND\"");
    vars.put(REFERENCE_NAME + "#1_1", "213");
    vars.put(REFERENCE_NAME + "#1_2", "123");
    vars.put(REFERENCE_NAME + "#1_matchNr", "2");

    processJonReplacement(extractor, REQUEST_JSONPATH, replacementString, false);

    String replacementStringExpected = "${__javaScript(${" + REFERENCE_NAME + "#1_2} + '3')}";
    Assertions.assertThat(getFirstArgumentValue()).isEqualTo(
        "{\"arg1\":\"" + escapeJsonValue(replacementStringExpected) + "\"}");
  }


  @Test
  public void shouldReplaceMatchWithoutQuotesWhenReplacedValueIsWithoutQuotes() {

    String extractJson = "{\"arg1\": [111, 222, 333]}";
    String extractJsonPath = "$.arg1";

    String replaceJson = "{\"arg1\":[333, 222, 111]}";
    String replaceJsonPath = "$.arg1";

    String expectedJson = "{\"arg1\":[${TEST_SWEACN_3},${TEST_SWEACN_2},${TEST_SWEACN_1}]}";

    SampleResult sampleResult = new SampleResult();
    sampleResult.setResponseData(extractJson, SampleResult.DEFAULT_HTTP_ENCODING);
    sampleResult.setContentType(ContentType.APPLICATION_JSON.toString());

    List<String> params = Arrays
        .asList(
            extractJsonPath,  // JSONPath
            "-1", // Match Number
            ResultField.BODY.name(), // Body
            "false"); // Multivalue False

    JsonCorrelationExtractor<?> jsonExtractor = new JsonCorrelationExtractor<>();
    jsonExtractor.setParams(params);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    JMeterVariables vars = new JMeterVariables();
    jsonExtractor.process(null, children,
        sampleResult,
        vars);

    replacer = new JsonCorrelationReplacement<>(replaceJsonPath);

    HTTPSampler postSampler = new HTTPSampler();
    postSampler.setMethod("POST");
    postSampler.setPath("/");
    postSampler.setPostBodyRaw(true);

    Arguments arguments = new Arguments();
    arguments.addArgument("", replaceJson);
    postSampler.setArguments(arguments);

    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(replaceContext);
    replacer.setExpressionEvaluator(expressionEvaluation);
    replacer.process(postSampler, Collections.singletonList(postSampler), sampleResult, vars);

    String jsonUpated = postSampler.getArguments().getArgument(0).getValue();

    assertThat(jsonUpated).isEqualTo(expectedJson);

  }

  @Test
  public void shouldReplaceMatchWhenJsonUseVariablesWithoutQuotes() {

    String extractJson = "{\"arg1\": [111, 222, 333]}";
    String extractJsonPath = "$.arg1";

    String replaceJson = "{\"arg1\":[333, 222, 111, ${var_2}], \"arg2\": ${var_1}}";
    String replaceJsonPath = "$.arg1";

    String expectedJson =
        "{\"arg1\":[${TEST_SWEACN_3},${TEST_SWEACN_2},${TEST_SWEACN_1},${var_2}],\"arg2\":${var_1}}";

    SampleResult sampleResult = new SampleResult();
    sampleResult.setResponseData(extractJson, SampleResult.DEFAULT_HTTP_ENCODING);
    sampleResult.setContentType(ContentType.APPLICATION_JSON.toString());

    List<String> params = Arrays
        .asList(
            extractJsonPath,  // JSONPath
            "-1", // Match Number
            ResultField.BODY.name(), // Body
            "false"); // Multivalue False

    JsonCorrelationExtractor<?> jsonExtractor = new JsonCorrelationExtractor<>();
    jsonExtractor.setParams(params);
    jsonExtractor.setVariableName(REFERENCE_NAME);
    List<TestElement> children = new ArrayList<>();
    JMeterVariables vars = new JMeterVariables();
    jsonExtractor.process(null, children,
        sampleResult,
        vars);

    replacer = new JsonCorrelationReplacement<>(replaceJsonPath);

    HTTPSampler postSampler = new HTTPSampler();
    postSampler.setMethod("POST");
    postSampler.setPath("/");
    postSampler.setPostBodyRaw(true);

    Arguments arguments = new Arguments();
    arguments.addArgument("", replaceJson);
    postSampler.setArguments(arguments);

    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(replaceContext);
    replacer.setExpressionEvaluator(expressionEvaluation);
    replacer.process(postSampler, Collections.singletonList(postSampler), sampleResult, vars);

    String jsonUpated = postSampler.getArguments().getArgument(0).getValue();

    assertThat(jsonUpated).isEqualTo(expectedJson);

  }


  @Test
  public void shouldNotReplaceJsonValueWithReferenceWhenJsonPathDoesNotMatch() {

    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);

    // Execute the replacement with a json path that doesn't match
    processJonReplacement(extractor, "$.arg2");

    // Get if the value of the argument of the sample not was updated wit the reference variable
    assertThat(getFirstArgumentValue()).isEqualTo(DEFAULT_SAMPLER_JSON);
  }

  @Test
  public void shouldNotReplaceJsonValueWithReferenceWhenJsonPathIsInvalid() {

    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);

    // Execute the replacement with an invalid json path
    processJonReplacement(extractor, "Invalid.JSON.Path");

    // Get if the value of the argument of the sample not was updated wit the reference variable
    assertThat(getFirstArgumentValue()).isEqualTo(DEFAULT_SAMPLER_JSON);
  }

  @Test
  public void shouldNotReplaceJsonValueWithReferenceWhenJsonPathIsEmpty() {

    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);

    // Execute the replacement.with an empty JSON path
    processJonReplacement(extractor, "");

    // Get if the value of the argument of the sample not was updated wit the reference variable
    assertThat(getFirstArgumentValue()).isEqualTo(DEFAULT_SAMPLER_JSON);
  }

  @Test
  public void shouldNotReplaceJsonValueWithReferenceWhenReferenceValueIsDifferent() {

    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);

    // Execute the replacement.with an empty JSON path
    vars.put(REFERENCE_NAME, "Other");
    processJonReplacement(extractor, "");

    // Get if the value of the argument of the sample not was updated wit the reference variable
    assertThat(getFirstArgumentValue()).isEqualTo(DEFAULT_SAMPLER_JSON);
  }

  @Test
  public void shouldReplaceJsonValueWithReferenceWhenMatchOneTime() {

    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor extractor =
        createJsonExtractor(context, "1", true);

    // Execute the replacement.with an empty JSON path
    processJonReplacement(extractor, REQUEST_JSONPATH);

    // Get if the value of the argument of the sample not was updated wit the reference variable
    assertThat(getFirstArgumentValue()).isEqualTo(EXPECTED_REPLACED_JSON);
  }

  private String escapeJsonValue(String value) {
    return value.replaceAll("\"", "\\\\\"");
  }

  private String getFirstArgumentValue() {
    return sampler.getArguments().getArgument(0).getValue();
  }

  private void processJonReplacement(JSONPostProcessor extractor, String jsonPath) {
    replacer = new JsonCorrelationReplacement<>(jsonPath);
    processJonReplacement(extractor);
  }

  private void processJonReplacement(JSONPostProcessor extractor, String jsonPath,
      String replacementString, boolean ignoreValue) {
    replacer = new JsonCorrelationReplacement<>(jsonPath, replacementString,
        Boolean.toString(ignoreValue));
    replacer.setExpressionEvaluator(expressionEvaluation);
    when(expressionEvaluation.apply(replacementString)).thenReturn("123");
    processJonReplacement(extractor);
  }

  private void processJonReplacement(JSONPostProcessor extractor) {
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(replaceContext);
    replacer.setExpressionEvaluator(expressionEvaluation);
    replacer.process(sampler, Collections.singletonList(extractor), null, vars);
  }

  private static JSONPostProcessor createJsonExtractor(JMeterContext context, String matchNumbers,
      boolean computeConcatenation) {
    String VAR_NAME = "varName";
    JSONPostProcessor processor = new JSONPostProcessor();
    processor.setThreadContext(context);
    processor.setRefNames(VAR_NAME);
    processor.setMatchNumbers(matchNumbers);
    processor.setComputeConcatenation(computeConcatenation);

    // The Extractor get the value 123 from JSON path $.hello.origin
    JMeterVariables vars = new JMeterVariables();
    processor.setDefaultValues("NOT_FOUND");
    processor.setJsonPathExpressions("$.hello.origin");
    processor.setRefNames(REFERENCE_NAME);
    processor.setScopeVariable("contentvar");
    context.setVariables(vars);
    String jsonResponse = "{\"hello\":{\"origin\":\"123\"}}";
    vars.put("contentvar", jsonResponse);

    return processor;
  }
}
