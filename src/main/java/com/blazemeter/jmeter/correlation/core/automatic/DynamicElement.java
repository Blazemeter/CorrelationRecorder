package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.List;

public class DynamicElement {
  private final String name;
  private final List<Appearances> originalAppearance;
  private final List<Appearances> otherAppearance;

  public DynamicElement(String name, List<Appearances> originalValue,
                        List<Appearances> otherAppearance) {
    this.name = name;
    this.originalAppearance = originalValue;
    this.otherAppearance = otherAppearance;
  }

  public String getName() {
    return name;
  }

  public List<Appearances> getOriginalAppearance() {
    return originalAppearance;
  }

  public List<Appearances> getOtherAppearance() {
    return otherAppearance;
  }

  @Override
  public String toString() {
    return "DynamicElement{"
        + "name='" + name + '\''
        + ", value='" + originalAppearance + '\''
        + ", otherValue='" + otherAppearance + '\''
        + '}';
  }
}
