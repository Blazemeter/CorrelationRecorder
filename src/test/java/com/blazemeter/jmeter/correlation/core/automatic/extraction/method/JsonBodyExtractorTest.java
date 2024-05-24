package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.JsonCorrelationExtractor;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minidev.json.JSONArray;
import org.apache.jmeter.samplers.SampleResult;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(Parameterized.class)
public class JsonBodyExtractorTest {

  private static final String FIRST_PATH =
      "$.objects.attributes.Transaction.TransactionsHelper_Account.value";
  private static final String NAME = "guids";
  private static final String VALUE = "43628621391136691";
  private static final String TOKEN_2 = "randomToken2";
  private static final String RESOURCES_PATH = "/responses/json/";
  private static final String TOKEN_1 = "randomToken1";
  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private SampleResult result;
  private JsonBodyExtractor extractor;
  @Parameter
  public String jsonFilePath;
  @Parameter(1)
  public String value;
  @Parameter(2)
  public List<CorrelationExtractor<?>> expectedExtractors;

  private String response;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"mendixJson.json", VALUE, generateMendixExpectedExtractors()},
        {"single-object.json", TOKEN_1, getSingletonExtractorList("$.token")},
        {"object-list.json", TOKEN_2, getSingletonExtractorList("$..token")},
        {"object-list-inner-object.json", TOKEN_2, getSingletonExtractorList("$..session.token")},
        {"object-list-with-duplicated-values.json", TOKEN_1, getSingletonExtractorList("$..token")},
        {"object-with-inner-list.json", TOKEN_2, getSingletonExtractorList("$.sessions..token")},
        {"single-object-integer-value.json", "5", getSingletonExtractorList("$.id")},
        {"object-with-integer-key.json", TOKEN_1, getSingletonExtractorList("$.15")},
        {"object-with-special-characters.json", TOKEN_1, getSingletonExtractorList("$.['URL$'].['test.com'].['..internal.token=:']")}
    });
  }

  private static List<CorrelationExtractor<?>> generateMendixExpectedExtractors() {
    return Collections.singletonList(
        getExtractor("$.objects..attributes.['Transaction.TransactionsHelper_Account'].value"));
  }

  private static JsonCorrelationExtractor<BaseCorrelationContext> getExtractor(String path) {
    JsonCorrelationExtractor<BaseCorrelationContext> baseCorrelationContextJsonCorrelationExtractor = new JsonCorrelationExtractor<>(
        path, NAME);
    baseCorrelationContextJsonCorrelationExtractor.setMatchNr(-1);
    baseCorrelationContextJsonCorrelationExtractor.setMultiValued(true);
    return baseCorrelationContextJsonCorrelationExtractor;
  }

  private static List<JsonCorrelationExtractor<BaseCorrelationContext>> getSingletonExtractorList(
      String s) {
    return Collections.singletonList(getExtractor(s));
  }

  @Before
  public void setUp() throws Exception {
    extractor = new JsonBodyExtractor();
    response = TestUtils.getFileContent(RESOURCES_PATH + jsonFilePath, getClass());
    when(result.getResponseDataAsString()).thenReturn(response);
  }

  @Test
  public void testGetCorrelationExtractors() {
    List<CorrelationExtractor<?>> extractors =
        extractor.getCorrelationExtractors(result, value, NAME);

    softly.assertThat(extractors.size()).isEqualTo(expectedExtractors.size());
    softly.assertThat(extractors).isEqualTo(expectedExtractors);
  }

  @Test
  public void shouldGetTeasedValueWhenExecutingJSONPathExpression() {
    List<CorrelationExtractor<?>> correlationExtractors = extractor.getCorrelationExtractors(result,
        value, NAME);
    assert correlationExtractors.size() == 1; //avoid IDE warnings
    CorrelationExtractor<?> correlationExtractor = correlationExtractors.get(0);
    assert correlationExtractor instanceof JsonCorrelationExtractor; //avoid IDE warnings
    JsonCorrelationExtractor<?> jsonCorrelationExtractor =
        (JsonCorrelationExtractor<?>) correlationExtractor;

    String jsonPath = jsonCorrelationExtractor.getPath();

    Object actualValue = JsonPath.parse(response).read(jsonPath);
    if (actualValue instanceof String) {
      assertThat(actualValue).isEqualTo(value);
    } else if(actualValue instanceof Integer) {
      assertThat(actualValue).isEqualTo(Integer.parseInt(value));
    } else if (actualValue instanceof JSONArray) {
      assertThat((JSONArray) actualValue).contains(value);
    }
  }

  @Test
  public void shouldGenerateUnaffectedPathWhenGetJsonPathFromResponseWithVariableUseContains()
      throws IOException {
    String response = TestUtils.getFileContent(RESOURCES_PATH + "mendixJson.json", getClass());
    Configuration configuration = new Configuration();
    ExtractorGenerator extractorGenerator = new ExtractorGenerator(configuration, NAME, VALUE);
    String pathWithContains = extractorGenerator.getJsonPathFromResponse(response, VALUE, true);
    String pathWithoutContains = extractorGenerator.getJsonPathFromResponse(response, VALUE, false);

    softly.assertThat(pathWithContains).isEqualTo(FIRST_PATH);
    softly.assertThat(pathWithoutContains).isEqualTo(FIRST_PATH);
  }
}
