package com.blazemeter.jmeter.correlation.core.automatic;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.helger.commons.annotation.VisibleForTesting;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jorphan.collections.HashTree;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordingExtraction implements AppearancesExtraction {

  private static final Logger LOG = LoggerFactory.getLogger(RecordingExtraction.class);
  private static final int MIN_ALPHANUMERIC_ID_LENGTH = 8;
  private static final Pattern PATH_CONCEPT = Pattern.compile("[A-Za-z][A-Za-z_-]*");
  private static final Pattern PATH_ID = Pattern.compile("[A-Za-z0-9_-]+");
  private final Configuration configuration;
  private final JMeterElementUtils utils;
  private final Map<String, List<Appearances>> appearanceMap;

  public RecordingExtraction(Configuration configuration) {
    this(configuration, new HashMap<>());
  }

  public RecordingExtraction(Configuration configuration, Map<String, List<Appearances>> map) {
    this.configuration = configuration;
    this.utils = new JMeterElementUtils(configuration);
    this.appearanceMap = map;
  }

  @VisibleForTesting
  public Map<String, HTTPSamplerBase> generateSamplerMap(String filepath) {
    HashTree testPlan = JMeterElementUtils.getTestPlan(filepath);
    List<HTTPSamplerBase> requests = getRequests(testPlan, false);
    Map<String, HTTPSamplerBase> samplerMap = new HashMap<>();
    for (HTTPSamplerBase sampler : requests) {
      samplerMap.put(sampler.getName(), (HTTPSamplerBase) sampler);
    }
    return samplerMap;
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

      storeValue(sampler, key, value);
    }
  }

  public void storeValue(HTTPSamplerBase sampler, String key, String value) {
    if (key.isEmpty() && JMeterElementUtils.isJson(value)) {
      try {
        if (JMeterElementUtils.isJsonArray(value)) {
          utils.extractParametersFromJsonArray(new JSONArray(value), appearanceMap, sampler, key,
              Sources.REQUEST_BODY_JSON);
          return;
        }

        JSONObject jsonObject = new JSONObject(value);
        utils.extractParametersFromJson(jsonObject, appearanceMap, sampler,
            Sources.REQUEST_BODY_JSON);
      } catch (JSONException e) {
        LOG.trace("There was an error while extracting JSON values for {}. ", value, e);
      }
    } else if (utils.isParameterized(value)) {
      LOG.warn("Parameterized value: '" + key + "'='" + value + "'");
    } else {
      addArgument(sampler, key, value, Sources.REQUEST_ARGUMENTS);
    }
  }

  // A generic argument (id) is named after its request's entity (order.id), so the ids of
  // different entities do not end up in one suggestion
  private void addArgument(HTTPSamplerBase sampler, String key, String value, String source) {
    String concept = configuration.isGenericParameter(key)
        ? EntityArgument.concept(sampler.getPath()) : null;
    if (concept == null) {
      utils.addToMap(appearanceMap, key, value, sampler, source);
    } else {
      utils.addToMap(appearanceMap, EntityArgument.name(concept, key), value, sampler,
          Sources.REQUEST_ENTITY_ARGUMENT);
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
      String basePath = urlPath;
      boolean containsQuestionMark = urlPath.contains("?");
      if (containsQuestionMark) { // If query data exist, remove to get the base path
        basePath = basePath.split("\\?")[0];
      }
      String[] pathValues = basePath.split("/");
      int pathLen = pathValues.length;
      if (pathLen > 1) {
        String keyPathValue = pathValues[pathLen - 2];
        String keyValue = pathValues[pathLen - 1];
        // Only when the key is a word and the value is a number
        if ((!StringUtils.isNumeric(keyPathValue) && StringUtils.isNumeric(keyValue))
            && (!utils.canBeFiltered(keyPathValue, keyValue))) {

          if (containsQuestionMark && urlPath.contains(keyValue + "?")) {
            utils.addToMap(appearanceMap, keyPathValue, keyValue, sampler,
                Sources.REQUEST_PATH_NUMBER_FOLLOWED_BY_QUESTION_MARK);
          } else if (urlPath.contains(keyValue + "/")) {
            utils.addToMap(appearanceMap, keyPathValue, keyValue, sampler,
                Sources.REQUEST_PATH_NUMBER_FOLLOWED_BY_SLASH);
          } else {
            utils.addToMap(appearanceMap, keyPathValue, keyValue, sampler,
                Sources.REQUEST_PATH);
          }
        }
      }
      extractAlphanumericPathIds(sampler, pathValues);

      if (containsQuestionMark) { // Extract query values
        String baseUrl = sampler.getUrl().toString();
        String query = urlPath.split("\\?")[1];
        String url = baseUrl.split("\\?")[0] + "?" + query;
        Map<String, List<String>> params = splitQuery(new URL(url), charset.toString());
        params.forEach((key, values) -> {
          String value = !values.isEmpty() ? values.get(0) : "";
          if (!utils.canBeFiltered(key, value)) {
            addArgument(sampler, key, value, Sources.REQUEST_QUERY);
          }
        });
      }
    } catch (MalformedURLException e) {
      LOG.error("Unexpected error", e);
    }
  }

  /*
   A REST path names a concept and then one of its ids: /carts/cart_62704d1930ac/items. Numeric ids
   are handled above. An alphanumeric segment is a candidate only when it looks like an id, so the
   generic words of a route (items, checkout, v2) never are; the comparison still keeps it only when
   it comes from an earlier response and changes between the recording and the replay.
   */
  private void extractAlphanumericPathIds(HTTPSamplerBase sampler, String[] segments) {
    for (int i = 0; i + 1 < segments.length; i++) {
      String concept = segments[i];
      String id = segments[i + 1];
      if (isPathConcept(concept) && isAlphanumericId(id) && !utils.canBeFiltered(concept, id)) {
        utils.addToMap(appearanceMap, concept, id, sampler, Sources.REQUEST_PATH_ALPHANUMERIC_ID);
      }
    }
  }

  @VisibleForTesting
  static boolean isPathConcept(String segment) {
    return PATH_CONCEPT.matcher(segment).matches();
  }

  /*
   Letters and digits together, and long enough not to be a word: cart_62704d1930ac, a UUID, a hex
   hash. A number alone is not one (the numeric rule covers it).
   */
  @VisibleForTesting
  static boolean isAlphanumericId(String segment) {
    return segment.length() >= MIN_ALPHANUMERIC_ID_LENGTH
        && PATH_ID.matcher(segment).matches()
        && segment.chars().anyMatch(Character::isDigit)
        && segment.chars().anyMatch(Character::isLetter);
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
      if (!HttpAuthorization.isCredentialsField(name)) {
        continue;
      }
      String token = HttpAuthorization.token68(header.getValue());
      if (token != null) {
        utils.addToMap(appearanceMap, name, token, headerManager, Sources.REQUEST_HEADER_FIELDS);
      }
    }
  }

  public Map<String, List<Appearances>> getAppearanceMap() {
    return appearanceMap;
  }
}
