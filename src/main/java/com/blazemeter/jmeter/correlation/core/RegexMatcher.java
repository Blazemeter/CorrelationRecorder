package com.blazemeter.jmeter.correlation.core;

import java.util.ArrayList;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

public class RegexMatcher {

  private final String regex;
  private final int group;

  public RegexMatcher(String regex, int group) {
    this.regex = regex;
    this.group = group;
  }

  public String findMatch(String input, int matchNumber) {
    Perl5Matcher matcher = JMeterUtils.getMatcher();
    Pattern pattern = null;
    try {
      pattern = JMeterUtils.getPatternCache().getPattern(regex, Perl5Compiler.READ_ONLY_MASK);
      PatternMatcherInput matcherInput = new PatternMatcherInput(input);
      int matchCount = 0;
      while (matchCount < matchNumber && matcher.contains(matcherInput, pattern)) {
        matchCount++;
      }
      return matchCount == matchNumber ? matcher.getMatch().group(group) : null;
    } finally {
      JMeterUtils.clearMatcherMemory(JMeterUtils.getMatcher(), pattern);
    }
  }

  public ArrayList<String> findMatches(String input) {
    ArrayList<String> matches = new ArrayList<>();
    Perl5Matcher matcher = JMeterUtils.getMatcher();
    Pattern pattern = null;
    try {
      pattern = JMeterUtils.getPatternCache().getPattern(regex, Perl5Compiler.READ_ONLY_MASK);
      PatternMatcherInput matcherInput = new PatternMatcherInput(input);
      while (matcher.contains(matcherInput, pattern)) {
        matches.add(matcher.getMatch().group(group));
      }
      return matches;
    } finally {
      JMeterUtils.clearMatcherMemory(JMeterUtils.getMatcher(), pattern);
    }
  }

}
