package com.blazemeter.jmeter.correlation.gui.automatic;

import static org.assertj.swing.fixture.Containers.showInFrame;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class CorrelationSuggestionsPanelTest {

  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private FrameFixture frame;
  private CorrelationSuggestionsPanel panel;

  @Mock
  private ActionListener listener;

  @Before
  public void setUp() throws Exception {
    panel = new CorrelationSuggestionsPanel();
    frame = showInFrame(panel);
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
}