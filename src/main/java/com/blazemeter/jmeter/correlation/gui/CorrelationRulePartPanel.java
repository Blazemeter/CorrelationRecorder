package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.gui.common.HelperDialog;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.common.ThemedIcon;
import com.blazemeter.jmeter.correlation.gui.common.ThemedIconLabel;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CorrelationRulePartPanel extends JPanel {

  private static final Function<Boolean, Color> COLOR_PROVIDER = isEnable -> isEnable
      ? new JTextField().getForeground() : new JTextField().getDisabledTextColor();
  private static final ImageIcon EXPANDED_ICON = ThemedIcon.fromResourceName("expanded.png");
  private static final ImageIcon EXPANDED_WITH_VALUES_ICON = ThemedIcon
      .fromResourceName("expanded-valued.png");
  private static final ImageIcon COLLAPSED_ICON = ThemedIcon.fromResourceName("collapsed.png");
  private static final ImageIcon COLLAPSED_WITH_VALUES_ICON = ThemedIcon
      .fromResourceName("collapsed-valued.png");
  private static final int MAX_DISPLAY_NAME_SUPPORTED = 17;
  private static final int FIXED_MAX_COMBO_WIDTH = 450;
  private final JComboBox<CorrelationRulePartTestElement<?>> comboBox = new JComboBox<>();
  private final ArrayList<Component> listComponents = new ArrayList<>();
  private final JLabel helper = new ThemedIconLabel("help.png");
  private final List<Component> listAdvancedComponents = new ArrayList<>();
  private HelperDialog helperDialog;
  private String description = "";
  private Runnable fieldsListener;
  private JPanel advancedPanel;
  private JLabel collapsibleIcon;
  private Consumer<CorrelationRulePartTestElement<?>> displayExtensionConsumer;
  private CorrelationRulePartTestElement<?> lastSelection;
  private Runnable update;
  private boolean usingEnabledColors = true;

  private CorrelationRulePartPanel(Runnable update, String name,
      List<CorrelationRulePartTestElement<?>> options) {
    setName(name);
    setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
    setFocusable(true);
    prepareComboBox(update, options);
    prepareHelper(name);
    prepareAdvancedSection(update);
  }

  private void setDisplayExtensionConsumer(
      Consumer<CorrelationRulePartTestElement<?>> displayExtensionConsumer) {
    this.displayExtensionConsumer = displayExtensionConsumer;
  }

  private void prepareComboBox(Runnable update, List<CorrelationRulePartTestElement<?>> options) {
    comboBox.setMinimumSize(new Dimension(RulesContainer.FIELD_PREFERRED_SIZE.width, 50));
    comboBox.setSize(new Dimension(RulesContainer.FIELD_PREFERRED_SIZE.width, 50));

    comboBox.setName(getName() + "-comboBox");
    comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
      if (value != null) {
        JLabel label =
            new JLabel(CorrelationRulePartTestElement.getDisplayName(value));
        label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        return label;
      }
      return null;
    });
    options.forEach(comboBox::addItem);
    this.update = update;
    comboBox.addItemListener(this::onChangeEvent);
    add(comboBox);
    add(helper);
  }

  private void onChangeEvent(ItemEvent e) {
    if (e.getStateChange() != ItemEvent.SELECTED) {
      return;
    }
    changeEvent();
  }

  private void changeEvent() {
    if (!isMoreSelected()) {
      lastSelection = getSelectedItem();
    }

    if (isMoreSelected()) {
      List<String> oldValues = getComponentsValues();
      setSelectedItem(lastSelection);
      updateComponentValues(oldValues);
      displayExtensionConsumer.accept(lastSelection);
      return;
    }

    removeComponents();
    this.description = getSelectedItem().getDescription();
    getSelectedItem().getParamsDefinition().forEach(p -> {
      Component field = buildField(p);
      if (p.isAdvanced()) {
        listAdvancedComponents.add(field);
        advancedPanel.add(field);
        field.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent e) {
            updateCollapsedIcon(isExpanded());
            update.run();
          }
        });
      } else {
        listComponents.add(field);
      }
    });

    addFields();
    fieldsListener.run();
    updateChildrenEnableColor();
    revalidate();
    repaint();
    update.run();
  }

  private void removeComponents() {
    listAdvancedComponents.forEach(advancedPanel::remove);
    listAdvancedComponents.clear();
    listComponents.forEach(this::remove);
    listComponents.clear();
    remove(collapsibleIcon);
    remove(advancedPanel);
  }

  protected CorrelationRulePartTestElement<?> getSelectedItem() {
    return (CorrelationRulePartTestElement<?>) comboBox.getModel().getSelectedItem();
  }

  protected void setSelectedItem(CorrelationRulePartTestElement<?> element) {
    comboBox.getModel().setSelectedItem(element);
  }

  public void addFields() {
    listComponents.forEach(this::add);
    //Don't add "advanced section icons" if there isn't advanced fields
    if (!listAdvancedComponents.isEmpty()) {
      add(collapsibleIcon);
      add(advancedPanel);
      collapsibleIcon.setIcon(COLLAPSED_ICON);
      advancedPanel.setVisible(false);
    }
  }

  private boolean isExpanded() {
    String iconString = collapsibleIcon.getIcon().toString();
    return iconString.equals(EXPANDED_ICON.toString())
        || iconString.equals(EXPANDED_WITH_VALUES_ICON.toString());
  }

  protected Component buildField(ParameterDefinition p) {
    Component field = (Component) ((ParameterDefinition.builderFromRawParameterDefinition(p)))
        .build(getName());
    field.setPreferredSize(RulesContainer.FIELD_PREFERRED_SIZE);
    return field;
  }

  private void prepareHelper(String name) {
    helper.setName(name + "-helper");
    helper.setToolTipText("Get more information");
    helper.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    int helperSideLength = 35;
    Dimension helperDimension = new Dimension(helperSideLength, helperSideLength);
    helper.setPreferredSize(helperDimension);
    helper.setSize(helperDimension);
    helper.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        helperDialog = new HelperDialog(CorrelationRulePartPanel.this);
        helperDialog.setTitle("Selector Information");
        helperDialog.updateDialogContent(description);
        helperDialog.setVisible(true);
      }
    });
  }

  private void prepareAdvancedSection(Runnable update) {
    advancedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    advancedPanel.setName(getName() + "-advancedPanel");
    collapsibleIcon = new JLabel();
    collapsibleIcon.setIcon(COLLAPSED_ICON);
    collapsibleIcon.setName(getName() + "-collapsedIcon");
    collapsibleIcon.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        toggleAdvanced(update);
      }
    });
  }

  @VisibleForTesting
  protected void toggleAdvanced(Runnable update) {
    boolean isExpanded = isExpanded();
    updateCollapsedIcon(!isExpanded);
    advancedPanel.setVisible(!isExpanded);
    setSize(new Dimension(getWidth(), getCellNeededHeight()));
    update.run();
  }

  private void updateCollapsedIcon(boolean isExpanded) {
    boolean isValued = hasNonDefaultAdvancedValues();
    if (isExpanded) {
      collapsibleIcon.setIcon(isValued ? EXPANDED_WITH_VALUES_ICON : EXPANDED_ICON);
    } else {
      collapsibleIcon.setIcon(isValued ? COLLAPSED_WITH_VALUES_ICON : COLLAPSED_ICON);
    }
  }

  private boolean hasNonDefaultAdvancedValues() {
    if (isMoreSelected()) {
      return false;
    }
    int step = listComponents.size();
    List<String> values = getComponentsValues();
    List<ParameterDefinition> definitions = getSelectedItem().getParamsDefinition();
    for (int i = 0; i < listAdvancedComponents.size(); i++) {
      int index = step + i;
      if (!definitions.get(index).getDefaultValue().equals(values.get(index))) {
        return true;
      }
    }
    return false;
  }

  public List<String> getComponentsValues() {
    List<Component> allComponents = getListComponents();
    return allComponents.stream().map(component -> {
      if (component instanceof JTextField) {
        return ((JTextField) component).getText();
      } else if (component instanceof JComboBox) {
        return (String) ((JComboBox<String>) component).getSelectedItem();
      } else {
        return Boolean.toString(((JCheckBox) component).isSelected());
      }
    }).collect(Collectors.toList());
  }

  public void useEnabledColors(boolean useEnabledColors) {
    usingEnabledColors = useEnabledColors;
    updateChildrenEnableColor();
  }

  private void updateChildrenEnableColor() {
    Color color = SwingUtils.getEnabledForegroundColor(usingEnabledColors);
    comboBox.setForeground(color);
    listComponents.forEach(c -> c.setForeground(color));
    listAdvancedComponents.forEach(c -> c.setForeground(color));
    repaint();
  }
  
  public int getCellNeededHeight() {
    if (listAdvancedComponents.isEmpty()) {
      return RulesContainer.ROW_PREFERRED_HEIGHT;
    }

    int advSectionWidth = listAdvancedComponents.size() * RulesContainer.FIELD_PREFERRED_SIZE.width;

    int lines =
        ((int) Math.ceil((double) advSectionWidth / (double) calculateRequiredColumnWidth()))
            + (isExpanded() ? 1 : 0);
    return lines * RulesContainer.ROW_PREFERRED_HEIGHT;
  }

  public int calculateRequiredColumnWidth() {
    int defaultComponentsWidth =
        calculateRequiredWithFromComboSize() + helper.getWidth() + (listComponents.size()
            * RulesContainer.FIELD_PREFERRED_SIZE.width);

    if (listAdvancedComponents.isEmpty()) {
      return defaultComponentsWidth;
    }

    //Using +10 as a gap
    return Math.max(defaultComponentsWidth + collapsibleIcon.getWidth(),
        listAdvancedComponents.size() * RulesContainer.FIELD_PREFERRED_SIZE.width + 10);
  }

  private int calculateRequiredWithFromComboSize() {

    int currentMaxDisplayName = 0;
    for (int i = 0; i < comboBox.getModel().getSize(); i++) {
      CorrelationRulePartTestElement<?> item =
          comboBox.getModel().getElementAt(i);
      int aux = (item == null ? currentMaxDisplayName
          : CorrelationRulePartTestElement.getDisplayName(item).length());
      currentMaxDisplayName = Math.max(currentMaxDisplayName, aux);
    }
    return (FIXED_MAX_COMBO_WIDTH * currentMaxDisplayName / MAX_DISPLAY_NAME_SUPPORTED);
  }

  public void setValuesFromRulePart(CorrelationRulePartTestElement<?> rulePartTestElement) {
    setSelectedItem(rulePartTestElement);
    changeEvent();
    updateComponentValues(rulePartTestElement.getParams());
  }

  public void updateComponentValues(List<String> values) {
    List<Component> allComponents = getListComponents();
    for (int i = 0; i < allComponents.size(); i++) {
      Component component = allComponents.get(i);
      String value = values.get(i);
      if (component instanceof JTextField) {
        ((JTextField) component).setText(value);
      } else if (component instanceof JComboBox) {
        ((JComboBox<String>) component).setSelectedItem(value);
      } else if (component instanceof JCheckBox) {
        ((JCheckBox) component).setSelected(Boolean.parseBoolean(value));
      }
    }

    if (!listAdvancedComponents.isEmpty()) {
      updateCollapsedIcon(isExpanded());
    }
  }

  public void updateComboOptions(List<CorrelationRulePartTestElement<?>> options) {
    DefaultComboBoxModel<CorrelationRulePartTestElement<?>> model = new DefaultComboBoxModel<>();
    options.forEach(model::addElement);
    List<String> values = getComponentsValues();
    CorrelationRulePartTestElement<?> selectedItem = getSelectedItem();
    model.setSelectedItem(selectedItem);
    comboBox.setModel(model);
    updateComponentValues(values);
  }

  public JPanel getAdvancedPanel() {
    return advancedPanel;
  }

  public List<Component> getListComponents() {
    List<Component> allComponents = new ArrayList<>(listComponents);
    allComponents.addAll(listAdvancedComponents);
    return allComponents;
  }

  private boolean isMoreSelected() {
    CorrelationRulePartTestElement<?> selectedItem = getSelectedItem();
    return CorrelationComponentsRegistry.MORE_EXTRACTOR.equals(selectedItem)
        || CorrelationComponentsRegistry.MORE_REPLACEMENT.equals(selectedItem);
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
    if (!(o instanceof CorrelationRulePartPanel)) {
      return false;
    }

    CorrelationRulePartPanel panel = (CorrelationRulePartPanel) o;

    return CorrelationRulePartTestElement.getDisplayName(getSelectedItem())
        .equals(CorrelationRulePartTestElement.getDisplayName(panel.getSelectedItem()))
        && getComponentsValues().equals(panel.getComponentsValues());
  }

  @Override
  public String toString() {
    return "ConfigurationPanel {" +
        "selectedItem=" + CorrelationRulePartTestElement.getDisplayName(getSelectedItem()) +
        ", values=" + getComponentsValues() +
        '}';
  }
  
  @VisibleForTesting
  public JLabel getHelper() {
    return helper;
  }

  public void setFieldsListener(Runnable fieldsListener) {
    this.fieldsListener = fieldsListener;
  }

  public <T extends JComponent> Optional<T> retrieveComponent(
      Class<T> componentClass, String componentName) {
    return listAdvancedComponents.stream()
        .filter(c -> c.getClass().equals(componentClass) && c.getName().contains(componentName))
        .map(componentClass::cast)
        .findAny();
  }

  public static class Builder {

    private Runnable tableUpdateRunner;
    private Runnable fieldsListener;
    private Consumer<CorrelationRulePartTestElement<?>> displayExtensionConsumer;
    private String name;
    private List<CorrelationRulePartTestElement<?>> options;

    public Builder() {
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withOptions(List<CorrelationRulePartTestElement<?>> options) {
      this.options = options;
      return this;
    }

    public Builder withTableUpdateRunner(Runnable tableUpdateRunner) {
      this.tableUpdateRunner = tableUpdateRunner;
      return this;
    }

    public Builder withFieldsListener(Runnable fieldsListener) {
      this.fieldsListener = fieldsListener;
      return this;
    }

    public Builder withDisplayExtensionConsumer(
        Consumer<CorrelationRulePartTestElement<?>> displayExtensionConsumer) {
      this.displayExtensionConsumer = displayExtensionConsumer;
      return this;
    }

    public CorrelationRulePartPanel build() {
      CorrelationRulePartPanel rulePartPanel = new CorrelationRulePartPanel(tableUpdateRunner, name,
          options);
      rulePartPanel.setFieldsListener(fieldsListener);
      rulePartPanel.setDisplayExtensionConsumer(displayExtensionConsumer);
      return rulePartPanel;
    }
  }
}
