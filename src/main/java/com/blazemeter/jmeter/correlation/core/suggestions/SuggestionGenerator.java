package com.blazemeter.jmeter.correlation.core.suggestions;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.suggestions.context.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.suggestions.method.AnalysisMethod;
import com.blazemeter.jmeter.correlation.core.suggestions.method.CorrelationMethod;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
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
  public List<CorrelationRule> parseToRules(List<CorrelationSuggestion> suggestions) {
    List<CorrelationRule> rules = new ArrayList<>();
    for (CorrelationSuggestion suggestion : suggestions) {
      List<CorrelationRule> correlationRules = suggestion.toCorrelationRules();
      rules.addAll(correlationRules);
    }
    return rules;
  }

  public void applySuggestions(List<CorrelationSuggestion> suggestions) {
    correlationMethod.applySuggestions(suggestions);
  }
}
