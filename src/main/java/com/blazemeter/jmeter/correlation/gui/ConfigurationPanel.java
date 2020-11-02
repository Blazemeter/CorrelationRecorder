package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ConfigurationPanel extends JPanel {

  private static final Function<Boolean, Color> COLOR_PROVIDER = isEnable -> isEnable ?
      new JTextField().getForeground() : new JTextField().getDisabledTextColor();
  private final JComboBox<CorrelationRulePartTestElement<?>> comboBox = new JComboBox<>();
  private final ArrayList<Component> listComponents = new ArrayList<>();
  private final JLabel helper = new ThemedIconLabel("help.png");
  private final int helperSideLength = 35;
  private HelperDialog helperDialog;
  private String description = "";
  private Runnable updateFieldListeners;
  private boolean enabled = true;

  public ConfigurationPanel(Runnable update, String name,
      List<CorrelationRulePartTestElement<?>> options) {
    setName(name);
    setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
    setFocusable(true);
    setPreferredSize(RulesContainer.FIELD_PREFERRED_SIZE);
    setSize(RulesContainer.FIELD_PREFERRED_SIZE);
    prepareComboBox(update, options);
    prepareHelper(name);
  }

  public List<String> getValues() {
    return listComponents.stream().map(component -> {
      if (component instanceof JTextField) {
        return ((JTextField) component).getText();
      } else if (component instanceof JComboBox) {
        return (String) ((JComboBox<String>) component).getSelectedItem();
      } else {
        return Boolean.toString(((JCheckBox) component).isSelected());
      }
    }).collect(Collectors.toList());
  }

  public void setParamValues(List<String> values) {
    for (int i = 0; i < listComponents.size(); i++) {
      Component component = listComponents.get(i);
      String value = values.get(i);
      if (component instanceof JTextField) {
        ((JTextField) component).setText(value);
      } else if (component instanceof JComboBox) {
        ((JComboBox<String>) component).setSelectedItem(value);
      } else if (component instanceof JCheckBox) {
        ((JCheckBox) component).setSelected(Boolean.parseBoolean(value));
      }
    }
  }

  public ArrayList<Component> getListComponents() {
    return listComponents;
  }

  protected CorrelationRulePartTestElement<?> getSelectedItem() {
    return (CorrelationRulePartTestElement<?>) comboBox.getModel().getSelectedItem();
  }

  protected void setSelectedItem(CorrelationRulePartTestElement<?> element) {
    comboBox.getModel().setSelectedItem(element);
  }

  public int getSumComponentsWidth() {
    return (listComponents.size()) * (RulesContainer.FIELD_PREFERRED_SIZE.width)
        + helperSideLength
        + /* current combo size used as a gap.
         Useful when bigger combo */  getRequiredWithFromComboSize();
  }

  private int getRequiredWithFromComboSize() {
    final int MAX_DISPLAY_NAME_SUPPORTED = 17;
    final int FIXED_MAX_COMBO_WIDTH = 450;
    int currentMaxDisplayName = 0;
    for (int i = 0; i < comboBox.getModel().getSize(); i++) {
      CorrelationRulePartTestElement<?> item =
          comboBox.getModel().getElementAt(i);
      int aux = (item == null ? currentMaxDisplayName : item.getDisplayName().length());
      currentMaxDisplayName = Math.max(currentMaxDisplayName, aux);
    }
    return (FIXED_MAX_COMBO_WIDTH * currentMaxDisplayName / MAX_DISPLAY_NAME_SUPPORTED);
  }

  public void setValuesFromRulePart(CorrelationRulePartTestElement<?> rulePartTestElement) {
    //This automatically triggers the onChangeEvent method, updating the component's list
    setSelectedItem(rulePartTestElement);
    setParamValues(rulePartTestElement.getParams());
  }

  private void prepareComboBox(Runnable update, List<CorrelationRulePartTestElement<?>> options) {
    comboBox.setMinimumSize(new Dimension(RulesContainer.FIELD_PREFERRED_SIZE.width, 50));
    comboBox.setSize(new Dimension(RulesContainer.FIELD_PREFERRED_SIZE.width, 50));

    comboBox.setName(getName() + "-comboBox");
    comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
      if (value != null) {
        JLabel label = new JLabel(value.getDisplayName());
        label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        return label;
      }
      return null;
    });
    options.forEach(comboBox::addItem);
    comboBox.addItemListener(e -> onChangeEvent(e, update));
  }

  public void setUpdateFieldListeners(Runnable updateFieldListeners) {
    this.updateFieldListeners = updateFieldListeners;
  }

  public Optional<? extends JComponent> retrieveComponent(
      Class<? extends JComponent> componentClass,
      String componentName) {
    return listComponents
        .stream()
        .filter(c -> c.getClass().equals(componentClass) && c.getName().contains(componentName))
        .map(componentClass::cast)
        .findAny();
  }

  private void onChangeEvent(ItemEvent e, Runnable update) {
    if (e.getStateChange() != ItemEvent.SELECTED) {
      return;
    }

    removeComponents();
    setHelperDescription(getSelectedItem().getDescription());
    if (isNoneSelected()) {
      addFields();
      repaint();
      update.run();
      return;
    }

    getSelectedItem().getParamsDefinition().forEach(p -> {
      Component field = (Component) ((ParameterDefinition.Builder<?>) p).build(getName());
      field.setPreferredSize(RulesContainer.FIELD_PREFERRED_SIZE);
      listComponents.add(field);
    });

    addFields();
    updateFieldListeners.run();
    //Added to apply disabled on fields even if the combo changes values
    setEnabled(this.enabled); 
    revalidate();
    repaint();
    update.run();
  }

  public void addFields() {
    add(comboBox);
    add(helper);
    listComponents.forEach(this::add);
  }

  private void prepareHelper(String name) {
    helper.setName(name + "-helper");
    helper.setToolTipText("Get more information");
    helper.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    Dimension helperDimension = new Dimension(helperSideLength, helperSideLength);
    helper.setPreferredSize(helperDimension);
    helper.setSize(helperDimension);
    helper.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        helperDialog = new HelperDialog(ConfigurationPanel.this);
        helperDialog.setTitle("Selector Information");
        helperDialog.updateDialogContent(description);
        helperDialog.setVisible(true);
      }
    });
  }

  public void removeComponents() {
    listComponents.forEach(this::remove);
    for (Component c : getComponents()) {
      if ((c instanceof JComboBox || c instanceof JTextField) && (!c.equals(comboBox))) {
        remove(c);
      }
    }
    listComponents.clear();
  }

  public boolean isNoneSelected() {
    CorrelationRulePartTestElement<?> selectedItem = getSelectedItem();
    return CorrelationComponentsRegistry.NONE_EXTRACTOR.equals(selectedItem) ||
        CorrelationComponentsRegistry.NONE_REPLACEMENT.equals(selectedItem);
  }

  @Override
  public int hashCode() {
    return Objects.hash(comboBox, getListComponents());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConfigurationPanel)) {
      return false;
    }

    ConfigurationPanel panel = (ConfigurationPanel) o;

    return getSelectedItem().getDisplayName().equals(panel.getSelectedItem().getDisplayName())
        && getValues().equals(panel.getValues());
  }

  @Override
  public String toString() {
    return "ConfigurationPanel {" +
        "selectedItem=" + getSelectedItem().getDisplayName() +
        ", values=" + getValues() +
        '}';
  }

  public void updateComboOptions(List<CorrelationRulePartTestElement<?>> options) {
    DefaultComboBoxModel<CorrelationRulePartTestElement<?>> model = new DefaultComboBoxModel<>();
    options.forEach(model::addElement);
    List<String> values = getValues();
    CorrelationRulePartTestElement<?> selectedItem = getSelectedItem();
    model.setSelectedItem(selectedItem);
    comboBox.setModel(model);
    setParamValues(values);
  }

  //simulate the enable/disable by changing font color
  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    comboBox.setForeground(COLOR_PROVIDER.apply(enabled));
    listComponents.forEach(c -> c.setForeground(COLOR_PROVIDER.apply(enabled)));
  }

  @VisibleForTesting
  public JLabel getHelper() {
    return helper;
  }

  public void setHelperDescription(String description) {
    this.description = description;
  }
}