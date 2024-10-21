package com.blazemeter.jmeter.correlation.core.suggestions;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    HashMap<CorrelationRule, Integer> correlationRules = new HashMap<>();
    for (CorrelationSuggestion suggestion : suggestions) {
      HashMap<CorrelationRule, Integer> rules = suggestion.toCorrelationRules();
      correlationRules.putAll(rules);
    }
    // Sort the hashmap based on the sequence and return as a list
    return sortRulesSequence(correlationRules);
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

  /**
   * Sorts a given {@code HashMap} of {@code CorrelationRule} objects based on their associated
   * sequence values and returns the rules as a list in ascending order of the sequence.
   *
   * <p>The method processes the entries of the provided {@code HashMap<CorrelationRule, Integer>}
   * by sorting them according to the integer values (sequence) and then collecting the keys
   * (the {@code CorrelationRule} objects) into a list. The resulting list maintains the order of
   * the rules based on their corresponding sequence values in ascending order.
   *
   * @param rulesSequence a {@code HashMap} where each key is a {@code CorrelationRule} and each
   *                      value is an {@code Integer} representing the rule's sequence
   * @return a {@code List<CorrelationRule>} of rules ordered by their sequence values in ascending
   * order
   */
  public static List<CorrelationRule> sortRulesSequence(
      HashMap<CorrelationRule, Integer> rulesSequence) {
    // Sort the hashmap based on the sequence and return as a list
    return rulesSequence.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())  // Sort by Sequence
        .map(Map.Entry::getKey)  // Extract the Rules and return as a List
        .collect(Collectors.toList());
  }
}
