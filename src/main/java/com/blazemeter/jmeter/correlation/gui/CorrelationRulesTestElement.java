package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.InvalidRulePartElementException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationRulesTestElement extends ConfigTestElement implements Serializable,
    Iterable<JMeterProperty> {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationProxyControl.class);
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

  private void updateExtractorFromTestElement(CorrelationRuleTestElement e,
                                              CorrelationRule correlationRule,
                                              String referenceName) {
    try {
      //Only when no Extractor was selected, this method returns null
      correlationRule.setCorrelationExtractor(e.getCorrelationExtractor());
    } catch (InvalidRulePartElementException exception) {
      LOG.warn("Couldn't load Correlation Extractor for Rule with {}'s refVar.", referenceName,
          exception);
    }
  }

  private void updateReplacementFromTestElement(CorrelationRuleTestElement e,
                                                CorrelationRule correlationRule,
                                                String referenceName) {
    try {
      //Only when no Replacement was selected, this method returns null
      correlationRule.setCorrelationReplacement(e.getCorrelationReplacement());
    } catch (InvalidRulePartElementException exception) {
      LOG.warn("Couldn't load Correlation Replacement for Rule with {}'s refVar.", referenceName,
          exception);
    }
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

  public List<CorrelationRule> getRulesList() {
    return getRules().stream().map(r -> {
      CorrelationRule correlationRule = new CorrelationRule();
      String referenceName = r.getReferenceName();
      correlationRule.setReferenceName(referenceName);
      correlationRule.setEnabled(r.isRuleEnabled());
      updateExtractorFromTestElement(r, correlationRule, referenceName);
      updateReplacementFromTestElement(r, correlationRule, referenceName);
      return correlationRule;
    }).collect(Collectors.toList());
  }
}
