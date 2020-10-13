package com.blazemeter.jmeter.correlation.gui;

import javax.swing.GroupLayout;
import javax.swing.JPanel;

public class CollapsiblePanel extends JPanel implements ToggleableComponent {

  private final JPanel contentPanel;
  private final HeaderPanel headerPanel;
  private boolean collapsed;

  public CollapsiblePanel(JPanel contentPanel, HeaderPanel title, boolean collapsed) {
    setName("Component-" + title);
    this.headerPanel = title;
    this.collapsed = false;
    this.contentPanel = contentPanel;

    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);

    layout.setHorizontalGroup(layout.createParallelGroup()
        .addComponent(title)
        .addComponent(this.contentPanel)
    );

    int preferredHeight = title.getPreferredSize().height;
    layout.setVerticalGroup(layout.createSequentialGroup()
        .addComponent(title, preferredHeight, preferredHeight, preferredHeight)
        .addComponent(this.contentPanel)
    );

    add(headerPanel);
    add(this.contentPanel);
    
    if (collapsed) {
      toggleCollapsed();
    }
  }

  public void toggleCollapsed() {
    collapsed = !collapsed;
    headerPanel.toggleCollapsed();
    contentPanel.setVisible(!collapsed);
    validate();
  }
}
