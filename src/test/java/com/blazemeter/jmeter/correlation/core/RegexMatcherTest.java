package com.blazemeter.jmeter.correlation.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class RegexMatcherTest {

  private static final String INPUT_STRING_ONE =
      "Test First Group=123\", Test Second Group=456\"\r\n" +
          "Test First Group=789\", Test Second Group=10\"";
  private static final String INPUT_STRING_TWO = "Test First Group=123\", Test Second Group=456\"";
  private static final String REGEX_MATCHES = "Test First Group=(.*?)\", Test Second Group=(.*?)\"";
  private static final String REGEX_DOES_NOT_MATCH = "Test Regex Does Not Match Group=(.*?)\", Test Second Group=(.*?)\"";
  private static final int REGEX_GROUP = 1;
  private static final String EXPECTED_FIRST_MATCH = "123";
  private static final String EXPECTED_SECOND_MATCH = "789";

  // we need this to avoid nasty logs about pdfbox
  @BeforeClass
  public static void setupClass() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  @Test
  public void findMatchShouldReturnTheExpectedMatchWhenRegexMatches() {
    RegexMatcher regexMatcher = new RegexMatcher(REGEX_MATCHES, REGEX_GROUP);
    assertThat(regexMatcher.findMatch(INPUT_STRING_ONE, 1)).isEqualTo(EXPECTED_FIRST_MATCH);
  }

  @Test
  public void findMatchShouldReturnNullWhenRegexMatchesButGroupNumberIsNotFound() {
    RegexMatcher regexMatcher = new RegexMatcher(REGEX_MATCHES, REGEX_GROUP);
    assertThat(regexMatcher.findMatch(INPUT_STRING_ONE, 3)).isNull();
  }

  @Test
  public void findMatchShouldReturnNullWhenGroupNumberIsLessThanZero() {
    RegexMatcher regexMatcher = new RegexMatcher(REGEX_MATCHES, REGEX_GROUP);
    assertThat(regexMatcher.findMatch(INPUT_STRING_ONE, -1)).isNull();
  }

  @Test
  public void findMatchShouldReturnNullWhenRegexDoesNotMatch() {
    RegexMatcher regexMatcher = new RegexMatcher(REGEX_DOES_NOT_MATCH,
        REGEX_GROUP);
    assertThat(regexMatcher.findMatch(INPUT_STRING_ONE, 1)).isNull();
  }

  @Test
  public void findMatchShouldReturnNullWhenMatchNumberIsBiggerThanOccurences() {
    RegexMatcher regexMatcher = new RegexMatcher(REGEX_MATCHES, REGEX_GROUP);
    assertThat(regexMatcher.findMatch(INPUT_STRING_ONE, 4)).isNull();
  }

  @Test
  public void findMatchesShouldReturnTheExpectedMatchesWhenRegexMatchesMultipleTimes() {
    RegexMatcher regexMatcher = new RegexMatcher(REGEX_MATCHES, REGEX_GROUP);
    ArrayList<String> matches = new ArrayList<>();
    matches.add(EXPECTED_FIRST_MATCH);
    matches.add(EXPECTED_SECOND_MATCH);
    assertThat(regexMatcher.findMatches(INPUT_STRING_ONE)).isEqualTo(matches);
  }

  @Test
  public void findMatchesShouldReturnASingleMatchIfItOnlyMatchesOnce() {
    RegexMatcher regexMatcher = new RegexMatcher(REGEX_MATCHES, REGEX_GROUP);
    ArrayList<String> matches = new ArrayList<>();
    matches.add(EXPECTED_FIRST_MATCH);
    assertThat(regexMatcher.findMatches(INPUT_STRING_TWO)).isEqualTo(matches);
  }

  @Test
  public void findMatchesShouldReturnNullWhenRegexDoesNotMatch() {
    RegexMatcher regexMatcher = new RegexMatcher(REGEX_DOES_NOT_MATCH,
        REGEX_GROUP);
    assertThat(regexMatcher.findMatches(INPUT_STRING_TWO)).isEqualTo(Collections.emptyList());
  }
}
