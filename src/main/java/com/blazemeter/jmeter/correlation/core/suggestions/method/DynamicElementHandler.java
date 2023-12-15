package com.blazemeter.jmeter.correlation.core.suggestions.method;

import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.DynamicElement;
import com.blazemeter.jmeter.correlation.core.suggestions.context.ComparisonContext;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DynamicElementHandler class is responsible for generating a list of DynamicElement
 * instances by comparing two maps of parameters.
 * It provides methods to check if the values of Appearances in both the original and replay
 * lists are parametrized, sort a list of Appearances and return a string representation
 * of the sorted list, generate a list of DynamicElement by comparing two maps of parameters,
 * generate a list of base dynamic elements by comparing two maps of parameters, locate
 * different arguments between two maps of parameters, check if the number of appearances of
 * a parameter in both the original and replay recordings exceeds the maximum allowed
 * appearances defined in the configuration, add dynamic elements to the differences
 * list based on a given condition, check if the parameter is in the list of manually
 * requested parameters, and check if two lists of Appearances are equal after sorting.
 */
public class DynamicElementHandler {
  private static final Logger LOG = LoggerFactory.getLogger(DynamicElementHandler.class);
  private final ComparisonContext context;

  public DynamicElementHandler(ComparisonContext context) {
    this.context = context;
  }

  /**
   * This method checks if the values of Appearances in both the original and replay lists
   * are parametrized.
   * A parametrized value is one that contains both "${" and "}".
   * It first filters the original and replay lists to only include Appearances with parametrized
   * values.
   * Then, it compares the string representations of the filtered lists.
   * If the string representations are not equal, it returns true. Otherwise, it returns false.
   *
   * @param originalAppearances a list of Appearances from the original recording.
   * @param replayAppearances a list of Appearances from the replay recording.
   * @return true if the string representations of the filtered lists are not equal,
   * false otherwise.
   */
  private static boolean isParametrizedArgument(List<Appearances> originalAppearances,
                                                List<Appearances> replayAppearances) {

    List<Appearances> originalParametrized = originalAppearances.stream()
        .filter(DynamicElementHandler::isParametrizedValue)
        .collect(Collectors.toList());

    List<Appearances> replayParametrized = replayAppearances.stream()
        .filter(DynamicElementHandler::isParametrizedValue)
        .collect(Collectors.toList());

    return !originalParametrized.toString().equals(replayParametrized.toString());
  }


  /**
   * This method checks if the value of an Appearance is parametrized.
   * A parametrized value is one that contains both "${" and "}".
   * Examples of parametrized values:
   * - ${__time()}
   * - ${__RandomString(10,abcdefghijklmn)}
   * - ${variable}
   *
   * @param appearance the Appearance to check.
   * @return true if the value of the Appearance is parametrized, false otherwise.
   */
  @VisibleForTesting
  public static boolean isParametrizedValue(Appearances appearance) {
    return appearance.getValue().contains("${")
        && appearance.getValue().contains("}");
  }

  /**
   * This method sorts a list of Appearances and returns a string representation of the sorted list.
   * It first maps each Appearance to a string in the format "  source:value".
   * Then, it sorts the strings and joins them with a newline character.
   *
   * @param appearances a list of Appearances to sort.
   * @return a string representation of the sorted list of Appearances.
   */
  @VisibleForTesting
  public static String getSortedAppearances(List<Appearances> appearances) {
    return appearances.stream()
        .map(app -> "  " + app.getSource() + ":" + app.getValue())
        .sorted().collect(Collectors.joining("\n"));
  }

  /**
   * Generates a list of {@link DynamicElement} by comparing two maps of parameters.
   * This method expects that the map comes from either a JMX recording or
   * a JTL recording/replay files and that the JTL files have both successful
   * and failed requests.
   * If the JTL file used for the <b>replayMap</b> only has failed requests, consider using
   * instead.
   *
   * @param originalMap the map from the JMX recording file.
   * @param replayMap   the map from the replay JTL file.
   * @return a list of elements that are dynamic between the two maps.
   */
  @VisibleForTesting
  public List<DynamicElement> getDynamicElements(Map<String, List<Appearances>> originalMap,
                                                 Map<String, List<Appearances>> replayMap) {

    Map<String, List<Appearances>> manuallyRequested = new HashMap<>(originalMap);
    Map<String, List<Appearances>> orphanReplay = new HashMap<>(replayMap);
    Map<String, List<Appearances>> apparentlyEquals = new HashMap<>();

    List<DynamicElement> differences =
        getBaseDynamicElements(originalMap, replayMap, manuallyRequested, orphanReplay,
            apparentlyEquals);

    Configuration conf = context.getConfiguration();
    addAppearancesToDynamicElementsByCondition(manuallyRequested, differences,
        conf.shouldAllowManuallyRequestValues(), "Original Trace");
    addAppearancesToDynamicElementsByCondition(orphanReplay, differences,
        conf.shouldAllowOrphanAppearances(), "Replay Trace");
    addAppearancesToDynamicElementsByCondition(apparentlyEquals, differences,
        conf.shouldAllowApparentlyEqualsValues(), "Apparently equal");

    return differences;
  }

  /**
   * This method generates a list of base dynamic elements by comparing two maps of parameters.
   * It iterates over the original map and for each entry, it locates different arguments between
   * the original and replay maps.
   * The differences are added to a list of dynamic elements which is then returned.
   *
   * @param originalMap a map of parameters from the original recording.
   * @param replayMap a map of parameters from the replay recording.
   * @param manuallyRequested a map of parameters that are manually requested.
   * @param orphanReplay a map of parameters that are present in the replay recording but not in
   *                     the original recording.
   * @param apparentlyEquals a map of parameters that are equal in both the original and replay
   *                         recordings after sorting.
   * @return a list of base dynamic elements that are different between the original and replay
   * maps.
   */
  @VisibleForTesting
  public List<DynamicElement> getBaseDynamicElements(
      Map<String, List<Appearances>> originalMap,
      Map<String, List<Appearances>> replayMap,
      Map<String, List<Appearances>> manuallyRequested,
      Map<String, List<Appearances>> orphanReplay,
      Map<String, List<Appearances>> apparentlyEquals) {
    List<DynamicElement> differences = new ArrayList<>();
    originalMap.forEach((referenceName, appearances) ->
        locateDifferentArguments(replayMap, referenceName, appearances,
            differences, manuallyRequested, orphanReplay,
            apparentlyEquals));
    return differences;
  }

  /**
   * This method is responsible for locating different arguments between two maps of parameters.
   * It checks if the parameter is manually requested, if it is present in the replay recording,
   * if it exceeds the maximum allowed appearances, if it is a parametrized argument,
   * and if it is equal after sorting.
   * Based on these checks, it manipulates the differences, manuallyRequested, orphanReplay,
   * and apparentlyEquals lists.
   *
   * @param replayMap         a map of parameters from the replay recording.
   * @param referenceName     the name of the parameter to check.
   * @param appearances       a list of appearances of the parameter in the original recording.
   * @param differences       a list of dynamic elements that are different between the original
   *                          and replay recordings.
   * @param manuallyRequested a map of parameters that are manually requested.
   * @param orphanReplay      a map of parameters that are present in the replay recording but not
   *                          in the original recording.
   * @param apparentlyEquals  a map of parameters that are equal in both the original and replay
   *                          recordings after sorting.
   */
  private void locateDifferentArguments(Map<String, List<Appearances>> replayMap,
                                        String referenceName,
                                        List<Appearances> appearances,
                                        List<DynamicElement> differences,
                                        Map<String, List<Appearances>> manuallyRequested,
                                        Map<String, List<Appearances>> orphanReplay,
                                        Map<String, List<Appearances>> apparentlyEquals) {

    if (isManuallyRequestedParameter(referenceName)) {
      differences.add(new DynamicElement(referenceName, appearances, new ArrayList<>()));
      manuallyRequested.remove(referenceName);
      return;
    }

    List<Appearances> otherAppearances = replayMap.get(referenceName);
    // Check if the parameter is present in the other recording
    if (otherAppearances == null || otherAppearances.isEmpty()) {
      LOG.warn("Parameter {} is not present in the replay recording. "
          + "Ignoring its {} appearances.", referenceName, appearances.size());
      return;
    }
    manuallyRequested.remove(referenceName);
    orphanReplay.remove(referenceName);

    // Skip if one of these conditions is true:
    if (exceedsMaxAllowedAppearances(appearances, otherAppearances)
        || isParametrizedArgument(appearances, otherAppearances)) {
      return;
    }

    if (areEqualsAfterSorting(appearances, otherAppearances)) {
      apparentlyEquals.put(referenceName, appearances);
      return;
    }

    differences.add(new DynamicElement(referenceName, appearances, otherAppearances));
  }

  /**
   * This method checks if the number of appearances of a parameter in both the original and
   * replay recordings exceeds the maximum allowed appearances defined in the configuration.
   * @param appearances a list of appearances of the parameter in the original recording.
   * @param otherAppearances a list of appearances of the parameter in the replay recording.
   * @return true if the number of appearances in either the original or replay recording exceeds
   * the maximum allowed appearances, false otherwise.
   */
  private boolean exceedsMaxAllowedAppearances(List<Appearances> appearances,
                                               List<Appearances> otherAppearances) {
    int maxAppearances = this.context.getConfiguration().getMaxNumberOfAppearances();
    return appearances.size() > maxAppearances || otherAppearances.size() > maxAppearances;
  }

  /**
   * This method adds dynamic elements to the differences list based on a given condition.
   * It first checks if the map is empty or if the condition is false. If either is true, it
   * returns without doing anything.
   * If the debug mode is enabled, it prints a message indicating the number of dynamic
   * elements being added and their source.
   * Finally, it iterates over the map and for each entry, it creates a new DynamicElement
   * with the key as the name, the value as the appearances, and an empty list as the
   * replay appearances, and adds it to the differences list.
   *
   * @param map a map of parameter names to their appearances.
   * @param differences a list of dynamic elements that are different between the original
   *                    and replay recordings.
   * @param condition a boolean condition that determines whether to add the dynamic elements
   *                  to the differences list.
   * @param source a string indicating the source of the dynamic elements.
   */
  private void addAppearancesToDynamicElementsByCondition(Map<String, List<Appearances>> map,
                                                          List<DynamicElement> differences,
                                                          boolean condition, String source) {
    if (map.isEmpty() || !condition) {
      return;
    }

    map.forEach((key, value) -> differences.add(new DynamicElement(key, value, new ArrayList<>())));
  }

  /**
   * Checks if the parameter is in the list of manually requested parameters.
   *
   * @param referenceName the name of the parameter to check.
   * @return true if the parameter is in the list of manually requested parameters, false otherwise.
   */
  private boolean isManuallyRequestedParameter(String referenceName) {
    return context.getConfiguration().getRequestedParameters().contains(referenceName);
  }

  /**
   * This method checks if two lists of Appearances are equal after sorting.
   * It first checks if the sizes of the two lists are equal. If they are not, it returns false.
   * Then, it sorts the Appearances in each list and compares them.
   * If the sorted lists are equal, it returns true. Otherwise, it returns false.
   *
   * @param originalRecording a list of Appearances from the original recording.
   * @param replayTrace a list of Appearances from the replay trace.
   * @return true if the sorted lists are equal, false otherwise.
   */
  private boolean areEqualsAfterSorting(List<Appearances> originalRecording,
                                        List<Appearances> replayTrace) {
    if (originalRecording.size() != replayTrace.size()) {
      return false;
    }

    String originalAppearances = getSortedAppearances(originalRecording);
    String replayAppearances = getSortedAppearances(replayTrace);

    return originalAppearances.equals(replayAppearances);
  }
}
