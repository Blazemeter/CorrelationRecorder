package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReplacementStringTest {

  @Test
  public void shouldDecodeValidEscapes() {
    assertEquals("a b&c", ReplacementString.URL_DECODE.applyFunction("a+b%26c"));
  }

  @Test
  public void shouldKeepValueWhenPercentIsNotAValidEscape() {
    // Decoding this value throws IllegalArgumentException in URLDecoder; it must not abort
    assertEquals("50 % p", ReplacementString.URL_DECODE.applyFunction("50 % p"));
  }

  @Test
  public void shouldKeepValueWhenEscapeIsTruncated() {
    assertEquals("abc%2", ReplacementString.URL_DECODE.applyFunction("abc%2"));
  }
}
