package com.blazemeter.jmeter.correlation.core;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;

public class BaseCorrelationContextTest {

  BaseCorrelationContext baseCorrelationContext;

  @Before
  public void setup() {
    baseCorrelationContext = new BaseCorrelationContext();
  }

  @Test
  public void shouldReturnOneWhenGetNextVarNrAndTheVariableIsNotPresent() {
    assertThat(baseCorrelationContext.getNextVariableNr("var")).isEqualTo(1);
  }

  @Test
  public void shouldReturnNextIndexWhenGetNextVarNrAndTheVariableIsPresent() {
    baseCorrelationContext.getNextVariableNr("var");
    assertThat(baseCorrelationContext.getNextVariableNr("var")).isEqualTo(2);
  }

  @Test
  public void shouldReturnZeroWhenGetVariableCountAndTheVariableIsNotPresent() {
    assertThat(baseCorrelationContext.getVariableCount("var")).isEqualTo(0);
  }

  @Test
  public void shouldReturnVarsCountWhenGetVariableCountAndTheVariableIsPresent() {
    baseCorrelationContext.getNextVariableNr("var");
    assertThat(baseCorrelationContext.getVariableCount("var")).isEqualTo(1);
  }

  @Test
  public void shouldClearVarsCountWhenReset() {
    baseCorrelationContext.getNextVariableNr("var");
    baseCorrelationContext.reset();
    assertThat(baseCorrelationContext.getVariableCount("var")).isEqualTo(0);
  }
}
