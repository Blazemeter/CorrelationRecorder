package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.JsonCorrelationExtractor;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.jmeter.samplers.SampleResult;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JsonBodyExtractorTest {
  private static final String FIRST_PATH =
      "$.objects.attributes.Transaction.TransactionsHelper_Account.value";
  private static final String NAME = "guids";
  private static final String VALUE = "43628621391136691";
  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Mock
  private SampleResult result;
  private JsonBodyExtractor extractor;

  private static JsonCorrelationExtractor<BaseCorrelationContext> getExtractor(String path) {
    return new JsonCorrelationExtractor<>(path, NAME);
  }

  @Before
  public void setUp() throws Exception {
    extractor = new JsonBodyExtractor();
  }

  @Test
  public void testGetCorrelationExtractors() throws IOException {
    String response = TestUtils.getFileContent("/responses/json/mendixJson.json", getClass());
    when(result.getResponseDataAsString()).thenReturn(response);

    List<CorrelationExtractor<?>> extractors =
        extractor.getCorrelationExtractors(result, VALUE, NAME);
    List<CorrelationExtractor<?>> expectedExtractors = generateExpectedExtractors();

    for (int i = 0; i < extractors.size(); i++) {
      softly.assertThat(extractors.get(i))
          .isNotNull()
          .isEqualToComparingFieldByFieldRecursively(expectedExtractors.get(i));
    }
  }

  private List<CorrelationExtractor<?>> generateExpectedExtractors() {
    return Arrays.asList(
        getExtractor("$.objects[0].attributes.Transaction.TransactionsHelper_Account.value"),
        getExtractor("$.user.attributes.System.changedBy.value"),
        getExtractor("$.user.attributes.System.owner.value"),
        getExtractor("$.user.guid"));
  }

  @Test
  public void shouldGenerateUnaffectedPathWhenGetJsonPathFromResponseWithVariableUseContains() throws IOException {
    String response = TestUtils.getFileContent("/responses/json/mendixJson.json", getClass());
    Configuration configuration = new Configuration();
    ExtractorGenerator extractorGenerator = new ExtractorGenerator(configuration, NAME, VALUE);
    String pathWithContains = extractorGenerator.getJsonPathFromResponse(response, VALUE, true);
    String pathWithoutContains = extractorGenerator.getJsonPathFromResponse(response, VALUE, false);

    softly.assertThat(pathWithContains).isEqualTo(FIRST_PATH);
    softly.assertThat(pathWithoutContains).isEqualTo(FIRST_PATH);
  }
}