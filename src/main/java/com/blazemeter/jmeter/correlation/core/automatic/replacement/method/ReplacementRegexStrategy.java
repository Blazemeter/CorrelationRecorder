package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import org.apache.jmeter.testelement.TestElement;

public class ReplacementRegexStrategy implements ReplacementStrategy {

  @Override
  public CorrelationReplacement<?> generateReplacement(TestElement usage, Appearances appearance,
      String referenceName) {
    String regex = ReplacementRegex.match(appearance.getName(), appearance.getSource());
    RegexCorrelationReplacement<?> replacement = new RegexCorrelationReplacement<>(regex);
    replacement.setVariableName(referenceName);
    return replacement;
  }
}
