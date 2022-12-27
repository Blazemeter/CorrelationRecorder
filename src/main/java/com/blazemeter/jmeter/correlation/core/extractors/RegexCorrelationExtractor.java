package com.blazemeter.jmeter.correlation.core.extractors;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.CheckBoxParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.ComboParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.TextParameterDefinition;
import com.blazemeter.jmeter.correlation.core.RegexMatcher;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.gui.RegexExtractorGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Correlation Extractor that obtains values from the responses using Regular expressions.
 *
 * @param <T> correlation context that can be used to store values during replay
 */
public class RegexCorrelationExtractor<T extends BaseCorrelationContext> extends
    CorrelationExtractor<T> {

  public static final String MATCH_NUMBER_NAME = EXTRACTOR_PREFIX + "matchNr";
  public static final String MULTIVALUED_NAME = EXTRACTOR_PREFIX + "multiValued";
  protected static final String GROUP_NUMBER_NAME = EXTRACTOR_PREFIX + "groupNr";
  protected static final String GROUP_NUMBER_DESCRIPTION = "Group number";
  protected static final String MATCH_NUMBER_DESCRIPTION = "Match number";
  protected static final String EXTRACTOR_REGEX_NAME = EXTRACTOR_PREFIX + "regex";
  protected static final String EXTRACTOR_REGEX_DESCRIPTION = "Regular expression extractor";
  protected static final String DEFAULT_MATCH_GROUP_NAME = "match group";
  protected static final int DEFAULT_MATCH_GROUP = 1;
  protected static final ResultField DEFAULT_TARGET_VALUE = ResultField.BODY;
  protected static final String DEFAULT_MATCH_NUMBER_NAME = "match number";
  protected static final String MULTIVALUED_DESCRIPTION = "Multivalued";
  protected static final boolean DEFAULT_MULTIVALUED = false;
  private static final Function<String, Pattern> VARIABLE_PATTERN_PROVIDER =
      (variableName) -> Pattern.compile(variableName + "_(\\d|matchNr)");
  private static final Logger LOG = LoggerFactory.getLogger(RegexCorrelationExtractor.class);
  private static final String REGEX_EXTRACTOR_GUI_CLASS = RegexExtractorGui.class.getName();
  private static final int DEFAULT_MATCH_NUMBER = 1;
  private static final String DEFAULT_REGEX_EXTRACTOR_SUFFIX = "_NOT_FOUND";
  protected boolean multiValued;
  protected String regex;
  protected int matchNr;
  protected int groupNr;
  private transient JMeterVariables currentVars;
  private transient List<TestElement> currentSamplersChild;

  /**
   * Default constructor added in order to satisfy the JSON conversion.
   *
   * <p>All the default values are set in this constructor. Like the <code>regex = param="(.+?)
   * "</code> to be an example for the Regular Expression.
   */
  public RegexCorrelationExtractor() {
    matchNr = DEFAULT_MATCH_NUMBER;
    regex = REGEX_DEFAULT_VALUE;
    groupNr = DEFAULT_MATCH_GROUP;
    target = ResultField.BODY;
    multiValued = DEFAULT_MULTIVALUED;
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
    this(regex, String.valueOf(matchNr), String.valueOf(groupNr), target.name(),
        Boolean.FALSE.toString());
  }

  /**
   * Constructor that receives all the fields from the GUI, as strings, and handles the parsing of
   * them.
   *
   * @param regex regular expression used to obtain the values from the responses
   * @param groupNr number of the group, in the regex, from where the values will be extracted
   * @param matchNr number of the matched appearance that needs to be considered for extracting the
   * value
   * @param targetDescription name of the target where the Correlation Extractor will be applied
   * @param multiValued flag which determines if extraction needs to be multi-evaluated.
   */
  public RegexCorrelationExtractor(String regex, String matchNr, String groupNr,
      String targetDescription, String multiValued) {
    super(ResultField.valueOf(targetDescription));
    this.regex = regex;
    this.matchNr = parseInteger(matchNr,
        RegexCorrelationExtractor.DEFAULT_MATCH_NUMBER_NAME, DEFAULT_MATCH_NUMBER);
    this.groupNr = parseInteger(groupNr, RegexCorrelationExtractor.DEFAULT_MATCH_GROUP_NAME,
        DEFAULT_MATCH_GROUP);
    this.multiValued = Boolean.parseBoolean(multiValued);
  }

  private static int getMatchNumber(CorrelationRuleTestElement testElem) {
    return parseInteger(testElem.getPropertyAsString(MATCH_NUMBER_NAME), DEFAULT_MATCH_NUMBER_NAME,
        1);
  }

  private static int getGroupNumber(CorrelationRuleTestElement testElem) {
    return parseInteger(testElem.getPropertyAsString(GROUP_NUMBER_NAME), DEFAULT_MATCH_GROUP_NAME,
        1);
  }

  private boolean isMultiValued(CorrelationRuleTestElement testElem) {
    return testElem.getPropertyAsBoolean(MULTIVALUED_NAME, DEFAULT_MULTIVALUED);
  }

  @Override
  public List<String> getParams() {
    return Arrays
        .asList(regex, Integer.toString(matchNr), Integer.toString(groupNr), target.name(),
            Boolean.toString(multiValued));
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
    multiValued = params.size() >= 4 && Boolean.parseBoolean(params.get(4));
  }

  @Override
  public String getDisplayName() {
    return "Regex";
  }

  public List<ParameterDefinition> getParamsDefinition() {
    return Arrays.asList(new TextParameterDefinition(EXTRACTOR_REGEX_NAME,
            EXTRACTOR_REGEX_DESCRIPTION,
            REGEX_DEFAULT_VALUE),
        new TextParameterDefinition(MATCH_NUMBER_NAME, MATCH_NUMBER_DESCRIPTION,
            String.valueOf(DEFAULT_MATCH_NUMBER), true),
        new TextParameterDefinition(GROUP_NUMBER_NAME, GROUP_NUMBER_DESCRIPTION,
            String.valueOf(DEFAULT_MATCH_GROUP), true),
        new ComboParameterDefinition(TARGET_FIELD_NAME, TARGET_FIELD_DESCRIPTION,
            ResultField.BODY.name(), ResultField.getNamesToCodesMapping(), true),
        new CheckBoxParameterDefinition(MULTIVALUED_NAME, MULTIVALUED_DESCRIPTION,
            DEFAULT_MULTIVALUED, true));
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setProperty(EXTRACTOR_REGEX_NAME, regex);
    testElem.setProperty(MATCH_NUMBER_NAME, String.valueOf(matchNr));
    testElem.setProperty(GROUP_NUMBER_NAME, String.valueOf(groupNr));
    testElem.setProperty(TARGET_FIELD_NAME,
        target != null ? target.name() : ResultField.BODY.name());
    testElem.setProperty(MULTIVALUED_NAME, multiValued);
  }

  protected void setGroupNr(int groupNr) {
    this.groupNr = groupNr;
  }

  /**
   * Used to process the response after a request is made to extract values from it.
   *
   * <p>In case the regular expression is matched, a {@link RegexExtractor} Post Processor will be
   * added to the children list for JMeter to extract the value and store it in the
   * <code>variableName</code>. During the recording, the matched value will be stored in the
   * JMeterVariables to be considered for future Correlation Extractors/Replacements
   *
   * @param sampler recorded sampler containing the information of the request
   * @param children list of children added to the sampler (if the Regular Expression is matched, a
   * Regex Extractor will be added to it)
   * @param result result containing information about request and associated response from server
   * @param vars stored variables shared between requests during recording
   */
  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
      JMeterVariables vars) {
    if (regex.isEmpty()) {
      return;
    }
    if (matchNr == 0) {
      LOG.warn("Extracting random appearances is not supported. Returning null instead.");
      return;
    }
    this.currentVars = vars;
    this.currentSamplersChild = children;

    RegexMatcher regexMatcher = new RegexMatcher(regex, groupNr);
    String varName = multiValued ? generateVariableName() : variableName;
    if (matchNr >= 0) {
      String match = regexMatcher.findMatch(target.getField(result), matchNr);
      if (match != null && !match.equals(vars.get(varName))) {
        addVarAndChildPostProcessor(match, varName,
            createPostProcessor(varName, matchNr));
      }
    } else {
      ArrayList<String> matches = regexMatcher.findMatches(target.getField(result));
      if (matches.size() == 1) {
        addVarAndChildPostProcessor(matches.get(0), varName,
            createPostProcessor(varName, 1));
      } else if (matches.size() > 1) {
        if (!multiValued) {
          clearJMeterVariables(vars);
        }
        addVarAndChildPostProcessor(String.valueOf(matches.size()),
            varName + "_matchNr", createPostProcessor(varName, matchNr));
        int matchNr = 1;
        for (String match : matches) {
          vars.put(varName + "_" + matchNr, match);
          matchNr++;
        }
      }
    }

  }

  private void clearJMeterVariables(JMeterVariables vars) {
    Set<Entry<String, Object>> entries = new HashSet<>(vars.entrySet());
    entries.forEach(e -> {
      if (VARIABLE_PATTERN_PROVIDER.apply(variableName).matcher(e.getKey()).matches()) {
        vars.remove(e.getKey());
      }
    });
  }

  private void addVarAndChildPostProcessor(String match, String variableName,
      RegexExtractor postProcessor) {
    currentSamplersChild.add(postProcessor);
    currentVars.put(variableName, match);
  }

  private String generateVariableName() {
    return variableName + "#" + context.getNextVariableNr(variableName);
  }

  /**
   * Creates a {@link RegexExtractor} Post Processor with the Regular Expression, the group number
   * and the match number, to extract and store the matched value, during the replay, in the
   * <code>variableName</code>.
   *
   * @return the created RegexExtractor Post Processor
   * @see
   * <a href="https://jmeter.apache.org/api/org/apache/jmeter/extractor/RegexExtractor.html">RegexExtractor</a>
   */
  protected RegexExtractor createPostProcessor(String varName, int matchNr) {
    RegexExtractor regexExtractor = new RegexExtractor();
    regexExtractor.setProperty(TestElement.GUI_CLASS, REGEX_EXTRACTOR_GUI_CLASS);
    regexExtractor.setName("RegExp - " + varName);
    regexExtractor.setRefName(varName);
    regexExtractor.setRegex(regex);
    regexExtractor.setTemplate("$" + groupNr + "$");
    regexExtractor.setMatchNumber(matchNr);
    regexExtractor.setDefaultValue(varName + DEFAULT_REGEX_EXTRACTOR_SUFFIX);
    regexExtractor.setUseField(target.getCode());
    return regexExtractor;
  }

  @Override
  public void update(CorrelationRuleTestElement testElem) {
    super.update(testElem);
    regex = testElem.getPropertyAsString(EXTRACTOR_REGEX_NAME);
    matchNr = getMatchNumber(testElem);
    groupNr = getGroupNumber(testElem);
    multiValued = isMultiValued(testElem);
  }

  @Override
  public String toString() {
    return "RegexCorrelationExtractor{" +
        "regex='" + regex + '\'' +
        ", matchNr=" + matchNr +
        ", groupNr=" + groupNr +
        ", multiValued=" + multiValued +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegexCorrelationExtractor<?> that = (RegexCorrelationExtractor<?>) o;
    return matchNr == that.matchNr &&
        groupNr == that.groupNr &&
        Objects.equals(regex, that.regex) &&
        multiValued == that.multiValued;
  }

  @Override
  public int hashCode() {
    return Objects.hash(regex, matchNr, groupNr, multiValued);
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return BaseCorrelationContext.class;
  }
}
