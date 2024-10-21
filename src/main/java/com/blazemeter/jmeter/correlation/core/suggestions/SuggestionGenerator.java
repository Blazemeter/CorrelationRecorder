package com.blazemeter.jmeter.correlation.core.suggestions;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.suggestions.context.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.suggestions.method.AnalysisMethod;
import com.blazemeter.jmeter.correlation.core.suggestions.method.CorrelationMethod;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Reminder: Maybe we can call this "SuggestionsHandler" or "SuggestionsManager"
 *  to be able to handle both the generation and the application of the suggestions.
 * */
public class SuggestionGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(SuggestionGenerator.class);
  private static SuggestionGenerator instance;
  private CorrelationMethod correlationMethod;

  public SuggestionGenerator(CorrelationMethod method) {
    this.correlationMethod = method;
  }

  public static SuggestionGenerator getInstance(CorrelationMethod method) {
    if (instance == null) {
      instance = new SuggestionGenerator(method);
    } else {
      instance.setCorrelationMethod(method);
    }

    return instance;
  }

  public static SuggestionGenerator getInstance() {
    if (instance == null) {
      // The only moment we call this is when we are applying the suggestions by using Templates
      instance = new SuggestionGenerator(new AnalysisMethod());
    }

    return instance;
  }

  public void setCorrelationMethod(CorrelationMethod correlationMethod) {
    this.correlationMethod = correlationMethod;
  }

  public List<CorrelationSuggestion> generateSuggestions(CorrelationContext context) {
    if (correlationMethod == null) {
      LOG.error("Correlation method cannot be null");
      return new ArrayList<>();
    }

    if (context == null) {
      LOG.error("Correlation context cannot be null");
      return new ArrayList<>();
    }

    return correlationMethod.generateSuggestions(context);
  }

  @VisibleForTesting
  public HashMap<CorrelationRule, Integer> parseToRules(List<CorrelationSuggestion> suggestions) {
    HashMap<CorrelationRule, Integer> rules = new HashMap<>();
    for (CorrelationSuggestion suggestion : suggestions) {
      HashMap<CorrelationRule, Integer> correlationRules = suggestion.toCorrelationRules();
      rules.putAll(correlationRules);
    }
    return rules;
  }
}
