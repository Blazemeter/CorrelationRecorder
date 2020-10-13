package com.blazemeter.jmeter.correlation.core;

import com.blazemeter.jmeter.correlation.gui.PlaceHolderTextField;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * @deprecated Since the incorporation of new components in version v1.1, ParameterDefinition class
 * is expected to become abstract, in order to ease the addition of new components. See {@link
 * TextParameterDefinition}, {@link ComboParameterDefinition}, {@link CheckBoxParameterDefinition} for
 * the creation of new components.
 */

@Deprecated
public class ParameterDefinition {

  private final String name;
  private final String description;
  private final String defaultValue;
  private final Map<String, String> availableValuesToDisplayNamesMapping;

  public ParameterDefinition(String name, String description, String defaultValue,
      Map<String, String> availableValuesToDisplayNamesMapping) {
    this.name = name;
    this.description = description;
    this.defaultValue = defaultValue;
    this.availableValuesToDisplayNamesMapping = availableValuesToDisplayNamesMapping;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public Map<String, String> getValueNamesMapping() {
    return availableValuesToDisplayNamesMapping;
  }

  @Override
  public String toString() {
    //Added the parenthesis to avoid issues with the concatenation
    return "ParameterDefinition{" +
        "name='" + name + '\'' +
        ", defaultValue='" + defaultValue + '\'' +
        ", availableValuesToDisplayNamesMapping=" + (availableValuesToDisplayNamesMapping != null
        ? availableValuesToDisplayNamesMapping.keySet().stream()
        .map(key -> key + "=" + availableValuesToDisplayNamesMapping.get(key))
        .collect(Collectors.joining(", ", "{", "}")) : "{}") + "}";
  }


  public static class TextParameterDefinition extends ParameterDefinition implements
      Builder<JTextField> {

    public TextParameterDefinition(String name, String description, String defaultValue) {
      super(name, description, defaultValue, null);
    }

    @Override
    public PlaceHolderTextField build(String prefix) {
      PlaceHolderTextField field = new PlaceHolderTextField();
      field.setName(prefix + "-text-" + getName());
      field.setToolTipText(getDescription());
      field.setPlaceHolder(getDescription());
      field.setText(!getDefaultValue().isEmpty() ? getDefaultValue() : "");
      return field;
    }
  }

  public interface Builder<T> {

    T build(String prefix);
  }

  public static class ComboParameterDefinition extends ParameterDefinition implements
      Builder<JComboBox<String>> {

    public ComboParameterDefinition(String name, String description, String defaultValue,
        Map<String, String> availableValuesToDisplayNamesMapping) {
      super(name, description, defaultValue, availableValuesToDisplayNamesMapping);
    }

    @Override
    public JComboBox<String> build(String prefix) {
      JComboBox<String> combo = new JComboBox<>();
      combo.setName(prefix + "-combo-" + getName());
      getValueNamesMapping().forEach((k, v) -> combo.addItem(k));
      combo.setToolTipText(getDescription());
      combo.setSelectedItem(!getDefaultValue().isEmpty() ? getDefaultValue() : "");
      return combo;
    }
  }

  public static class CheckBoxParameterDefinition extends ParameterDefinition implements
      Builder<JCheckBox> {
    
    public CheckBoxParameterDefinition(String name, String description, boolean defaultValue) {
      super(name, description, Boolean.toString(defaultValue), null);
    }

    @Override
    public JCheckBox build(String prefix) {
      JCheckBox check = new JCheckBox(getDescription());
      check.setName(prefix + "-check-" + getName());
      check.setToolTipText(getDescription());
      check.setSelected(Boolean.parseBoolean(getDefaultValue()));
      return check;
    }
  }
}
