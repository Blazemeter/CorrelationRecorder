package com.blazemeter.jmeter.correlation.gui.common;

import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;

public enum RulePartType {
  EXTRACTOR("Correlation Extractor"),
  REPLACEMENT("Correlation Replacement");

  private final String name;

  RulePartType(String name) {
    this.name = name;
  }

  public static RulePartType fromComponent(CorrelationRulePartTestElement<?> component) {
    if (component instanceof CorrelationExtractor
        || component.equals(CorrelationComponentsRegistry.NONE_EXTRACTOR)) {
      return EXTRACTOR;
    } else if (component instanceof CorrelationReplacement
        || component.equals(CorrelationComponentsRegistry.NONE_REPLACEMENT)) {
      return REPLACEMENT;
    } else {
      throw new IllegalArgumentException(
          "Unknown component type " + component.getClass().getCanonicalName());
    }
  }

  @Override
  public String toString() {
    return name;
  }

}
