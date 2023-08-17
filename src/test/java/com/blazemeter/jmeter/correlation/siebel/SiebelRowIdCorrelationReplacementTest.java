package com.blazemeter.jmeter.correlation.siebel;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.oro.text.regex.MalformedPatternException;
import org.junit.Before;
import org.junit.Test;

public class SiebelRowIdCorrelationReplacementTest {

  private static final String VARIABLE_NAME = "Siebel_Star_Array41";
  private static final String ROW_ID = "1-639";
  private static final String REGEX_THAT_MATCHES = "SWERowId=([^&\\n]+)";
  private static final String REGEX_THAT_DOES_NOT_MATCH = "SWERowId_Not_Match=([^&\\n]+)";
  private static final String INPUT_STRING = "SWERowId=1-639";

  private JMeterVariables vars;
  private SiebelRowIdCorrelationReplacement siebelRowIdReplacement;
  private final SiebelContext siebelContext = new SiebelContext();

  @Before
  public void setup() {
    vars = new JMeterVariables();
    siebelRowIdReplacement = new SiebelRowIdCorrelationReplacement();
    siebelRowIdReplacement.setContext(siebelContext);
    siebelRowIdReplacement.setVariableName("SWERowID");
  }

  @Test
  public void shouldReplaceInputStringWhenRegexMatches() throws MalformedPatternException {
    siebelContext.addRowVar(ROW_ID, VARIABLE_NAME);
    vars.put(VARIABLE_NAME + "_rowId", ROW_ID);
    String expectedString = "SWERowId=${" + VARIABLE_NAME + "_rowId}";
    String replacedString = siebelRowIdReplacement
        .replaceWithRegex(INPUT_STRING, REGEX_THAT_MATCHES, VARIABLE_NAME, vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }

  @Test
  public void shouldNotReplaceInputStringWhenRegexMatchesButThereIsNoAJMeterVar()
      throws MalformedPatternException {
    siebelContext.addRowVar(ROW_ID, VARIABLE_NAME);
    String expectedString = "SWERowId=" + ROW_ID;
    String replacedString = siebelRowIdReplacement
        .replaceWithRegex(INPUT_STRING, REGEX_THAT_MATCHES, VARIABLE_NAME, vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }

  @Test
  public void shouldNotReplaceInputStringWhenRegexDoesNotMatch() throws MalformedPatternException {
    siebelContext.addRowVar(ROW_ID, VARIABLE_NAME);
    vars.put(VARIABLE_NAME + "_rowId", ROW_ID);
    String expectedString = "SWERowId=" + ROW_ID;
    String replacedString = siebelRowIdReplacement
        .replaceWithRegex(INPUT_STRING, REGEX_THAT_DOES_NOT_MATCH, VARIABLE_NAME, vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }

  @Test
  public void shouldNotReplaceInputStringWhenRegexMatchesButRowVarIsNotInContext()
      throws MalformedPatternException {
    siebelContext.addRowVar("TEST_ROW_ID", VARIABLE_NAME);
    vars.put(VARIABLE_NAME + "_rowId", ROW_ID);
    String expectedString = "SWERowId=" + ROW_ID;
    String replacedString = siebelRowIdReplacement
        .replaceWithRegex(INPUT_STRING, REGEX_THAT_DOES_NOT_MATCH, VARIABLE_NAME, vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }

  @Test
  public void shouldNotReplaceInputStringWhenRegexMatchesButContextDoesNotContainAnyVar()
      throws MalformedPatternException {
    vars.put(VARIABLE_NAME + "_rowId", ROW_ID);
    String expectedString = "SWERowId=" + ROW_ID;
    String replacedString = siebelRowIdReplacement
        .replaceWithRegex(INPUT_STRING, REGEX_THAT_DOES_NOT_MATCH, VARIABLE_NAME, vars);
    assertThat(replacedString).isEqualTo(expectedString);
  }
}
