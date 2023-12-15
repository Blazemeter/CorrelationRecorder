package com.blazemeter.jmeter.correlation.core.automatic;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.extractor.XPath2Extractor;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.functions.RegexFunction;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExtractorGeneratorTest {
  private static final String VAL_NAME = "value";
  private static final String VAL_NAME_NR = "value_matchNr";
  private ExtractorGenerator generator;

  private XPath2Extractor extractor;
  private SampleResult result;
  private String data;
  private JMeterVariables vars;
  private JMeterContext jmctx;

  @Before
  public void setup() {

  }

  @Test
  public void shouldGenerateJsonPath() throws IOException {
    String name = "objectType";
    String value = "DTAP.DTAPInformation";
    Configuration configuration = new Configuration();
    generator = new ExtractorGenerator(configuration, name, value);
    String content = TestUtils.getFileContent("/responses/json/response.json", getClass());
    String path = generator.getJsonPathFromResponse(content, value, true);
    assertThat(path).isEqualTo("$.metadata.objectType");
  }

  @Test
  public void shouldGenerateCorrectJsonPath() throws IOException {
    String variableName = "varName";
    JMeterContext context = JMeterContextService.getContext();
    JSONPostProcessor processor = setupProcessor(context, "1", true);
    JMeterVariables vars = new JMeterVariables();
    processor.setDefaultValues("NOT_FOUND");
    processor.setJsonPathExpressions("$.metadata[:3].attributes.PageNumber");
    processor.setRefNames(variableName);
    processor.setScopeVariable("contentvar");
    context.setVariables(vars);
    String jsonResponse = TestUtils.getFileContent("/responses/json/response.json", getClass());
    vars.put("contentvar", jsonResponse);
    processor.process();
    assertEquals("{\"type\":\"Long\"}", vars.get(variableName));
  }

  private static JSONPostProcessor setupProcessor(JMeterContext context,
                                                  String matchNumbers, boolean computeConcatenation) {
    String VAR_NAME = "varName";
    JSONPostProcessor processor = new JSONPostProcessor();
    processor.setThreadContext(context);
    processor.setRefNames(VAR_NAME);
    processor.setMatchNumbers(matchNumbers);
    processor.setComputeConcatenation(computeConcatenation);
    return processor;
  }

//  @Test
//  public void shouldGenerateXpath() throws IOException {
//    String name = "sort";
//    String value = "IsInTeam";
//    Configuration configuration = new Configuration();
//    generator = new ExtractorGenerator(configuration, name, value);
//    String content = TestUtils.getFileContent("/responses/xml/cleanResponse.xml", getClass());
//    String xmlPath = generator.getXmlPath(content, value, name);
//    String expectedPath = "/m:page/m:templates/m:template/div/div/div/div/@data-mendix-props";
//    assertThat(xmlPath).isEqualTo(expectedPath);
//  }

  @Test
  public void shouldExtractValueUsingGeneratedXpath() throws IOException {

    JMeterContext context = JMeterContextService.getContext();

    XPath2Extractor extractor = new XPath2Extractor();
    extractor.setThreadContext(context);// This would be done by the run command
    extractor.setRefName(VAL_NAME);
    extractor.setDefaultValue(VAL_NAME + "_NOT_FOUND");
//    extractor.setTolerant(true);
//    extractor.setReportErrors(true);
    extractor.setMatchNumber(1);

    SampleResult result = new SampleResult();
    String data = TestUtils.getFileContent("/responses/xml/dirtyResponse.xml", getClass());
//    String data = "<book><preface title='Intro'>zero</preface><page>one</page><page>two</page><empty></empty><a><b></b></a></book>";
    result.setResponseData(data.getBytes("UTF-8"));

    JMeterVariables vars = new JMeterVariables();
    context.setVariables(vars);
    context.setPreviousResult(result);
    //.//*[self::abstract or self::subject or self::note][position() <= 2]
//    String path = "//*[namespace-uri()='http://schemas.mendix.com/forms/1.0' and local-name()='argument']//div[@data-mendix-props]/@data-mendix-props";
//    String path = "//*[namespace-uri()='http://schemas.mendix.com/forms/1.0' and local-name()='argument']//div[@data-mendix-props]/@data-mendix-props";
//    String path = "//div[starts-with(@data-mendix-id, '382.Transaction.Transaction_Staff_Overview_Optimized')]/@data-mendix-props";
    String path = "//div[@data-mendix-props]/@data-mendix-props";
//    String path = "/m:page/m:templates/m:template/div/div/div/div/@data-mendix-props";
    extractor.setXPathQuery(path);
//    extractor.setXPathQuery("/book/preface/@title");

    // TODO:We need to investigate what version of jmeter runtime or how to test
    //extractor.process();
    //assertEquals("Intro", vars.get(VAL_NAME));
    //assertEquals("1", vars.get(VAL_NAME_NR));
    //assertEquals("Intro", vars.get(VAL_NAME + "_1"));
  }

  @Test
  public void testPreviousResultIsEmpty() throws Exception {
    JMeterVariables vars = new JMeterVariables();
    JMeterContext jmctx = JMeterContextService.getContext();
    JMeterContext jmc = JMeterContextService.getContext();
    XPath2Extractor extractor = new XPath2Extractor();
    extractor.setThreadContext(jmctx);// This would be done by the run command
    extractor.setRefName(VAL_NAME);
    extractor.setDefaultValue("Default");
    jmc.setPreviousResult(null);
    extractor.setXPathQuery("/book/preface");
    extractor.process();
    assertEquals(null, vars.get(VAL_NAME));
  }

  @Test
  public void testWithNamespace() throws Exception {
    SampleResult result = new SampleResult();
    result.setResponseData("<age:ag xmlns:age=\"http://www.w3.org/wgs84_pos#\"><head><title>test</title></head></age:ag>", null);


    vars = new JMeterVariables();
    JMeterContext jmctx = JMeterContextService.getContext();
    jmctx.setVariables(vars);
    jmctx.setPreviousResult(result);

    String namespaces = "age=http://www.w3.org/wgs84_pos#";
    String xPathQuery = "/age:ag/head/title";

    XPath2Extractor extractor = new XPath2Extractor();
    extractor.setThreadContext(jmctx);// This would be done by the run command
    extractor.setXPathQuery(xPathQuery);
    extractor.setNamespaces(namespaces);

    // TODO: we need to change the default runtime of jmeter to a more updated version to test it

    //extractor.process();

    //assertEquals("test", vars.get(VAL_NAME));
    //assertEquals("1", vars.get(VAL_NAME_NR));
    //assertEquals("test", vars.get(VAL_NAME + "_1"));
  }

  @Test
  public void shouldExtractValueUsingGeneratedXpath2() throws IOException {
    JMeterContext context = JMeterContextService.getContext();

    XPath2Extractor extractor = new XPath2Extractor();
    extractor.setThreadContext(context);// This would be done by the run command
    extractor.setRefName(VAL_NAME);
    extractor.setDefaultValue(VAL_NAME + "_NOT_FOUND");

    SampleResult result = new SampleResult();
    String data = TestUtils.getFileContent("/responses/xml/cleanResponse.xml", getClass());
//    String data = "<book><preface title='Intro'>zero</preface><page>one</page><page>two</page><empty></empty><a><b></b></a></book>";
    result.setResponseData(data.getBytes("UTF-8"));

    JMeterVariables vars = new JMeterVariables();
    context.setVariables(vars);
    context.setPreviousResult(result);
    //.//*[self::abstract or self::subject or self::note][position() <= 2]
    String path = "//*[namespace-uri()='http://schemas.mendix.com/forms/1.0' and local-name()='argument']//div[@data-mendix-props]/@data-mendix-props";
//    String path = "//div[starts-with(@data-mendix-id, '382.Transaction.Transaction_Staff_Overview_Optimized')]/@data-mendix-props";
//    String path = "//div[@data-mendix-props]/@data-mendix-props";
//    String path = "/m:page/m:templates/m:template/div/div/div/div/@data-mendix-props";
    extractor.setXPathQuery(path);
//    extractor.setXPathQuery("/book/preface/@title");

    // TODO: Investigate version od jmeter and how to test
    //extractor.process();
    //assertEquals("Intro", vars.get(VAL_NAME));
    //assertEquals("1", vars.get(VAL_NAME_NR));
    //assertEquals("Intro", vars.get(VAL_NAME + "_1"));
  }
  @Test
  public void should() {
    String name = "header-2";
    String value = "ABC";
    Configuration configuration = new Configuration();
    generator = new ExtractorGenerator(configuration, name, value);

  }

  @Test
  public void shouldGenerateLazyRegexExtractor() throws InvalidVariableException {
    String value = "ABC";
    String valueName = "header-2";

    String responseHeader = "header-1=QWE;\n" +
        "header-2=ABC;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;\n" +
        "header-3=XYW;\n" +
        "csrf_token: \n&quot;459581b016d5b6bed071a02&quot;ABCDEF\n" +
        "session_id: &quot;qweasdxcvfgh&quot;\n";
    generator = new ExtractorGenerator(new Configuration(), valueName, value);
    String contextString = generator.getContextString(responseHeader, value);
    RegexCorrelationExtractor<?> correlationExtractor = generator
        .generateExtractor(valueName, value, contextString, ResultField.BODY);

    assertEquals(value, getMatchedString(getRegex(correlationExtractor), responseHeader));
  }

  private String getMatchedString(String regex, String responseData)
      throws InvalidVariableException {
    SampleResult result = new SampleResult();
    result.setResponseData(responseData, null);

    JMeterContext context = JMeterContextService.getContext();
    context.setVariables(new JMeterVariables());
    context.setPreviousResult(result);

    Collection<CompoundVariable> params = makeParams(regex, "$1$", "1");
    RegexFunction variable = new RegexFunction();
    variable.setParameters(params);
    return variable.execute(result, null);
  }

  private String getRegex(RegexCorrelationExtractor<?> correlationExtractor) {
    //The first element of the params is the Regex
    return correlationExtractor.getParams().get(0);
  }

  private static Collection<CompoundVariable> makeParams(String... params) {
    return Stream.of(params)
        .map(CompoundVariable::new)
        .collect(Collectors.toList());
  }

  @Test
  public void shouldGenerateRegexSupportingNewLines() throws InvalidVariableException {
    String rawHeader = "Cache-Control: no-cache, must-revalidate, max-age=0\n" +
        "X-WP-Nonce: e617becc44\n" +
        "Allow: GET, POST, PUT, PATCH\n";
    String value = "e617becc44";
    String valueName = "X-WP-Nonce";
    generator = new ExtractorGenerator(new Configuration(), valueName, value);
    String contextString = generator.getContextString(rawHeader, value);
    RegexCorrelationExtractor<?> correlationExtractor = generator
        .generateExtractor(valueName, value, contextString, ResultField.RESPONSE_HEADERS);
    String matchedString = getMatchedString(getRegex(correlationExtractor), rawHeader);
    assertThat(matchedString).isEqualTo(value);
  }
}
