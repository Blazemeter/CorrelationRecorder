package com.blazemeter.jmeter.correlation.core.replacements;

import static com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils.jsonFindMatches;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.analysis.AnalysisReporter;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.google.common.annotations.VisibleForTesting;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Correlation Replacements that applies the replacement using Json Xpath and the captured values.
 *
 * @param <T> correlation context that can be used to store and share values during replay
 */

public class JsonCorrelationReplacement<T extends BaseCorrelationContext> extends
    CorrelationReplacement<T> {

  protected static final String JSONPATH_DEFAULT_VALUE = "$.jsonpath.expression";
  protected static final String REPLACEMENT_JSON_PROPERTY_NAME = PROPERTIES_PREFIX + "jsonpath";
  protected static final String REPLACEMENT_JSON_PROPERTY_DESCRIPTION = "JSONPath expression";

  private static final Logger LOG = LoggerFactory.getLogger(RegexCorrelationReplacement.class);
  private static final boolean IGNORE_VALUE_DEFAULT = false;
  private static final String REPLACEMENT_STRING_DEFAULT_VALUE = "";

  private static final String ESCAPE_QUOTE_LEFT = "_CR_L_";
  private static final String ESCAPE_QUOTE_RIGHT = "_CR_R_";

  protected String jsonpath = JSONPATH_DEFAULT_VALUE;
  protected boolean ignoreValue = IGNORE_VALUE_DEFAULT;

  private Object currentSampler;

  public JsonCorrelationReplacement() {
  }

  public JsonCorrelationReplacement(String jsonpath) {
    this.jsonpath = jsonpath;
  }

  public JsonCorrelationReplacement(String jsonpath, String replacementString, String ignoreValue) {
    this.jsonpath = jsonpath;
    this.replacementString = replacementString;
    this.ignoreValue = Boolean.parseBoolean(ignoreValue);
  }

  @Override
  public String getDisplayName() {
    return "JSON";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList(jsonpath, replacementString, Boolean.toString(ignoreValue));
  }

  @Override
  public void setParams(List<String> params) {
    jsonpath = !params.isEmpty() ? params.get(0) : JSONPATH_DEFAULT_VALUE;
    replacementString = params.size() > 1 ? params.get(1) : REPLACEMENT_STRING_DEFAULT_VALUE;
    ignoreValue = params.size() > 2 ? Boolean.parseBoolean(params.get(2)) : IGNORE_VALUE_DEFAULT;
  }

  @Override
  public List<ParameterDefinition> getParamsDefinition() {
    return Arrays.asList(
        new ParameterDefinition.TextParameterDefinition(REPLACEMENT_JSON_PROPERTY_NAME,
            REPLACEMENT_JSON_PROPERTY_DESCRIPTION, JSONPATH_DEFAULT_VALUE),
        new ParameterDefinition.TextParameterDefinition(REPLACEMENT_STRING_PROPERTY_NAME,
            "Replacement string", REPLACEMENT_STRING_DEFAULT_VALUE, true),
        new ParameterDefinition.CheckBoxParameterDefinition(REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME,
            "Ignore Value", IGNORE_VALUE_DEFAULT, true));
  }

  @Override
  protected String replaceString(String input, JMeterVariables vars) {
    // https://github.com/json-path/JsonPath?tab=readme-ov-file#set-a-value
    // Skip empty inputs
    if (input == null || input.isEmpty() || jsonpath == null || jsonpath.isEmpty()
        || variableName == null || variableName.isEmpty()) {
      return input;
    }
    // For previous replaced matches with variables, escape the unquoted variables
    String inputProcessed = escapeUnquotedVariablesWithMarks(input);
    if (JMeterElementUtils.isJson(inputProcessed)) {
      HashSet<Pair<String, String>> valuesReplaced = new HashSet();

      // Test if the path of the jsonpath match
      Pair<Class, ArrayList<String>> result = jsonFindMatches(inputProcessed, jsonpath);
      String updatedInput = inputProcessed;
      Class resultType = result.getLeft();
      ArrayList<String> matches = result.getRight();
      if (matches.size() > 0) {
        // Ok, match, try to each match get the path and replace
        for (int varNr = 0; varNr < matches.size(); varNr++) {
          String valueStr = matches.get(varNr);
          String varMatched = searchVariable(vars, valueStr, replacementString);
          String replaceExpression = null;
          // When ignore value, use the replacement string
          if (!replacementString.isEmpty() && ignoreValue) {
            replaceExpression = replacementString;
          } else if (varMatched != null) {
            replaceExpression = varMatched;
          }
          if (replaceExpression != null) {
            boolean inArray = resultType == JSONArray.class;
            String updatedJsonPath = inArray ? jsonpath + "[" + varNr + "]" : jsonpath;
            Pair<Class, ArrayList<String>> toUpdateMatches = jsonFindMatches(updatedInput,
                updatedJsonPath);
            if (toUpdateMatches.getRight() != null) {
              Class updateResultType = toUpdateMatches.getLeft();
              boolean originIsUnQuoted = JMeterElementUtils.classIsNumberOrBoolean(
                  updateResultType);
              if (originIsUnQuoted) {
                // When value is needed to put in the json structure without the quotes
                // this not is allowed by jayway because generate an invalid json with free text
                // inside, we need to post process to remove the left and the right marks to o that
                // Remember, jayway put the value as a quoted String,
                // and is why we need to put marks to recover the format without quotes at the end.
                replaceExpression = ESCAPE_QUOTE_LEFT + replaceExpression + ESCAPE_QUOTE_RIGHT;
              }
              try {
                updatedInput = JsonPath.parse(updatedInput)
                    .set(updatedJsonPath, replaceExpression)
                    .jsonString();
                // Store the values matched and used in the replacement
                valuesReplaced.add(Pair.of(valueStr, variableName));
              } catch (InvalidPathException e) {
                LOG.debug("JSONPath used to update target value doesn't match in the set: "
                    + "value:{} jsonpath={}", valueStr, updatedJsonPath);
              }
            } else {
              LOG.debug("JSONPath used to update target value doesn't match in the get: "
                  + "value:{} jsonpath={}", valueStr, updatedJsonPath);
            }
          }
        }
        // The json path match, replace the value with the replacement variable
        if (updatedInput != null && !updatedInput.equals(inputProcessed)) {
          for (Pair<String, String> valueReplaced : valuesReplaced) {
            analysis(valueReplaced.getLeft(), valueReplaced.getRight());
          }

          // Replace the start and end marks used for the values without quotes
          // This is needed to recover the original format
          updatedInput = unescapeQuotedVariablesWithMarks(updatedInput);
          if (AnalysisReporter.canCorrelate()) {
            return updatedInput;
          } else {
            return input;
          }

        }
      }
    }
    return input; // When none of previous logic generate a return, return default input
  }

  private void analysis(String literalMatched, String currentVariableName) {
    AnalysisReporter.report(this, literalMatched, currentSampler, currentVariableName);
  }

  private String searchVariable(JMeterVariables vars, String value, String replacementString) {
    int varNr = 0;
    Function<String, String> expressionProvider = replaceExpressionProvider();
    while (varNr <= context.getVariableCount(variableName)) {
      String varName = varNr == 0 ? variableName : variableName + "#" + varNr;
      String varMatchesCount = vars.get(varName + "_matchNr");
      int matchNr = varMatchesCount == null ? 0 : Integer.parseInt(varMatchesCount);
      if (matchNr == 0) {
        String computedVal =
            replacementString.isEmpty() ? vars.get(varName) : computeStringReplacement(varName);
        if (computedVal != null && computedVal.equals(value)) {
          return replacementString.isEmpty() ? expressionProvider.apply(varName)
              : expressionProvider.apply(buildReplacementStringForMultivalued(varName));
        }
      }
      int varMatch = 1;
      while (varMatch <= matchNr) {
        String varNameMatch = varName + "_" + varMatch;
        String computedVal =
            replacementString.isEmpty() ? vars.get(varNameMatch)
                : computeStringReplacement(varNameMatch);
        if (computedVal != null && computedVal.equals(value)) {
          return replacementString.isEmpty() ? expressionProvider.apply(varNameMatch)
              : expressionProvider.apply(buildReplacementStringForMultivalued(varNameMatch));
        }
        varMatch += 1;
      }
      varNr++;
    }
    return null;
  }

  private String escapeUnquotedVariablesWithMarks(String json) {
    // Try to escape unquoted variable/function in json
    // this try to allow a json parse without error for json path evaluation

    String regex = "[^(\"](\\$\\{.+?\\})(?=[^)\"])";

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(json);

    StringBuffer result = new StringBuffer();
    // Search the variable/function and escape with the particular pre-fix / sub-fix
    // the usage of this format allow to recover the original format
    while (matcher.find()) {
      matcher.appendReplacement(result,
          "\"" + ESCAPE_QUOTE_LEFT + "$1" + ESCAPE_QUOTE_RIGHT + "\"");
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private String unescapeQuotedVariablesWithMarks(String json) {
    // Recover the format, values quoted with special marks
    // are transformed to unquote value
    return json.replace(
        "\"" + ESCAPE_QUOTE_LEFT,
        "").replace(
        ESCAPE_QUOTE_RIGHT + "\"", "");
  }

  String buildReplacementStringForMultivalued(String varNameMatch) {
    if (replacementString != null && replacementString.contains(variableName)) {
      return replacementString.replace(variableName, varNameMatch);
    }
    return replacementString;
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return BaseCorrelationContext.class;
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setProperty(REPLACEMENT_JSON_PROPERTY_NAME, jsonpath);
    testElem.setProperty(REPLACEMENT_STRING_PROPERTY_NAME, replacementString);
    testElem.setProperty(REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME, ignoreValue);
  }

  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult
      result,
      JMeterVariables vars) {
    if (jsonpath.isEmpty()) {
      return;
    }
    currentSampler = sampler;
    super.process(sampler, children, result, vars);
  }

  @Override
  public void update(CorrelationRuleTestElement testElem) {
    jsonpath = testElem.getPropertyAsString(REPLACEMENT_JSON_PROPERTY_NAME);
    replacementString = testElem.getPropertyAsString(REPLACEMENT_STRING_PROPERTY_NAME);
    ignoreValue = testElem.getPropertyAsBoolean(REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME);
  }

  @Override
  public String toString() {
    return "RegexCorrelationReplacement{" + ", jsonpath='" + jsonpath + "'"
        + ", replacementString='" + replacementString + "'" + ", ignoreValue=" + ignoreValue
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JsonCorrelationReplacement)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    JsonCorrelationReplacement<?> that = (JsonCorrelationReplacement<?>) o;
    return ignoreValue == that.ignoreValue && Objects.equals(jsonpath, that.jsonpath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), jsonpath, ignoreValue);
  }

  @VisibleForTesting
  public void setExpressionEvaluator(Function<String, String> expressionEvaluator) {
    this.expressionEvaluator = expressionEvaluator;
  }
}
