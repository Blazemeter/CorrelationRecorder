package com.blazemeter.jmeter.correlation.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

class ComponentContainer extends JPanel {

  private static final Dimension LABEL_PREFERRED_SIZE = new Dimension(150, 35);
  private static final Dimension TEXTAREA_PREFERRED_SIZE = new Dimension(800, 150);

  private JTextArea componentsTextArea;

  public ComponentContainer(Consumer<String> componentsValidations) {
    JLabel customExtensionsDescription = SwingUtils
        .createComponent("componentsListLabel", new JLabel(
                "Add the Custom Extension's class names, comma separated"),
            LABEL_PREFERRED_SIZE);
    JLabel extraInfo = SwingUtils.createComponent("componentsInfo",
        new JLabel("Once the area loses focus, the rules will be validated, "
            + "to avoid using Extensions that are not in this area."));
    extraInfo.setFont(getFont().deriveFont(Font.ITALIC));

    componentsTextArea = SwingUtils
        .createComponent("componentsList", new JTextArea(3, 3), TEXTAREA_PREFERRED_SIZE);
    componentsTextArea.addFocusListener(createLostFocusListener(componentsValidations));

    GroupLayout groupLayout = new GroupLayout(this);
    setLayout(groupLayout);
    groupLayout.setHorizontalGroup(groupLayout.createParallelGroup()
        .addGap(GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
        .addComponent(customExtensionsDescription)
        .addComponent(componentsTextArea)
        .addComponent(extraInfo));

    groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
        .addGap(GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
        .addComponent(customExtensionsDescription)
        .addComponent(componentsTextArea)
        .addComponent(extraInfo));
  }

  private FocusAdapter createLostFocusListener(Consumer<String> componentsValidations) {
    return new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        String componentsTextAreaText = componentsTextArea.getText().replace("\n", "");
        componentsValidations.accept(componentsTextAreaText);
        //Proper formatting requires removing all previous ones
        componentsTextArea.setText(componentsTextAreaText.replace("\n", "").replace(",", ",\n"));
      }
    };
  }

  public void setComponentsTextArea(String componentsTextArea) {
    this.componentsTextArea.setText(componentsTextArea);
  }

  public String getCorrelationComponents() {
    return componentsTextArea.getText();
  }

  public void clear() {
    componentsTextArea.setText("");
  }
}
