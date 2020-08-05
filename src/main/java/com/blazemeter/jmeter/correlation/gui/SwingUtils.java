package com.blazemeter.jmeter.correlation.gui;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import org.apache.jmeter.util.JMeterUtils;

public class SwingUtils {

  public static <T extends JComponent> T createComponent(String name, T component) {
    component.setName(name);
    return component;
  }

  public static <T extends JComponent> T createComponent(String name, T component,
      Dimension minimumSize) {
    component.setName(name);
    component.setMinimumSize(minimumSize);
    component.setPreferredSize(minimumSize);
    return component;
  }

  public static JButton buildJButton(String name, String text, String action,
      ActionListener listener) {
    String translatedText = JMeterUtils.getResString(text.toLowerCase());
    JButton button = createComponent(name, new JButton(
        translatedText.contains("res_key") ? StringUtils.capitalize(text) : translatedText));
    button.setActionCommand(action);
    button.addActionListener(listener);
    return button;
  }
}
