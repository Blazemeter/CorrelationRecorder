package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public enum ReplacementString {
  NONE("", s -> s),
  URL_DECODE("__urldecode", s -> {
    try {
      return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }),
  URL_ENCODE("__urlencode", s -> {
    try {
      return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  });

  private final String replacementString;
  private final Function<String, String> replacementFunction;

  ReplacementString(String replacementString, Function<String, String> replacementFunction) {
    this.replacementString = replacementString;
    this.replacementFunction = replacementFunction;
  }

  public String getExpression(String refName) {
    if (replacementString.isEmpty()) {
      return "";
    } else {
      return String.format("${%s(${%s})}", this.replacementString, refName);
    }
  }

  public String applyFunction(String value) {
    return replacementFunction.apply(value);
  }
}
