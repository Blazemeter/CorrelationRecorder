package com.blazemeter.jmeter.correlation.core.replacements;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.oro.text.regex.MalformedPatternException;

/**
 * @deprecated This class is no longer maintained. The v1.1 brought along the
 * incorporation of Function functionalities inside @see com.blazemeter.jmeter.correlation.core
 * .extractors.RegexCorrelationExtractor.
 * @since 1.1
 */

@Deprecated
public class FunctionCorrelationReplacement<T extends BaseCorrelationContext> extends
    RegexCorrelationReplacement<T> {

  // Constructor added in order to satisfy json conversion
  public FunctionCorrelationReplacement() {
  }

  public FunctionCorrelationReplacement(String regex) {
    super(regex);
  }

  @Override
  public String getDisplayName() {
    return "Function";
  }

  @Override
  public String replaceWithRegex(String input, String regex, String variableName,
      JMeterVariables vars) throws MalformedPatternException {
    return replaceWithRegexAndPredicate(input, regex, variableName, match -> true);
  }

  @Override
  public String toString() {
    return "FunctionCorrelationReplacement{" +
        "paramValues='" + getParams() + '\'' +
        '}';
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return null;
  }

  public RegexCorrelationReplacement<?> translateToRegexReplacement(String replacementString) {
    return new RegexCorrelationReplacement<>(regex,
        "${" + replacementString + "}", Boolean.toString(true));
  }
}
