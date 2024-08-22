package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.suggestions.method.ComparisonMethod.ReplacementParameters;
import org.apache.jmeter.testelement.TestElement;

public class ReplacementRegexStrategy implements ReplacementStrategy {

  @Override
  public CorrelationReplacement<?> generateReplacement(TestElement usage, Appearances appearance,
      ReplacementParameters replacementParameters) {
    String regex = ReplacementRegex.match(appearance.getName(), appearance.getSource());
    RegexCorrelationReplacement<?> replacement =
        replacementParameters.getReplacementString() == ReplacementString.NONE
            ? new RegexCorrelationReplacement<>(regex) : new RegexCorrelationReplacement<>(regex,
            replacementParameters.getReplacementString()
                .getExpression(replacementParameters.getRefName()), Boolean.toString(false));
    replacement.setVariableName(replacementParameters.getRefName());
    return replacement;
  }
}
