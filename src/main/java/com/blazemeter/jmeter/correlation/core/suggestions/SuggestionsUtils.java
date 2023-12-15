package com.blazemeter.jmeter.correlation.core.suggestions;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a utility class that provides methods to parse CorrelationSuggestion and
 * CorrelationRule objects.
 */
public class SuggestionsUtils {

  /**
   * This method is used to convert a list of CorrelationSuggestion objects into a list of
   * CorrelationRule objects.
   * It iterates over each CorrelationSuggestion in the provided list, converts it into a
   * list of CorrelationRule objects,
   * and adds all these rules to a new list which is then returned.
   *
   * @param suggestions A list of CorrelationSuggestion objects to be converted into
   *                    CorrelationRule objects.
   * @return A list of CorrelationRule objects derived from the provided
   * CorrelationSuggestion objects.
   */
  public static List<CorrelationRule> parseSuggestionsToRules(
      List<CorrelationSuggestion> suggestions) {
    List<CorrelationRule> correlationRules = new ArrayList<>();
    for (CorrelationSuggestion suggestion : suggestions) {
      List<CorrelationRule> rules = suggestion.toCorrelationRules();
      correlationRules.addAll(rules);
    }
    return correlationRules;
  }

  /**
   * This method is used to convert a list of CorrelationRule objects into a
   * list of RulesGroup objects.
   * It iterates over each CorrelationRule in the provided list, creates a
   * new RulesGroup with the current index as ID and the list of rules,
   * and adds this group to a new list which is then returned.
   *
   * @param rules A list of CorrelationRule objects to be converted into RulesGroup objects.
   * @return A list of RulesGroup objects derived from the provided CorrelationRule objects.
   */
  public static List<RulesGroup> parseToGroup(List<CorrelationRule> rules) {
    List<RulesGroup> rulesGroups = new ArrayList<>();
    RulesGroup group = new RulesGroup.Builder()
        .withId("group-" + System.currentTimeMillis())
        .withRules(rules)
        .build();
    rulesGroups.add(group);
    return rulesGroups;
  }
}
