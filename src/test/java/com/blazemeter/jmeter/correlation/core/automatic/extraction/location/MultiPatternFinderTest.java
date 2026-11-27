package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

public class MultiPatternFinderTest {

  // A small alphabet makes overlaps, repeated prefixes and hash candidates frequent; it includes
  // a non-ASCII char (Greek capital omega) and the chars that URL encoding cares about
  private static final String ALPHABET = "abcΩ-%0";

  @Test
  public void shouldFindExactlyTheStringsThatContainsFinds() {
    Random random = new Random(20261015L);
    for (int round = 0; round < 200; round++) {
      String text = randomString(random, random.nextInt(400));
      List<String> strings = new ArrayList<>();
      int count = random.nextInt(60);
      for (int i = 0; i < count; i++) {
        // half taken from the text, so many of them are present
        if (random.nextBoolean() && text.length() > 0) {
          int start = random.nextInt(text.length());
          int end = Math.min(text.length(), start + 1 + random.nextInt(12));
          strings.add(text.substring(start, end));
        } else {
          strings.add(randomString(random, 1 + random.nextInt(12)));
        }
      }
      assertEquals("round " + round, expected(text, strings),
          new MultiPatternFinder(strings).findIn(text));
    }
  }

  @Test
  public void shouldScanLengthsWithManyStringsAndCheckTheOthersOneByOne() {
    // twenty strings of length 4 (scanned) and two of length 7 (checked one by one)
    List<String> strings = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      strings.add(String.format("k%03d", i));
    }
    strings.addAll(Arrays.asList("present", "missing"));
    String text = "x k003 y k017z presentk019";
    assertEquals(expected(text, strings), new MultiPatternFinder(strings).findIn(text));
  }

  @Test
  public void shouldHandleEmptyLongerAndRepeatedStrings() {
    List<String> strings = Arrays.asList("", "longer than the text", "ab", "ab", "b");
    assertEquals(expected("abab", strings), new MultiPatternFinder(strings).findIn("abab"));
  }

  @Test
  public void shouldFindNothingInAnEmptyTextExceptTheEmptyString() {
    List<String> strings = Arrays.asList("", "a");
    assertEquals(new HashSet<>(Arrays.asList("")), new MultiPatternFinder(strings).findIn(""));
  }

  private static Set<String> expected(String text, List<String> strings) {
    Set<String> found = new HashSet<>();
    for (String string : strings) {
      if (text.contains(string)) {
        found.add(string);
      }
    }
    return found;
  }

  private static String randomString(Random random, int length) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < length; i++) {
      builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return builder.toString();
  }
}
