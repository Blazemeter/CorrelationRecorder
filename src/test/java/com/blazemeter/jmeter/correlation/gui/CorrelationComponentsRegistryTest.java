package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.InvalidRulePartElementException;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.JsonCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.JsonCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CorrelationComponentsRegistryTest {

  private static final RegexCorrelationExtractor<?> REGEX_EXTRACTOR =
      new RegexCorrelationExtractor<>();
  private static final RegexCorrelationReplacement<?> REGEX_REPLACEMENT =
      new RegexCorrelationReplacement<>();
  private static final JsonCorrelationReplacement<?> JSON_REPLACEMENT =
      new JsonCorrelationReplacement<>();

  private CorrelationComponentsRegistry registry;

  @Before
  public void setup() {
    registry = CorrelationComponentsRegistry.getNewInstance();
    registry.reset();
  }

  @Test
  public void shouldNotReturnErrorsWhenComponentStringValid() {
    registry.updateActiveComponents(REGEX_EXTRACTOR.getClass().getCanonicalName(),
        new ArrayList<>());
  }

  @Test
  public void shouldNotAddReplacementWhenAddComponentWithRepeatedComponent() {
    List<CorrelationRulePartTestElement<?>> initialReplacements = registry
        .buildActiveReplacementRulePart();
    registry.updateActiveComponents(REGEX_EXTRACTOR.getClass().getCanonicalName(),
        new ArrayList<>());
    assertThat(initialReplacements.toString()).isEqualTo(
        (registry.buildActiveReplacementRulePart()).toString());
  }

  @Test
  public void shouldReturnTrueWhenIsAllowedWithValidExtractor() {
    assertThat(registry.isExtractorActive(REGEX_EXTRACTOR)).isTrue();
  }

  @Test
  public void shouldReturnTrueWhenIsAllowedWithValidReplacement() {
    assertThat(registry.isReplacementActive(REGEX_REPLACEMENT)).isTrue();
  }

  @Test
  public void shouldGetNullWhenGetCorrelationExtractorWithNullClass()
      throws InvalidRulePartElementException {
    assertThat(registry
        .getCorrelationExtractor(null) == null).isTrue();
  }

  @Test
  public void shouldGetExtractorWhenGetCorrelationExtractorWithAllowedExtractor()
      throws InvalidRulePartElementException {
    assertThat(REGEX_EXTRACTOR).isEqualTo(registry.getCorrelationExtractor(
        (Class<? extends CorrelationExtractor<?>>) REGEX_EXTRACTOR.getClass()));
  }

  @Test
  public void shouldGetNullWhenGetCorrelationReplacementWithNullClass()
      throws InvalidRulePartElementException {
    assertThat(registry.getCorrelationReplacement(null) == null).isTrue();
  }


  @Test
  public void shouldGetReplacementWhenGetCorrelationReplacementWithAllowedReplacement()
      throws InvalidRulePartElementException {
    assertThat(REGEX_REPLACEMENT).isEqualTo(registry
        .getCorrelationReplacement(
            (Class<? extends CorrelationReplacement<?>>) REGEX_REPLACEMENT.getClass()));
  }

  @Test
  public void shouldRulePartWhenGetCorrelationRulePartTestElement() {
    assertThat(new RegexCorrelationExtractor<>()).isEqualTo(registry
        .buildRulePartFromClassName(RegexCorrelationExtractor.class.getCanonicalName()));
  }

  @Test
  public void shouldGetDefaultAllowedExtractorsWhenGetAllowedExtractors() {
    List<CorrelationRulePartTestElement<?>> activeExtractor =
        registry.buildActiveExtractorRulePart();
    assertThat(new HashSet<>(activeExtractor)).isEqualTo(new HashSet<>(
        Arrays.asList(CorrelationComponentsRegistry.NONE_EXTRACTOR,
            new RegexCorrelationExtractor<>(),
            new JsonCorrelationExtractor<>(),
            CorrelationComponentsRegistry.MORE_EXTRACTOR)));
  }

  @Test
  public void shouldGetDefaultAllowedReplacementsWhenGetAllowedReplacements() {
    List<CorrelationRulePartTestElement<?>> expectedDefaultAllowedReplacements = Arrays
        .asList(CorrelationComponentsRegistry.NONE_REPLACEMENT, REGEX_REPLACEMENT,
            JSON_REPLACEMENT, CorrelationComponentsRegistry.MORE_REPLACEMENT);
    assertThat(new HashSet<>(expectedDefaultAllowedReplacements).toString()).isEqualTo(
        new HashSet<>(registry.buildActiveReplacementRulePart()).toString());
  }

}
