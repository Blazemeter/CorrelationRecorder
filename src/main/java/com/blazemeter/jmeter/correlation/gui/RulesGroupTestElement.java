package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.RulesGroup;
import java.io.Serializable;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;

public class RulesGroupTestElement extends ConfigTestElement implements Serializable {

  private static final String CORRELATION_GROUP_ID = "CorrelationProxyControl.RulesGroup.id";
  private static final String CORRELATION_GROUP_RULES = "CorrelationProxyControl.RulesGroup.rules";

  public RulesGroupTestElement() {
    this("", true, new CorrelationRulesTestElement());
  }

  public RulesGroupTestElement(String id, boolean enable,
      CorrelationRulesTestElement correlationRulesTestElement) {
    setProperty(new StringProperty(CORRELATION_GROUP_ID, id));
    setProperty(new TestElementProperty(CORRELATION_GROUP_RULES, correlationRulesTestElement));
    setEnabled(enable);
  }

  public StringProperty getIdProperty() {
    return (StringProperty) getProperty(CORRELATION_GROUP_ID);
  }

  public CorrelationRulesTestElement getRulesGroupsProperty() {
    return (CorrelationRulesTestElement) getProperty(CORRELATION_GROUP_RULES).getObjectValue();
  }

  @Override
  public void clear() {
    super.clear();
    setProperty(new StringProperty(CORRELATION_GROUP_ID, ""));
    setProperty(
        new TestElementProperty(CORRELATION_GROUP_RULES, new CorrelationRulesTestElement()));
  }

  public RulesGroup getRulesGroup() {
    RulesGroup.Builder builder = new RulesGroup.Builder();
    builder.withId(getIdProperty().getStringValue());
    builder.withRules(getRulesGroupsProperty().getRulesList());
    builder.isEnabled(isEnabled());
    return builder.build();
  }
}
