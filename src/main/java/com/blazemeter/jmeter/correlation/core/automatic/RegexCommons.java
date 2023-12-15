package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.regex.PatternSyntaxException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexCommons {
  private static final Logger LOG = LoggerFactory.getLogger(RegexCommons.class);
  private static final String SIMPLE_REGEX = "(.+?)";
  private static final String DELIMITER_REGEX_TEMPLATE = "((?:[^%s\\\\]|%s)*?)";
  private static final String NON_GREEDY_DELIMITER_REGEX_TEMPLATE = "((?:[^%s\\\\]|%s)*?)";
  private static final String CR_IGNORE = "_CR_IGNORE_";
  private static final String LINE_BREAK = "\n";

  public static String dynamicGenerateExtractorRegex(String originalValue, String contextString) {
    String regex;
    String simpleRegexCapture = RegexCommons.SIMPLE_REGEX;
    try {
      String[] boundaries = contextString.split(originalValue);
      if (boundaries.length > 2) {
        boundaries[1] = boundaries[boundaries.length - 1];
      }
      boolean isBeggingOfTheLine = false;
      if (hasLineBreak(boundaries[0])) {
        isBeggingOfTheLine = true;
        boundaries[0] = boundaries[0].substring(boundaries[0].lastIndexOf(LINE_BREAK) + 1);
      }

      boolean isEndOfTheLine = false;
      if (hasRightBoundary(boundaries) && hasLineBreak(boundaries[1])) {
        isEndOfTheLine = true;
        boundaries[1] = boundaries[1].substring(0, boundaries[1].indexOf(LINE_BREAK));
      } else if (hasRightBoundary(boundaries)) {
        boundaries[1] = boundaries[1].substring(0, 1);
      }

      // We check if the value is "surrounded" by the same character
      if (hasRightBoundary(boundaries) && notEmpty(boundaries[0]) && notEmpty(boundaries[1])) {
        String lastChar = boundaries[0].substring(boundaries[0].length() - 1);
        String firstChar = boundaries[1].substring(0, 1);
        if (lastChar.equals(firstChar)) {
          simpleRegexCapture = getDelimiterRegex(lastChar.charAt(0));
        }
      }

      // We escape the context boundaries to avoid regex issues
      String regexLeft =
          (isBeggingOfTheLine ? LINE_BREAK : "") + Perl5Compiler.quotemeta(boundaries[0]);
      String regexRight = (hasRightBoundary(boundaries) ? Perl5Compiler.quotemeta(boundaries[1])
          + (isEndOfTheLine ? "\\n" : "") : "$");
      regex = regexLeft + simpleRegexCapture + regexRight;
      // Fix regex when /[space] change to /s
      regex = regex.replace("\\ ", "\\s");
      // In case of _CR_IGNORE_ replace to wildcard matcher
      if (regex.contains(CR_IGNORE)) {
        regex = regex.replaceAll(CR_IGNORE, simpleRegexCapture);
      }
    } catch (IndexOutOfBoundsException | PatternSyntaxException exception) {
      LOG.error("Error trying to generate regex for value '{}' in context '{}'", originalValue,
          contextString);
      exception.printStackTrace();
      regex = contextString.replace(originalValue, simpleRegexCapture);
    }
    return regex;
  }

  private static boolean notEmpty(String boundary) {
    return boundary.length() > 0;
  }

  private static boolean hasLineBreak(String boundary) {
    return boundary.contains(LINE_BREAK);
  }

  private static boolean hasRightBoundary(String[] boundaries) {
    return boundaries.length > 1;
  }

  private static String getDelimiterRegex(char delimiter) {
    String escapedDelimiter = Perl5Compiler.quotemeta(String.valueOf(delimiter));
    return String.format(NON_GREEDY_DELIMITER_REGEX_TEMPLATE, escapedDelimiter, escapedDelimiter);
  }

  public static void main(String[] args) {
    String context = "min-width:1010px!important;";
    String response = "";
    String value = "10";

    String regex = dynamicGenerateExtractorRegex(value, context);
    System.out.println("regex: " + regex);
  }
}
