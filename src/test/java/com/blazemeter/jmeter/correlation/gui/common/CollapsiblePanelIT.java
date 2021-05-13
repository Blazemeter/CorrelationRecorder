package com.blazemeter.jmeter.correlation.gui.common;

import static org.assertj.swing.fixture.Containers.showInFrame;

import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPanelFixture;
import org.junit.After;
import org.junit.Test;

public class CollapsiblePanelIT {

  public static final String CONTENT_PANEL_NAME = "contentPanel";
  private FrameFixture frame;
  private CollapsiblePanel collapsiblePanel;

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldDisplayContentWhenCreateWithCollapsedFalse() {
    setupPanel(new CollapsiblePanel.Builder());
    findContentPanel().requireVisible();
  }

  private JPanelFixture findContentPanel() {
    return frame.panel(CONTENT_PANEL_NAME);
  }

  private void setupPanel(CollapsiblePanel.Builder builder) {
    collapsiblePanel = builder
        .withTitle("header")
        .withContent(SwingUtils.createComponent(CONTENT_PANEL_NAME, new JPanel()))
        .build();
    frame = showInFrame(collapsiblePanel);
  }

  @Test
  public void shouldHideContentWhenCollapsed() {
    setupPanel(new CollapsiblePanel.Builder());
    JPanelFixture contentPanel = findContentPanel();
    collapsiblePanel.toggleCollapsed();
    contentPanel.requireNotVisible();
  }

  @Test
  public void shouldHideButtonsWhenIsCollapsed() {
    setupPanel(new CollapsiblePanel.Builder()
        .withButtons(Collections.singletonList(new JButton("button"))));
    JPanelFixture buttonsPanel = frame.panel("-collapsiblePanel-header-buttonsPanel");
    collapsiblePanel.toggleCollapsed();
    buttonsPanel.requireNotVisible();
  }

  @Test
  public void shouldTitleNotBeEditableWhenNotClickEditableIcon() {
    setupPanel(new CollapsiblePanel.Builder()
        .withEditableTitle());
    frame.textBox("-collapsiblePanel-header-title").requireNotEditable();
  }
}
