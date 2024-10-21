package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import org.apache.jmeter.testelement.TestElement;

public class ReplacementSuggestion {
  private final CorrelationReplacement<?> replacementSuggestion;
  private final TestElement usage;
  private String source;
  private String value;
  private String name;
  private int sequence = 0;

  public ReplacementSuggestion(CorrelationReplacement<?> replacementSuggestion,
                               TestElement usage) {
    this.replacementSuggestion = replacementSuggestion;
    this.usage = usage;
  }

  public CorrelationReplacement<?> getReplacementSuggestion() {
    return replacementSuggestion;
  }

  public TestElement getUsage() {
    return usage;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public int getSequence() {
    return this.sequence;
  }

  public void setSequence(int sequence) {
    this.sequence = sequence;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "ReplacementSuggestion{" +
        "replacementSuggestion=" + replacementSuggestion +
        ", usage=" + usage +
        ", source='" + source + '\'' +
        ", value='" + value + '\'' +
        ", name='" + name + '\'' +
        '}';
  }

}
