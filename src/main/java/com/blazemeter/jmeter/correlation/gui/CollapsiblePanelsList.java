package com.blazemeter.jmeter.correlation.gui;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class CollapsiblePanelsList extends JPanel {

  private final List<ToggleableComponent> componentsList = new ArrayList<>();

  public CollapsiblePanelsList() {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setAutoscrolls(true);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        componentsList.stream()
            .filter(listener -> ((JPanel) e.getSource()).getParent() == listener)
            .forEach(ToggleableComponent::toggleCollapsed);
      }
    });
  }

  public void addComponent(ToggleableComponent component) {
    componentsList.add(component);
    add((JPanel) component);
    //With this we ensure that the next component has a space and is glued to last one
    add(Box.createRigidArea(new Dimension(getWidth(), 10)));
    add(Box.createVerticalGlue());
  }
}
