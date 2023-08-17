package com.blazemeter.jmeter.correlation.core.automatic;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.protocol.http.util.HTTPConstants;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jorphan.collections.HashTree;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordingExtraction implements AppearancesExtraction {
  private static final Logger LOG = LoggerFactory.getLogger(RecordingExtraction.class);
  private final JMeterElementUtils utils;
  private final Map<String, List<Appearances>> appearanceMap;

  public RecordingExtraction(Configuration configuration) {
    this.utils = new JMeterElementUtils(configuration);
    this.appearanceMap = new HashMap<>();
  }

  public RecordingExtraction(Configuration configuration, Map<String, List<Appearances>> map) {
    this.utils = new JMeterElementUtils(configuration);
    this.appearanceMap = map;
  }

  @Override
  public Map<String, List<Appearances>> extractAppearanceMap(String filepath) {
    appearanceMap.clear();
    boolean shouldFilter = utils.shouldFilter();
    HashTree testPlan = JMeterElementUtils.getTestPlan(filepath);
    List<HTTPSamplerBase> requests = getRequests(testPlan, shouldFilter);
    for (HTTPSamplerBase sampler : requests) {
      extractParametersFromHttpSampler(sampler);
    }

    List<HeaderManager> headers = getHeaders(testPlan, shouldFilter);
    for (HeaderManager header : headers) {
      extractParametersFromHeader(header);
    }

    return appearanceMap;
  }

  public void extractParametersFromHttpSampler(HTTPSamplerBase sampler) {
    extractParametersFromArguments(sampler);
    extractParametersFromURLPath(sampler);
  }

  /**
   * Returns a list of all HTTPSamplerBase elements in the test plan.
   *
   * @param testPlan HashTree of the test plan.
   * @return List of HTTPSamplerBase elements.
   */
  public List<HTTPSamplerBase> getRequests(HashTree testPlan, boolean shouldFilter) {
    List<TestElement> extractedSamplers = new LinkedList<>();
    JMeterElementUtils.extractHttpSamplers(testPlan, extractedSamplers);

    List<HTTPSamplerBase> samplers = extractedSamplers.stream()
        .map(HTTPSamplerBase.class::cast)
        .collect(toList());

    if (!shouldFilter) {
      return samplers;
    }

    samplers.removeIf(sampler -> utils.canBeFiltered(() -> sampler));
    return samplers;
  }

  public void extractParametersFromArguments(HTTPSamplerBase sampler) {
    for (JMeterProperty property : sampler.getArguments()) {
      HTTPArgument argument = JMeterElementUtils.getHttpArgument(property);
      String key = argument.getName();
      String value = argument.getValue();
      if (utils.canBeFiltered(key, value)) {
        LOG.trace("Filtered: '" + key + "' '" + value + "'");
        continue;
      }

      if (JMeterElementUtils.isJson(value)) {
        try {
          if (JMeterElementUtils.isJsonArray(value)) {
            LOG.warn("JSON Arrays as Arguments on HTTP requests is not supported yet. "
                + "Skipping value: '{}'='{}'", key, value);
            continue;
          }

          JSONObject jsonObject = new JSONObject(value);
          utils.extractParametersFromJson(jsonObject, appearanceMap, sampler, 2);
        } catch (JSONException e) {
          LOG.trace("There was an error while extracting JSON values for {}. ", value, e);
        }
      } else if (utils.isParameterized(value)) {
        LOG.warn("Parameterized value: '" + key + "'='" + value + "'");
      } else if (sampler.getPostBodyRaw()) {
        utils.addToMap(appearanceMap, key, value, sampler, "Body Data (JSON)");
      } else {
        utils.addToMap(appearanceMap, key, value, sampler, "HTTP arguments");
      }
    }
  }

  public Map<String, List<String>> splitQuery(URL url, String contentEncoding) {
    if (StringUtils.isEmpty(url.getQuery())) {
      return Collections.emptyMap();
    }
    return Arrays.stream(url.getQuery().split("&"))
        .map(value -> {
          try {
            return splitQueryParameter(value, contentEncoding);
          } catch (UnsupportedEncodingException e) {
            LOG.error("Error processing value + value" + value + " from url " + url.toString(), e);
          }
          return null;
        })
        .collect(Collectors.groupingBy(AbstractMap.SimpleImmutableEntry::getKey, LinkedHashMap::new,
            mapping(Map.Entry::getValue, toList())));
  }

  public AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(String it,
                                                                              String charset)
      throws UnsupportedEncodingException {
    final int idx = it.indexOf("=");
    final String key = idx > 0 ? it.substring(0, idx) : it;
    final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
    return new AbstractMap.SimpleImmutableEntry<>(
        URLDecoder.decode(key, charset),
        URLDecoder.decode(value, charset)
    );
  }

  private void extractParametersFromURLPath(HTTPSamplerBase sampler) {
    String contentEncoding = sampler.getContentEncoding();
    Charset charset = JMeterElementUtils.getCharset(contentEncoding);
    try {
      // Path in Rest format (/key/value)
      String urlPath = sampler.getPath();
      int intPort = sampler.getPortIfSpecified();
      String basePath = urlPath;
      if (urlPath.contains("?")) { // If query data exist, remove to get the base path
        basePath = basePath.split("\\?")[0];
      }
      String[] pathValues = basePath.split("/");
      int pathLen = pathValues.length;
      if (pathLen > 1) {
        String keyPathValue = pathValues[pathLen - 2];
        String keyValue = pathValues[pathLen - 1];
        // Only when the key is a word and the value is a number
        if ((!StringUtils.isNumeric(keyPathValue) && StringUtils.isNumeric(keyValue)) &&
            (!utils.canBeFiltered(keyPathValue, keyValue))) {
          utils.addToMap(appearanceMap, keyPathValue, keyValue, sampler,
              "Request Path");
        }
      }
      if (urlPath.contains("?")) { // Extract query values
        String baseUrl = sampler.getUrl().toString();
        String query = urlPath.split("\\?")[1];
        String url = baseUrl.split("\\?")[0] + "?" + query;
        Map<String, List<String>> params = splitQuery(new URL(url), charset.toString());
        params.forEach((key, values) -> {
          String value = values.size() > 0 ? values.get(0) : "";
          if (!utils.canBeFiltered(key, value)) {
            utils.addToMap(appearanceMap, key, value, sampler, "Request Query");
          }
        });
      }
    } catch (MalformedURLException e) {
      LOG.error("Unexpected error", e);
    }
  }

  private List<HeaderManager> getHeaders(HashTree testPlanTree, boolean shouldFilter) {
    List<TestElement> headerManagers = new LinkedList<>();
    JMeterElementUtils.extractHeaderManagers(testPlanTree, headerManagers);

    List<org.apache.jmeter.protocol.http.control.HeaderManager> headers = headerManagers.stream()
        .map(org.apache.jmeter.protocol.http.control.HeaderManager.class::cast)
        .collect(toList());

    if (!shouldFilter) {
      return headers;
    }

    headers.removeIf(header -> utils.canBeFiltered(() -> header));
    return headers;
  }

  private void extractParametersFromHeader(HeaderManager headerManager) {
    for (JMeterProperty property : headerManager.getHeaders()) {
      Header header = (Header) property.getObjectValue();
      String name = header.getName();
      String value = header.getValue();
      String[] parsedAuthValue = value.trim().split(" ");
      if (!StringUtils.equalsIgnoreCase(HTTPConstants.HEADER_AUTHORIZATION, name)
          || parsedAuthValue.length < 2) {
        return;
      }

      if (parsedAuthValue[0].equals("Bearer")) {
        utils.addToMap(appearanceMap, name, parsedAuthValue[1], headerManager, "Bearer");
      }
    }
  }
}
