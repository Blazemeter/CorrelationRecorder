package com.blazemeter.jmeter.correlation.core.automatic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jmeter.testelement.TestElement;

public class Appearances  {

  private String name;
  private String value;
  private String source;
  private final List<TestElement> list = new ArrayList<>();

  public Appearances() {
    // Added for JSON deserialization
  }

  public Appearances(String value, String name, TestElement appearance) {
    this.value = value;
    this.name = name;
    list.add(appearance);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
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

  public String toJSon() {
    return "{\"from\": \"" + source + "\", \"value\": \"" + value + "\", \"name\": \""
        + name + "\", \"appearanceList\": " + list.stream()
        .map(element ->
            "{\"name\": \"" + element.getPropertyAsString("TestElement.name") +
                "\", \"path\": \"" + element.getPropertyAsString("HTTPSampler.path") +
                "\", \"method\": \"" + element.getPropertyAsString("HTTPSampler.method") + "\"}")
        .collect(Collectors.toList()) + "}";
  }

  public String toJSON() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(this);
  }

  public static Appearances fromJSON(String json) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(json, Appearances.class);
  }
}
