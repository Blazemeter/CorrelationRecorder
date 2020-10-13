package com.blazemeter.jmeter.correlation.siebel;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.TextParameterDefinition;
import com.blazemeter.jmeter.correlation.core.RegexMatcher;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.siebel.SiebelContext.Field;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;

/**
 * Siebel CRM Custom Extension that handles the replacements of the Parameters based on the
 * existence of the values in the Siebel Correlation Context.
 *
 * @see com.blazemeter.jmeter.correlation.siebel.SiebelContext
 */
public class SiebelRowParamsCorrelationReplacement extends
    RegexCorrelationReplacement<SiebelContext> {

  private final String DEFAULT_ROW_VAR_PREFIX = "SWERowId";
  @JsonIgnore
  private String rowVarPrefix;

  // Constructor added in order to satisfy json conversion
  public SiebelRowParamsCorrelationReplacement() {
    rowVarPrefix = DEFAULT_ROW_VAR_PREFIX;
  }

  public SiebelRowParamsCorrelationReplacement(String regex) {
    super(regex);
    rowVarPrefix = DEFAULT_ROW_VAR_PREFIX;
  }

  @Override
  public String getDisplayName() {
    return "Siebel Row Params";
  }

  @Override
  public List<String> getParams() {
    return Collections.singletonList(regex);
  }

  @Override
  public List<ParameterDefinition> getParamsDefinition() {
    return Collections.singletonList(new TextParameterDefinition(REPLACEMENT_REGEX_PROPERTY_NAME,
        REPLACEMENT_REGEX_PROPERTY_DESCRIPTION, REGEX_DEFAULT_VALUE));
  }

  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
      JMeterVariables vars) {
    String rowId = new RegexMatcher(regex, 1).findMatch(result.getSamplerData(), 1);
    rowVarPrefix = context.getRowVars().get(rowId);
    super.process(sampler, children, result, vars);
  }

  /**
   * Handles the replacement of the row fields parameters
   *
   * Receives an input and apply replacement for all known Siebel CRM row fields parameters (the
   * ones following the s_\d+\d+\d+_\d+ pattern)'. The rowId comes from the ones stored into the
   * {@link SiebelContext}.
   *
   * Works together with the
   * {@link com.blazemeter.jmeter.correlation.siebel.SiebelRowCorrelationExtractor}
   *
   * @param input the input against the condition will test and replacements will be applied
   * @param regex the regular expression used to eval the input
   * @param variableName the variable name ignored in this case
   * @param vars the stored values during the recording
   * @return the resultant input. Will be the same if it doesn't meet the condition
   */
  @Override
  protected String replaceWithRegex(String input, String regex, String variableName,
      JMeterVariables vars)
      throws MalformedPatternException {
    for (Map.Entry<String, Field> entry : context.getParamRowFields().entrySet()) {
      /*
      we remove _\d+$ from param names and then add same regex, since when navigating rows the last
      index is the position of the row and is dynamic.
       */
      String paramRegex =
          Perl5Compiler.quotemeta(entry.getKey().replaceAll("_\\d+$", "")) + "_\\d+=(.*)";
      String varName = rowVarPrefix + "_" + (entry.getValue().getPosition() + 1);
      String varValue = vars.get(varName);
      Predicate<String> matchCondition = match -> varValue != null && varValue
          .equals(match.replaceAll(entry.getValue().getIgnoredCharsRegex(), ""));
      input = replaceWithRegexAndPredicate(input, paramRegex, varName, matchCondition);
    }
    return input;
  }

  @Override
  public String toString() {
    return "SiebelRowParamsCorrelationReplacement{" +
        "paramValues=" + getParams() + '}';
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return SiebelContext.class;
  }
}
