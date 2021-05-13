package com.blazemeter.jmeter.correlation.core;

import com.blazemeter.jmeter.correlation.gui.CorrelationRulesTestElement;
import com.blazemeter.jmeter.correlation.gui.RulesGroupTestElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RulesGroup {

  private String id;
  private List<CorrelationRule> rules;
  private boolean enable;

  //Default constructor to meet serialization
  public RulesGroup() {

  }

  private RulesGroup(Builder builder) {
    this.id = builder.id;
    this.rules = builder.rules;
    this.enable = builder.enable;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<CorrelationRule> getRules() {
    return rules;
  }

  public void setRules(List<CorrelationRule> rules) {
    this.rules = rules;
  }

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RulesGroup that = (RulesGroup) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(rules, that.rules) &&
        enable == that.enable;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, rules, enable);
  }

  @Override
  public String toString() {
    return "RulesGroup{" +
        "id='" + id + '\'' +
        ", rules=" + rules +
        ", enable=" + enable +
        '}';
  }

  public RulesGroupTestElement buildTestElement() {
    return new RulesGroupTestElement(id, enable, rules != null ? new CorrelationRulesTestElement(
        rules.stream().map(CorrelationRule::buildTestElement).collect(Collectors.toList()))
        : new CorrelationRulesTestElement());
  }

  public static final class Builder {

    private String id;
    private List<CorrelationRule> rules = new ArrayList<>();
    private boolean enable = true;

    public Builder() {
    }

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withRules(List<CorrelationRule> rules) {
      this.rules = rules;
      return this;
    }

    public Builder isEnabled(boolean enable) {
      this.enable = enable;
      return this;
    }

    public RulesGroup build() {
      return new RulesGroup(this);
    }
  }
}
