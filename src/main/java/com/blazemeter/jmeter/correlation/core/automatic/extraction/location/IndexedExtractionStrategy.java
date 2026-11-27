package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import com.blazemeter.jmeter.correlation.core.automatic.replacement.method.ReplacementString;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.samplers.SampleResult;

/**
 * A strategy that searches values in one part of a response (its headers, its cookies, its body).
 *
 * <p>The part is computed once per response. When the values of a search are known in advance,
 * the part is scanned once for all of them and their encoding variants, and each later lookup is a
 * set membership instead of a new scan. The answer is the same as searching the value directly:
 * the first variant, in {@link ReplacementString} order, that the part contains.
 */
public abstract class IndexedExtractionStrategy implements ExtractionStrategy {

  private final Map<SampleResult, String> contents = new IdentityHashMap<>();
  private final Map<SampleResult, Set<String>> foundByResponse = new IdentityHashMap<>();
  private Set<String> candidateValues = Collections.emptySet();
  private MultiPatternFinder finder;

  protected abstract String searchableContent(SampleResult response);

  protected abstract LocationType locationType();

  /**
   * Sets the values the next lookups will ask about, with a finder built for those values and
   * their encoding variants. A value not among them is still searched directly.
   */
  public void setCandidates(Set<String> values, MultiPatternFinder variantsFinder) {
    candidateValues = values;
    finder = variantsFinder;
    foundByResponse.clear();
  }

  @Override
  public Pair<LocationType, String> identifyLocationInResponse(SampleResult response,
      String value) {
    String content = contents.computeIfAbsent(response, this::searchableContent);
    if (finder == null || !candidateValues.contains(value)) {
      return ExtractionStrategy.getLocationAndValue(content, value, locationType());
    }
    Set<String> found = foundByResponse.computeIfAbsent(response, r -> finder.findIn(content));
    for (ReplacementString variant : ReplacementString.values()) {
      String appliedValue = variant.applyFunction(value);
      if (found.contains(appliedValue)) {
        return Pair.of(locationType(), appliedValue);
      }
    }
    return Pair.of(LocationType.UNKNOWN, "");
  }
}
