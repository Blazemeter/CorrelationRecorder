package com.blazemeter.jmeter.correlation.core.automatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReplacementRegexTest {

  @Before
  public void setup() throws IOException {
    JMeterTestUtils.setupUpdatedJMeter();
  }

  @Test
  public void testValidNameAndValidSource() {
    String regex = ReplacementRegex.match("ValidName", Sources.REQUEST);
    assertEquals("ValidName: ([^&]+)", regex);
  }

  @Test
  public void testInvalidNameAndValidSource() {
    String regex = ReplacementRegex.match("", Sources.REQUEST);
    assertEquals(": ([^&]+)", regex);
  }

  @Test
  public void testValidNameAndInvalidSource() {
    String regex = ReplacementRegex.match("ValidName", "InvalidSource");
    assertEquals("", regex);
  }

  @Test
  public void testInvalidNameAndInvalidSource() {
    String regex = ReplacementRegex.match("", "InvalidSource");
    assertEquals("", regex);
  }

  @Test
  public void testRequestOrRequestHeaderFields() {
    String regex = ReplacementRegex.match("Name", Sources.REQUEST);
    assertEquals("Name: ([^&]+)", regex);

    regex = ReplacementRegex.match("Name", Sources.REQUEST_HEADER_FIELDS);
    assertEquals("Name: ([^&]+)", regex);
  }

  @Test
  public void testRequestBodyJsonNumeric() {
    String regex = ReplacementRegex.match("Name", Sources.REQUEST_BODY_JSON_NUMERIC);
    assertEquals("\"Name\":([0-9]+)", regex);
  }

  @Test
  public void testRequestBodyJson() {
    String regex = ReplacementRegex.match("Name", Sources.REQUEST_BODY_JSON);
    assertEquals("\"Name\":\"([^&]+?)\"", regex);
  }

  @Test
  public void testRequestUrl() {
    String regex = ReplacementRegex.match("Name", Sources.REQUEST_URL);
    assertEquals("(?:\\?|&)Name=(.+?)(?:&|$)", regex);
  }

  @Test
  public void testRequestArguments() {
    String regex = ReplacementRegex.match("Name", Sources.REQUEST_ARGUMENTS);
    assertEquals("Name=([^&]+)", regex);
  }

  @Test
  public void testRequestPath() {
    String regex = ReplacementRegex.match("Name", Sources.REQUEST_PATH);
    assertEquals("\\/Name\\/([^\\/]+)(?:\\?[^&]*)?$", regex);
  }


  @Test
  public void shouldGenerateNullRegexForInvalidSource() {
    String input = null;
    String result = ReplacementRegex.match("Name", Sources.REQUEST_URL, input);
    assertNull(result);
  }

  @Test
  public void shouldGenerateValidRegexForNumericValueInPathURLFollowedByQuestionMark() {
    String input = "http://localhost/index.php/wp-json/wp/v2/posts/1418?_locale=use";
    String result = ReplacementRegex.match("posts",
        Sources.REQUEST_PATH_NUMBER_FOLLOWED_BY_QUESTION_MARK, input);
    assertEquals("1418", result);
  }

  @Test
  public void shouldGenerateValidRegexForNumericValueInPathURLFollowedBySlash() {
    String input = "http://localhost/index.php/wp-json/wp/v2/posts/1418/edit";
    String result = ReplacementRegex.match("posts",
        Sources.REQUEST_PATH_NUMBER_FOLLOWED_BY_SLASH, input);
    assertEquals("1418", result);
  }

  @Test
  public void shouldGenerateValidRegexForNumericValueInPathURL() {
    String input = "http://localhost/index.php/wp-json/wp/v2/posts/1418";
    String result = ReplacementRegex.match("posts", Sources.REQUEST_PATH, input);
    assertEquals("1418", result);
  }

  @Test
  public void shouldGenerateValidRegexForNamesWithSpecialCharacters() {
    String
        input = "data[wp-check-locked-posts][]=post-1396&data[wp-check-locked-posts][]=post-1418";
    String result = ReplacementRegex.match("data[wp-check-locked-posts][]",
        Sources.REQUEST_ARGUMENTS, input);
    assertEquals("post-1396", result);
  }
}