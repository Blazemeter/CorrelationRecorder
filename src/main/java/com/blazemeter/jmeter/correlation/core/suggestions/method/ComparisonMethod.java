package com.blazemeter.jmeter.correlation.core.suggestions.method;

import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.DynamicElement;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractionSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ReplacementSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ResponseAnalyzer;
import com.blazemeter.jmeter.correlation.core.automatic.Sources;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.StructureType;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.LocationType;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.method.Extractor;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.method.ExtractorFactory;
import com.blazemeter.jmeter.correlation.core.automatic.replacement.method.ReplacementContext;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.suggestions.context.ComparisonContext;
import com.blazemeter.jmeter.correlation.core.suggestions.context.CorrelationContext;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ComparisonMethod class implements the CorrelationMethod interface and provides methods for
 * generating correlation suggestions by comparing the parameters of the requests and responses of
 * the recording and replaying.
 */
public class ComparisonMethod implements CorrelationMethod {

  private static final Logger LOG = LoggerFactory.getLogger(ComparisonMethod.class);
  private final Function<SampleResult, String> getLabel = SampleResult::getSampleLabel;
  private final Function<SampleResult, String> resultQueryString
      = (sampleResult) -> ((HTTPSampleResult) sampleResult).getQueryString();
  private final Map<String, String> valueToReferenceName = new HashMap<>();
  private final List<CorrelationSuggestion> suggestions = new ArrayList<>();
  private final List<CorrelationSuggestion> orphanSuggestions = new ArrayList<>();
  private List<SampleResult> results;
  private ComparisonContext context;
  private final HashMap<LocationType, StructureType> structureTypeCache =
      new HashMap<>();

  public ComparisonMethod() {
  }

  /**
   * This method checks if a CorrelationSuggestion has orphan elements. A CorrelationSuggestion has
   * orphan elements if it has no extraction suggestions or no replacement suggestions. Extraction
   * suggestions are used to extract values from responses, while replacement suggestions are used
   * to replace values in requests. If a CorrelationSuggestion has orphan elements, it means that it
   * is incomplete and cannot be used for correlation.
   *
   * @param suggestion the CorrelationSuggestion to check for orphan elements.
   * @return true if the CorrelationSuggestion has orphan elements, false otherwise.
   */
  private static boolean hasOrphans(CorrelationSuggestion suggestion) {
    return suggestion.getExtractionSuggestions().isEmpty()
        || suggestion.getReplacementSuggestions().isEmpty();
  }

  /**
   * This method generates an ExtractionSuggestion for a given appearance in a SampleResult. An
   * ExtractionSuggestion represents a suggestion for extracting a value from a response. The method
   * first creates a new ExtractionSuggestion with the given CorrelationExtractor and SampleResult.
   * Then, it sets the source of the ExtractionSuggestion to the name of the structure type. It also
   * sets the value and name of the ExtractionSuggestion to the value and name of the appearance.
   * Finally, it returns the populated ExtractionSuggestion.
   *
   * @param result        the SampleResult to use for generating the ExtractionSuggestion.
   * @param appearance    the Appearances to use for setting the value and name of the
   *                      ExtractionSuggestion.
   * @param extractor     the CorrelationExtractor to use for creating the ExtractionSuggestion.
   * @param structureType the StructureType to use for setting the source of the
   *                      ExtractionSuggestion.
   * @param name          the name to set for the ExtractionSuggestion.
   * @return the populated ExtractionSuggestion.
   */
  private static ExtractionSuggestion generateCandidateExtractor(SampleResult result,
                                                                 Appearances appearance,
                                                                 CorrelationExtractor extractor,
                                                                 StructureType structureType,
                                                                 String name) {
    ExtractionSuggestion suggestion = new ExtractionSuggestion(extractor, result);
    suggestion.setSource(structureType.name());
    suggestion.setValue(appearance.getValue());
    suggestion.setName(name);
    return suggestion;
  }

  /**
   * This method generates correlation suggestions by comparing the parameters of the requests and
   * responses of the recording and replaying. It first checks if the context is an instance of
   * ComparisonContext. If it is not, it logs an error and returns an empty list. Then, it sets the
   * context and results fields and retrieves the recording and replay maps from the context. If
   * either the recording or replay map is empty, it logs a warning and returns an empty list.
   * Otherwise, it creates a new DynamicElementHandler and generates a list of dynamic elements by
   * comparing the replay and recording maps. It then loads the dynamic elements into the
   * suggestions list and returns it.
   *
   * @param context the CorrelationContext to use for generating suggestions.
   * @return a list of correlation suggestions.
   */
  @Override
  public List<CorrelationSuggestion> generateSuggestions(CorrelationContext context) {
    if (!(context instanceof ComparisonContext)) {
      LOG.error("ComparisonContext expected, found {}. Returning empty suggestions.",
          context.getClass());
      return new ArrayList<>();
    }
    this.context = (ComparisonContext) context;
    this.results = this.context.getRecordingSampleResults(); // Load the results once

    Map<String, List<Appearances>> recordingMap = this.context.getRecordingMap();
    Map<String, List<Appearances>> replayMap = this.context.getReplayMap();

    if (recordingMap.isEmpty() || replayMap.isEmpty()) {
      LOG.warn("Empty recording or replay map. Returning empty suggestions.");
      return new ArrayList<>();
    }
    DynamicElementHandler handler = new DynamicElementHandler(this.context);
    List<DynamicElement> dynamicElements = handler.getDynamicElements(recordingMap, replayMap);
    dynamicElements.forEach(this::loadFromDynamicElements);
    return suggestions;
  }

  /**
   * This method loads dynamic elements into the suggestions list. It first creates a new
   * CorrelationSuggestion from the replay candidate. Then, it populates the suggestion with the
   * replay candidate's data. If the suggestion has orphan elements (i.e., elements that are not
   * matched in both the recording and replaying), it adds the suggestion to the orphanSuggestions
   * list and returns. Otherwise, it adds the suggestion to the suggestions list.
   *
   * @param replayCandidate the DynamicElement to load into the suggestions list.
   */
  private void loadFromDynamicElements(DynamicElement replayCandidate) {
    CorrelationSuggestion suggestion = new CorrelationSuggestion(replayCandidate.getName(),
        appearancesToString(replayCandidate.getOriginalAppearance()),
        appearancesToString(replayCandidate.getOtherAppearance()));

    populateSuggestion(replayCandidate, suggestion);
    if (hasOrphans(suggestion)) {
      orphanSuggestions.add(suggestion);
      return;
    }
    suggestions.add(suggestion);
  }

  /**
   * This method populates a CorrelationSuggestion with extraction and replacement suggestions. It
   * first adds multivalued extraction suggestions to the CorrelationSuggestion by comparing the
   * element's appearances with the results and the valueToReferenceName map. Then, it adds
   * multivalued replacement suggestions to the CorrelationSuggestion in the same way. Finally, it
   * returns the populated CorrelationSuggestion.
   *
   * @param element    the DynamicElement to use for generating the extraction and replacement
   *                   suggestions.
   * @param suggestion the CorrelationSuggestion to populate with the generated suggestions.
   * @return the populated CorrelationSuggestion.
   */
  private CorrelationSuggestion populateSuggestion(DynamicElement element,
                                                   CorrelationSuggestion suggestion) {
    addMultivaluedExtractor(element, suggestion, results, valueToReferenceName);
    addMultivaluedReplacement(element, suggestion, valueToReferenceName);
    return suggestion;
  }

  /**
   * This method adds multivalued extraction suggestions to a CorrelationSuggestion. It iterates
   * over the results and for each result, it adds extraction suggestions for both the original and
   * other appearances of the element. The extraction suggestions are added by comparing the
   * element's appearances with the results and the valueToReferenceName map.
   *
   * @param element              the DynamicElement to use for generating the extraction
   *                             suggestions.
   * @param suggestion           the CorrelationSuggestion to add the extraction suggestions to.
   * @param results              a list of SampleResults to use for generating the extraction
   *                             suggestions.
   * @param valueToReferenceName a map of values to reference names to use for generating the
   *                             extraction suggestions.
   */
  private void addMultivaluedExtractor(DynamicElement element,
                                       CorrelationSuggestion suggestion, List<SampleResult> results,
                                       Map<String, String> valueToReferenceName) {

    for (SampleResult result : results) {
      //We use both the "original" and the "other" appearances since the map can come from either
      //the original recorder or from the failing replay
      List<Appearances> appearancesList = new ArrayList<>(element.getOriginalAppearance());
      appearancesList.addAll(element.getOtherAppearance());
      addExtractorSuggestions(valueToReferenceName, suggestion, result, appearancesList);
    }
  }

  /**
   * This method adds extraction suggestions to a CorrelationSuggestion. It iterates over the
   * appearances and for each appearance, it checks if the number of appearances exceeds the maximum
   * allowed. If it does, it logs a warning and returns. If the sampler that uses the value is
   * reached or if the value is in use, it continues to the next appearance. It then creates a
   * ResponseAnalyzer and identifies the argument location and structure type. If the location type
   * is unknown, it logs a debug message and continues to the next appearance. It then gets the
   * correlation extractors for the location type and structure type. If there are no extractors, it
   * continues to the next appearance. For each extractor, it generates an extraction suggestion and
   * if the suggestion is not repeated, it adds it to the CorrelationSuggestion. It also adds the
   * result to the appearances of the CorrelationSuggestion and maps the appearance value to the
   * extraction parameter name if it is not already mapped.
   *
   * @param valueToReferenceName a map of values to reference names.
   * @param suggestion           the CorrelationSuggestion to add the extraction suggestions to.
   * @param result               the SampleResult to use for generating the extraction suggestions.
   * @param appearances          a list of Appearances to use for generating the extraction
   *                             suggestions.
   */
  private void addExtractorSuggestions(Map<String, String> valueToReferenceName,
                                       CorrelationSuggestion suggestion, SampleResult result,
                                       List<Appearances> appearances) {
    structureTypeCache.clear();
    // Flowing fields declared beforehand for performance proposes
    StructureType structureType;
    ExtractorFactory ef = new ExtractorFactory(context.getConfiguration());
    HashMap<String, Extractor> extractorCache = new HashMap<>();
    ResponseAnalyzer analyzer = new ResponseAnalyzer();
    String name;
    Extractor extractor;

    for (Appearances appearance : appearances) {
      if (!Sources.isRequestSource(appearance.getSource())) {
        continue;
      }
      name = suggestion.getParamName();
      if (suggestion.getAppearances().size() > getConfiguration().getMaxNumberOfAppearances()
          && getConfiguration().getMaxNumberOfAppearances() != -1) {
        LOG.warn("Too many appearances for element  '{}'. Please review the total appearances.",
            name);
        return;
      }

      // If we reach the sampler that uses the value, we don't need to extract it.
      if (reachedUsageSampler(result, appearance.getList())
          || valueInUse(result, appearance.getValue())) {
        continue;
      }

      LocationType locationType = analyzer.identifyArgumentLocation(result, appearance.getValue());
      if (locationType == LocationType.UNKNOWN) {
        // "Couldn't associate a location for the param in the responses.
        // Skipping this value.
        continue;
      }
      structureType = getStructureType(result, structureTypeCache, locationType, analyzer);
      extractor = getExtractor(locationType, structureType, extractorCache, ef);
      List<CorrelationExtractor<?>> extractors = extractor
          .getCorrelationExtractors(result, appearance.getValue(), name);

      if (extractors == null || extractors.isEmpty()) {
        continue;
      }

      for (CorrelationExtractor<?> correlationExtractor : extractors) {
        ExtractionSuggestion extractionSuggestion =
            generateCandidateExtractor(result, appearance, correlationExtractor, structureType,
                name);
        if (isRepeated(suggestion, extractionSuggestion)) {
          continue;
        }

        suggestion.addExtractionSuggestion(extractionSuggestion);
        suggestion.addAppearances(result);
        valueToReferenceName.putIfAbsent(appearance.getValue(),
            suggestion.getExtractionParamName());
      }
    }
  }

  private static Extractor getExtractor(LocationType locationType, StructureType structureType,
      HashMap<String, Extractor> extractorCache, ExtractorFactory ef) {
    String extractorKey;
    Extractor extractor;
    extractorKey = locationType + ":" + structureType;
    if (extractorCache.containsKey(extractorKey)) {
      extractor = extractorCache.get(extractorKey);
    } else {
      extractor = ef.getExtractor(locationType, structureType);
      extractorCache.put(extractorKey, extractor);
    }
    return extractor;
  }

  private static StructureType getStructureType(SampleResult result,
      HashMap<LocationType, StructureType> structureTypeCache, LocationType locationType,
      ResponseAnalyzer analyzer) {
    StructureType structureType;
    if (structureTypeCache.containsKey(locationType)) {
      structureType = structureTypeCache.get(locationType);
    } else {
      structureType = analyzer.identifyStructureType(result, locationType);
      structureTypeCache.put(locationType, structureType);
    }
    return structureType;
  }

  /**
   * This method converts a list of Appearances into a string representation. It first creates a map
   * of appearance values to their counts. Then, it converts the map into a string where each entry
   * is in the format "value (count)". The entries are separated by commas.
   *
   * @param appearances a list of Appearances to convert into a string.
   * @return a string representation of the list of Appearances.
   */
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

  /**
   * This method checks if a given value is used in the query string of a SampleResult. It first
   * retrieves the query string of the SampleResult. Then, it checks if the query string contains
   * the encoded or raw value. If the query string contains either the encoded or raw value, it
   * returns true. Otherwise, it returns false.
   *
   * @param result the SampleResult to check for the value.
   * @param value  the value to check for in the query string.
   * @return true if the query string contains the encoded or raw value, false otherwise.
   */
  private boolean valueInUse(SampleResult result, String value) {
    String queryString = this.resultQueryString.apply(result);
    return queryString.contains(value);
  }

  /**
   * This method checks if the current SampleResult is the one that uses the value of the dynamic
   * element. It does this by comparing the label of the SampleResult with the names of the
   * TestElements in the appearances list. If the label of the SampleResult is found in the names of
   * the TestElements, it returns true. Otherwise, it returns false.
   *
   * @param result      the SampleResult to check for usage of the dynamic element.
   * @param appearances a list of TestElements that use the dynamic element.
   * @return true if the SampleResult uses the dynamic element, false otherwise.
   */
  private boolean reachedUsageSampler(SampleResult result, List<TestElement> appearances) {
    return appearances.stream()
        .map(TestElement::getName)
        .collect(Collectors.toList())
        .contains(this.getLabel.apply(result));
  }

  /**
   * This method checks if a given replacement suggestion is already present in the list of
   * replacement suggestions of a CorrelationSuggestion. It iterates over the existing replacement
   * suggestions and compares each one to the given replacement suggestion. If a match is found, it
   * sets the repeated flag to true and breaks the loop. Finally, it returns the value of the
   * repeated flag.
   *
   * @param suggestion            the CorrelationSuggestion containing the list of replacement
   *                              suggestions to check.
   * @param replacementSuggestion the replacement suggestion to check for in the list.
   * @return true if the replacement suggestion is already present in the list, false otherwise.
   */
  private boolean isRepeated(CorrelationSuggestion suggestion,
                             CorrelationReplacement<?> replacementSuggestion) {
    return suggestion.getExtractionSuggestionsString().contains(replacementSuggestion.toString());
  }

  /**
   * This method checks if a given extraction suggestion is already present in the list of
   * extraction suggestions of a CorrelationSuggestion. It iterates over the existing extraction
   * suggestions and compares each one to the given extraction suggestion. If a match is found, it
   * sets the repeated flag to true and breaks the loop. Finally, it returns the value of the
   * repeated flag.
   *
   * @param suggestion           the CorrelationSuggestion containing the list of extraction
   *                             suggestions to check.
   * @param extractionSuggestion the extraction suggestion to check for in the list.
   * @return true if the extraction suggestion is already present in the list, false otherwise.
   */
  private boolean isRepeated(CorrelationSuggestion suggestion,
                             ExtractionSuggestion extractionSuggestion) {
    return suggestion.getExtractionSuggestionsString().contains(extractionSuggestion.toString());
  }

  /**
   * This method adds multivalued replacement suggestions to a CorrelationSuggestion. It first
   * retrieves the original and other appearances from the DynamicElement. The original and other
   * appearances are used because the element could be generated from a recording (original) or a
   * replay (other). Then, it adds replacement suggestions to the CorrelationSuggestion for both the
   * original and other appearances. The replacement suggestions are added by comparing the
   * appearances with the valueToReferenceName map.
   *
   * @param element              the DynamicElement to use for generating the replacement
   *                             suggestions.
   * @param suggestion           the CorrelationSuggestion to add the replacement suggestions to.
   * @param valueToReferenceName a map of values to reference names to use for generating the
   *                             replacement suggestions.
   */
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

  /**
   * This method adds replacement suggestions to a CorrelationSuggestion. It iterates over the
   * appearances and for each appearance, it checks if the reference name is null or if the source
   * contains the response. If either condition is true, it continues to the next appearance. It
   * then iterates over the list of TestElements in the appearance and for each TestElement, it
   * generates a replacement suggestion. If the replacement suggestion is repeated, it continues to
   * the next TestElement. Otherwise, it creates a new ReplacementSuggestion, sets its source,
   * value, and name, and adds it to the CorrelationSuggestion. It also adds the TestElement to the
   * usages of the CorrelationSuggestion.
   *
   * @param suggestion           the CorrelationSuggestion to add the replacement suggestions to.
   * @param valueToReferenceName a map of values to reference names.
   * @param originalAppearances  a list of Appearances to use for generating the replacement
   *                             suggestions.
   */
  private void addReplacementSuggestions(CorrelationSuggestion suggestion,
                                         Map<String, String> valueToReferenceName,
                                         List<Appearances> originalAppearances) {
    String name = suggestion.getParamName();
    for (Appearances appearance : originalAppearances) {
      String referenceName = valueToReferenceName.get(appearance.getValue());
      if (referenceName == null) {
        continue;
      }
      String source = appearance.getSource();
      if (source.contains(Sources.RESPONSE) || source.contains(Sources.RESPONSE_BODY_JSON_NUMERIC)
          || source.contains(Sources.RESPONSE_BODY_JSON)) {
        continue;
      }
      for (TestElement usage : appearance.getList()) {
        CorrelationReplacement<?> replacement = ReplacementContext.getStrategy(source)
            .generateReplacement(usage, appearance, referenceName);
        if (replacement == null || isRepeated(suggestion, replacement)) {
          continue;
        }
        ReplacementSuggestion replacementSug = new ReplacementSuggestion(replacement, usage);
        replacementSug.setSource(source);
        replacementSug.setValue(appearance.getValue());
        replacementSug.setName(name);
        suggestion.addReplacementSuggestion(replacementSug);
        suggestion.addUsage(usage);
      }
    }
  }

  /**
   * This method retrieves the Configuration object from the current ComparisonContext. The
   * Configuration object contains the settings and parameters used for generating correlation
   * suggestions.
   *
   * @return the Configuration object from the current ComparisonContext.
   */
  private Configuration getConfiguration() {
    return this.context.getConfiguration();
  }

  /**
   * This method sets the ComparisonContext for this instance. The ComparisonContext contains the
   * recording and replay maps used for generating correlation suggestions.
   *
   * @param context the ComparisonContext to set.
   */
  @VisibleForTesting
  public void setContext(ComparisonContext context) {
    this.context = context;
  }

  @Override
  public void applySuggestions(List<CorrelationSuggestion> suggestions) {
    // Do nothing. Suggestions are applied using the same mechanism as the AnalysisMethod.
  }
}
