package com.blazemeter.jmeter.correlation.regression;

import java.util.regex.Pattern;

public class StringReplacement {

  private final Pattern pattern;
  private final String replacement;

  public StringReplacement(String pattern, String replacement) {
    this.pattern = Pattern.compile(pattern);
    this.replacement = replacement;
  }

  public boolean matches(String url) {
    return pattern.matcher(url).find();
  }

  public String apply(String val) {
    return pattern.matcher(val).replaceAll(replacement);
  }

}
