package com.blazemeter.jmeter.correlation.core.suggestions.method;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.FileManagementUtils;
import com.blazemeter.jmeter.correlation.core.automatic.ReplacementSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ResultFileParser;
import com.blazemeter.jmeter.correlation.core.automatic.ResultsExtraction;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.JsonCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.suggestions.context.AnalysisContext;
import com.blazemeter.jmeter.correlation.core.suggestions.context.ComparisonContext;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterVariables;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import us.abstracta.jmeter.javadsl.core.engines.JmeterEnvironment;

@RunWith(MockitoJUnitRunner.class)
public class ComparisonMethodTest extends ReplacementTest {

  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Mock
  private ComparisonContext context;
  @Mock
  private BaseCorrelationContext correlationContext;
  @Mock
  private AnalysisContext wrongContext;

  private ComparisonMethod method = new ComparisonMethod();

  @Before
  public void setUp() throws Exception {
    JmeterEnvironment env = new JmeterEnvironment();
    String path = "/" + PARAM_NAME + "=" + PARAM_VALUE + "&Test_Path=1";
    JMeterTestUtils.HttpSamplerBuilder builder = new JMeterTestUtils
        .HttpSamplerBuilder("GET", "test.com", path);
    sampler = builder.build();
    vars = new JMeterVariables();
    vars.put(REFERENCE_NAME, PARAM_VALUE);
    replacer = new RegexCorrelationReplacement<>(REQUEST_REGEX);
    replacer.setVariableName(REFERENCE_NAME);
    replacer.setContext(correlationContext);
    doReturn(0).when(correlationContext).getVariableCount(anyString());
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void shouldReturnEmptyWhenGenerateSuggestionsWithWrongContext() {
    assert method.generateSuggestions(wrongContext).isEmpty();
  }

  @Test
  public void shouldReturnEmptyWhenGenerateSuggestionsWithContextGetRecordingMapEmpty() {
    HashMap<String, List<Appearances>> map = new HashMap<>();
    when(context.getRecordingMap()).thenReturn(map);
    when(context.getReplayMap()).thenReturn(map);
    assert method.generateSuggestions(context).isEmpty();
  }

  @Test
  public void shouldGenerateSuggestionsWhenGenerateSuggestionsWithValidContextAndMaps()
      throws IOException {
    String path = TestUtils.getFolderPath("/xmlObjects", getClass());
    mockGetRecordingMap(path);
    mockGetReplayMap(path);

    Configuration configuration = new Configuration();
    mockGetRecordingResults(configuration);
    when(context.getConfiguration()).thenReturn(configuration);

    List<CorrelationSuggestion> suggestions = method.generateSuggestions(context);
    softly.assertThat(suggestions.size()).isEqualTo(24);
  }

  private void mockGetReplayMap(String path) {
    loadAndMockIfPresent(path, "replayMapSerialization.xml", context.getReplayMap());
  }

  private void loadAndMockIfPresent(String path, String filename,
      Map<String, List<Appearances>> context) {
    loadMap(path, filename)
        .ifPresent(map -> when(context)
            .thenReturn((HashMap<String, List<Appearances>>) map));
  }

  private void mockGetRecordingMap(String path) {
    loadAndMockIfPresent(path, "recordingMapSerialization.xml", context.getRecordingMap());
  }

  private void mockGetRecordingResults(Configuration configuration) throws IOException {
    String path = TestUtils
        .getFolderPath("/recordings/recordingTrace/recordingForMendix.jtl", getClass());
    List<SampleResult> results = new ResultFileParser(configuration)
        .loadFromFile(new File(path), true);
    when(context.getRecordingSampleResults()).thenReturn(results);
  }

  private Optional<Object> loadMap(String path, String filename) {
    return FileManagementUtils.loadObjectFromXmlPath(path, filename);
  }

  @Test
  public void shouldGenerateMatchingReplacementsWhenAddReplacementSuggestions()
      throws IOException {
    String folderPath = TestUtils.getFolderPath("/xmlObjects", getClass());
    mockGetRecordingMap(folderPath);
    mockGetReplayMap(folderPath);

    Configuration configuration = new Configuration();
    mockGetRecordingResults(configuration);
    when(context.getConfiguration()).thenReturn(configuration);

    List<CorrelationSuggestion> suggestions = method.generateSuggestions(context);

    CorrelationSuggestion suggestion = suggestions.get(20);
    List<ReplacementSuggestion> replacements = suggestion.getReplacementSuggestions();

    ReplacementSuggestion replacementSuggestion = replacements.get(0);

    RegexCorrelationReplacement<BaseCorrelationContext> replacer =
        new RegexCorrelationReplacement<>();
    RegexCorrelationReplacement<?> replacement =
        (RegexCorrelationReplacement) replacementSuggestion.getReplacementSuggestion();
    replacer.setParams(replacement.getParams());
    replacer.setContext(correlationContext);
    replacer.setVariableName(REFERENCE_NAME);
    softly.assertThat(replacer).isEqualTo(replacement);

    when(correlationContext.getVariableCount(REFERENCE_NAME)).thenReturn(1);

    headerManager = new HeaderManager();
    headerManager.add(new Header(PARAM_NAME, PARAM_VALUE));

    replacer.process(sampler, Collections.singletonList(headerManager), null, vars);
    softly.assertThat(headerManager.getHeader(0))
        .isEqualTo(new Header(PARAM_NAME, "${" + REFERENCE_NAME + "}"));
  }

  @Test
  public void shouldGenerateSuggestionWithReplacementString() throws IOException {
    Configuration configuration = new Configuration();
    when(context.getConfiguration()).thenReturn(configuration);
    String path = TestUtils
        .getFolderPath("/recordings/recordingTrace/recording-encode-decode.jtl", getClass());
    List<SampleResult> results = new ResultFileParser(configuration)
        .loadFromFile(new File(path), true);
    ResultsExtraction resultsExtraction = new ResultsExtraction(configuration);
    when(context.getRecordingSampleResults()).thenReturn(results);
    when(context.getRecordingMap()).thenReturn(resultsExtraction.extractAppearanceMap(TestUtils
        .getFolderPath("/recordings/recordingTrace/recording-encode-decode.jtl", getClass())
        .toString()));
    when(context.getReplayMap()).thenReturn(resultsExtraction.extractAppearanceMap(TestUtils
        .getFolderPath("/recordings/recordingTrace/replay-encode-decode.jtl", getClass())
        .toString()));
    List<CorrelationSuggestion> suggestions = method.generateSuggestions(context);
    List<CorrelationReplacement> replacementSuggestions =
        suggestions.stream().map(CorrelationSuggestion::getReplacementSuggestions)
            .collect(Collectors.toList()).stream().flatMap(List::stream)
            .collect(Collectors.toList()).stream()
            .map(ReplacementSuggestion::getReplacementSuggestion).collect(Collectors.toList());
    JsonCorrelationReplacement<BaseCorrelationContext> expectedDecodeReplacement = new JsonCorrelationReplacement<>(
        "$.decodeValue", "${__urlencode(${decodeValue})}", "false");
    expectedDecodeReplacement.setVariableName("decodeValue");
    JsonCorrelationReplacement<BaseCorrelationContext> expectedEncodeReplacement = new JsonCorrelationReplacement<>(
        "$.encodedValue", "${__urldecode(${encodedValue})}", "false");
    expectedEncodeReplacement.setVariableName("encodedValue");
    softly.assertThat(replacementSuggestions).contains(expectedDecodeReplacement);
    softly.assertThat(replacementSuggestions).contains(expectedEncodeReplacement);
  }
}
