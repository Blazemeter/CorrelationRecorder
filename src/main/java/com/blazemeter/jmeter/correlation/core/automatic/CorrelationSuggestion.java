package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;

public class CorrelationSuggestion {
  private String paramName;
  private String originalValue;
  private String newValue;
  private final List<SampleResult> appearances = new ArrayList<>();
  private final List<TestElement> usages = new ArrayList<>();
  private final List<ExtractionSuggestion> extractionSuggestions = new ArrayList<>();
  private final List<ReplacementSuggestion> replacementSuggestions = new ArrayList<>();
  private String method = "Replay";

  public CorrelationSuggestion() {

  }

  public CorrelationSuggestion(String paramName) {
    this.paramName = paramName;
  }

  public CorrelationSuggestion(String paramName, String originalValue, String newValue) {
    this.paramName = paramName;
    this.originalValue = originalValue;
    this.newValue = newValue;
  }

  public CorrelationSuggestion(String paramName, String originalValue) {
    this.paramName = paramName;
    this.originalValue = originalValue;
    // When doing a replay, the arguments are the same, thus, there can't be a new value
    this.newValue = "";
  }

  public void addAppearances(SampleResult result) {
    appearances.add(result);
  }

  public void addUsage(TestElement usage) {
    usages.add(usage);
  }

  public String getParamName() {
    return paramName;
  }

  public void setParamName(String paramName) {
    this.paramName = paramName;
  }

  public void setOriginalValue(String originalValue) {
    this.originalValue = originalValue;
  }

  public String getOriginalValue() {
    return originalValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public List<SampleResult> getAppearances() {
    return appearances;
  }

  public List<TestElement> getUsages() {
    return usages;
  }

  public void addExtractionSuggestion(ExtractionSuggestion extractionSuggestion) {
    extractionSuggestions.add(extractionSuggestion);
  }

  public void addReplacementSuggestion(ReplacementSuggestion replacementSuggestion) {
    replacementSuggestions.add(replacementSuggestion);
  }

  public List<ExtractionSuggestion> getExtractionSuggestions() {
    return extractionSuggestions;
  }

  public List<ReplacementSuggestion> getReplacementSuggestions() {
    return replacementSuggestions;
  }

  public String getExtractionParamName() {
    return paramName + "#" + extractionSuggestions.size();
  }

  public String getUsedOnString() {
    if ("Replay".equals(method)) {
      if (!usages.isEmpty()) {
        String amountString = "(" + usages.size() + ") ";
        return amountString + usages.stream()
            .map(TestElement::getName)
            .collect(Collectors.joining(", "));
      }

      if (!appearances.isEmpty()) {
        String amountString = "(" + appearances.size() + ") ";
        return amountString + appearances.stream()
            .map(SampleResult::getSampleLabel)
            .collect(Collectors.joining(", "));
      }
      return "Failed to generate used on string";
    } else {
      String amountString = "(" + replacementSuggestions.size() + ") ";
      return amountString + replacementSuggestions.stream()
          .map(r -> r.getUsage().getName())
          .collect(Collectors.joining(", "));
    }
  }

  public String getObtainedFromString() {
    if ("Replay".equals(method)) {
      if (!appearances.isEmpty()) {
        String amountString = "(" + appearances.size() + ") ";
        return amountString + appearances.stream()
            .map(SampleResult::getSampleLabel)
            .collect(Collectors.joining(", "));
      }
      return "Failed to generate obtained from string";
    } else {
      String amountString = "(" + extractionSuggestions.size() + ") ";
      return amountString + extractionSuggestions.stream()
          .map(r -> r.getSampler().getName())
          .collect(Collectors.joining(", "));
    }
  }

  public String getOriginalValueString() {
    if ("Replay".equals(method)) {
      return originalValue;
    } else {
      Map<String, Integer> map = new HashMap<>();
      for (ExtractionSuggestion s : extractionSuggestions) {
        String key = s.getValue();
        if (map.containsKey(key)) {
          map.put(key, map.get(key) + 1);
        } else {
          map.put(key, 1);
        }
      }

      for (ReplacementSuggestion s : replacementSuggestions) {
        String key = s.getValue();
        if (map.containsKey(key)) {
          map.put(key, map.get(key) + 1);
        } else {
          map.put(key, 1);
        }
      }

      return map.entrySet().stream()
          .map(entry ->  "(" + entry.getValue() + ") " + entry.getKey())
          .collect(Collectors.joining(", "));
    }
  }

  public List<CorrelationRule> getCorrelationRules() {
    List<CorrelationRule> rules = new ArrayList<>();
    extractionSuggestions.forEach(s -> rules.add(
        new CorrelationRule(s.getName(), s.getExtractor(), null)));
    replacementSuggestions.forEach(s -> rules.add(
        new CorrelationRule(s.getName(), null, s.getReplacementSuggestion())));
    return rules;
  }

  @Override
  public String toString() {
    return "CorrelationSuggestion{"
        + "paramName='" + paramName + '\''
        + ", originalValue='" + originalValue + '\''
        + ", newValue='" + newValue + '\''
        + ", appearances=" + appearances
        + ", usages=" + usages
        + ", \nextractionSuggestions={" + extractionSuggestions + "}"
        + ", \nreplacementSuggestions={" + replacementSuggestions + "}}";
  }

  public static class Builder {
    private String paramName;
    private String originalValue = "";
    private String newValue = "";
    private String method = "Replay";

    public Builder() {

    }

    public Builder withParamName(String paramName) {
      this.paramName = paramName;
      return this;
    }

    public Builder withOriginalValue(String originalValue) {
      this.originalValue = originalValue;
      return this;
    }

    public Builder withNewValue(String newValue) {
      this.newValue = newValue;
      return this;
    }

    public Builder withMethod(String method) {
      this.method = method;
      return this;
    }

    public Builder fromRulesAnalysis() {
      this.method = "Rules Analysis";
      return this;
    }

    public Builder fromReplay() {
      this.method = "Replay";
      return this;
    }

    public CorrelationSuggestion build() {
      CorrelationSuggestion suggestion =
          new CorrelationSuggestion(paramName, originalValue, newValue);
      suggestion.method = method;
      return suggestion;
    }
  }
}
