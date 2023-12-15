package com.blazemeter.jmeter.correlation.core.suggestions.method;

import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.suggestions.context.CorrelationContext;
import java.util.ArrayList;
import java.util.List;

/*
* Reminder: We might need to remove this class, since we don't actually want to separate the
*  logic from the CorrelationEngine.
* */
public class LegacyMethod implements CorrelationMethod {
  @Override
  public List<CorrelationSuggestion> generateSuggestions(CorrelationContext context) {
    return new ArrayList<>();
  }

  @Override
  public void applySuggestions(List<CorrelationSuggestion> suggestions) {
    // Do nothing
  }
}
