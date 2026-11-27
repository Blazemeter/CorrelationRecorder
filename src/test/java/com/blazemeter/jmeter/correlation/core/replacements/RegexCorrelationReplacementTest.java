package com.blazemeter.jmeter.correlation.core.replacements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.automatic.Sources;
import com.blazemeter.jmeter.correlation.core.automatic.replacement.method.ReplacementRegex;
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
  public void shouldReplaceOnlyTheTokenOfBearerCredentials() {
    assertCredentialsTokenReplaced("Authorization", "Bearer ");
  }

  @Test
  public void shouldReplaceCredentialsTokenWhenFieldNameAndSchemeInLowerCase() {
    // HTTP/2 sends field names in lower case; names and schemes are case-insensitive (RFC 9110)
    assertCredentialsTokenReplaced("authorization", "bearer ");
  }

  @Test
  public void shouldReplaceAlphanumericIdInTheMiddleOfThePath() {
    String cartId = "cart_62704d1930ac";
    vars.put(REFERENCE_NAME, cartId);
    replacer = new RegexCorrelationReplacement<>(
        ReplacementRegex.match("carts", Sources.REQUEST_PATH_ALPHANUMERIC_ID));
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(context);
    sampler.setPath("/api/carts/" + cartId + "/items");
    replacer.process(sampler, Collections.emptyList(), null, vars);
    assertThat(sampler.getPath()).isEqualTo("/api/carts/${" + REFERENCE_NAME + "}/items");
  }

  @Test
  public void shouldReferToThePathIdInTheNameAsAFixedReferenceAndKeepTheComment() {
    // JMeter's recorder names a sampler after its path ("{prefix}{path}-{counter}"); ACR identifies
    // the sampler by the ORIGINAL_NAME in its comment, which stays
    String cartId = "cart_62704d1930ac";
    String comment = "ORIGINAL_NAME USED BY CR, DON'T DELETE. Prepend addition comments if"
        + " necessary.;ORIGINAL_NAME=/api/carts/" + cartId + "/items-102";
    sampler.setComment(comment);
    replacePathId("carts", cartId, "/api/carts/" + cartId + "/items",
        "/api/carts/" + cartId + "/items-102");
    assertThat(sampler.getPath()).isEqualTo("/api/carts/${" + REFERENCE_NAME + "}/items");
    assertThat(sampler.getName()).isEqualTo("/api/carts/[" + REFERENCE_NAME + "]/items-102");
    assertThat(sampler.getComment()).isEqualTo(comment);
  }

  @Test
  public void shouldReplaceThePathIdBeforeTheRecordingNumberInTheName() {
    replacePathId("orders", "ord_6301cb020b20", "/api/orders/ord_6301cb020b20",
        "/api/orders/ord_6301cb020b20-35");
    assertThat(sampler.getName()).isEqualTo("/api/orders/[" + REFERENCE_NAME + "]-35");
  }

  @Test
  public void shouldReplaceAPathIdEndingInANumberInANameWithoutRecordingNumber() {
    replacePathId("orders", "ord-1001", "/api/orders/ord-1001", "/api/orders/ord-1001");
    assertThat(sampler.getName()).isEqualTo("/api/orders/[" + REFERENCE_NAME + "]");
  }

  @Test
  public void shouldReplaceThePathIdInANameWithJMeterFreeFormat() {
    // "#{counter,number,000} - #{path} (#{name})", JMeter's default free format
    replacePathId("orders", "ord_6301cb020b20", "/api/orders/ord_6301cb020b20",
        "035 - /api/orders/ord_6301cb020b20 (Lumen)");
    assertThat(sampler.getName()).isEqualTo("035 - /api/orders/[" + REFERENCE_NAME + "] (Lumen)");
  }

  @Test
  public void shouldTurnOnlyTheAddedReferencesOfANameIntoFixedOnes() {
    assertThat(CorrelationReplacement.asNameReferences("/users/${user}/orders/ord_1",
        "/users/${user}/orders/${__urlencode(${order})}"))
        .isEqualTo("/users/${user}/orders/[__urlencode([order])]");
  }

  @Test
  public void shouldReplaceTheEntityArgumentInTheRequestAndInTheReferer() {
    // The Referer is not a candidate, but the rule of the argument it carries replaces it too
    String orderId = "ord_6301cb020b20";
    vars.put(REFERENCE_NAME, orderId);
    replacer = new RegexCorrelationReplacement<>(
        ReplacementRegex.match("order.id", Sources.REQUEST_ENTITY_ARGUMENT));
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(context);
    sampler.setPath("/order.html");
    sampler.addArgument("id", orderId);
    HeaderManager headerManager = new HeaderManager();
    headerManager.add(new Header("Referer", "http://localhost:8090/order.html?id=" + orderId));
    replacer.process(sampler, Collections.singletonList(headerManager), null, vars);
    assertThat(getFirstArgumentValue()).isEqualTo("${" + REFERENCE_NAME + "}");
    assertThat(headerManager.getHeader(0).getValue())
        .isEqualTo("http://localhost:8090/order.html?id=${" + REFERENCE_NAME + "}");
  }

  private void replacePathId(String concept, String id, String path, String name) {
    vars.put(REFERENCE_NAME, id);
    replacer = new RegexCorrelationReplacement<>(
        ReplacementRegex.match(concept, Sources.REQUEST_PATH_ALPHANUMERIC_ID));
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(context);
    sampler.setName(name);
    sampler.setPath(path);
    replacer.process(sampler, Collections.emptyList(), null, vars);
  }

  private void assertCredentialsTokenReplaced(String fieldName, String scheme) {
    replacer = new RegexCorrelationReplacement<>(
        ReplacementRegex.match("Authorization", Sources.REQUEST_HEADER_FIELDS));
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(context);
    HeaderManager headerManager = new HeaderManager();
    headerManager.add(new Header(fieldName, scheme + PARAM_VALUE));
    replacer.process(sampler, Collections.singletonList(headerManager), null, vars);
    assertThat(headerManager.getHeader(0))
        .isEqualTo(new Header(fieldName, scheme + "${" + REFERENCE_NAME + "}"));
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
