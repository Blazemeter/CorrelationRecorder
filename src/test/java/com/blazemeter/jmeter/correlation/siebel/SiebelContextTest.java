package com.blazemeter.jmeter.correlation.siebel;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.siebel.SiebelContext.Field;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SiebelContextTest {

  private static final String EXPECTED_PARAM = "s_2_1_4_0";
  private static final String NOT_EXPECTED_PARAM = "s_2_1_9_0";
  private static final String PARAM_WITH_TELEPHONE_TYPE_FIELD = "s_2_1_21_0";
  private static final String PARAM_WITH_STRING_TYPE_FIELD = "s_2_1_24_0";
  private static final String IGNORED_CHARS_REGEX = "[\\()\\- ]";
  private static final String VALID_RESPONSE_DATA_PATH = "src/test/resources/validResponseData.txt";
  private static final String INVALID_RESPONSE_DATA_PATH = "src/test/resources/invalidResponseData.txt";
  private static final SampleResult validResponse = new SampleResult();
  private static final SampleResult invalidResponse = new SampleResult();
  private final SiebelContext siebelContext = new SiebelContext();
  private Map<String, Field> paramRowFields;

  @BeforeClass
  public static void beforeClass() {
    validResponse
        .setResponseData(TestUtils.readFile(VALID_RESPONSE_DATA_PATH, Charset.defaultCharset()));
    invalidResponse
        .setResponseData(TestUtils.readFile(INVALID_RESPONSE_DATA_PATH, Charset.defaultCharset()));
  }

  @Before
  public void setUp() {
    siebelContext.update(validResponse);
    paramRowFields = siebelContext.getParamRowFields();
  }


  @Test
  public void validateWhetherExpectedParamRowFieldsIsPresent() {
    assertThat(paramRowFields.containsKey(EXPECTED_PARAM)).isTrue();
  }

  @Test
  public void validateWhetherNotExpectedParamRowFieldsIsNotPresent() {
    assertThat(paramRowFields.containsKey(NOT_EXPECTED_PARAM)).isFalse();
  }

  @Test
  public void validateWhetherParamsAreAddedIfInputStringMatchesSiebel() {
    assertThat(paramRowFields).isNotEmpty();
  }

  @Test
  public void validateWhetherNoParamsAreAddedIfInputStringDoesNotMatchSiebel() {
    siebelContext.reset();
    siebelContext.update(invalidResponse);
    paramRowFields = siebelContext.getParamRowFields();
    assertThat(paramRowFields).isEmpty();
  }

  @Test
  public void shouldReturnExpectedCharactersWhenFieldTypeIsTelephone() {
    Field field = paramRowFields.get(PARAM_WITH_TELEPHONE_TYPE_FIELD);
    assertThat(IGNORED_CHARS_REGEX).isEqualTo(field.getIgnoredCharsRegex());
  }

  @Test
  public void shouldReturnEmptyStringWhenFieldTypeIsString() {
    Field field = paramRowFields.get(PARAM_WITH_STRING_TYPE_FIELD);
    assertThat("").isEqualTo(field.getIgnoredCharsRegex());
  }
}
