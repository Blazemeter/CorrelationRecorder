package com.blazemeter.jmeter.correlation.gui.common;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.HyperlinkEvent;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;

public class SwingUtils {

  private SwingUtils() {
  }

  public static Color getEnabledForegroundColor(boolean enabled) {
    JTextField field = new JTextField();
    return enabled ? field.getForeground() : field.getDisabledTextColor();
  }

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

  public static <T> JList<T> buildJList(String name, DefaultListModel<T> model,
      Dimension preferredSize) {
    JList<T> jList = SwingUtils.createComponent(name, new JList<>());
    jList.setPreferredSize(preferredSize);
    jList.setModel(model);
    jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    return jList;
  }

  public static JTextPane buildJTextPane(String name, Logger log) {
    JTextPane pane = SwingUtils.createComponent(name, new JTextPane());
    pane.setContentType("text/html");
    pane.setEditable(false);
    pane.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && Desktop.isDesktopSupported()) {
        try {
          Desktop.getDesktop().browse(e.getURL().toURI());
        } catch (IOException | URISyntaxException ex) {
          log.error("There was an issue trying to open the url {}", e.getURL(), ex);
        }
      }
    });
    return pane;
  }

  public static JScrollPane buildScrollPanel(Component list, String name, Dimension preferredSize) {
    JScrollPane scroll = new JScrollPane();
    scroll.setName(name);
    scroll.setViewportView(list);
    scroll.createVerticalScrollBar();
    scroll.createHorizontalScrollBar();
    scroll.setPreferredSize(preferredSize);
    return scroll;
  }

  public static final class ButtonBuilder {

    private ActionListener actionListener;
    private String action;
    private String name;
    private String iconName = "";

    public ButtonBuilder() {

    }

    public ButtonBuilder withActionListener(ActionListener actionListener) {
      this.actionListener = actionListener;
      return this;
    }

    public ButtonBuilder withAction(String action) {
      this.action = action;
      return this;
    }

    public ButtonBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ButtonBuilder withIcon(String iconName) {
      this.iconName = iconName;
      return this;
    }

    public JButton build() {
      String parsedName = JMeterUtils.getResString(name);

      JButton button = createComponent(name + "Button", iconName.isEmpty()
          ? new JButton(parsedName.contains("res_key") ? StringUtils.capitalize(name)
          : parsedName) : new JButton(ThemedIcon.fromResourceName(iconName)));

      button.setActionCommand(action);
      button.addActionListener(actionListener);
      return button;
    }
  }

  public static void setFieldMinimumAndPreferredColumns(JTextField field, int minCols,
      int prefCols) {
    field.setColumns(minCols);
    field.setMinimumSize(field.getPreferredSize());
    field.setColumns(prefCols);
    field.setMaximumSize(new Dimension(Short.MAX_VALUE, field.getPreferredSize().height));
  }

}
