package com.blazemeter.jmeter.correlation.gui.automatic;

import static org.assertj.swing.fixture.Containers.showInFrame;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import us.abstracta.jmeter.javadsl.core.engines.JmeterEnvironment;

@RunWith(SwingTestRunner.class)
public class CorrelationSuggestionsPanelTest {

  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private FrameFixture frame;
  private CorrelationSuggestionsPanel panel;
  private Supplier<String> getRecordingTraceSupplier = () -> "";
  public JmeterEnvironment jmeterEnvironment;


  @Before
  public void setUp() throws Exception {
    panel = new CorrelationSuggestionsPanel(new CorrelationWizard());
    frame = showInFrame(panel);
    jmeterEnvironment = new JmeterEnvironment();
  }

  @After
  public void tearDown() {
    frame.cleanUp();
    frame = null;
  }

  @Test
  public void shouldExportOnlySelectedSuggestionsWhenExportSelectedSuggestions() {
    List<CorrelationSuggestion> originalSuggestions = generateSuggestions(3);
    panel.loadSuggestions(originalSuggestions);
    panel.toggleSuggestionItem(1);
    List<CorrelationSuggestion> suggestionList = panel.exportSelectedSuggestions();
    softly.assertThat(suggestionList.size()).isEqualTo(2);
    softly.assertThat(suggestionList.size()).isNotEqualTo(originalSuggestions.size());
    softly.assertThat(suggestionList).isEqualTo(originalSuggestions.remove(1));
  }

  private List<CorrelationSuggestion> generateSuggestions(int count) {
    List<CorrelationSuggestion> suggestions = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      CorrelationSuggestion suggestion = new CorrelationSuggestion();
      suggestion.setParamName("paramName" + i);
      suggestion.setOriginalValue("originalValue" + i);
      suggestion.setNewValue("newValue" + i);
      suggestions.add(suggestion);
    }
    return suggestions;
  }

  @Test
  public void shouldLoadSuggestionsWhenLoadSuggestions() {
    int initialSuggestionsCount = panel.getSuggestionsCount();
    panel.loadSuggestions(generateSuggestions(0));
    int currentSuggestionsCount = panel.getSuggestionsCount();
    softly.assertThat(currentSuggestionsCount).isNotEqualTo(initialSuggestionsCount);
    softly.assertThat(currentSuggestionsCount).isEqualTo(1);
  }

  @Ignore
  @Test
  public void shouldNotifyWhenApplySuggestionsButton() throws InterruptedException {
    panel.loadSuggestions(generateSuggestions(10));
    panel.setGetRecordingTraceSupplier(getRecordingTraceSupplier);
    clickApply();
    Thread.sleep(60000);
    //frame.dialog().label("We are applying the suggestions, please wait...").requireVisible();
    frame.optionPane().requireMessage("The suggestions were applied successfully to your Test Plan.\n" +
            "Please review the changes and, when you are ready, replay to review the results.");
  }

  private void clickApply() {frame.button("correlateButton").click();}
}
