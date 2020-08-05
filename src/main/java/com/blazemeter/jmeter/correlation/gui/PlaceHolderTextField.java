package com.blazemeter.jmeter.correlation.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JTextField;

public class PlaceHolderTextField extends JTextField {

  private String placeHolder = "";

  public PlaceHolderTextField() {
    this(null);
  }

  public PlaceHolderTextField(String text) {
    super(text);
  }

  @Override
  protected void paintComponent(Graphics pG) {
    super.paintComponent(pG);

    if (placeHolder == null || placeHolder.length() == 0 || getText().length() > 0) {
      return;
    }

    final Graphics2D g = (Graphics2D) pG;
    g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(getDisabledTextColor());
    g.drawString(placeHolder, getInsets().left, pG.getFontMetrics()
        .getMaxAscent() + getInsets().top);
  }

  public void setPlaceHolder(String placeHolder) {
    this.placeHolder = placeHolder;
  }


}
