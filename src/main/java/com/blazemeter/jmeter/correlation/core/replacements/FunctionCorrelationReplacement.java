package com.blazemeter.jmeter.correlation.core.replacements;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.oro.text.regex.MalformedPatternException;

public class FunctionCorrelationReplacement<T extends CorrelationContext> extends
    RegexCorrelationReplacement<T> {

  // Constructor added in order to satisfy json conversion
  public FunctionCorrelationReplacement() {
  }

  public FunctionCorrelationReplacement(String regex) {
    super(regex);
  }

  @Override
  public String replaceWithRegex(String input, String regex, String variableName,
      JMeterVariables vars) throws MalformedPatternException {
    return replaceExpression(input, regex, variableName, match -> true);
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setProperty(REPLACEMENT_REGEX_PROPERTY_NAME, regex);
  }

  @Override
  public void update(CorrelationRuleTestElement testElem) {
    super.update(testElem);
    regex = testElem.getPropertyAsString(REPLACEMENT_REGEX_PROPERTY_NAME);
  }

  @Override
  public String toString() {
    return "FunctionCorrelationReplacement{" +
        "regex='" + regex + '\'' +
        '}';
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return null;
  }
}
