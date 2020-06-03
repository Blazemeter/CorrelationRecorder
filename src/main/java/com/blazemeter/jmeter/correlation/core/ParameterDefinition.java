package com.blazemeter.jmeter.correlation.core;

import java.util.Map;
import java.util.stream.Collectors;

public class ParameterDefinition {

  private String name;
  private String description;
  private String defaultValue;
  private Map<String, String> availableValuesToDisplayNamesMapping;

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
}
