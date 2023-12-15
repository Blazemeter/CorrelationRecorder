package com.blazemeter.jmeter.correlation.core.suggestions.method;

import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.suggestions.context.CorrelationContext;
import java.util.List;

public interface CorrelationMethod {
  List<CorrelationSuggestion> generateSuggestions(CorrelationContext context);

  void applySuggestions(List<CorrelationSuggestion> suggestions);
}
