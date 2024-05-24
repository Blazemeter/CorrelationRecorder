package com.blazemeter.jmeter.correlation.core.automatic;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class JsonUtils {

  public static String findPath(JsonNode node, String valueToFind) {
    if (node.asText().equals(valueToFind)) {
      return "";
    }
    if (node.isObject()) {
      Iterator<Entry<String, JsonNode>> it = node.fields();
      while (it.hasNext()) {
        Entry<String, JsonNode> entry = it.next();
        String key = handleSpecialCharactersInJsonKey(entry.getKey());
        JsonNode value = entry.getValue();
        String currentPath = findPath(value, valueToFind);
        if (currentPath != null) {
          return "." + key + (currentPath);
        }
      }
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        String currentPath = findPath(node.get(i), valueToFind);
        if (currentPath != null) {
          return (currentPath.startsWith(".") ? "." : "..") + currentPath;
        }
      }
    }
    return null;
  }

  private static String handleSpecialCharactersInJsonKey(String key) {
    List<String> specialCharacters = Arrays.asList(".", "$", "@", "[", "]", "?", "(", ")");
    boolean containJsonPathExpression = specialCharacters.stream().anyMatch(key::contains);
    if (containJsonPathExpression) {
      return "['" + key + "']";
    }
    return key;
  }
}
