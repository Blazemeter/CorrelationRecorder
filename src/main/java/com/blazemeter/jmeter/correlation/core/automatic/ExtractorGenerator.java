package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.core.automatic.extraction.method.CookieExtractor;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.method.ExtractionConstants;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.method.XmlBodyExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import com.helger.commons.annotation.VisibleForTesting;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractorGenerator {
  private static final Logger LOG = LoggerFactory.getLogger(ExtractorGenerator.class);
  private Configuration configuration;
  private String name;
  private String value;
  private String usedValue;
  private String usedResponse;
  private String source;
  private String contextString;
  private List<Integer> indexes;
  private ResultField targetField;
  private List<RegexCorrelationExtractor<?>> extractors = new ArrayList<>();

  private String encodedValue;
  private String responseHeaders;
  private String responseBody;
  private String responseCookies;

  private boolean isEncoded;
  private boolean isInHeader;
  private boolean isInHeaderEncoded;
  private boolean isInSetCookie;
  private boolean isInSetCookieEncoded;
  private boolean isInBody;
  private boolean isInBodyEncoded;
  private boolean isXml = false;
  private boolean isJson = false;
  private String sampleName;

  public ExtractorGenerator(Configuration configuration, String name, String value) {
    this.configuration = configuration;
    this.name = name;
    this.value = value;
  }

  public boolean isValueNotPresent() {
    this.encodedValue = encodeValue(value);
    this.isInHeader = responseHeaders.contains(value);
    this.isInHeaderEncoded = !isInHeader && responseHeaders.contains(encodedValue);
    this.isInSetCookie = responseCookies.contains(value);
    this.isInSetCookieEncoded = !isInSetCookie && responseCookies.contains(encodedValue);
    this.isInBody = responseBody.contains(value);
    this.isInBodyEncoded = !isInBody && responseBody.contains(encodedValue);

    return !isInBody && !isInBodyEncoded && !isInHeader && !isInHeaderEncoded && !isInSetCookie
        && !isInSetCookieEncoded;
  }

  public static String encodeValue(String value) {
    String encodedValue = URLEncoder.encode(value);
    if (encodedValue.indexOf(":////") == -1) {
      encodedValue = encodedValue.replace("+", "%20");
    }
    return encodedValue;
  }

  public void updateUsedValues() {
    if (inHeaders()) {
      updateHeadersValues();
    } else if (inBody()) {
      updateBodyValues();
    } else if (inCookies()) {
      updateSetCookiesValues();
    } else {
      LOG.warn("There was an error while trying to extract the value '{}'. Skipping element '{}'",
          value, name);
    }
  }

  private void updateHeadersValues() {
    isEncoded = isInHeaderEncoded;
    usedValue = getUsedValue();
    usedResponse = responseHeaders;
    targetField = ResultField.RESPONSE_HEADERS;
    source = "Response Header ('" + getEncodedSourceString(isEncoded) + "')";

    contextString = getContextString(usedResponse, usedValue);
  }

  private String getUsedValue() {
    return isEncoded ? encodeValue(value) : value;
  }

  private void updateBodyValues() {
    isEncoded = isInBodyEncoded;
    usedValue = getUsedValue();
    usedResponse = responseBody;
    targetField = ResultField.BODY;

    source = "Response Body ('" + getEncodedSourceString(isEncoded) + "')";
    isJson = JMeterElementUtils.isJson(usedResponse);
    isXml = !isJson && JMeterElementUtils.isXml(usedResponse);
  }

  private void updateSetCookiesValues() {
    isEncoded = isInSetCookieEncoded;
    usedValue = getUsedValue();
    usedResponse = responseCookies;
    targetField = ResultField.RESPONSE_HEADERS;

    source = "Response Header Set-Cookie ('" + getEncodedSourceString(isEncoded) + "')";
    contextString =
        CookieExtractor.getSetCookieContextString(usedResponse, name, usedValue, source);
  }

  private static String getEncodedSourceString(boolean isEncoded) {
    return !isEncoded ? ExtractionConstants.TEXT_NOT_ENCODED : ExtractionConstants.TEXT_ENCODED;
  }

  public String getContextString(String response, String value) {
    int location = response.indexOf(value);
    int contextLength = configuration.getContextLength();
    int indexStart = Math.max(location - contextLength, 0);
    // +2 for the 0-based index and the length of the value
    int indexEnd = location + value.length() + contextLength;

    if (response.length() < indexEnd) {
      indexEnd = response.length();
    }

    try {
      return response.substring(indexStart, indexEnd);
    } catch (StringIndexOutOfBoundsException e) {
      LOG.debug("Error getting context string for value '{}' in response '{}'", value, response);
      return "";
    }
  }

  public void setIndexes(List<Integer> indexes) {
    this.indexes = indexes;
  }

  public List<Integer> getIndexes() {
    return indexes;
  }

  public static List<Integer> getIndexes(String v, String text) {
    if (v.isEmpty()) {
      return new ArrayList<>();
    }

    return IntStream.range(0, text.length() - v.length() + 1)
        .filter(index -> text.substring(index, index + v.length()).equalsIgnoreCase(v))
        .boxed()
        .collect(Collectors.toList());
  }

  public void generateExtractors() {
    this.extractors.clear();
    updateUsedValues();
    //TODO: Remove this short-circuit (used to avoid debugging the whole process)
    if (usedValue.length() < 4) {
      return;
    }

    List<Integer> indexes = getIndexes(usedValue, usedResponse);
    if (indexes.isEmpty()) {
      LOG.warn("No matches found for value '{}' in response on source {}", usedValue, source);
      System.out.println(
          name + ") No matches found for value '" + usedValue + "' in response on source '" +
              source + "'.");
      return;
    }

    String context = "";
    if (indexes.size() == 1) {
      // It is a single value, a simple regex extractor should suffice
      context = getContextString(usedResponse, usedValue);
      RegexCorrelationExtractor<?> extractor =
          generateExtractor(name, usedValue, context, targetField);
      extractors.add(extractor);
      return;
    }

    System.out.println(
        "Name: '" + name + "'. " + (isXml ? "XML" : (isJson ? "JSON" : "Raw")) + " value: '" +
            usedValue + "'.");
    System.out.println(" - " + indexes.size() + " matches found in response '" + sampleName + "'");

    List<String> contexts = new ArrayList<>();
    List<String> paths = new ArrayList<>();
    if (isJson) {
      String usedValueWithQuotes = "\"" + usedValue + "\"";
      List<Integer> indexesWithQuotes = getIndexes(usedValueWithQuotes, usedResponse);
      if (!indexesWithQuotes.isEmpty()) {
        usedValue = usedValueWithQuotes;
      }
      System.out.println(
          " - " + indexesWithQuotes.size() + " matches found (with quotes) in response '" +
              sampleName + "'");
      List<Pair<String, Object>> parameters =
          JMeterElementUtils.extractDataParametersFromJson(usedResponse);
      String jsonPath = getJsonPathToValue(parameters, value, true);
      System.out.println(" - JSON Path: '" + jsonPath + "'");
      contextString = getJsonContextString(usedResponse, name, usedValue);
      contexts.add(contextString);
      paths.add(jsonPath);
    } else if (isXml) {
      String xmlPath = XmlBodyExtractor.getXmlPath(usedResponse, usedValue, name);
      System.out.println(" - XML Path: '" + xmlPath + "'");
      contextString = getContextString(usedResponse, usedValue);
      contexts.add(contextString);
      paths.add(xmlPath);
    } else {
      contextString = getContextString(usedResponse, usedValue);
      contexts = getContexts(usedResponse, usedValue);
    }

    System.out.println(" Contexts: " + contexts.size() + ". Paths: " + paths.size());
  }

  @VisibleForTesting
  public RegexCorrelationExtractor<?> generateExtractor(String valueName, String originalValue,
                                                        String contextString,
                                                        ResultField targetField) {
    RegexCorrelationExtractor<?> extractor = new RegexCorrelationExtractor<>();
    extractor.setVariableName(valueName);
    String regex = RegexCommons.dynamicGenerateExtractorRegex(originalValue, contextString);
    extractor.setParams(Arrays.asList(regex, "1", "1", targetField.name(), "true"));
    return extractor;
  }

  @VisibleForTesting
  public List<String> getContexts(String response, String value) {
    List<String> contexts = new ArrayList<>();
    List<Integer> indices = getIndexes(value, response);
    for (Integer index : indices) {
      int location = index;
      int contextLength = configuration.getContextLength();
      int indexStart = Math.max(location - contextLength, 0);
      // +2 for the 0-based index and the length of the value
      int indexEnd = location + value.length() + contextLength;

      if (response.length() < indexEnd) {
        indexEnd = response.length();
      }

      try {
        contexts.add(response.substring(indexStart, indexEnd));
      } catch (StringIndexOutOfBoundsException e) {
        LOG.debug("Error getting context string for value '{}' in response '{}'", value, response);
      }
    }
    return contexts;
  }

  public String getJsonContextString(String response, String name, String value) {
    String jsonPath = getJsonPathFromResponse(response, value, true);
    if (!jsonPath.isEmpty()) {
      return getContextString(response, value);
    }
    LOG.debug(
        "getJsonContextString Error getting context string for {} value '{}' in response '{}'",
        name, value, response);
    return "";
  }

  @VisibleForTesting
  public String getJsonPathFromResponse(String response, String value, boolean useContains) {
    List<Pair<String, Object>> parameters =
        JMeterElementUtils.extractDataParametersFromJson(response);

    return getJsonPathToValue(parameters, value, useContains);
  }

  public static String getJsonPathToValue(List<Pair<String, Object>> parameters, String value,
                                          boolean useContains) {
    return getJsonPathToValue(parameters, value, useContains, 0, value.startsWith("["));
  }

  private static String getJsonPathToValue(List<Pair<String, Object>> parameters, String value,
                                           boolean useContains, int level, boolean inArray) {
    String jsonPath = "";
    for (Pair<String, Object> param : parameters) {
      Object paramValue = param.getValue();
      if (paramValue == null) {
        LOG.debug("Null value for parameter '{}'", param.getKey());
        continue;
      }
      String keyValue = paramValue.toString();
      boolean isString = (param.getValue() instanceof String);
      if (JMeterElementUtils.isJson(keyValue)) {
        inArray = keyValue.startsWith("[");
        List<Pair<String, Object>> subParameters =
            JMeterElementUtils.extractDataParametersFromJson(keyValue);
        String subJsonPath =
            getJsonPathToValue(subParameters, value, useContains, level + 1, inArray);
        if (subJsonPath.length() > 0) {
          if (level == 0) {
            jsonPath = "$";
          }
          jsonPath += "." + param.getKey() + subJsonPath;
          break;
        }
      } else {
        if ((!useContains && keyValue.equals(value)) || (useContains && keyValue.contains(value))) {
          if (inArray) {
            if (isString) {
              jsonPath += "[?(@." + param.getKey() + " == '" + keyValue + "')]." + param.getKey();
            } else {
              jsonPath += "[?(@." + param.getKey() + " == " + keyValue + ")]." + param.getKey();
            }
          } else {
            jsonPath += "." + param.getKey();
          }
          break;
        }
      }
    }
    return jsonPath;
  }

  public List<RegexCorrelationExtractor<?>> getExtractors() {
    return extractors;
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

  public String getUsedResponse() {
    return usedResponse;
  }

  public void setUsedResponse(String usedResponse) {
    this.usedResponse = usedResponse;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setResponseHeaders(String responseHeader) {
    this.responseHeaders = responseHeader;
  }

  public void setResponseBody(String responseDataAsString) {
    this.responseBody = responseDataAsString;
  }

  public void setResponseHeadersSetCookies(String responseCookies) {
    this.responseCookies = responseCookies;
  }

  public boolean inHeaders() {
    return isInHeader || isInHeaderEncoded;
  }

  public boolean inBody() {
    return isInBody || isInBodyEncoded;
  }

  public boolean inCookies() {
    return isInSetCookie || isInSetCookieEncoded;
  }

  public void verifyExtractors() {
    //    try {
    //      String regex = RegexCommons.getRegexFromExtractor(extractors.get(0));
    //      int totalOfMatches = RegexCommons.findMatchNumber(regex,
    //                            valueLocation.getUsedResponse(),
    //                            valueLocation.getValue());
    //      if (totalOfMatches > 1) {
    //        System.out.println(paramName + ") Value: '" + value
    //            + "' Times matched '" + totalOfMatches + "'. Should be '" + indices.size()
    //            + "'. JSON Response: " + isJSONResponse
    //            + "'. XML Response: " + isXml
    //            + ". Using regex '" + regex + "' in response '" + result.getSampleLabel() + "'");
    //      }
    //          RegexCorrelationExtractor extractorTuned =
    //              RegexCommons.detectMatchingNumber(extractionSuggestion.getExtractor(),
    //                  usedResponse, value);
    //          System.out.println("Extractor tuned: " + extractorTuned);
    //    } catch (Exception e) {
    //      LOG.error("Error tuning extractor for value '{}' in response '{}'", value,
    //              usedResponse);
    //      System.out.println("Error tuning extractor for value '" + value + "' in response '" +
    //      usedResponse + "'");
    //    }
  }

  public void setSampleResultName(String sampleLabel) {
    this.sampleName = sampleLabel;
  }
}
