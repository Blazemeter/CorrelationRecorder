package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.jmeter.threads.JMeterVariables;

public class ComparableJMeterVariables extends JMeterVariables {

  private Map<String, String> vars = new HashMap<>();

  @Override
  public void put(String key, String value) {
    vars.put(key, value);
    super.put(key, value);
  }

  @Override
  public Object remove(String key) {
    vars.remove(key);
    return super.remove(key);
  }

  @Override
  public String toString() {
    return vars.keySet().stream()
        .map(k -> "{" + k + "," + vars.get(k) + "}")
        .collect(Collectors.joining(","));
  }

  public void clear() {
    vars.keySet().forEach(super::remove);
    vars.clear();
  }

  @Override
  public boolean equals(Object obj) {
    return vars.equals(((ComparableJMeterVariables) obj).vars);
  }
}
