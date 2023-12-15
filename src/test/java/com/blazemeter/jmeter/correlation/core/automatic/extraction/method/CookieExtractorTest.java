package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CookieExtractorTest {

  private CookieExtractor cookieExtractor;
  @Before
  public void setup() {
    cookieExtractor = new CookieExtractor();
  }

  @Test
  public void shouldReturnCorrectContextStringWhenGetSetCookieContextStringWithValidResponseAndName() {
    String mockResponse = "Content-Type: text/html\n"
        + "Set-Cookie: testCookie=testValue; Path=/; Domain=.example.com\n"
        + "Cache-Control: no-cache";
    String mockName = "testCookie[Path=/, Domain=.example.com]";
    String mockValue = "testValue";
    String mockSource = "Response Header Set-Cookie ('" + "Raw" + "')";

    String expectedResult = "Set-Cookie: testCookie=testValue; Path=/; Domain=.example.com";
    String result = CookieExtractor.getSetCookieContextString(mockResponse, mockName, mockValue, mockSource);

    assertEquals(expectedResult, result);
  }
}