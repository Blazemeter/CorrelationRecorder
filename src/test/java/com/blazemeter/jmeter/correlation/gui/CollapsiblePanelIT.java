package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;

import javax.swing.JPanel;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CollapsiblePanelIT {

  private HeaderPanel title;
  private FrameFixture frame;
  private CollapsiblePanel collapsiblePanel;
  private JPanel content;

  @Before
  public void setup() {
    content = new JPanel();
    content.setName("contentPanel");
    title = new HeaderPanel("header");
    collapsiblePanel = new CollapsiblePanel(content, title, false);
    JPanel display = new JPanel();
    display.add(collapsiblePanel);
    frame = showInFrame(display);
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }
  
  @Test
  public void shouldDisplayContentWhenCreateWithCollapsedFalse() {
    assertThat(frame.robot()
        .finder()
        .findByName("contentPanel", JPanel.class, false)
        .isVisible())
        .isTrue();
  }

  @Test
  public void shouldToggleContentVisibilityWhenToggleCollapsed() {
    JPanel contentPanel = frame.panel("contentPanel").target();
    boolean original = contentPanel.isVisible();
    collapsiblePanel.toggleCollapsed();
    assertThat(original != contentPanel.isVisible()).isTrue();
  }
}