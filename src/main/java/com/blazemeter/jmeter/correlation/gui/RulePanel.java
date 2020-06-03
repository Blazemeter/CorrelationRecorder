package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;

public class RulePanel extends JPanel implements Serializable {

  public static final int RULE_HEIGHT = 25;
  protected static final String DEFAULT_COMBO_VALUE = "None";
  private static final String REFERENCE_VARIABLE_NAME = "RefVar";
  private static final String REFERENCE_VARIABLE_DESCRIPTION = "Reference Variable";
  private static final Color DEFAULT_PANEL_COLOR = UIManager.getColor("Panel.background");
  private static final Color FOCUSED_PANEL_COLOR = UIManager.getColor("Table.selectionBackground");
  private static final String CORRELATION_REPLACEMENT_SUFFIX = "CorrelationReplacement";
  private static final String CORRELATION_EXTRACTOR_SUFFIX = "CorrelationExtractor";
  private static final int TOTAL_NON_DYNAMIC_COMPONENTS = 3;
  private static final int ESTIMATED_FIELD_TOTAL_WIDTH = 130;
  private static final Dimension COMPONENT_PREFERRED_SIZE = new Dimension(100, RULE_HEIGHT);
  private JTextField referenceVariableField;
  private ContainerPanelsHandler extractorHandler;
  private ContainerPanelsHandler replacementHandler;

  public RulePanel(RulesContainer container) {
    setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

    //Needed for testing and differentiate purposes for panel and it's components
    String ruleNameId = "Rule-" + System.currentTimeMillis();
    setName(ruleNameId);
    setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
    referenceVariableField = new JTextField();
    referenceVariableField.setName(ruleNameId + "-referenceVariable");
    /*
    This is necessary to avoid issues when multiple empty rules are added
    and one moves or its deleted.
    * */
    referenceVariableField.setText("refVar" + (container.getRules().size() + 1));
    referenceVariableField.setToolTipText(REFERENCE_VARIABLE_DESCRIPTION);
    SwingUtils.setPlaceHolder(referenceVariableField, REFERENCE_VARIABLE_NAME);
    referenceVariableField.setPreferredSize(COMPONENT_PREFERRED_SIZE);
    PanelMouseEvent panelMouseEvent = new PanelMouseEvent(container, this);
    referenceVariableField.addMouseListener(panelMouseEvent);
    add(referenceVariableField);

    extractorHandler = setUpDynamicPanel(false, container, ruleNameId, panelMouseEvent);
    replacementHandler = setUpDynamicPanel(true, container, ruleNameId, panelMouseEvent);

    add(extractorHandler.getContainerPanel());
    add(replacementHandler.getContainerPanel());

    setFocusable(true);
    addMouseListener(panelMouseEvent);
  }

  public void removeFocusBackground() {
    updatePanelsBackground(DEFAULT_PANEL_COLOR);
  }

  public void setFocusBackground() {
    updatePanelsBackground(FOCUSED_PANEL_COLOR);
  }


  private ContainerPanelsHandler setUpDynamicPanel(boolean isReplacement, RulesContainer container,
      String ruleNameId, PanelMouseEvent panelMouseEvent) {

    JComboBox<String> typeComboBox = new JComboBox<>();

    String correlationType = isReplacement ? CORRELATION_REPLACEMENT_SUFFIX
        : CORRELATION_EXTRACTOR_SUFFIX;
    typeComboBox.setName(ruleNameId + "-" + correlationType + "-comboBox");
    typeComboBox.addItem(DEFAULT_COMBO_VALUE);
    typeComboBox.addMouseListener(panelMouseEvent);

    (isReplacement ? container.getReplacements() : container.getExtractors()).forEach(
        c -> typeComboBox.addItem(cleanCorrelationName(isReplacement, c)));
    ArrayList<Component> componentsList = new ArrayList<>();
    JPanel containerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    typeComboBox.setToolTipText(correlationType);
    typeComboBox.addItemListener(
        e -> setUpComboBox(e, isReplacement, ruleNameId, containerPanel, componentsList, container,
            panelMouseEvent));
    containerPanel.add(typeComboBox, BorderLayout.WEST);
    return new ContainerPanelsHandler(containerPanel, typeComboBox, componentsList);
  }

  private String cleanCorrelationName(boolean isReplacement, String simpleName) {
    return simpleName
        .replace(isReplacement ? CORRELATION_REPLACEMENT_SUFFIX : CORRELATION_EXTRACTOR_SUFFIX, "");
  }

  private void setUpComboBox(ItemEvent e, boolean isReplacement, String ruleId,
      JPanel containerPanel, ArrayList<Component> componentsList,
      RulesContainer container, PanelMouseEvent panelMouseEvent) {

    if (e.getStateChange() != ItemEvent.SELECTED) {
      return;
    }

    componentsList.forEach(containerPanel::remove);
    componentsList.clear();
    String selectedItem = Objects.requireNonNull(((JComboBox) e.getSource()).getSelectedItem())
        .toString();
    if (selectedItem.equals(DEFAULT_COMBO_VALUE)) {
      container.reDrawScroll();
      return;
    }

    String fieldName = ruleId + "-" + (isReplacement ? "replacement" : "extractor");
    List<ParameterDefinition> correlationParametersDefinition =
        isReplacement ? container.getReplacementParamsDefinition(selectedItem)
            : container.getExtractorParamsDefinition(selectedItem);

    correlationParametersDefinition.forEach(p -> {
      Map<String, String> possibleValues = p.getValueNamesMapping();
      boolean isComboBox = possibleValues != null && !possibleValues.isEmpty();

      Component field = isComboBox ? new JComboBox() : new JTextField();
      field.addMouseListener(panelMouseEvent);
      field.setPreferredSize(COMPONENT_PREFERRED_SIZE);

      if (isComboBox) {
        field.setName(fieldName + "-combo-" + p.getName());
        possibleValues.forEach((k, v) -> ((JComboBox) field).addItem(k));
        ((JComboBox) field).setToolTipText(p.getDescription());
        ((JComboBox) field)
            .setSelectedItem(!p.getDefaultValue().isEmpty() ? p.getDefaultValue() : "");
      } else {
        field.setName(fieldName + "-text-" + p.getName());
        ((JTextField) field).setToolTipText(p.getDescription());
        SwingUtils
            .setPlaceHolder(((JTextField) field), removeCorrelationRulePropertyPrefix(p.getName()));
        ((JTextField) field).setText(!p.getDefaultValue().isEmpty() ? p.getDefaultValue() : "");
      }

      containerPanel.add(field, BorderLayout.WEST);
      componentsList.add(field);
    });

    int totalPixelsRequired = (getTotalComponents()) * ESTIMATED_FIELD_TOTAL_WIDTH;

    if (totalPixelsRequired > container.getSizePanelThatContainsRules().getWidth()) {

      container.updatePanelThatContainsRules(totalPixelsRequired);
      container.getRules()
          .forEach(r -> r.setSize(new Dimension(totalPixelsRequired, r.getHeight())));
    }

    //This is required to paint the new components
    container.reDrawScroll();
  }

  private String removeCorrelationRulePropertyPrefix(String propertyName) {
    String[] propertySplit = propertyName.split("\\.");
    if (propertySplit.length > 1) {
      propertyName = propertySplit[propertySplit.length - 1];
    }
    return propertyName;
  }

  private void updatePanelsBackground(Color backgroundColor) {
    setBackground(backgroundColor);
    extractorHandler.getContainerPanel().setBackground(backgroundColor);
    replacementHandler.getContainerPanel().setBackground(backgroundColor);
  }

  public void setValuesFromRulePart(CorrelationRulePartTestElement partRuleValues,
      ContainerPanelsHandler handler) {
    String selected = partRuleValues.getClass().getSimpleName();
    handler.getComboBox().setSelectedItem(
        cleanCorrelationName(partRuleValues instanceof CorrelationReplacement, selected));
    List<String> params = partRuleValues.getParams();
    ArrayList<Component> components = handler.getListComponents();

    IntStream.range(0, components.size()).forEach(i -> {
      Component component = components.get(i);
      if (component instanceof JTextField) {
        ((JTextField) component).setText((params.size() >= i ? params.get(i) : ""));
      } else if (component instanceof JComboBox && params.size() >= i) {
        ((JComboBox) component).setSelectedItem(params.get(i));
      }
    });
  }

  public boolean isComplete() {
    return !referenceVariableField.getText().isEmpty() && !(
        Objects.requireNonNull(getReplacementHandler().getComboBox().getSelectedItem())
            .equals(DEFAULT_COMBO_VALUE) &&
            Objects.requireNonNull(getExtractorHandler().getComboBox().getSelectedItem())
                .equals(DEFAULT_COMBO_VALUE));
  }

  public JTextField getReferenceVariableField() {
    return referenceVariableField;
  }

  public ContainerPanelsHandler getExtractorHandler() {
    return extractorHandler;
  }

  public ContainerPanelsHandler getReplacementHandler() {
    return replacementHandler;
  }

  void setReferenceVariableNameText(String referenceVariableNameText) {
    this.referenceVariableField.setText(referenceVariableNameText);
  }

  @Override
  public String toString() {
    return "RulePanel{" +
        "Name=" + this.getName() +
        ", referenceVariableField=" + (referenceVariableField == null ? ""
        : referenceVariableField.getText()) +
        ", extractorHandler={" + (extractorHandler == null ? "" : extractorHandler.toString()) +
        "}, replacementHandler=" + (replacementHandler == null ? "" : replacementHandler.toString())
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RulePanel rulePanel = (RulePanel) o;
    if (rulePanel.getName().equals(this.getName())) {
      return true;
    }
    if (rulePanel.referenceVariableField != null && this.getReferenceVariableField().getText()
        .equals(rulePanel.referenceVariableField.getText())) {
      return true;
    }

    return Objects.equals(this.toString(), rulePanel.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(referenceVariableField, extractorHandler, replacementHandler);
  }

  public int getTotalComponents() {
    int totalDynamicComponents =
        this.getExtractorHandler().getListComponents().size() + this.getReplacementHandler()
            .getListComponents().size();
    //Because we need to consired the refVariable Field and the correlation type's combo boxes
    totalDynamicComponents += TOTAL_NON_DYNAMIC_COMPONENTS;
    return totalDynamicComponents;
  }

  @VisibleForTesting
  public List<Component> getReplacementsComponents() {
    return getReplacementHandler().getListComponents();
  }

  //Allows highlighting the last active panel
  private class PanelMouseEvent implements MouseListener {

    private RulesContainer container;
    private RulePanel source;

    PanelMouseEvent(RulesContainer container, RulePanel source) {
      this.container = container;
      this.source = source;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
      container.updateLastActivePanel(source);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
  }

  public class ContainerPanelsHandler {

    private JPanel containerPanel;
    private JComboBox comboBox;
    private ArrayList<Component> listComponents;

    public ContainerPanelsHandler(JPanel containerPanel, JComboBox comboBox,
        ArrayList<Component> listComponents) {
      this.containerPanel = containerPanel;
      this.comboBox = comboBox;
      this.listComponents = listComponents;
    }

    public JPanel getContainerPanel() {
      return containerPanel;
    }

    public ArrayList<Component> getListComponents() {
      return listComponents;
    }

    public JComboBox getComboBox() {
      return comboBox;
    }

    @Override
    public String toString() {
      return "ContainerPanelsHandler{" +
          ", comboBox=" + Objects.requireNonNull(comboBox.getSelectedItem()).toString() +
          ", listComponents={" + listComponents.stream().map(this::getText)
          .collect(Collectors.joining(";")) + "}}";
    }

    String getText(Component c) {
      if (c instanceof JComboBox) {
        return Objects.requireNonNull(((JComboBox) c).getSelectedItem()).toString();
      }
      return ((JTextField) c).getText();
    }
  }


}
