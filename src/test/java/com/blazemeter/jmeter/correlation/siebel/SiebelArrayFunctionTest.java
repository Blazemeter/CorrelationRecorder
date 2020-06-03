package com.blazemeter.jmeter.correlation.siebel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SiebelArrayFunctionTest {

  private static final String STRING_TO_SPLIT_STAR_ARRAY = "8*testUser12*testPassword6*VRId-0";
  private static final String EXPECTED_VAR_ONE = "testUser";
  private static final String EXPECTED_VAR_TWO = "testPassword";
  private static final String EXPECTED_VAR_THREE = "VRId-0";
  private static final String EXPECTED_NUMBER_OF_MATCHES = "3";
  private static final String STRING_TO_SPLIT_NOT_STAR_ARRAY = "This is not a Star Array String";
  private static final String VAR_NAME_PREFIX = "testVariable";
  private static final JMeterVariables EMPTY_VARS = new JMeterVariables();
  private static final String PARAM_NAME_ONE = "s_1_2_20_1";
  private static final String PARAM_VALUE_ONE = "3 CommmmT";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private JMeterVariables vars = new JMeterVariables();
  private HTTPSampler sampler;
  private SampleResult sampleResult;

  @Before
  public void setup() {
    sampler = new HTTPSampler();
    sampler.setMethod("GET");
    sampler.setPath("/" + PARAM_NAME_ONE + "=" + PARAM_VALUE_ONE + "&Test_Path=1");
    sampleResult = new SampleResult();
    vars = new JMeterVariables();
  }

  @Test
  public void shouldCreateTheExpectedJMeterVariableVarOne() {
    SiebelArrayFunction.split(STRING_TO_SPLIT_STAR_ARRAY, VAR_NAME_PREFIX, vars);
    assertThat(vars.get("testVariable_1")).isEqualTo(EXPECTED_VAR_ONE);
  }

  @Test
  public void shouldCreateTheExpectedJMeterVariableVarTwo() {
    SiebelArrayFunction.split(STRING_TO_SPLIT_STAR_ARRAY, VAR_NAME_PREFIX, vars);
    assertThat(vars.get("testVariable_2")).isEqualTo(EXPECTED_VAR_TWO);
  }

  @Test
  public void shouldCreateTheExpectedJMeterVariableVarThree() {
    SiebelArrayFunction.split(STRING_TO_SPLIT_STAR_ARRAY, VAR_NAME_PREFIX, vars);
    assertThat(vars.get("testVariable_3")).isEqualTo(EXPECTED_VAR_THREE);
  }

  @Test
  public void shouldCreateTheExpectedJMeterVariableWhichAssignTheNumberOfVariables() {
    SiebelArrayFunction.split(STRING_TO_SPLIT_STAR_ARRAY, VAR_NAME_PREFIX, vars);
    assertThat(vars.get("testVariable_n")).isEqualTo(EXPECTED_NUMBER_OF_MATCHES);
  }

  @Test
  public void shouldThrowExpectedExceptionWhenInputStringIsNotAStarArrayString() {
    expectedException.expect(IllegalArgumentException.class);
    SiebelArrayFunction.split(STRING_TO_SPLIT_NOT_STAR_ARRAY, VAR_NAME_PREFIX, vars);
  }

  @Test
  public void shouldThrowExpectedExceptionMessageWhenInputStringIsNotAStarArrayString() {
    expectedException.expectMessage("Given string does not comply star array format.");
    SiebelArrayFunction.split(STRING_TO_SPLIT_NOT_STAR_ARRAY, VAR_NAME_PREFIX, vars);
  }

  @Test
  public void shouldThrowExpectedExceptionWhenInputStringIsNull() {
    expectedException.expect(IllegalArgumentException.class);
    SiebelArrayFunction.split(null, VAR_NAME_PREFIX, vars);
  }

  @Test
  public void shouldThrowExpectedExceptionMessageWhenInputStringIsNull() {
    expectedException.expectMessage("Input string cannot be null.");
    SiebelArrayFunction.split(null, VAR_NAME_PREFIX, vars);
  }

  @Test
  public void validateWhetherNoJMeterVariablesAreCreatedWhenInputStringIsNotAStarArrayString() {
    try {
      SiebelArrayFunction.split(STRING_TO_SPLIT_NOT_STAR_ARRAY, VAR_NAME_PREFIX, vars);
      fail("Expected IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {
      assertThat(vars.entrySet()).isEqualTo(EMPTY_VARS.entrySet());
    }
  }

  @Test
  public void validateWhetherNoJMeterVariablesAreCreatedWhenInputStringIsNull() {
    try {
      SiebelArrayFunction.split(null, VAR_NAME_PREFIX, vars);
      fail("Expected IllegalArgumentException not thrown");
    } catch (IllegalArgumentException e) {
      assertThat(vars.entrySet()).isEqualTo(EMPTY_VARS.entrySet());
    }
  }

  @Test
  public void shouldReturnInputStringToSplit() throws InvalidVariableException {
    JMeterContextService.getContext().setVariables(vars);
    Collection<CompoundVariable> parameters = new ArrayList<>();
    CompoundVariable stringToSplit = new CompoundVariable(STRING_TO_SPLIT_STAR_ARRAY);
    parameters.add(stringToSplit);
    CompoundVariable varNamePrefix = new CompoundVariable(VAR_NAME_PREFIX);
    parameters.add(varNamePrefix);
    SiebelArrayFunction siebelArrayFunction = new SiebelArrayFunction();
    siebelArrayFunction.setParameters(parameters);
    String returnedStringToSplit = siebelArrayFunction.execute(sampleResult, sampler);
    assertThat(returnedStringToSplit).isEqualTo(STRING_TO_SPLIT_STAR_ARRAY);
  }

  @Test
  public void shouldReturnEmptyStringIfInputStringToSplitIsNull() throws InvalidVariableException {
    JMeterContextService.getContext().setVariables(vars);
    Collection<CompoundVariable> parameters = new ArrayList<>();
    CompoundVariable stringToSplit = new CompoundVariable(null);
    parameters.add(stringToSplit);
    CompoundVariable varNamePrefix = new CompoundVariable(VAR_NAME_PREFIX);
    parameters.add(varNamePrefix);
    SiebelArrayFunction siebelArrayFunction = new SiebelArrayFunction();
    siebelArrayFunction.setParameters(parameters);
    String returnedStringToSplit = siebelArrayFunction.execute(sampleResult, sampler);
    assertThat(returnedStringToSplit).isEqualTo("");
  }
}
