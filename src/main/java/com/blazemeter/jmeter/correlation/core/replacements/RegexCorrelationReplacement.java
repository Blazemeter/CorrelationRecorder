package com.blazemeter.jmeter.correlation.core.replacements;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.CheckBoxParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.TextParameterDefinition;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Correlation Replacements that applies the replacement using Regular expressions and the captured
 * values.
 *
 * @param <T> correlation context that can be used to store and share values during replay
 */
public class RegexCorrelationReplacement<T extends BaseCorrelationContext> extends
    CorrelationReplacement<T> {

  public static final String REPLACEMENT_STRING_PROPERTY_NAME = PROPERTIES_PREFIX +
      "replacementString";
  public static final String REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME = PROPERTIES_PREFIX +
      "ignoreValue";
  protected static final String REPLACEMENT_REGEX_PROPERTY_NAME = PROPERTIES_PREFIX + "regex";
  protected static final String REPLACEMENT_REGEX_PROPERTY_DESCRIPTION =
      "Regular expression " + "replacement";
  protected static final String FUNCTION_REF_PREFIX = "${"; //$NON-NLS-1$
  /**
   * Functions are wrapped in ${ and }.
   */
  protected static final String FUNCTION_REF_SUFFIX = "}"; //$NON-NLS-1$
  private static final Logger LOG = LoggerFactory.getLogger(RegexCorrelationReplacement.class);
  private static final boolean IGNORE_VALUE_DEFAULT = false;
  private static final String REPLACEMENT_STRING_DEFAULT_VALUE = "";
  protected String regex = REGEX_DEFAULT_VALUE;
  protected boolean ignoreValue = IGNORE_VALUE_DEFAULT;
  protected String replacementString = REPLACEMENT_STRING_DEFAULT_VALUE;
  private Function<String, String> expressionEvaluator =
      (expression) -> new CompoundVariable(expression).execute();

  /**
   * Default constructor added in order to satisfy the JSON conversion.
   *
   * <p>Implementing a Custom Correlation Replacement requires to mimic this behavior
   */
  public RegexCorrelationReplacement() {
  }

  public RegexCorrelationReplacement(String regex) {
    this.regex = regex;
  }

  public RegexCorrelationReplacement(String regex, String replacementString, String ignoreValue) {
    this.regex = regex;
    this.replacementString = replacementString;
    this.ignoreValue = Boolean.parseBoolean(ignoreValue);
  }

  @Override
  public String getDisplayName() {
    return "Regex";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList(regex, replacementString, Boolean.toString(ignoreValue));
  }

  @Override
  public void setParams(List<String> params) {
    regex = !params.isEmpty() ? params.get(0) : REGEX_DEFAULT_VALUE;
    replacementString = params.size() > 1 ? params.get(1) : REPLACEMENT_STRING_DEFAULT_VALUE;
    ignoreValue = params.size() > 2 ? Boolean.parseBoolean(params.get(2)) : IGNORE_VALUE_DEFAULT;
  }

  @Override
  public List<ParameterDefinition> getParamsDefinition() {
    return Arrays.asList(
        new TextParameterDefinition(REPLACEMENT_REGEX_PROPERTY_NAME,
            REPLACEMENT_REGEX_PROPERTY_DESCRIPTION, REGEX_DEFAULT_VALUE),
        new TextParameterDefinition(REPLACEMENT_STRING_PROPERTY_NAME, "Replacement string",
            REPLACEMENT_STRING_DEFAULT_VALUE, true),
        new CheckBoxParameterDefinition(REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME, "Ignore Value",
            IGNORE_VALUE_DEFAULT, true));
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return BaseCorrelationContext.class;
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setProperty(REPLACEMENT_REGEX_PROPERTY_NAME, regex);
    testElem.setProperty(REPLACEMENT_STRING_PROPERTY_NAME, replacementString);
    testElem.setProperty(REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME, ignoreValue);
  }

  /**
   * Process every request sent to the server and apply replacements over specific values. Differs
   * from the father's class {@link CorrelationReplacement}'s by applying short circuit evaluation
   * for better performance.
   *
   * @param sampler recorded sampler containing the information of the request
   * @param children list of children added to the sampler (if the value matches the one associated
   * to the Reference Variable, components could be added to help the correlation process)
   * @param result containing information about the request and associated response from server
   * @param vars stored variables shared between requests during recording
   * @see <a href="https://en.wikipedia.org/wiki/Short-circuit_evaluation">Short-circuit
   * evaluation</a>
   */
  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
      JMeterVariables vars) {
    if (regex.isEmpty()) {
      return;
    }
    super.process(sampler, children, result, vars);
  }

  /**
   * Receives the property's string and tries to match it with the regular expression and, if it
   * does match, it has to be equals to the one stored in the JMeterVariables with the
   * <code>variableName</code> to be replaced in the string.
   *
   * <p>Matched values will be replaced with the variableName surrounded by <code>${}</code>. eg:
   * for <pre>variableName=VAR_1</pre> the replacement would be <pre>${VAR_1}</pre>
   *
   * @param input property's string to check and replace
   * @param vars stored variables shared between request during the recording
   * @return the resultant input after been processed
   */
  @Override
  protected String replaceString(String input, JMeterVariables vars) {
    try {
      return replaceWithRegex(input, regex, variableName, vars);
    } catch (MalformedPatternException e) {
      LOG.warn("Malformed pattern: {}", regex, e);
      return input;
    }
  }

  /**
   * Handles the method used to evaluate all the matched values by the Regular Expression.
   *
   * <p>Establish the condition that, if a value is matched with the Regular expression, it should 
   * also be equals to the value stored in the JMeterVariables with the variable name. Not all 
   * the values matched with the regex needs to be correlated. Overwrite it when the condition 
   * wants to be changed.
   *
   * @param input property's string to check and replace
   * @param regex regular expression used to do the evaluation
   * @param variableName name of the variable name associated to this Correlation Replacement
   * @param vars stored variables shared between requests during recording
   * @return the resultant input after been processed
   */
  protected String replaceWithRegex(String input, String regex,
      String variableName, JMeterVariables vars)
      throws MalformedPatternException {
    PatternMatcher matcher = JMeterUtils.getMatcher();
    Pattern pattern = new Perl5Compiler().compile(regex);
    PatternMatcherInput patternMatcherInput = new PatternMatcherInput(input);
    int beginOffset = patternMatcherInput.getBeginOffset();
    char[] inputBuffer = patternMatcherInput.getBuffer();
    StringBuilder result = new StringBuilder();
    Function<String, String> expressionProvider = replaceExpressionProvider();
    while (matcher.contains(patternMatcherInput, pattern)) {
      MatchResult match = matcher.getMatch();
      boolean hasMatch = false;
      int varNr = 0;
      while (varNr <= context.getVariableCount(variableName) && !hasMatch) {
        /* varNr could be 0 if non MultiValuedExtractor is used
         so this code is to support when yo use MultiValuedReplacement with 
         SingleValuedExtractor */
        String varName = varNr == 0 ? variableName : variableName + "#" + varNr;
        String varMatchesCount = vars.get(varName + "_matchNr");
        String literalMatched = match.group(1);
        String replaceExpression = null;
        if (varMatchesCount == null) {
          if (vars.get(varName) != null && vars.get(varName).equals(literalMatched)
              && replacementString.isEmpty()) {
            replaceExpression = expressionProvider.apply(varName);
            hasMatch = true;
          } else if (ignoreValue && !replacementString.isEmpty()) {
            replaceExpression = replacementString;
            /* This case does not care if the value is 'matching'. Because ignore value is 
            activated, therefore we need to step out of loop by setting hasMatch.*/
            hasMatch = true;
          } else if (computeStringReplacement(varName)
              .equals(literalMatched) && !ignoreValue) {
            replaceExpression = expressionProvider
                .apply(buildReplacementStringForMultivalued(varName));
            hasMatch = true;
          }
          if (replaceExpression != null) {
            result = replaceMatch(result, patternMatcherInput, match,
                beginOffset, inputBuffer, replaceExpression);
          }
        } else {
          int matchNr = Integer.parseInt(varMatchesCount);
          int varMatch = 1;
          while (varMatch <= matchNr && !hasMatch) {
            String varNameMatch = varName + "_" + varMatch;
            if (vars.get(varNameMatch).equals(literalMatched) && replacementString.isEmpty()) {
              replaceExpression = varNameMatch;
              hasMatch = true;
            } else if (!ignoreValue && !replacementString.isEmpty()) {
              if (computeStringReplacement(varNameMatch).equals(literalMatched)) {
                replaceExpression = buildReplacementStringForMultivalued(varNameMatch);
                hasMatch = true;
              }
            }
            if (replaceExpression != null) {
              result = replaceMatch(result, patternMatcherInput, match,
                  beginOffset, inputBuffer, expressionProvider.apply(replaceExpression));
            }
            varMatch++;
          }
        }
        varNr++;
      }
      if (!hasMatch) {
        result.append(inputBuffer, beginOffset,
            patternMatcherInput.getMatchEndOffset() - beginOffset);
      }
      beginOffset = patternMatcherInput.getMatchEndOffset();
    }
    result.append(inputBuffer, beginOffset, input.length() - beginOffset);
    return result.toString();
  }

  private Function<String, String> replaceExpressionProvider() {
    return s -> replacementString == null
        || !java.util.regex.Pattern.compile("(\\$\\{.+?})").matcher(replacementString).matches()
        || replacementString.isEmpty()
        ? FUNCTION_REF_PREFIX + s + FUNCTION_REF_SUFFIX : s;
  }

  private String computeStringReplacement(String varName) {
    String rawReplacementString = buildReplacementStringForMultivalued(varName);
    String computed = expressionEvaluator.apply(rawReplacementString);
    LOG.debug("Result of {} was {}", rawReplacementString, computed);
    return computed;
  }

  private String buildReplacementStringForMultivalued(String varNameMatch) {
    if (replacementString != null && replacementString.contains(variableName)) {
      return replacementString.replace(variableName, varNameMatch);
    }
    return replacementString;
  }

  private StringBuilder replaceMatch(StringBuilder result, PatternMatcherInput patternMatcherInput,
      MatchResult match, int beginOffset, char[] inputBuffer, String expression) {
    return result.append(inputBuffer, beginOffset, match.beginOffset(1) - beginOffset)
        .append(expression)
        .append(inputBuffer, match.endOffset(1),
            patternMatcherInput.getMatchEndOffset() - match.endOffset(1));
  }

  /**
   * Handles the replacements of a value using a Regular Expression in am input, using the condition
   * and the expression to use as the replacement.
   *
   * <p>Receives a matching condition that evaluates the String input. If the condition is met, 
   * every appearance of the matched value will be replaced by <code>${expression}</code>.
   *
   * @param input property's string to check and replace
   * @param regex regular expression used to do the evaluation
   * @param expression expression that will replace the matched value(s)
   * @param matchCondition predicate that will serve for evaluating whether or not the matched value
   * should be replaced by the expression
   * @return the resultant input after been processed
   */
  protected String replaceWithRegexAndPredicate(String input, String regex, String expression,
      Predicate<String> matchCondition)
      throws MalformedPatternException {
    PatternMatcher matcher = JMeterUtils.getMatcher();
    Pattern pattern = new Perl5Compiler().compile(regex);
    PatternMatcherInput patternMatcherInput = new PatternMatcherInput(input);
    int beginOffset = patternMatcherInput.getBeginOffset();
    char[] inputBuffer = patternMatcherInput.getBuffer();
    StringBuilder result = new StringBuilder();
    while (matcher.contains(patternMatcherInput, pattern)) {
      MatchResult match = matcher.getMatch();
      if (matchCondition.test(match.group(1))) {
        replaceMatch(result, patternMatcherInput, match, beginOffset, inputBuffer,
            FUNCTION_REF_PREFIX + expression + FUNCTION_REF_SUFFIX);
      } else {
        result.append(inputBuffer, beginOffset,
            patternMatcherInput.getMatchEndOffset() - beginOffset);
      }
      beginOffset = patternMatcherInput.getMatchEndOffset();
    }
    result.append(inputBuffer, beginOffset, input.length() - beginOffset);
    return result.toString();
  }

  @Override
  public void update(CorrelationRuleTestElement testElem) {
    regex = testElem.getPropertyAsString(REPLACEMENT_REGEX_PROPERTY_NAME);
    replacementString = testElem.getPropertyAsString(REPLACEMENT_STRING_PROPERTY_NAME);
    ignoreValue = testElem.getPropertyAsBoolean(REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME);
  }

  @Override
  public String toString() {
    return "RegexCorrelationReplacement{" +
        "paramValues='" + getParams() + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegexCorrelationReplacement<?> that = (RegexCorrelationReplacement<?>) o;
    return Objects.equals(regex, that.regex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(regex);
  }

  @VisibleForTesting
  public void setExpressionEvaluator(Function<String, String> expressionEvaluator) {
    this.expressionEvaluator = expressionEvaluator;
  }
}
