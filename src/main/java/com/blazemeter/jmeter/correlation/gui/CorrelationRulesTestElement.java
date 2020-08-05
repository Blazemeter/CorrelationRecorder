package com.blazemeter.jmeter.correlation.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;

public class CorrelationRulesTestElement extends ConfigTestElement implements Serializable,
    Iterable<JMeterProperty> {

  private static final String RULES = "CorrelationRules.rules";

  public CorrelationRulesTestElement() {
    this(new ArrayList<>());
  }

  public CorrelationRulesTestElement(List<CorrelationRuleTestElement> rules) {
    setProperty(new CollectionProperty(RULES, rules));
  }

  public List<CorrelationRuleTestElement> getRules() {
    PropertyIterator iter = getCollectionProperty().iterator();
    List<CorrelationRuleTestElement> rules = new ArrayList<>();
    while (iter.hasNext()) {
      rules.add((CorrelationRuleTestElement) iter.next().getObjectValue());
    }
    return rules;
  }

  private CollectionProperty getCollectionProperty() {
    return (CollectionProperty) getProperty(RULES);
  }

  @Override
  public void clear() {
    super.clear();
    setProperty(new CollectionProperty(RULES, new ArrayList<CorrelationRuleTestElement>()));
  }

  @Override
  public Iterator<JMeterProperty> iterator() {
    return getCollectionProperty().iterator();
  }
}
