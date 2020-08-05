package com.blazemeter.jmeter.correlation.siebel;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import java.util.Map;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.oro.text.regex.MalformedPatternException;

public class SiebelRowIdCorrelationReplacement extends RegexCorrelationReplacement<SiebelContext> {

  // Constructor added in order to satisfy json conversion
  public SiebelRowIdCorrelationReplacement() {
  }

  public SiebelRowIdCorrelationReplacement(String regex) {
    super(regex);
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
  protected String replaceWithRegex(String input, String regex, String variableName,
      JMeterVariables vars)
      throws MalformedPatternException {
    for (Map.Entry<String, String> rowVar : context.getRowVars().entrySet()) {
      input = super.replaceWithRegex(input, regex, rowVar.getValue() + "_rowId", vars);
    }
    return input;
  }

  @Override
  public String toString() {
    return "SiebelRowIdCorrelationReplacement{" +
        "regex='" + regex + '\'' +
        '}';
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return SiebelContext.class;
  }
}
