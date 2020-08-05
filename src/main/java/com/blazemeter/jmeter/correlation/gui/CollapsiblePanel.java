package com.blazemeter.jmeter.correlation.gui;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class CollapsiblePanel extends JPanel {


  private static final String BUTTON_SHOW = "Show";
  private static final String BUTTON_HIDE = "Hide";
  private JButton showButton = new JButton();
  private JPanel contentPanel = new JPanel();

  public CollapsiblePanel(String title) {
    this(title, null);
  }

  public CollapsiblePanel(String title, Runnable expandAction) {
    setBorder(BorderFactory.createTitledBorder(title));
    showButton.setText("Show");
    showButton.addActionListener(e -> {
      contentPanel.setVisible(!contentPanel.isVisible());
      showButton.setText(contentPanel.isVisible() ? BUTTON_HIDE : BUTTON_SHOW);
      revalidate();
      repaint();
      if (expandAction != null) {
        expandAction.run();
      }
    });
    BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
    setLayout(layout);
    showButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    super.add(showButton);
    super.add(contentPanel);
    contentPanel.setVisible(false);
  }

  @Override
  public Component add(Component component) {
    return contentPanel.add(component);
  }
}
