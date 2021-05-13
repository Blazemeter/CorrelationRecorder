package com.blazemeter.jmeter.correlation.core;

import com.blazemeter.jmeter.correlation.gui.common.PlaceHolderTextField;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * @deprecated Since the incorporation of new components in version v1.1, ParameterDefinition
 * class is expected to become abstract, in order to ease the addition of new components. See
 * {@link TextParameterDefinition}, {@link ComboParameterDefinition},
 * {@link CheckBoxParameterDefinition} for the creation of new components.
 */

@Deprecated
public class ParameterDefinition {

  private final String name;
  private final String description;
  private final String defaultValue;
  private final Map<String, String> availableValuesToDisplayNamesMapping;
  private final boolean advanced;

  //Left for backward compatibility
  public ParameterDefinition(String name, String description, String defaultValue,
      Map<String, String> availableValuesToDisplayNamesMapping) {
    this.name = name;
    this.description = description;
    this.defaultValue = defaultValue;
    this.availableValuesToDisplayNamesMapping = availableValuesToDisplayNamesMapping;
    this.advanced = false;
  }

  public ParameterDefinition(String name, String description, String defaultValue,
      Map<String, String> availableValuesToDisplayNamesMapping, boolean advanced) {
    this.name = name;
    this.description = description;
    this.defaultValue = defaultValue;
    this.availableValuesToDisplayNamesMapping = availableValuesToDisplayNamesMapping;
    this.advanced = advanced;
  }

  /*Ensure backward compatibility with extensions created
  before new ParameterDefinitions (text, combo, check)*/
  public static ParameterDefinition.Builder<?> builderFromRawParameterDefinition(
      ParameterDefinition definition) {
    if (definition instanceof TextParameterDefinition
        || definition instanceof ComboParameterDefinition
        || definition instanceof CheckBoxParameterDefinition) {
      return (Builder<?>) definition;
    }
    if (definition.availableValuesToDisplayNamesMapping == null) {
      //Assume that definition is a TextParameter
      return new TextParameterDefinition(definition.name,
          definition.description, definition.defaultValue);
    } else {
      return new ComboParameterDefinition(definition.name, definition.description,
          definition.defaultValue, definition.availableValuesToDisplayNamesMapping);
    }
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

  public boolean isAdvanced() {
    return advanced;
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

  public interface Builder<T> {

    T build(String prefix);
  }

  public static class TextParameterDefinition extends ParameterDefinition implements
      Builder<JTextField> {

    //Left for backward compatibility
    public TextParameterDefinition(String name, String description, String defaultValue) {
      super(name, description, defaultValue, null);
    }

    public TextParameterDefinition(String name, String description, String defaultValue,
        boolean advanced) {
      super(name, description, defaultValue, null, advanced);
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

  public static class ComboParameterDefinition extends ParameterDefinition implements
      Builder<JComboBox<String>> {

    //Left for backward compatibility
    public ComboParameterDefinition(String name, String description, String defaultValue,
        Map<String, String> availableValuesToDisplayNamesMapping) {
      super(name, description, defaultValue, availableValuesToDisplayNamesMapping);
    }

    public ComboParameterDefinition(String name, String description, String defaultValue,
        Map<String, String> availableValuesToDisplayNamesMapping, boolean advanced) {
      super(name, description, defaultValue, availableValuesToDisplayNamesMapping, advanced);
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

    //Left for backward compatibility
    public CheckBoxParameterDefinition(String name, String description, boolean defaultValue) {
      super(name, description, Boolean.toString(defaultValue), null);
    }

    public CheckBoxParameterDefinition(String name, String description, boolean defaultValue,
        boolean advanced) {
      super(name, description, Boolean.toString(defaultValue), null, advanced);
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
