package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.JsonCorrelationExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonBodyExtractor extends BodyExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(JsonBodyExtractor.class);
  private final ObjectMapper objectMapper;

  public JsonBodyExtractor() {
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public List<String> extractValue(SampleResult response, String value) {
    List<String> values = new ArrayList<>();
    String body = response.getResponseDataAsString();
    List<Pair<String, Object>> parameters = JMeterElementUtils.extractDataParametersFromJson(body);
    String jsonPath = "";
    // String jsonPath = ExtractorGenerator.getJsonPathToValue(parameters, value, true);

    // Example: Extract values using regex (simplified for demonstration)
    Pattern pattern = Pattern.compile("someJsonPattern");  // Define your regex pattern
    Matcher matcher = pattern.matcher(body);
    while (matcher.find()) {
      values.add(matcher.group(1));  // Assuming you're interested in the first capturing group
    }

    return values;
  }

  @Override
  public List<CorrelationExtractor<?>> getCorrelationExtractors(SampleResult response, String value,
                                                                String name) {
    String responseDataAsString = response.getResponseDataAsString();
    List<Integer> indexes = ExtractorGenerator.getIndexes(value, responseDataAsString);
    if (LOG.isDebugEnabled() && indexes.size() > 1) {
      System.out.println(
          "[" + getClass().getSimpleName() + "]" + ": " + name + ") " + indexes.size() + " found.");
    }
    List<CorrelationExtractor<?>> extractors = new ArrayList<>();
    for (int i = 0; i < indexes.size(); i++) {
      try {
        int matchNumber = i + 1;
        String path = extractPath(responseDataAsString, value, matchNumber);
        if (path == null) {
          if (LOG.isDebugEnabled()) {
            System.out.println(
                "No match found for '" + value + "' in response '" + response.getSampleLabel() +
                    "'");
          }
          continue;
        }
        JsonCorrelationExtractor<?> extractor = new JsonCorrelationExtractor();
        extractor.setRefName(name);
        extractor.setPath(path);
        if (LOG.isDebugEnabled()) {
          System.out.println(name + ") matchNumber: " + matchNumber + " - " + path);
        }
        extractors.add(extractor);
      } catch (IOException e) {
        LOG.error("Error extracting path from JSON response: ",  e);
      }
    }
    return extractors;
  }

  public String extractPath(String jsonString, String valueToFind, int matchNumber) throws
      IOException {
    JsonNode root = objectMapper.readTree(jsonString);
    List<String> paths = new ArrayList<>();
    findPath(root, valueToFind, "$", paths);
    if (paths.size() >= matchNumber) {
      return paths.get(matchNumber - 1); // 1-based index
    } else {
      return null;
    }
  }

  private void findPath(JsonNode node, String valueToFind, String currentPath, List<String> paths) {
    if (node.isObject()) {
      node.fields().forEachRemaining(entry -> {
        String key = entry.getKey();
        JsonNode value = entry.getValue();
        findPath(value, valueToFind, currentPath + "." + key, paths);
      });
    } else if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        findPath(node.get(i), valueToFind, currentPath + "[" + i + "]", paths);
      }
    } else {
      if (node.asText().equals(valueToFind)) {
        paths.add(currentPath);
      }
    }
  }
}
