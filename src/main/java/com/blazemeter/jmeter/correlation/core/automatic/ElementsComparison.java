package com.blazemeter.jmeter.correlation.core.automatic;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.indexOfIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.helger.commons.annotation.VisibleForTesting;
import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.oro.text.regex.Perl5Compiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for the comparison of JMeter elements and the generation of
 * {@link CorrelationSuggestion}.
 */
public class ElementsComparison {
  private static final Logger LOG = LoggerFactory.getLogger(ElementsComparison.class);
  private Configuration configuration;
  private boolean acceptRecordingOrphans = false;
  private boolean acceptReplayOrphans = false;
  private boolean acceptEqualAppearances = false;
  private ResultFileParser parser;
  private Function<SampleResult, String> resultQueryString
      = (sampleResult) -> ((HTTPSampleResult) sampleResult).getQueryString();
  private Function<SampleResult, String> getResponseDataAsString
      = SampleResult::getResponseDataAsString;
  private Function<SampleResult, String> getResponseHeaders
      = SampleResult::getResponseHeaders;
  private Function<SampleResult, String> getCookies
      = SampleResult::getResponseCode;
  private Function<SampleResult, String> getLabel
      = (sampleResult) -> ((HTTPSampleResult) sampleResult).getSampleLabel();
  private Function<SampleResult, String> getResponseContentType
      = SampleResult::getContentType;

  public ElementsComparison() {
    this.configuration = new Configuration();
    this.parser = new ResultFileParser(configuration);
  }

  public ElementsComparison(Configuration configuration) {
    this.configuration = configuration;
    this.parser = new ResultFileParser(configuration);
  }

  @VisibleForTesting
  public void setCookies(Function<SampleResult, String> function) {
    this.getCookies = function;
  }

  @VisibleForTesting
  public CorrelationSuggestion generateMultivaluedSuggestion(DynamicElement element,
                                                             List<SampleResult> results) {
    CorrelationSuggestion suggestion = new CorrelationSuggestion(element.getName(),
        appearancesToString(element.getOriginalAppearance()),
        appearancesToString(element.getOtherAppearance()));

    Map<String, String> valueToReferenceName = new HashMap<>();
    addMultivaluedExtractor(element, suggestion, results, valueToReferenceName);
    addMultivaluedReplacement(element, suggestion, valueToReferenceName);
    return suggestion;
  }

  private void addMultivaluedReplacement(DynamicElement element,
                                         CorrelationSuggestion suggestion,
                                         Map<String, String> valueToReferenceName) {

    List<Appearances> originalAppearances = element.getOriginalAppearance();
    List<Appearances> otherAppearances = element.getOtherAppearance();
    // We use the original and the other appearances since the element could be generated
    // from a recording (original) or a replay (other)
    addReplacementSuggestions(suggestion, valueToReferenceName, originalAppearances);
    addReplacementSuggestions(suggestion, valueToReferenceName, otherAppearances);
  }

  private void addReplacementSuggestions(CorrelationSuggestion suggestion,
                                         Map<String, String> valueToReferenceName,
                                         List<Appearances> originalAppearances) {
    String name = suggestion.getParamName();
    for (Appearances appearance : originalAppearances) {
      String referenceName = valueToReferenceName.get(appearance.getValue());
      if (referenceName == null) {
        continue;
      }

      if (appearance.getSource().contains("Response")) {
        continue;
      }

      for (TestElement usage : appearance.getList()) {
        String source = appearance.getSource();
        String regex = "";
        switch (source) {
          case "Header":
          case "Header Request (Fields)":
            regex = name + ": ([^&]+)";
            break;
          case "Body Data (JSON)":
            //We don't expect this to work for now. Need to improve the regex
            regex = name + ":([^&]+)";
            break;
          case "URL":
            regex = "(?:\\?|?:&)" + name + "=(.+?)(?:&|?:$)";
            break;
          case "HTTP arguments":
            regex = name + "=([^&]+)";
            break;
          case "Request Path":
            regex = "\\/" + name + "\\/(.+?)(?:\\/|?:\\\\?|?:$)";
            break;
          default:
            break;
        }

        RegexCorrelationReplacement<?> replacementSuggestion
            = generateReplacementSuggestion(referenceName, regex);

        if (isRepeated(suggestion, replacementSuggestion)) {
          LOG.debug("Replacement suggestion repeated, excluded:" + name + " "
              + appearance.getValue() + " " + usage);
          continue;
        }

        ReplacementSuggestion replacementSug =
            new ReplacementSuggestion(replacementSuggestion, usage);
        replacementSug.setSource(appearance.getSource());
        replacementSug.setValue(appearance.getValue());
        replacementSug.setName(name);
        suggestion.addReplacementSuggestion(replacementSug);
        suggestion.addUsage(usage);

        LOG.debug("Add replacement suggestion:" + replacementSug.getName()
            + " " + replacementSug.getValue() + " " + replacementSug.getUsage());
      }
    }
  }

  private boolean isRepeated(CorrelationSuggestion suggestion,
                             RegexCorrelationReplacement<?> replacementSuggestion) {
    List<ReplacementSuggestion> replacementSuggestions = suggestion.getReplacementSuggestions();
    boolean repeated = false;
    for (ReplacementSuggestion existing : replacementSuggestions) {
      String s = existing.toString();
      if (s.equals(replacementSuggestion.toString())) {
        repeated = true;
        break;
      }
    }
    return repeated;
  }

  private boolean isRepeated(CorrelationSuggestion suggestion,
                             ExtractionSuggestion extractionSuggestion) {
    boolean repeated = false;
    for (ExtractionSuggestion existing : suggestion.getExtractionSuggestions()) {
      if (existing.toString().equals(extractionSuggestion.toString())) {
        repeated = true;
        break;
      }
    }
    return repeated;
  }

  private void addMultivaluedExtractor(
      DynamicElement element, CorrelationSuggestion suggestion, List<SampleResult> results,
      Map<String, String> valueToReferenceName) {

    for (SampleResult result : results) {
      //We use both the "original" and the "other" appearances since the map can come from either
      //the original recorder or from the failing replay
      addExtractorSuggestions(valueToReferenceName, suggestion, result,
          element.getOriginalAppearance());
      addExtractorSuggestions(valueToReferenceName, suggestion, result,
          element.getOtherAppearance());
    }
  }

  private String encodeValue(String value) {
    String encodedValue = URLEncoder.encode(value);
    if (encodedValue.indexOf(":////") == -1) {
      encodedValue = encodedValue.replace("+", "%20");
    }
    return encodedValue;
  }

  private void addExtractorSuggestions(Map<String, String> valueToReferenceName,
                                      CorrelationSuggestion suggestion, SampleResult result,
                                      List<Appearances> appearances) {
    for (Appearances appearance : appearances) {
      String name = suggestion.getParamName();
      if (suggestion.getAppearances().size() > configuration.getMaxNumberOfAppearances()
          && configuration.getMaxNumberOfAppearances() != -1) {
        LOG.warn("Too many appearances for element  '{}'. Please review the total appearances.",
            name);
        return;
      }

      List<String> samplerNameList = appearance.getList().stream()
          .map(TestElement::getName)
          .collect(Collectors.toList());

      if (samplerNameList.contains(this.getLabel.apply(result))) {
        String queryString = this.resultQueryString.apply(result);
        boolean encodedUsed = queryString.contains(URLEncoder.encode(appearance.getValue()));
        boolean rawUsed = queryString.contains(appearance.getValue());
        String usage = encodedUsed ? "encoded" : "raw";
        if (encodedUsed || rawUsed) {
          System.out.println("'" + name + "') Is used on '" + result.getSampleLabel()
              + "' with a '" + usage + "' value '" + appearance.getValue() + "'");
          //If the value is already being used, we don't need to extract it.
          continue;
        }
      }

      String value = appearance.getValue();
      String encodedValue = encodeValue(value);

      String responseDataAsString =
          result.getDataType().equals("bin") ? "" : this.getResponseDataAsString.apply(result);
      String responseHeadersRaw = this.getResponseHeaders.apply(result);
      // Remove Set-Cookie from Response Headers
      String responseHeaders = responseHeadersRaw.replaceAll(
          "(?m)^Set-Cookie.*", "");
      String responseHeadersSetCookies = responseHeadersRaw.replaceAll(
          "(?m)^(?!Set-Cookie).*", "").replaceAll("(?m)^\\s+$", "");

      // If the value is not in the response, we can't extract it. So we skip it.
      String source = "";

      boolean isInHeader = responseHeaders.contains(value);
      boolean isInHeaderEncoded = !isInHeader ? responseHeaders.contains(encodedValue) : false;
      boolean isInSetCookie = responseHeadersSetCookies.contains(value);
      boolean isInSetCookieEncoded =
          !isInSetCookie ? responseHeadersSetCookies.contains(encodedValue) : false;
      boolean isInBody = responseDataAsString.contains(value);
      boolean isInBodyEncoded = !isInBody ? responseDataAsString.contains(encodedValue) : false;

      if (!isInBody && !isInBodyEncoded && !isInHeader && !isInHeaderEncoded && !isInSetCookie &&
          !isInSetCookieEncoded) {
        continue;
      }

      String contextString;
      boolean isEncoded;
      ResultField targetField;
      if (isInHeader || isInHeaderEncoded) {
        isEncoded = isInHeaderEncoded;
        contextString = getContextString(responseHeaders, isEncoded ? encodedValue : value);
        targetField = ResultField.RESPONSE_HEADERS;
        source = "Response Header ('" + (!isEncoded ? "Raw" : "Encoded") + "')";
      } else if (isInBody || isInBodyEncoded) {
        isEncoded = isInBodyEncoded;
        if (JMeterElementUtils.isJson(responseDataAsString)) {
          contextString =
              getJsonContextString(responseDataAsString, name,
                  isInHeaderEncoded ? encodedValue : value, source);
        } else {
          contextString =
              getContextString(responseDataAsString, isInHeaderEncoded ? encodedValue : value);
        }
        targetField = ResultField.BODY;
        source = "Response Body ('" + (!isEncoded ? "Raw" : "Encoded") + "')";
      } else if (isInSetCookie || isInSetCookieEncoded) {
        isEncoded = isInSetCookieEncoded;
        contextString = getSetCookieContextString(responseHeadersSetCookies, appearance.getName(),
            isEncoded ? encodedValue : value, appearance.getSource());
        targetField = ResultField.RESPONSE_HEADERS;
        source = "Response Header Set-Cookie ('" + (!isEncoded ? "Raw" : "Encoded") + "')";
      } else {
        LOG.warn("There was an error while trying to extract the value '{}'. Skipping element '{}'",
            value, name);
        continue;
      }
      if (isEmpty(contextString)) {
        LOG.debug(
            "There was an error while trying to get context value from '{}'. "
                + "Skipping element '{}' in {}",
            value, name, source);
        continue;
      }

      String paramName = suggestion.getExtractionParamName();
      ExtractionSuggestion extractionSuggestion = new ExtractionSuggestion(
          generateExtractor(paramName, isEncoded ? encodedValue : value,
              contextString, targetField), result);
      extractionSuggestion.setSource(source);
      extractionSuggestion.setValue(value);
      extractionSuggestion.setName(name);

      if (isRepeated(suggestion, extractionSuggestion)) {
        continue;
      }

      suggestion.addExtractionSuggestion(extractionSuggestion);
      suggestion.addAppearances(result);
      valueToReferenceName.putIfAbsent(value, paramName);
    }
  }

  private String appearancesToString(List<Appearances> appearances) {
    Map<String, Integer> appearancesMap = new HashMap<>();
    for (Appearances app : appearances) {
      String key = app.getValue();
      if (appearancesMap.containsKey(key)) {
        appearancesMap.put(key, appearancesMap.get(key) + 1);
      } else {
        appearancesMap.put(key, 1);
      }
    }

    return appearancesMap.entrySet().stream()
        .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
        .collect(Collectors.joining(", "));
  }

  private RegexCorrelationReplacement<?> generateReplacementSuggestion(String valueName,
                                                                       String regex) {
    RegexCorrelationReplacement<?> replacement = new RegexCorrelationReplacement<>(regex);
    replacement.setVariableName(valueName);
    return replacement;
  }

  @VisibleForTesting
  public RegexCorrelationExtractor<?> generateExtractor(String valueName, String originalValue,
                                                        String contextString,
                                                        ResultField targetField) {
    RegexCorrelationExtractor<?> extractor = new RegexCorrelationExtractor<>();
    extractor.setVariableName(valueName);
    String regex = "";
    String simpleRegexCapture = "(.+?)";
    try {
      String[] boundaries = contextString.split(originalValue);
      boolean isBeggingOfTheLine = false;
      if (boundaries[0].contains("\n")) {
        isBeggingOfTheLine = true;
        boundaries[0] = boundaries[0].substring(boundaries[0].lastIndexOf("\n") + 1);
      }

      boolean isEndOfTheLine = false;
      if (boundaries.length > 1 && boundaries[1].contains("\n")) {
        isEndOfTheLine = true;
        boundaries[1] = boundaries[1].substring(0, boundaries[1].indexOf("\n"));
      }

      // We escape the context boundaries to avoid regex issues
      String regexLeft = (isBeggingOfTheLine ? "\n" : "") + Perl5Compiler.quotemeta(boundaries[0]);
      String regexRigth = (boundaries.length > 1 ? Perl5Compiler.quotemeta(boundaries[1])
          + (isEndOfTheLine ? "\\n" : "") : "$");
      regex = regexLeft + simpleRegexCapture + regexRigth;
      // Fix regex when /[space] change to /s
      regex = regex.replace("\\ ", "\\s");
      // In case of _CR_IGNORE_ replace to wildcard matcher
      if (regex.contains("_CR_IGNORE_")) {
        regex = regex.replaceAll("_CR_IGNORE_", simpleRegexCapture);
      }
    } catch (IndexOutOfBoundsException | PatternSyntaxException exception) {
      LOG.error("Error trying to generate regex for value '{}' in context '{}'", originalValue,
          contextString);
      exception.printStackTrace();
      regex = contextString.replace(originalValue, simpleRegexCapture);
    }

    extractor.setParams(Arrays.asList(regex, "1", "1", targetField.name(), "true"));
    return extractor;
  }

  @VisibleForTesting
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

  @VisibleForTesting
  public static String getJsonPathToValue(List<Pair<String, Object>> parameters, String value,
                                          boolean useContains) {
    return getJsonPathToValue(parameters, value, useContains, 0, value.startsWith("["));
  }

  @VisibleForTesting
  public static String getJsonPathToValue(List<Pair<String, Object>> parameters, String value,
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

  @VisibleForTesting
  public String getJsonContextString(String response, String name, String value,
                                     String source) {
    List<Pair<String, Object>> parameters =
        JMeterElementUtils.extractDataParametersFromJson(response);

    String jsonPath = getJsonPathToValue(parameters, value, true);
    if (jsonPath.length() > 0) {
      return getContextString(response, value);
    }
    LOG.debug(
        "getJsonContextString Error getting context string for {} value '{}' in response '{}'",
        name, value, response);
    return "";
  }

  @VisibleForTesting
  public String getSetCookieContextString(String response, String name, String value,
                                          String source) {
    // Get context from name if the name have [Path=, Domain=]
    Map<String, String> setCookieContext = new HashMap<>();
    Map<String, String> contextCompare;
    String rawName = name;
    if (name.contains("[")) {
      rawName = name.split("\\[")[0];
      String[] contextValues = name.split("\\[")[1].split("]")[0].split(",");
      for (String contextEntry : contextValues) {
        String[] contextKeyValues = contextEntry.split("=");
        String entryKey = contextKeyValues[0].trim();
        String entryValue = contextKeyValues.length > 1 ? contextKeyValues[1].trim() : "";
        if ("Path".equalsIgnoreCase(entryKey) || "Domain".equalsIgnoreCase(entryKey)) {
          setCookieContext.put(entryKey, entryValue);
        }
      }
    }

    String[] responseLines = response.split("\n");
    for (String responseLine : responseLines) {
      if (indexOfIgnoreCase(responseLine, "Set-Cookie") == 0) {
        String withoutSetCookie = responseLine.replaceAll("(?i)Set-Cookie", "").trim();
        withoutSetCookie = withoutSetCookie.substring(withoutSetCookie.indexOf(":") + 1).trim();
        String[] entries = withoutSetCookie.split(";");

        String[] keyValue = entries[0].trim().split("=");

        String cookieName = keyValue[0];

        // When the origin of the appearance is set cookie, control the name
        if (source.contains("Set-Cookie") && !cookieName.equals(rawName)) {
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
        if (setCookieContext.size() > 0) {
          for (int i = 1; i < entries.length; i++) {
            String field = entries[i];
            if (containsIgnoreCase(field, "Path") || containsIgnoreCase(field, "Domain")) {
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
          String returnLine = responseLine.split(";")[0];
          for (int i = 1; i < entries.length; i++) {
            if (!containsIgnoreCase(entries[i], "Path") &&
                !containsIgnoreCase(entries[i], "Domain")) {
              returnLine += ";_CR_IGNORE_";
            } else {
              returnLine += ";" + entries[i];
            }
          }
          return returnLine;
        }

      }
    }
    LOG.debug(
        "SetCookieContextString Error getting context string for {} value '{}' in response '{}'",
        name, value, response);
    return "";
  }

  private String removeNewLines(String value, String contextString) {
    int locationIndex = contextString.indexOf(value);
    int newLineIndex = contextString.indexOf("\n");
    if (newLineIndex == -1) {
      return contextString;
    } else if (newLineIndex < locationIndex) {
      return removeNewLines(value, contextString.substring(newLineIndex + 1));
    } else {
      return contextString.substring(0, newLineIndex);
    }
  }

  @VisibleForTesting
  public Map<String, List<Appearances>> jmxToMap(String filepath) {
    RecordingExtraction recordingExtraction = new RecordingExtraction(configuration);
    Map<String, List<Appearances>> map = recordingExtraction.extractAppearanceMap(filepath);
    return map;
  }

  /**
   * Generates a list of {@link DynamicElement} by comparing two maps of parameters.
   * This method expects that the map comes from either a JMX recording or
   * a JTL recording/replay files and that the JTL files have both successful
   * and failed requests.
   * <p>If the JTL file used for the <b>replayMap</b> only has failed requests, consider using
   * {@link ElementsComparison#getDynamicElementsFromFailedReplay(java.util.Map, java.util.Map)}
   * instead. </p>
   *
   * @param originalMap the map from the JMX recording file.
   * @param replayMap   the map from the replay JTL file.
   * @return a list of elements that are dynamic between the two maps.
   */
  @VisibleForTesting
  public List<DynamicElement> getDynamicElements(Map<String, List<Appearances>> originalMap,
                                                 Map<String, List<Appearances>> replayMap) {
    Map<String, List<Appearances>> orphanOriginal = new HashMap<>(originalMap);
    Map<String, List<Appearances>> orphanReplay = new HashMap<>(replayMap);
    Map<String, List<Appearances>> apparentlyEquals = new HashMap<>();

    List<DynamicElement> differences = new ArrayList<>();
    originalMap.forEach((referenceName, appearances) -> {
      // Allow to manually add parameters to the list of dynamic elements
      if (configuration.getRequestedParameters().contains(referenceName)) {
        differences.add(new DynamicElement(referenceName, appearances, new ArrayList<>()));
        orphanOriginal.remove(referenceName);
        return;
      }

      List<Appearances> otherAppearances = replayMap.get(referenceName);
      // Check if the parameter is present in the other recording

      if (otherAppearances == null || otherAppearances.isEmpty()) {
        LOG.warn("Parameter {} is not present in the replay recording. "
            + "Ignoring its {} appearances.", referenceName, appearances.size());
        return;
      }
      orphanOriginal.remove(referenceName);
      orphanReplay.remove(referenceName);

      int maxAppearances = configuration.getMaxNumberOfAppearances();
      if (appearances.size() > maxAppearances || otherAppearances.size() > maxAppearances) {
        return;
      }

      List<Appearances> originalParametrized = appearances.stream()
          .filter(appearance -> appearance.getValue().contains("${")
              && appearance.getValue().contains("}"))
          .collect(Collectors.toList());

      List<Appearances> replayParametrized = otherAppearances.stream()
          .filter(appearance -> appearance.getValue().contains("${")
              && appearance.getValue().contains("}"))
          .collect(Collectors.toList());

      if (!originalParametrized.toString().equals(replayParametrized.toString())) {
        System.out.println(referenceName + ": there are values that are parametrized that might "
            + "be causing the difference. ");
      }

      if (areEqualsAfterSorting(appearances, otherAppearances)) {
        apparentlyEquals.put(referenceName, appearances);
        return;
      }

      differences.add(new DynamicElement(referenceName, appearances, otherAppearances));
    });

    reportAndAcceptOrphans(orphanOriginal, differences, acceptRecordingOrphans, "Original Trace");
    reportAndAcceptOrphans(orphanReplay, differences, acceptReplayOrphans, "Replay Trace");
    reportAndAcceptOrphans(apparentlyEquals, differences, acceptEqualAppearances,
        "Apparently equal");

    return differences;
  }

  private void reportAndAcceptOrphans(Map<String, List<Appearances>> orphanOriginal,
                                      List<DynamicElement> differences,
                                      boolean condition, String source) {
    if (!orphanOriginal.isEmpty()) {
      if (condition) {
        orphanOriginal.forEach((key, value) ->
            differences.add(new DynamicElement(key, value, new ArrayList<>())));
      } else if (!configuration.getRequestedParameters().isEmpty()) {
        orphanOriginal.forEach((key, value) -> {
          if (configuration.getRequestedParameters().contains(key)) {
            differences.add(new DynamicElement(key, value, new ArrayList<>()));
          }
        });
      }
    }
  }

  private boolean areEqualsAfterSorting(List<Appearances> originalRecording,
                                        List<Appearances> replayTrace) {
    if (originalRecording.size() != replayTrace.size()) {
      return false;
    }

    String originalAppearances = getSortedAppearances(originalRecording);
    String replayAppearances = getSortedAppearances(replayTrace);

    return originalAppearances.equals(replayAppearances);
  }

  public static String getSortedAppearances(List<Appearances> appearances) {
    return appearances.stream()
        .map(app -> "  " + app.getSource() + ":" + app.getValue())
        .sorted().collect(Collectors.joining("\n"));
  }

  /**
   * Generates a list of {@link CorrelationSuggestion} by comparing one JMX file and two JTL files.
   * <p>
   * Generates a list of {@link CorrelationSuggestion} by comparing the arguments of the JMX with
   * the JTL file of another recording. Later on the JTL file of the original recording is used to
   * {@link com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement}
   * required to do the Correlation process.
   * </p>
   *
   * @param originalJmxPath the path of the JMX file where the changes will be applied.
   * @param originalJtlPath the path of the JTL file from the recording of the originalJmxPath.
   * @param replayJtlPath   the path of the JTL file from another recording.
   * @return a list of {@link CorrelationSuggestion} with the changes to apply
   */
  public List<CorrelationSuggestion> generateSuggestionsFromResultFile(String originalJmxPath,
                                                                       String originalJtlPath,
                                                                       String replayJtlPath) {
    List<SampleResult> originalResults = parser
        .loadFromFile(new File(originalJtlPath), false);

    List<DynamicElement> dynamicElements = getDynamicElements(jmxToMap(originalJmxPath),
        jtlToMap(replayJtlPath));

    return dynamicElements.stream()
        .map(element -> generateMultivaluedSuggestion(element, originalResults))
        .collect(Collectors.toList());
  }

  /**
   * Generates a map of the parameters contained in a jtl file and a list of {@link Appearances}
   * for each possible value of the parameter in that file. Note: The list of appearances is
   * important since it contains: the {@link TestElement} where the parameter is used and
   * the value of the parameter in that element (which could be different).
   *
   * @param filepath the path of the jtl file.
   * @return a map of the parameters contained in a jtl file and the list of {@link Appearances}.
   */
  @VisibleForTesting
  public Map<String, List<Appearances>> jtlToMap(String filepath) {
    return new ResultsExtraction(configuration).extractAppearanceMap(filepath);
  }

  public Map<String, List<Appearances>> failingJtlToMap(String replayTrace, String recordingTrace) {
    ResultFileParser parser = new ResultFileParser(configuration);
    List<SampleResult> replayResults = parser.loadFromFile(new File(replayTrace), true);
    List<SampleResult> recordingResults = parser.loadFromFile(new File(recordingTrace), true);

    List<SampleResult> failedRecordingResults = recordingResults.stream()
        .filter(result -> !result.isSuccessful())
        .collect(Collectors.toList());

    List<SampleResult> failedReplayResults = replayResults.stream()
        .filter(result -> !result.isSuccessful())
        .collect(Collectors.toList());

    List<SampleResult> failedReplayResultsWithoutRecordingFailures = failedReplayResults
        .stream()
        .filter(result -> failedRecordingResults.stream()
            .noneMatch(recordingResult -> recordingResult.getSampleLabel()
                .equals(result.getSampleLabel())))
        .collect(Collectors.toList());

    return new ResultsExtraction(configuration)
        .extractAppearanceMap(failedReplayResultsWithoutRecordingFailures);
  }

  public List<CorrelationSuggestion> generateSuggestionsFromFailingReplayTraceOnly(
      String originalTrace,
      String replayTrace) {
    LOG.info("Generating suggestions from failing replay trace only.");
    LOG.info("Recording's Trace '{}'.", originalTrace);
    LOG.info("Failing replay's Trace '{}'.", replayTrace);

    Map<String, List<Appearances>> recordingTraceMap = jtlToMap(originalTrace);
    Map<String, List<Appearances>> replayFailingMap = failingJtlToMap(replayTrace, originalTrace);

    setAcceptEqualAppearances(true);
    List<DynamicElement> replayDynamicParams =
        getDynamicElements(replayFailingMap, recordingTraceMap);

    List<SampleResult> recordingResults = new ResultFileParser(configuration)
        .loadFromFile(new File(originalTrace), true);

    return getSuggestions(replayDynamicParams, recordingResults);
  }

  private List<CorrelationSuggestion> getSuggestions(
      List<DynamicElement> dynamicElements,
      List<SampleResult> originalResults) {
    List<CorrelationSuggestion> suggestions = new ArrayList<>();
    List<CorrelationSuggestion> orphanSuggestions = new ArrayList<>();
    for (DynamicElement replayCandidate : dynamicElements) {
      CorrelationSuggestion suggestion =
          generateMultivaluedSuggestion(replayCandidate, originalResults);
      if (suggestion.getExtractionSuggestions().isEmpty()
          || suggestion.getReplacementSuggestions().isEmpty()) {
        orphanSuggestions.add(suggestion);
        continue;
      }
      suggestions.add(suggestion);
    }

    //Left for debug purposes
    System.out.println("===   ORPHANS REPORT ==");
    StringBuilder builder = new StringBuilder();
    builder.append(orphanSuggestions.size()).append("- Orphan Suggestions: \n");
    for (CorrelationSuggestion orphan : orphanSuggestions) {
      builder.append(orphan.getParamName()).append(" ")
          .append("Ext=").append(orphan.getExtractionSuggestions().size()).append(" ")
          .append("Rep=").append(orphan.getReplacementSuggestions().size()).append("\n");
    }

    for (CorrelationSuggestion suggestion : suggestions) {
      builder.append(suggestion.getParamName()).append(" ")
          .append("Ext=").append(suggestion.getExtractionSuggestions().size()).append(" ")
          .append("Rep=").append(suggestion.getReplacementSuggestions().size()).append("\n");
    }
    System.out.println(builder.toString());

    return suggestions;
  }

  /**
   * Generates a list of {@link DynamicElement} by comparing the arguments of the JMX with the JTL
   * from a failing recording. Note: The failing recording should be configured to only have "failed
   * samples" in the JTL file. This is used to generate a list of {@link DynamicElement} that can be
   * used to generate a list of {@link CorrelationSuggestion} from request that failed.
   *
   * @param recordingMap the map obtained from the JMX file (see {@link #jmxToMap(String)}).
   * @param replayMap    the map obtained from the JTL file of the replay.
   * @return a list of {@link DynamicElement} with the elements that could be correlated.
   */
  public List<DynamicElement> getDynamicElementsFromFailedReplay(
      Map<String, List<Appearances>> recordingMap,
      Map<String, List<Appearances>> replayMap) {
    List<DynamicElement> dynamicElements = new ArrayList<>();
    replayMap.forEach((key, value) -> {
      List<Appearances> originalAppearance = recordingMap.get(key);
      if (originalAppearance == null || originalAppearance.isEmpty()) {
        return;
      }
      // since it is a replay, the "new value" will be the same as the "old recorded value"
      dynamicElements.add(new DynamicElement(key, originalAppearance, originalAppearance));
    });
    return dynamicElements;
  }

  public void setAcceptEqualAppearances(boolean acceptEqualAppearances) {
    this.acceptEqualAppearances = acceptEqualAppearances;
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}
