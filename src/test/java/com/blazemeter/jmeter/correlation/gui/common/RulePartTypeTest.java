package com.blazemeter.jmeter.correlation.gui.common;

import static org.assertj.core.api.Assertions.assertThat;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import org.junit.Test;

public class RulePartTypeTest {


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
