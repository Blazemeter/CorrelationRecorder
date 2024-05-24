package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.automatic.JsonUtils;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.JsonCorrelationReplacement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.testelement.TestElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplacementJsonStrategy implements ReplacementStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(ReplacementJsonStrategy.class);

  @Override
  public CorrelationReplacement<?> generateReplacement(TestElement usage, Appearances appearance,
      String referenceName) {
    if (!(usage instanceof HTTPSamplerBase)) {
      return null;
    }
    HTTPSamplerBase httpUsage = (HTTPSamplerBase) usage;
    if (!httpUsage.getSendParameterValuesAsPostBody()) {
      return null;
    }
    ObjectMapper objectMapper = new ObjectMapper();
    String path;
    String jsonRaw = httpUsage.getArguments().getArgument(0).getValue();
    try {
      JsonNode root = objectMapper.readTree(jsonRaw);
      path = JsonUtils.findPath(root, appearance.getValue());
    } catch (JsonProcessingException e) {
      LOG.warn("Failure on reading JSON {}", jsonRaw, e);
      return null;
    }
    if (path == null) {
      return null;
    }
    path = "$" + path;
    JsonCorrelationReplacement<?> replacement = new JsonCorrelationReplacement<>(path);
    replacement.setVariableName(referenceName);
    return replacement;
  }
}
