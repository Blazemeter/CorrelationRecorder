package com.blazemeter.jmeter.correlation.core.suggestions.method;

import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.EntityArgument;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractionSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ReplacementSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.Sources;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.JsonCorrelationExtractor;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A reformulation of the comparison's suggestions, once all of them are found: the suggestions
 * that take the same value from the same response are one variable under several names (the API
 * path's "orders" and the page's "order.id", both $.orderId of the checkout). Each such group
 * becomes one suggestion, with one extractor and one variable for every replacement.
 *
 * <p>Grouping needs that evidence, never a resemblance of names. Where the right name is, the
 * extraction or the replacement, is not known for sure, so every name the evidence gives is kept
 * as an alias, for the user to pick one or set another. The suggested one is a request's: an
 * entity (a REST concept or an entity argument) before a specific name, a generic one last; then
 * the name that covers more values, and the one used on more requests.
 */
final class SuggestionUnification {

  private static final Set<String> ENTITY_SOURCES = new HashSet<>(Arrays.asList(
      Sources.REQUEST_ENTITY_ARGUMENT, Sources.REQUEST_PATH_ALPHANUMERIC_ID, Sources.REQUEST_PATH,
      Sources.REQUEST_PATH_NUMBER_FOLLOWED_BY_QUESTION_MARK,
      Sources.REQUEST_PATH_NUMBER_FOLLOWED_BY_SLASH));
  private static final Pattern JSON_PATH_INDEX = Pattern.compile("\\[[^\\]]*]");
  private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
  private static final int ENTITY = 2;
  private static final int SPECIFIC = 1;
  private static final int GENERIC = 0;

  private SuggestionUnification() {
  }

  static List<CorrelationSuggestion> unify(List<CorrelationSuggestion> suggestions,
      Configuration configuration) {
    List<List<CorrelationSuggestion>> groups = groupBySharedExtraction(suggestions);
    List<CorrelationSuggestion> unified = new ArrayList<>();
    for (List<CorrelationSuggestion> group : groups) {
      List<CorrelationSuggestion> ranked = new ArrayList<>(group);
      ranked.sort(Comparator.<CorrelationSuggestion>comparingInt(s -> evidence(s, configuration))
          .thenComparingInt(s -> values(s).size())
          .thenComparingInt(s -> s.getUsages().size())
          .reversed()
          .thenComparing(CorrelationSuggestion::getParamName));
      CorrelationSuggestion kept = ranked.get(0);
      ranked.subList(1, ranked.size()).forEach(alias -> absorb(kept, alias));
      extractionNames(kept, configuration).forEach(kept::addAlias);
      unified.add(kept);
    }
    return unified;
  }

  /*
   Suggestions joined, directly or through others, by a value they take from the same response,
   when the values of one are all values of the other: a name can be used for some of an entity's
   instances only (the page of the last order, the API path of every order). Two multivalued
   suggestions that only share some values are not one variable (a sort parameter and the
   attribute names it sorts by).
   */
  private static List<List<CorrelationSuggestion>> groupBySharedExtraction(
      List<CorrelationSuggestion> suggestions) {
    int[] parent = new int[suggestions.size()];
    for (int i = 0; i < parent.length; i++) {
      parent[i] = i;
    }
    Map<String, List<Integer>> withExtraction = new HashMap<>();
    for (int i = 0; i < suggestions.size(); i++) {
      Set<String> values = values(suggestions.get(i));
      for (String extraction : extractions(suggestions.get(i))) {
        List<Integer> others = withExtraction.computeIfAbsent(extraction, k -> new ArrayList<>());
        for (int other : others) {
          Set<String> otherValues = values(suggestions.get(other));
          if (otherValues.containsAll(values) || values.containsAll(otherValues)) {
            parent[root(parent, i)] = root(parent, other);
          }
        }
        others.add(i);
      }
    }
    Map<Integer, List<CorrelationSuggestion>> groups = new LinkedHashMap<>();
    for (int i = 0; i < suggestions.size(); i++) {
      groups.computeIfAbsent(root(parent, i), k -> new ArrayList<>()).add(suggestions.get(i));
    }
    return new ArrayList<>(groups.values());
  }

  private static int root(int[] parent, int i) {
    while (parent[i] != i) {
      i = parent[i];
    }
    return i;
  }

  private static Set<String> values(CorrelationSuggestion suggestion) {
    return suggestion.getExtractionSuggestions().stream()
        .map(ExtractionSuggestion::getValue)
        .collect(Collectors.toSet());
  }

  /*
   The names the responses give the value, the other side of the evidence: $.orderId is orderId,
   $.order.id is order.id. They are offered as aliases; the kept name is still a request's.
   */
  private static Set<String> extractionNames(CorrelationSuggestion suggestion,
      Configuration configuration) {
    Set<String> names = new LinkedHashSet<>();
    for (ExtractionSuggestion extraction : suggestion.getExtractionSuggestions()) {
      List<CorrelationExtractor<?>> extractors = new ArrayList<>(extraction.getExtractors());
      if (extraction.getExtractor() != null) {
        extractors.add(extraction.getExtractor());
      }
      for (CorrelationExtractor<?> extractor : extractors) {
        if (extractor instanceof JsonCorrelationExtractor) {
          String name = jsonPathName(((JsonCorrelationExtractor<?>) extractor).getPath(),
              configuration);
          if (!name.isEmpty()) {
            names.add(name);
          }
        }
      }
    }
    return names;
  }

  /*
   The key that holds the value, with the one that holds it when that key is generic: $.orderId is
   orderId, $.order.id is order.id, $.objects[0].attributes.Name.value is Name.value. Indexes and
   keys that are data (an object's id used as a key) are left out.
   */
  @VisibleForTesting
  static String jsonPathName(String path, Configuration configuration) {
    List<String> keys = Arrays.stream(JSON_PATH_INDEX.matcher(path).replaceAll(".").split("\\."))
        .filter(key -> !key.isEmpty() && !key.equals("$") && !HAS_DIGIT.matcher(key).find())
        .collect(Collectors.toList());
    if (keys.isEmpty()) {
      return "";
    }
    String last = keys.get(keys.size() - 1);
    boolean generic = configuration.isGenericParameter(last) || "value".equalsIgnoreCase(last);
    return generic && keys.size() > 1 ? keys.get(keys.size() - 2) + "." + last : last;
  }

  private static Set<String> extractions(CorrelationSuggestion suggestion) {
    return suggestion.getExtractionSuggestions().stream()
        .filter(e -> e.comesFromASampleResult() && e.getSampleResult() != null)
        .map(SuggestionUnification::extraction)
        .collect(Collectors.toSet());
  }

  private static String extraction(ExtractionSuggestion extraction) {
    return extraction.getSampleResult().getSampleLabel() + "\n" + extraction.getValue();
  }

  private static int evidence(CorrelationSuggestion suggestion, Configuration configuration) {
    boolean entity = suggestion.getReplacementSuggestions().stream()
        .map(ReplacementSuggestion::getSource)
        .anyMatch(ENTITY_SOURCES::contains);
    if (entity) {
      return ENTITY;
    }
    return configuration.isGenericParameter(EntityArgument.argument(suggestion.getParamName()))
        ? GENERIC : SPECIFIC;
  }

  // The alias's rules take the kept variable; an extraction the kept one already has is dropped
  private static void absorb(CorrelationSuggestion kept, CorrelationSuggestion alias) {
    String name = kept.getParamName();
    Set<String> keptExtractions = extractions(kept);
    for (ExtractionSuggestion extraction : alias.getExtractionSuggestions()) {
      if (extraction.comesFromASampleResult() && extraction.getSampleResult() != null
          && keptExtractions.contains(extraction(extraction))) {
        continue;
      }
      extraction.setName(name);
      kept.addExtractionSuggestion(extraction);
    }
    for (ReplacementSuggestion replacement : alias.getReplacementSuggestions()) {
      replacement.setName(name);
      kept.addReplacementSuggestion(replacement);
    }
    alias.getUsages().forEach(kept::addUsage);
    alias.getAppearances().stream()
        .filter(result -> kept.getAppearances().stream().noneMatch(r -> r == result))
        .forEach(kept::addAppearances);
    kept.addAlias(alias.getParamName());
    alias.getAliases().forEach(kept::addAlias);
  }
}
