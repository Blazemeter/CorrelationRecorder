package com.blazemeter.jmeter.correlation.core.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractionSuggestion;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnalysisReporterTest extends AnalysisTest {

  @Before
  public void setup() throws IOException {
    JMeterTestUtils.setupUpdatedJMeter();
    AnalysisReporter.startCollecting();
  }

  @After
  public void tearDown() {
    AnalysisReporter.stopCollecting();
  }

  @Test
  public void shouldAddExtractorInformationWhenReportWithRegexCorrExtractor() throws IOException {
    extractor.setVariableName(VALUE_NAME);
    HTTPSamplerBase loggedUserRequest = createLoggedUserRequest();
    AnalysisReporter.report(extractor, loggedUserRequest, extractor.getTarget().toString(),
        TOKEN_VALUE);
    String reportString = AnalysisReporter.getReporter().getReportAsString();
    assertThat(reportString)
        .isEqualToIgnoringWhitespace(getExpectedReport(extractor));
  }

  @Test
  public void shouldAddReplacementInformationWhenReportWithRegexCorrReplacement()
      throws IOException {
    replacement.setVariableName(VALUE_NAME);
    HTTPSamplerBase loggedUserRequest = createLoggedUserRequest();
    AnalysisReporter.report(replacement, loggedUserRequest, TOKEN_VALUE);
    String reportString = AnalysisReporter.getReporter().getReportAsString();
    List<Pair<String, CorrelationRulePartTestElement<?>>> parts = Collections
        .singletonList(Pair.of(LOGGED_REQUEST_NAME, replacement));
    assertThat(reportString)
        .isEqualToIgnoringWhitespace(getExpectedReport(parts));
  }

  @Test
  public void shouldNotAddReportsWhenNotCollecting() {
    AnalysisReporter.stopCollecting();
    extractor.setVariableName(VALUE_NAME);
    AnalysisReporter.report(extractor, (Object) LOGGED_REQUEST_NAME, VALUE_NAME, TOKEN_VALUE);
    String reportString = AnalysisReporter.getReporter().getReportAsString();
    assertThat(reportString).isEqualToIgnoringWhitespace(AnalysisReporter.NO_REPORT);
  }

  @Test
  public void shouldDisplayNotAppliedRulesWhenNoRulesApplied() {
    String reportString = AnalysisReporter.getReporter().getReportAsString();

    String LS = System.lineSeparator();
    assertThat(reportString).isEqualToIgnoringWhitespace("Correlations Report:"
        + LS + "Total rules appliances=0."
        + LS + "No rules were applied successfully. Review them and try again.");
  }

  @Test
  public void shouldGenerateCorrelationSuggestionsWhenGenerateCorrelationSuggestions()
      throws IOException {
    extractor.setVariableName(VALUE_NAME);
    HTTPSamplerBase loggedUserRequest = createLoggedUserRequest();
    AnalysisReporter.report(extractor, loggedUserRequest, extractor.getTarget().toString(),
        TOKEN_VALUE);
    List<CorrelationSuggestion> suggestions = AnalysisReporter.generateCorrelationSuggestions();
    List<CorrelationSuggestion> expectedSuggestions = Collections
        .singletonList(createExpectedSuggestions(loggedUserRequest));
    assertThat(suggestions.toString()).isEqualTo(expectedSuggestions.toString());
  }

  private CorrelationSuggestion createExpectedSuggestions(HTTPSamplerBase affectedRequest) {
    CorrelationSuggestion suggestion = new CorrelationSuggestion(VALUE_NAME, TOKEN_VALUE);
    suggestion.setParamName(VALUE_NAME);
    suggestion.setOriginalValue(TOKEN_VALUE);
    ExtractionSuggestion extractionSuggestion
        = new ExtractionSuggestion(extractor, affectedRequest);
    extractionSuggestion.setName(VALUE_NAME);
    extractionSuggestion.setSource(extractor.getTarget().toString());
    extractionSuggestion.setValue(TOKEN_VALUE);
    suggestion.addExtractionSuggestion(extractionSuggestion);
    return suggestion;
  }
}
