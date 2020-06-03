package com.blazemeter.jmeter.correlation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import org.apache.jmeter.util.JMeterUtils;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.prompt.PromptSupport;

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

  public static void setPlaceHolder(JTextField field, String placeHolder) {
    PromptSupport.setPrompt(placeHolder, field);
    PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.HIDE_PROMPT, field);
    PromptSupport.setFontStyle(Font.BOLD, field);
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

  public static JXTaskPane buildCollapsiblePane(String name) {
    JXTaskPane pane = SwingUtils.createComponent(name, new JXTaskPane());
    pane.setCollapsed(true);
    pane.setAnimated(false);
    pane.setScrollOnExpand(true);
    ((JComponent) pane.getContentPane()).setBorder((Border) UIManager.get("Panel.border"));
    final Color panelBackground = (Color) UIManager.get("Panel.background");
    final Color labelForeground = (Color) UIManager.get("Label.foreground");
    UIManager.put("TaskPane.titleBackgroundGradientStart", panelBackground);
    UIManager.put("TaskPane.titleBackgroundGradientEnd", panelBackground);
    UIManager.put("TaskPane.titleBackgroundGradientStart", panelBackground);
    UIManager.put("TaskPane.titleForeground", labelForeground);
    UIManager.put("TaskPane.titleOver", labelForeground);
    pane.setAlignmentX(0);
    return pane;
  }
}
