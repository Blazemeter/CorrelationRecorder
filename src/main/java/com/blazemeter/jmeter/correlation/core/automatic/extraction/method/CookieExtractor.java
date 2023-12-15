package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.indexOfIgnoreCase;

import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.automatic.RegexCommons;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CookieExtractor extends Extractor {
  private static final Logger LOG = LoggerFactory.getLogger(CookieExtractor.class);
  private static final String SET_COOKIE = "Set-Cookie";
  private static final String PATH = "Path";
  private static final String DOMAIN = "Domain";
  private static final String IGNORE_TOKEN = "_CR_IGNORE_";

  @Override
  public List<String> extractValue(SampleResult response, String value) {
    // Logic to extract value from the cookies
    return null; // Placeholder
  }

  @Override
  public List<PostProcessor> getPostProcessors(SampleResult response, String value, String name) {
    return new ArrayList<>();
  }

  @Override
  public List<CorrelationExtractor<?>> getCorrelationExtractors(SampleResult response, String value,
                                                                String name) {
    String responseDataAsString = response.getResponseHeaders();
    List<Integer> indexes = ExtractorGenerator.getIndexes(value, responseDataAsString);
    List<CorrelationExtractor<?>> extractors = new ArrayList<>();
    for (int i = 0; i < indexes.size(); i++) {
      String source = "Response Header Set-Cookie ('" + ExtractionConstants.TEXT_NOT_ENCODED + "')";
      String context = getSetCookieContextString(responseDataAsString, name, value, source);
      RegexCorrelationExtractor<?> extractor =
          generateExtractor(name, value, context, ResultField.RESPONSE_HEADERS);
      extractors.add(extractor);
    }
    return extractors;
  }

  @VisibleForTesting
  public static String getSetCookieContextString(String response, String name, String value,
                                                 String source) {
    // Get context from name if the name have [Path=, Domain=]
    String rawName = name;
    Map<String, String> setCookieContext = new HashMap<>();
    Map<String, String> contextCompare;
    if (name.contains("[")) {
      rawName = name.split("\\[")[0];
      String[] contextValues = name.split("\\[")[1].split("]")[0].split(",");
      for (String contextEntry : contextValues) {
        String[] contextKeyValues = contextEntry.split("=");
        String entryKey = contextKeyValues[0].trim();
        String entryValue = contextKeyValues.length > 1 ? contextKeyValues[1].trim() : "";
        if (PATH.equalsIgnoreCase(entryKey) || DOMAIN.equalsIgnoreCase(entryKey)) {
          setCookieContext.put(entryKey, entryValue);
        }
      }
    }

    String[] responseLines = response.split("\n");
    for (String responseLine : responseLines) {
      if (indexOfIgnoreCase(responseLine, SET_COOKIE) == 0) {
        String withoutSetCookie = responseLine.replaceAll("(?i)" + SET_COOKIE, "").trim();
        withoutSetCookie = withoutSetCookie.substring(withoutSetCookie.indexOf(":") + 1).trim();
        String[] entries = withoutSetCookie.split(";");

        String[] keyValue = entries[0].trim().split("=");

        String cookieName = keyValue[0];

        // When the origin of the appearance is set cookie, control the name
        if (source.contains(SET_COOKIE) && !cookieName.equals(rawName)) {
          continue;
        }

        String cookieValue = keyValue.length > 1 ? keyValue[1] : "";
        boolean theSameValue = cookieValue.equals(value);
        if (!theSameValue) {
          continue;
        }

        // Analyze the context, only process when the context is the same
        contextCompare = new HashMap<>();
        // only when value is in value, and the origin use context, generate context
        if (!setCookieContext.isEmpty()) {
          for (int i = 1; i < entries.length; i++) {
            String field = entries[i];
            if (containsIgnoreCase(field, PATH) || containsIgnoreCase(field, DOMAIN)) {
              String[] fieldValues = field.split("=");
              String fieldKey = fieldValues[0].trim();
              String fieldValue = fieldValues.length > 1 ? fieldValues[1].trim() : "";
              contextCompare.put(fieldKey, fieldValue);
            }
          }
        }
        // If the origin not use context, match and if use context, only match the same context
        if (setCookieContext.equals(contextCompare)) {
          // Generate the line removing others data not related to the context
          StringBuilder returnLine = new StringBuilder(responseLine.split(";")[0]);
          for (int i = 1; i < entries.length; i++) {
            returnLine.append(";");
            if (!containsIgnoreCase(entries[i], PATH)
                && !containsIgnoreCase(entries[i], DOMAIN)) {
              returnLine.append(IGNORE_TOKEN);
            } else {
              returnLine.append(entries[i]);
            }
          }
          return returnLine.toString();
        }

      }
    }
    LOG.debug(
        "SetCookieContextString Error getting context string for {} value '{}' in response '{}'",
        name, value, response);
    return "";
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
}
