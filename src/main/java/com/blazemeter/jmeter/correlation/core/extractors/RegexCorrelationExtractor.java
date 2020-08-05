package com.blazemeter.jmeter.correlation.core.extractors;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.RegexMatcher;
import com.blazemeter.jmeter.correlation.core.ResultField;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;

public class RegexCorrelationExtractor<T extends CorrelationContext> extends
    CorrelationExtractor<T> {

  protected static final String GROUP_NUMBER_NAME = EXTRACTOR_PREFIX + "groupNr";
  protected static final String GROUP_NUMBER_DESCRIPTION = "Group number";
  protected static final String MATCH_NUMBER_NAME = EXTRACTOR_PREFIX + "matchNr";
  protected static final String MATCH_NUMBER_DESCRIPTION = "Match number";
  protected static final String EXTRACTOR_REGEX_NAME = EXTRACTOR_PREFIX + "regex";
  protected static final String EXTRACTOR_REGEX_DESCRIPTION = "Regular expression extractor";
  protected static final String DEFAULT_MATCH_GROUP_NAME = "match group";
  protected static final int DEFAULT_MATCH_GROUP = 1;
  protected static final ResultField DEFAULT_TARGET_VALUE = ResultField.BODY;
  private static final String REGEX_EXTRACTOR_GUI_CLASS = RegexExtractorGui.class.getName();
  private static final String DEFAULT_MATCH_NUMBER_NAME = "match number";
  private static final int DEFAULT_MATCH_NUMBER = 1;
  protected String regex;
  protected int matchNr;
  protected int groupNr;

  // Constructor added in order to satisfy json conversion
  public RegexCorrelationExtractor() {
    matchNr = DEFAULT_MATCH_NUMBER;
    regex = REGEX_DEFAULT_VALUE;
    groupNr = DEFAULT_MATCH_GROUP;
    target = ResultField.BODY;
  }

  public RegexCorrelationExtractor(String regex) {
    this(regex, DEFAULT_MATCH_GROUP);
  }

  public RegexCorrelationExtractor(String regex, int groupNr) {
    this(regex, groupNr, 1, ResultField.BODY);
  }

  public RegexCorrelationExtractor(String regex, int matchNr, ResultField target) {
    this(regex, DEFAULT_MATCH_GROUP, matchNr, target);
  }

  public RegexCorrelationExtractor(String regex, int groupNr, int matchNr,
      ResultField target) {
    super(target);
    this.regex = regex;
    this.matchNr = matchNr;
    this.groupNr = groupNr;
  }

  public RegexCorrelationExtractor(String regex, String groupNr, String matchNr,
      String targetDescription) {
    super(ResultField.valueOf(targetDescription));
    this.regex = regex;
    this.matchNr = parseInteger(matchNr,
        RegexCorrelationExtractor.DEFAULT_MATCH_NUMBER_NAME, DEFAULT_MATCH_NUMBER);
    this.groupNr = parseInteger(groupNr, RegexCorrelationExtractor.DEFAULT_MATCH_GROUP_NAME,
        DEFAULT_MATCH_GROUP);
  }

  private static int getMatchNumber(CorrelationRuleTestElement testElem) {
    return parseInteger(testElem.getPropertyAsString(MATCH_NUMBER_NAME), DEFAULT_MATCH_NUMBER_NAME,
        1);
  }

  private static int getGroupNumber(CorrelationRuleTestElement testElem) {
    return parseInteger(testElem.getPropertyAsString(GROUP_NUMBER_NAME), DEFAULT_MATCH_GROUP_NAME,
        1);
  }

  @Override
  public List<String> getParams() {
    return Arrays
        .asList(regex, Integer.toString(matchNr), Integer.toString(groupNr), target.name());
  }

  @Override
  public void setParams(List<String> params) {
    regex = params.size() > 0 ? params.get(0) : REGEX_DEFAULT_VALUE;
    matchNr = params.size() > 1 ? parseInteger(params.get(1), DEFAULT_MATCH_NUMBER_NAME,
        DEFAULT_MATCH_NUMBER) : DEFAULT_MATCH_NUMBER;
    groupNr = params.size() > 2 ? parseInteger(params.get(2), DEFAULT_MATCH_GROUP_NAME,
        DEFAULT_MATCH_GROUP) : DEFAULT_MATCH_GROUP;
    target = params.size() > 3 && !params.get(3).isEmpty() ? ResultField.valueOf(params.get(3))
        : DEFAULT_TARGET_VALUE;
  }

  public List<ParameterDefinition> getParamsDefinition() {
    return Arrays.asList(new ParameterDefinition(EXTRACTOR_REGEX_NAME, EXTRACTOR_REGEX_DESCRIPTION,
            REGEX_DEFAULT_VALUE,
            null),
        new ParameterDefinition(MATCH_NUMBER_NAME, MATCH_NUMBER_DESCRIPTION,
            String.valueOf(DEFAULT_MATCH_GROUP), null),
        new ParameterDefinition(GROUP_NUMBER_NAME, GROUP_NUMBER_DESCRIPTION,
            String.valueOf(DEFAULT_MATCH_GROUP), null),
        new ParameterDefinition(TARGET_FIELD_NAME, TARGET_FIELD_DESCRIPTION,
            ResultField.BODY.name(),
            ResultField.getNamesToCodesMapping()));
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setProperty(EXTRACTOR_REGEX_NAME, regex);
    testElem.setProperty(MATCH_NUMBER_NAME, String.valueOf(matchNr));
    testElem.setProperty(GROUP_NUMBER_NAME, String.valueOf(groupNr));
    testElem.setProperty(TARGET_FIELD_NAME,
        target != null ? target.name() : ResultField.BODY.name());
  }

  protected void setGroupNr(int groupNr) {
    this.groupNr = groupNr;
  }

  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
      JMeterVariables vars) {
    if (regex.isEmpty()) {
      return;
    }
    String match = new RegexMatcher(regex, groupNr)
        .findMatch(target.getField(result), matchNr == -1 ? 1 : matchNr);
    if (match != null) {
      children.add(createPostProcessor());
      vars.put(variableName, match);
    }
  }

  private RegexExtractor createPostProcessor() {
    RegexExtractor regexExtractor = new RegexExtractor();
    regexExtractor.setProperty(TestElement.GUI_CLASS, REGEX_EXTRACTOR_GUI_CLASS);
    regexExtractor.setName("RegExp - " + variableName);
    regexExtractor.setRefName(variableName);
    regexExtractor.setRegex(regex);
    regexExtractor.setTemplate("$" + groupNr + "$");
    regexExtractor.setMatchNumber(matchNr);
    regexExtractor.setDefaultValue(REGEX_DEFAULT_VALUE);
    regexExtractor.setUseField(target.getCode());
    return regexExtractor;
  }

  @Override
  public void update(CorrelationRuleTestElement testElem) {
    super.update(testElem);
    regex = testElem.getPropertyAsString(EXTRACTOR_REGEX_NAME);
    matchNr = getMatchNumber(testElem);
    groupNr = getGroupNumber(testElem);
  }

  @Override
  public String toString() {
    return "RegexCorrelationExtractor{" +
        "regex='" + regex + '\'' +
        ", matchNr=" + matchNr +
        ", groupNr=" + groupNr +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RegexCorrelationExtractor)) {
      return false;
    }
    RegexCorrelationExtractor<?> that = (RegexCorrelationExtractor<?>) o;
    return matchNr == that.matchNr &&
        groupNr == that.groupNr &&
        Objects.equals(regex, that.regex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(regex, matchNr, groupNr);
  }
}
