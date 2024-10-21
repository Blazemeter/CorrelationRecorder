package com.blazemeter.jmeter.correlation.core.suggestions;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.suggestions.context.AnalysisContext;
import com.blazemeter.jmeter.correlation.core.suggestions.method.AnalysisMethod;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class SuggestionGeneratorIT {

  private static final Logger LOG = LoggerFactory.getLogger(SuggestionGeneratorIT.class);

  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private AnalysisContext analysisContext;
  private AnalysisMethod analysisMethod;
  private SuggestionGenerator suggestionGenerator;

  @Before
  public void setUp() throws IOException {
    JMeterTestUtils.setupUpdatedJMeter();
    analysisContext = new AnalysisContext();
    analysisContext.setRegistry(CorrelationComponentsRegistry.getNewInstance());
    analysisMethod = new AnalysisMethod();
    suggestionGenerator = new SuggestionGenerator(analysisMethod);
  }

  @Test
  public void shouldReturnEmptyWhenGenerateSuggestionsWithoutContext() {
    LOG.info("shouldReturnEmptyWhenGenerateSuggestionsWithoutContext: "
        + "We expect errors to be logged here.");
    softly.assertThat(suggestionGenerator.generateSuggestions(null)).isEmpty();
  }

  @Test
  public void shouldReturnEmptyWhenGenerateSuggestionsWithContextWithoutTemplate() {
    LOG.info("shouldReturnEmptyWhenGenerateSuggestionsWithContextWithoutTemplate: "
        + "We expect errors to be logged here.");
    analysisContext.setTemplate(null);
    softly.assertThat(suggestionGenerator.generateSuggestions(analysisContext)).isEmpty();
  }

  @Test
  public void shouldReturnEmptyWhenGenerateSuggestionsWithContextWithTemplateWithoutGroups() {
    LOG.info("shouldReturnEmptyWhenGenerateSuggestionsWithContextWithTemplateWithoutGroups: "
        + "We expect errors to be logged here.");
    Template template = getTemplate(new ArrayList<>());

    analysisContext.setTemplate(template);
    softly.assertThat(suggestionGenerator.generateSuggestions(analysisContext)).isEmpty();
  }

  private static Template getTemplate(List<RulesGroup> rulesGroup) {
    return new Template.Builder()
        .withId("template-id")
        .withGroups(rulesGroup)
        .build();
  }

  @Test
  public void shouldReturnEmptyWhenGenerateSuggestionsWithContextWithTemplateGroupsWithoutRules() {
    LOG.info("shouldReturnEmptyWhenGenerateSuggestionsWithContextWithTemplateGroupsWithoutRules: "
        + "We expect errors to be logged here.");
    RulesGroup rulesGroup = new RulesGroup.Builder()
        .withId("group-id")
        .withRules(new ArrayList<>())
        .build();

    analysisContext.setTemplate(getTemplate(Collections.singletonList(rulesGroup)));
    softly.assertThat(suggestionGenerator.generateSuggestions(analysisContext)).isEmpty();
  }

  @Test
  public void shouldReturnEmptyWhenGenerateSuggestionsWithContextWithEmptyElements() {
    LOG.info("shouldReturnEmptyWhenGenerateSuggestionsWithContextWithEmptyElements: "
        + "We expect errors to be logged here.");

    if (didntLoadedTemplateToContextSuccessfully()) {
      return;
    }

    if (didntLoadTestPlanToContextSuccessfully()) {
      return;
    }

    softly.assertThat(suggestionGenerator.generateSuggestions(analysisContext)).isEmpty();
  }

  private boolean didntLoadedTemplateToContextSuccessfully() {
    String templateFilepath = "/templates/template.json";
    Optional<Template> templateOptional = TestUtils.getTemplateFromFilePath(templateFilepath);
    if (!templateOptional.isPresent()) {
      LOG.error("Template not found at '{}'. Skipping test.", templateFilepath);
      softly.fail("Template not found at " + templateFilepath);
      return true;
    }
    analysisContext.setTemplate(templateOptional.get());
    return false;
  }

  private boolean didntLoadTestPlanToContextSuccessfully() {
    String recordingFilepath = "/recordings/testplans/recordingWithNonces.jmx";
    Optional<String> recordingOptional = TestUtils.getFilePathFromResources(recordingFilepath,
        getClass());
    if (!recordingOptional.isPresent()) {
      LOG.error("Recording not found at '{}'. Skipping test.", recordingFilepath);
      softly.fail("Recording not found at " + recordingFilepath);
      return true;
    }
    analysisContext.setRecordingTestPlanPath(recordingOptional.get());
    return false;
  }

  //  @Test // Need to mock the node supplier
  public void shouldReturnListWhenGenerateSuggestionsWithValidContext() {
    if (didntLoadedTemplateToContextSuccessfully()) {
      return;
    }

    if (didntLoadTestPlanToContextSuccessfully()) {
      return;
    }

    if (didntLoadRecordingTraceToContextSuccessfully()) {
      return;
    }
    analysisContext.setRegistry(CorrelationComponentsRegistry.getNewInstance());

    List<CorrelationSuggestion> suggestions = suggestionGenerator.generateSuggestions(
        analysisContext);
    softly.assertThat(suggestions).isNotEmpty();
    softly.assertThat(suggestions.size()).isEqualTo(1);
  }

  private boolean didntLoadRecordingTraceToContextSuccessfully() {
    String recordingTraceFilepath = "/recordings/recordingTrace/recordingWithNonces.jtl";
    Optional<String> recordingTraceOptional = TestUtils.getFilePathFromResources(
        recordingTraceFilepath,
        getClass());
    if (!recordingTraceOptional.isPresent()) {
      LOG.error("Recording trace not found at '{}'. Skipping test.", recordingTraceFilepath);
      softly.fail("Recording trace not found at " + recordingTraceFilepath);
      return true;
    }
    analysisContext.setRecordingTraceFilePath(recordingTraceOptional.get());
    return false;
  }

  //  @Test
  public void shouldReturnListOfRulesWhenParseToRulesWhenApplySuggestions() {
    if (didntLoadedTemplateToContextSuccessfully()) {
      return;
    }

    if (didntLoadTestPlanToContextSuccessfully()) {
      return;
    }

    if (didntLoadRecordingTraceToContextSuccessfully()) {
      return;
    }
    analysisContext.setRegistry(CorrelationComponentsRegistry.getNewInstance());

    List<CorrelationSuggestion> suggestions = suggestionGenerator.generateSuggestions(
        analysisContext);
    softly.assertThat(suggestions).isNotEmpty();
    softly.assertThat(suggestions.size()).isEqualTo(1);

    HashMap<CorrelationRule, Integer> correlationRules = suggestionGenerator.parseToRules(
        suggestions);
    softly.assertThat(correlationRules).isNotEmpty();
    softly.assertThat(correlationRules.size()).isEqualTo(2);
  }

}
