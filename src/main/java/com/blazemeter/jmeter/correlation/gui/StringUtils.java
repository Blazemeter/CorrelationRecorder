package com.blazemeter.jmeter.correlation.gui;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringUtils {

  public static String capitalize(String text) {
    int length;
    if (text == null || (length = text.length()) == 0) {
      return text;
    }
    return IntStream.range(0, length)
        .mapToObj(i -> i == 0 ? String.valueOf(text.charAt(i)).toUpperCase()
            : String.valueOf(text.charAt(i)))
        .collect(Collectors.joining());
  }

  public static boolean isBlank(String str) {
    if (str == null || str.length() == 0) {
      return true;
    }
    return str.trim().isEmpty();
  }

  public static String substringAfterLast(String str, String separator) {
    String[] splicedStr = str.split(separator);
    return splicedStr.length == 0 ? "" : splicedStr[splicedStr.length - 1];
  }
}
