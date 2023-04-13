package com.blazemeter.jmeter.correlation.gui.analysis;

import static org.assertj.swing.fixture.Containers.showInFrame;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import java.util.ArrayList;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SwingTestRunner.class)
public class CorrelationTemplatesSelectionPanelTest {

  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private FrameFixture frame;
  private CorrelationTemplatesSelectionPanel panel;

  @Before
  public void setUp() throws Exception {
    panel = new CorrelationTemplatesSelectionPanel(() -> new ArrayList<>());
    frame = showInFrame(panel);
  }

  @After
  public void tearDown() throws Exception {
    frame.cleanUp();
    frame = null;
  }

  @Test
  public void shouldDisplayCorrelationTemplates() throws InterruptedException {
    Thread.sleep(100);
  }
}