package com.blazemeter.jmeter.correlation.core.templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

/**
 * Templates come from remote repositories, and the "type" of each extractor and replacement is a
 * class name (@JsonTypeInfo(use = Id.CLASS)). Reading one must only create subtypes of
 * CorrelationExtractor and CorrelationReplacement, never an arbitrary class it names.
 */
public class TemplateTypeIdTest {

  private static final String EXTRACTOR = RegexCorrelationExtractor.class.getName();
  private static final String REPLACEMENT = RegexCorrelationReplacement.class.getName();
  private static final String NOT_A_RULE_PART = NotARulePart.class.getName();

  private LocalConfiguration configuration;

  @Before
  public void setUp() {
    configuration = new LocalConfiguration();
    configuration.setTemplateLogParseError(false);
    NotARulePart.instances = 0;
  }

  @Test
  public void shouldReadTheExtractorAndReplacementWhenTheirTypesAreRuleParts()
      throws IOException {
    Template template = configuration.readTemplateFromString(template(EXTRACTOR, REPLACEMENT));

    CorrelationRule rule = template.getGroups().get(0).getRules().get(0);
    assertThat(rule.getCorrelationExtractor()).isInstanceOf(RegexCorrelationExtractor.class);
    assertThat(rule.getCorrelationReplacement()).isInstanceOf(RegexCorrelationReplacement.class);
  }

  @Test
  public void shouldRejectTheTemplateWithoutCreatingTheClassWhenTheExtractorTypeIsNotAnExtractor() {
    assertThatThrownBy(
        () -> configuration.readTemplateFromString(template(NOT_A_RULE_PART, REPLACEMENT)))
        .isInstanceOf(IOException.class)
        .hasStackTraceContaining("Not a subtype");
    assertThat(NotARulePart.instances).isZero();
  }

  @Test
  public void shouldRejectTheTemplateWithoutCreatingTheClassWhenTheReplacementTypeIsNotAReplacement() {
    assertThatThrownBy(
        () -> configuration.readTemplateFromString(template(EXTRACTOR, NOT_A_RULE_PART)))
        .isInstanceOf(IOException.class)
        .hasStackTraceContaining("Not a subtype");
    assertThat(NotARulePart.instances).isZero();
  }

  private static String template(String extractorType, String replacementType) {
    return "{\"id\":\"type-id-test\",\"version\":\"1.0\",\"groups\":[{\"id\":\"group\","
        + "\"enable\":true,\"rules\":[{\"referenceName\":\"token\","
        + "\"correlationExtractor\":{\"type\":\"" + extractorType + "\",\"regex\":\"t=(.*?)&\"},"
        + "\"correlationReplacement\":{\"type\":\"" + replacementType + "\",\"regex\":\"t=(.*)\"}"
        + "}]}]}";
  }

  /**
   * A class a template could name that is neither an extractor nor a replacement.
   */
  public static class NotARulePart {

    private static int instances;

    public String regex;

    public NotARulePart() {
      instances++;
    }
  }
}
