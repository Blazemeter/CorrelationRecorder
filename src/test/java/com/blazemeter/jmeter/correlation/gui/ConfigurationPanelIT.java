package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.util.Arrays;
import java.util.List;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class ConfigurationPanelIT {

  private final CorrelationRulePartTestElement<?> none = CorrelationComponentsRegistry.NONE_EXTRACTOR;
  private final CorrelationRulePartTestElement<?> replacement =
      new RegexCorrelationReplacement<>();
  private final CorrelationRulePartTestElement<?> extractor =
      new RegexCorrelationExtractor<>();
  private FrameFixture frame;
  private ConfigurationPanel panel;

  @Mock
  private Runnable modelUpdate;

  @Mock
  private Runnable fieldUpdate; 
  
  @Before
  public void setup() {
    List<CorrelationRulePartTestElement<?>> options = Arrays.asList(none, extractor, replacement);
    panel = new ConfigurationPanel(modelUpdate, "TestRulePart", options);
    panel.setUpdateFieldListeners(fieldUpdate);
    panel.addFields();
    panel.repaint();
    frame = showInFrame(panel);
    frame.target().pack();
    doNothing().when(fieldUpdate).run();
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldUpdateComponentWidthWhenOptionSelected() {
    int initialRequiredWidth = panel.getSumComponentsWidth();
    panel.setSelectedItem(extractor);
    assertThat(panel.getSumComponentsWidth()).isNotEqualTo(initialRequiredWidth);
  }
}
