package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import static com.mongodb.util.MyAsserts.assertNotNull;
import static com.mongodb.util.MyAsserts.assertTrue;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RawTextBodyExtractorTest {

  private static final String REFERENCE_NAME = "Reference Name";
  private RawTextBodyExtractor rawExtractor;
  private ComparableJMeterVariables vars;
  private BaseCorrelationContext context;
  @Mock
  private SampleResult result;
  @Mock
  private HTTPSamplerBase sampler;

  @Before
  public void setUp() throws Exception {
    rawExtractor = new RawTextBodyExtractor(new Configuration());
    vars = new ComparableJMeterVariables();
    context = new BaseCorrelationContext();
  }

  @Test
  public void testGetCorrelationExtractors() throws IOException {
    String value = "5854b013f4";
    String name = "_wpnonce";
    String response = TestUtils.getFileContent("/responses/html/response.html", getClass());
    when(result.getResponseDataAsString()).thenReturn(response);

    List<CorrelationExtractor<?>> extractors =
        rawExtractor.getCorrelationExtractors(result, value, name);
    System.out.println("extractors: " + extractors.size());

    assertNotNull(extractors);
    assertTrue(extractors.size() == 3);
  }

  @Test
  public void shouldGenerateRegexExtractorsThatExtractsOnlyTheDesiredValue() throws IOException {
    String value = "5854b013f4";
    String name = "_wpnonce";
    String response = TestUtils.getFileContent("/responses/html/response.html", getClass());
    when(result.getResponseDataAsString()).thenReturn(response);

    List<CorrelationExtractor<?>> extractors =
        rawExtractor.getCorrelationExtractors(result, value, name);

    for (CorrelationExtractor<?> extractor : extractors) {
      if (!(extractor instanceof RegexCorrelationExtractor)) {
        continue;
      }
      vars.clear();
      System.out.println("Vars size before: " + vars.entrySet().size());
      List<TestElement> children = new ArrayList<>();
      HTTPSamplerBase sampler = new HTTPSampler();

      RegexCorrelationExtractor regexExtractor = (RegexCorrelationExtractor) extractor;
      regexExtractor.setContext(context);
      regexExtractor.process(sampler, children, result, vars);
      System.out.println("Vars size after: " + vars.entrySet().size());
      System.out.println("Content vars: " + vars);
      System.out.println("extractor: " + extractor);
      System.out.println("=====================================");
    }
  }

  @Test
  public void testGetContextString() throws IOException {
    String value = "5854b013f4";
    String name = "_wpnonce";
    String response = TestUtils.getFileContent("/responses/html/response.html", getClass());

    List<Integer> indexes = ExtractorGenerator.getIndexes(value, response);
    assertTrue(indexes.size() == 3);
  }

  @Test
  public void shouldGenerateMultipleExtractorsWhenGetCorrelationExtractorsWithMultipleAppearances()
      throws IOException {
    String value = "0260c32326";
    String name = "X-WP-Nonce";
    String filename = "/responses/html/responseWithTwoBodyNonces.html";
    when(result.getResponseDataAsString()).thenReturn(TestUtils.getFileContent(filename, getClass()));
    List<CorrelationExtractor<?>> extractors = rawExtractor.getCorrelationExtractors(result, value, name);
    assertThat(extractors).hasSize(2);
  }

  private List<CorrelationExtractor<?>> generateExpectedExtractors() {
    RegexCorrelationExtractor<?> extractor = new RegexCorrelationExtractor<>();
    extractor.setParams(Arrays.asList("dleware\\(\\s\\\"((?:[^\\\"\\\\]|\\\")*?)\\\"\\s\\)\\;\\n", "-1", "1", "BODY", "true"));
    extractor.setVariableName("X-WP-Nonce");
    return Arrays.asList(extractor);
  }

  private RegexCorrelationExtractor generateExpectedExtractor(String regex) {
    RegexCorrelationExtractor<?> extractor = new RegexCorrelationExtractor<>();
    extractor.setParams(Arrays.asList(regex, "-1", "1", "BODY", "true"));
    extractor.setVariableName("X-WP-Nonce");
    return extractor;
  }
}
