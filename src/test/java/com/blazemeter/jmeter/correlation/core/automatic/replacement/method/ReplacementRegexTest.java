package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.core.automatic.Sources;
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
  public void shouldMatchOnlyTheTokenOfCredentialsFields() {
    assertEquals("abc.def-ghi_~+/==", ReplacementRegex.match("Authorization",
        Sources.REQUEST_HEADER_FIELDS, "Authorization: Bearer abc.def-ghi_~+/=="));
    assertEquals("abc", ReplacementRegex.match("Proxy-Authorization",
        Sources.REQUEST_HEADER_FIELDS, "proxy-authorization: token  abc"));
  }

  @Test
  public void shouldMatchAlphanumericIdAnywhereInThePath() {
    assertEquals("cart_62704d1930ac", ReplacementRegex.match("carts",
        Sources.REQUEST_PATH_ALPHANUMERIC_ID, "/api/carts/cart_62704d1930ac/items"));
    assertEquals("cart_62704d1930ac", ReplacementRegex.match("carts",
        Sources.REQUEST_PATH_ALPHANUMERIC_ID, "/api/carts/cart_62704d1930ac"));
    assertEquals("ord_6301cb020b20", ReplacementRegex.match("orders",
        Sources.REQUEST_PATH_ALPHANUMERIC_ID, "/api/orders/ord_6301cb020b20?view=full"));
  }

  @Test
  public void shouldMatchTheArgumentOfAnEntityByItsOwnName() {
    assertEquals("id=([^&]+)", ReplacementRegex.match("order.id",
        Sources.REQUEST_ENTITY_ARGUMENT));
    assertEquals("ord_6301cb020b20", ReplacementRegex.match("order.id",
        Sources.REQUEST_ENTITY_ARGUMENT, "id=ord_6301cb020b20"));
  }

  @Test
  public void shouldNotMatchAlphanumericIdOfAnotherConcept() {
    assertNull(ReplacementRegex.match("carts", Sources.REQUEST_PATH_ALPHANUMERIC_ID,
        "/api/mycarts/cart_62704d1930ac/items"));
  }

  @Test
  public void shouldNotMatchCredentialsWithoutScheme() {
    assertNull(ReplacementRegex.match("Authorization", Sources.REQUEST_HEADER_FIELDS,
        "Authorization: abc"));
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