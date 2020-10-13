package com.blazemeter.jmeter.correlation.siebel;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.TextParameterDefinition;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.oro.text.regex.MalformedPatternException;

/**
 * Handles the replacement of Row Ids on Siebel CRM Correlations
 */
public class SiebelRowIdCorrelationReplacement extends RegexCorrelationReplacement<SiebelContext> {

  // Constructor added in order to satisfy json conversion
  public SiebelRowIdCorrelationReplacement() {
  }

  public SiebelRowIdCorrelationReplacement(String regex) {
    super(regex);
  }

  @Override
  public String getDisplayName() {
    return "Siebel Row Id";
  }

  @Override
  public List<ParameterDefinition> getParamsDefinition() {
    return Collections.singletonList(
        new TextParameterDefinition(REPLACEMENT_REGEX_PROPERTY_NAME,
            REPLACEMENT_REGEX_PROPERTY_DESCRIPTION, REGEX_DEFAULT_VALUE));
  }

  /**
   * Handles the replacement of the row ids.
   *
   * Receives an input and applies the replacement based in the values stored in the {@link
   * SiebelContext}, followed by "_rowId". Works together with the {@link
   * com.blazemeter.jmeter.correlation.siebel.SiebelRowCorrelationExtractor}.
   *
   * The method is overwritten because is necessary to alter the way inputs are made but not how the
   * Regular Expression is handled by the father class
   * {@link com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement}
   * .
   *
   * @param input the input against the condition will test and replacements will be applied
   * @param regex the regular expression used to eval the input
   * @param variableName the variable name ignored in this case
   * @param vars the stored values during the recording
   * @return the resultant input. Will be the same if it doesn't meet the condition
   */
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
        "paramValues='" + getParams() + '\'' +
        '}';
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return SiebelContext.class;
  }
}
