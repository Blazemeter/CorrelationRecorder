package com.blazemeter.jmeter.correlation.core;

import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

public class CorrelationRule {

  private String referenceName;
  private CorrelationExtractor<?> correlationExtractor;
  private CorrelationReplacement<?> correlationReplacement;
  private boolean enabled;

  public CorrelationRule(String referenceName, CorrelationExtractor<?> correlationExtractor,
      CorrelationReplacement<?> correlationReplacement) {
    this.referenceName = referenceName;
    this.enabled = true;
    this.correlationExtractor = correlationExtractor;
    if (correlationExtractor != null) {
      correlationExtractor.setVariableName(referenceName);
    }
    this.correlationReplacement = correlationReplacement;
    if (correlationReplacement != null) {
      correlationReplacement.setVariableName(referenceName);
    }
  }

  // Constructor added in order to satisfy json conversion
  public CorrelationRule() {
    referenceName = "";
    correlationExtractor = null;
    correlationReplacement = null;
    enabled = true;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public CorrelationRuleTestElement buildTestElement(
      CorrelationComponentsRegistry registry) {
    CorrelationRuleTestElement testElem = new CorrelationRuleTestElement(registry);
    testElem.setReferenceName(referenceName);
    testElem.setRuleEnable(enabled);

    if (correlationExtractor != null) {
      correlationExtractor.updateTestElem(testElem);
    }
    if (correlationReplacement != null) {
      correlationReplacement.updateTestElem(testElem);
    }
    return testElem;
  }

  public CorrelationExtractor<?> getCorrelationExtractor() {
    return correlationExtractor;
  }

  public void setCorrelationExtractor(
      CorrelationExtractor<?> correlationExtractor) {
    this.correlationExtractor = correlationExtractor;
  }

  public CorrelationReplacement<?> getCorrelationReplacement() {
    return correlationReplacement;
  }

  public void setCorrelationReplacement(
      CorrelationReplacement<?> correlationReplacement) {
    this.correlationReplacement = correlationReplacement;
  }

  public String getReferenceName() {
    return referenceName;
  }

  public void setReferenceName(String referenceName) {
    this.referenceName = referenceName;
  }

  @Override
  public String toString() {
    return "CorrelationRule{" +
        "referenceName='" + referenceName + '\'' +
        ", correlationExtractor=" + correlationExtractor +
        ", correlationReplacement=" + correlationReplacement +
        ", enabled=" + enabled +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CorrelationRule that = (CorrelationRule) o;
    return Objects.equals(referenceName, that.referenceName) &&
        Objects.equals(correlationExtractor, that.correlationExtractor) &&
        Objects.equals(correlationReplacement, that.correlationReplacement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(referenceName, correlationExtractor, correlationReplacement);
  }

  @JsonIgnore
  public boolean isComplete() {
    return correlationExtractor != null
        || correlationReplacement != null;
  }
}
