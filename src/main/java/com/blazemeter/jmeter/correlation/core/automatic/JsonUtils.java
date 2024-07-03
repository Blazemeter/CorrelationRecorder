package com.blazemeter.jmeter.correlation.core.automatic;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;

public class JsonUtils {

  public static String findPath(JsonNode node, String valueToFind) {
    if (node.asText().equals(valueToFind)) {
      return "";
    }
    String key;
    Entry<String, JsonNode> entry;
    JsonNode value;
    String currentPath;
    if (node.isObject()) {
      Iterator<Entry<String, JsonNode>> it = node.fields();
      while (it.hasNext()) {
        entry = it.next();
        key = handleSpecialCharactersInJsonKey(entry.getKey());
        value = entry.getValue();
        currentPath = findPath(value, valueToFind);
        if (currentPath != null) {
          return "." + key + (currentPath);
        }
      }
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        currentPath = findPath(node.get(i), valueToFind);
        if (currentPath != null) {
          return (currentPath.startsWith(".") ? "." : "..") + currentPath;
        }
      }
    }
    return null;
  }

  private static String handleSpecialCharactersInJsonKey(String key) {
    if (StringUtils.containsAny(key, ".$@[]?()")) {
      return "['" + key + "']";
    }
    return key;
  }
}
