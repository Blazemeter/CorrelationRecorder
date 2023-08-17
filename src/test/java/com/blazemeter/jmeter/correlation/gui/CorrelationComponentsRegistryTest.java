package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.Assertions.assertThat;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.InvalidRulePartElementException;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.siebel.SiebelContext;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowCorrelationExtractor;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowIdCorrelationReplacement;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowParamsCorrelationReplacement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CorrelationComponentsRegistryTest {

  private static final RegexCorrelationExtractor<?> REGEX_EXTRACTOR =
      new RegexCorrelationExtractor<>();
  private static final RegexCorrelationReplacement<?> REGEX_REPLACEMENT =
      new RegexCorrelationReplacement<>();
  private static final SiebelRowCorrelationExtractor SIEBEL_EXTRACTOR_EXTENSION =
      new SiebelRowCorrelationExtractor();
  private static final SiebelRowIdCorrelationReplacement SIEBEL_REPLACEMENT_EXTENSION =
      new SiebelRowIdCorrelationReplacement();

  private CorrelationComponentsRegistry registry;

  @Before
  public void setup() {
    registry = CorrelationComponentsRegistry.getInstance();
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
    assertThat(initialReplacements).isEqualTo((registry.buildActiveReplacementRulePart()));
  }

  @Test
  public void shouldAddReplacementWhenAddComponent() {
    List<CorrelationRulePartTestElement<?>> initialAllowedReplacements =
        registry.buildActiveReplacementRulePart();
    registry.updateActiveComponents(SiebelRowParamsCorrelationReplacement.class.getCanonicalName(),
        new ArrayList<>());
    assertThat(initialAllowedReplacements).isNotEqualTo(registry
        .buildActiveReplacementRulePart());
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
  public void shouldReturnFalseWhenIsAllowedWithNotAddedReplacement() {
    assertThat(!registry.isReplacementActive(SIEBEL_EXTRACTOR_EXTENSION))
        .isTrue();
  }

  @Test
  public void shouldReturnTrueWhenIsAllowedAfterAddingComponent() {
    registry.updateActiveComponents(SIEBEL_EXTRACTOR_EXTENSION.getClass().getCanonicalName(),
        new ArrayList<>());
    assertThat(registry.isExtractorActive(SIEBEL_EXTRACTOR_EXTENSION))
        .isTrue();
  }

  @Test
  public void shouldGetNullWhenGetCorrelationExtractorWithNullClass()
      throws InvalidRulePartElementException {
    assertThat(registry
        .getCorrelationExtractor(null) == null).isTrue();
  }

  @Test(expected = InvalidRulePartElementException.class)
  public void shouldThrowExceptionWhenGetCorrelationExtractorWithNotActiveExtractor()
      throws InvalidRulePartElementException {
    registry.getCorrelationExtractor(SIEBEL_EXTRACTOR_EXTENSION.getClass());
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

  @Test(expected = InvalidRulePartElementException.class)
  public void shouldThrowExceptionWhenGetCorrelationReplacementWithNotAllowedReplacement()
      throws InvalidRulePartElementException {
    registry.getCorrelationReplacement(SIEBEL_REPLACEMENT_EXTENSION.getClass());
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
  public void shouldAddComponentsWhenUpdateAllowedComponents() {
    registry.updateActiveComponents(SiebelRowParamsCorrelationReplacement.class.getCanonicalName(),
        new ArrayList<>());
    List<CorrelationRulePartTestElement<?>> allowedReplacements = registry
        .buildActiveReplacementRulePart();
    assertThat(allowedReplacements.equals(registry.buildActiveReplacementRulePart()));
  }

  @Test
  public void shouldResetAllowedComponentsWhenUpdateAllowedComponents() {
    List<CorrelationRulePartTestElement<?>> initialActiveReplacements = new ArrayList<>(
        registry.buildActiveReplacementRulePart());
    registry.updateActiveComponents(SiebelRowParamsCorrelationReplacement.class.getCanonicalName(),
        new ArrayList<>());
    assertThat(initialActiveReplacements).isNotEqualTo(registry.buildActiveReplacementRulePart());
  }

  @Test
  public void shouldGetDefaultAllowedExtractorsWhenGetAllowedExtractors() {
    List<CorrelationRulePartTestElement<?>> expectedDefaultAllowedExtractors =
        Arrays.asList(CorrelationComponentsRegistry.NONE_EXTRACTOR,
            new RegexCorrelationExtractor<>(),
            CorrelationComponentsRegistry.MORE_EXTRACTOR);
    assertThat(expectedDefaultAllowedExtractors).isEqualTo(registry.buildActiveExtractorRulePart());
  }

  @Test
  public void shouldGetDefaultAllowedReplacementsWhenGetAllowedReplacements() {
    List<CorrelationRulePartTestElement<?>> expectedDefaultAllowedReplacements = Arrays
        .asList(CorrelationComponentsRegistry.NONE_REPLACEMENT, REGEX_REPLACEMENT,
            CorrelationComponentsRegistry.MORE_REPLACEMENT);
    assertThat(expectedDefaultAllowedReplacements).isEqualTo(
        registry.buildActiveReplacementRulePart());
  }

  @Test
  public void shouldNotGetSiebelExtensionsWhenGetAllowedExtractorsWithAddingBefore() {
    assertThat(registry.buildActiveExtractorRulePart().stream()
        .noneMatch(e -> e.equals(SIEBEL_EXTRACTOR_EXTENSION)))
        .isTrue();
  }

  @Test
  public void shouldGetContextInstanceWhenGetContext() {
    assertThat(new SiebelContext().toString())
        .isEqualTo(registry.getContext(SiebelContext.class).toString());
  }
}
