package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.automatic.JsonUtils;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.JsonCorrelationExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonBodyExtractor extends Extractor {

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
  public List<PostProcessor> getPostProcessors(SampleResult response, String value, String name) {
    return Collections.emptyList();
  }

  @Override
  public List<CorrelationExtractor<?>> getCorrelationExtractors(SampleResult response, String value,
      String name) {
    String path;
    try {
      JsonNode root = objectMapper.readTree(response.getResponseDataAsString());
      path = JsonUtils.findPath(root, value);
      if (path == null) {
        if (LOG.isDebugEnabled()) {
          System.out.printf(
              "No match found for '%s' in response '%s'", value, response.getSampleLabel());
        }
        return Collections.emptyList();
      }
    } catch (JsonProcessingException e) {
      LOG.warn("Failure on reading JSON {}", response.getResponseDataAsString(), e);
      return Collections.emptyList();
    }
    path = "$" + path;

    JsonCorrelationExtractor<?> extractor = new JsonCorrelationExtractor<>();
    extractor.setRefName(name);
    extractor.setPath(path);
    extractor.setMultiValued(true);
    extractor.setMatchNr(-1);
    return Collections.singletonList(extractor);
  }
}
