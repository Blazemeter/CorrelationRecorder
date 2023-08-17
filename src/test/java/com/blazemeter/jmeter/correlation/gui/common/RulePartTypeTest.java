package com.blazemeter.jmeter.correlation.gui.common;

import static org.assertj.core.api.Assertions.assertThat;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowCorrelationExtractor;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowParamsCorrelationReplacement;
import org.junit.Test;

public class RulePartTypeTest {

  @Test
  public void shouldReturnReplacementTypeWhenFromReplacementExtension() {
    SiebelRowParamsCorrelationReplacement replacement = new SiebelRowParamsCorrelationReplacement();
    assertThat(RulePartType.fromComponent(replacement)).isEqualTo(RulePartType.REPLACEMENT);
  }

  @Test
  public void shouldReturnExtractorTypeWhenFromExtractorExtension() {
    SiebelRowCorrelationExtractor extractor = new SiebelRowCorrelationExtractor();
    assertThat(RulePartType.fromComponent(extractor)).isEqualTo(RulePartType.EXTRACTOR);
  }

  @Test
  public void shouldReturnExtractorTypeWhenFromComponentWithNoneExtractor() {
    assertThat(RulePartType.fromComponent(CorrelationComponentsRegistry.NONE_EXTRACTOR))
        .isEqualTo(RulePartType.EXTRACTOR);
  }

  @Test
  public void shouldReturnReplacementTypeWhenFromComponentWithNoneReplacement() {
    assertThat(RulePartType.fromComponent(CorrelationComponentsRegistry.NONE_REPLACEMENT))
        .isEqualTo(RulePartType.REPLACEMENT);
  }

}