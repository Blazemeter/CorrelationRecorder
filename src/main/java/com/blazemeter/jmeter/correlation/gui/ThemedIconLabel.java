package com.blazemeter.jmeter.correlation.gui;

import java.awt.Graphics;
import javax.swing.JLabel;

public class ThemedIconLabel extends JLabel {

  private String iconResourceName;

  public ThemedIconLabel(String iconResourceName) {
    super(ThemedIcon.fromResourceName(iconResourceName));
    this.iconResourceName = iconResourceName;
  }

  @Override
  public void paint(Graphics g) {
    setIcon(ThemedIcon.fromResourceName(iconResourceName));
    super.paint(g);
  }
}
