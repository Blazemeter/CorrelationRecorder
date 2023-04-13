package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jmeter.testelement.TestElement;

public class Appearances {
  private final List<TestElement> list = new ArrayList<>();
  private final String value;
  private final String name;
  private String source;

  public Appearances(String value, String name, TestElement appearance) {
    this.value = value;
    this.name = name;
    list.add(appearance);
  }

  public String getValue() {
    return value;
  }

  public String getName() {
    return name;
  }

  public List<TestElement> getList() {
    return list;
  }

  @Override
  public String toString() {
    return "Appearances {" +
        "from: '" + source + '\'' +
        ", value='" + value + '\'' +
        ", name='" + name + '\'' +
        ", appearanceList=" + list.stream()
        .map(element ->
            "name: '" + element.getPropertyAsString("TestElement.name") +
                "', path: '" + element.getPropertyAsString("HTTPSampler.path") +
                "', method: " + element.getPropertyAsString("HTTPSampler.method"))
        .collect(Collectors.toList()) + "'}";
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getSource() {
    return this.source;
  }
}
