package com.blazemeter.jmeter.correlation.siebel;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.ComboParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.TextParameterDefinition;
import com.blazemeter.jmeter.correlation.core.RegexMatcher;
import com.blazemeter.jmeter.correlation.core.ResultField;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.jmeter.extractor.JSR223PostProcessor;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Correlation Extractor that gets the value of the Siebel CRM Context in every response and
 * extracts its rows. Applies parsing Array function with the regex, adding post processors for each
 * matching string.
 */
public class SiebelRowCorrelationExtractor extends RegexCorrelationExtractor<SiebelContext> {

  private static final Logger LOG = LoggerFactory.getLogger(SiebelRowCorrelationExtractor.class);
  private static final ResultField DEFAULT_TARGET = ResultField.BODY;

  // Constructor added in order to satisfy json conversion
  public SiebelRowCorrelationExtractor() {
  }

  public SiebelRowCorrelationExtractor(String regex) {
    super(regex, -1, DEFAULT_TARGET);
  }

  public SiebelRowCorrelationExtractor(String regex, int group) {
    super(regex, group, -1, DEFAULT_TARGET);
  }

  /**
   * Constructor that receives only strings to create the Correlation Extractor. Needed to create
   * from the values in the UI.
   *
   * @param regex Regular Expression used to extract the value
   * @param groupNumber Group Number from where the information will be extracted in the regex
   * @param targetName Target Field where the extraction will be applied
   */
  public SiebelRowCorrelationExtractor(String regex, String groupNumber, String targetName) {
    super(regex, Integer.parseInt(groupNumber), -1,
        ResultField.valueOf(targetName));
  }

  //Added to support backward compatibility with beta version
  @Deprecated
  public SiebelRowCorrelationExtractor(String regex, String groupNumber, String targetName,
      String matchNumber) {
    super(regex, Integer.parseInt(groupNumber), Integer.parseInt(matchNumber),
        ResultField.valueOf(targetName));
  }

  @Override
  public String getDisplayName() {
    return "Siebel Row";
  }

  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
      JMeterVariables vars) {
    super.process(sampler, children, result, vars);
    vars.remove(variableName);
    JSR223PostProcessor jsr223PostProcessor = buildArrayParserPostProcessor(result, vars);
    if (jsr223PostProcessor != null) {
      children.add(jsr223PostProcessor);
    }
  }

  private JSR223PostProcessor buildArrayParserPostProcessor(SampleResult result,
      JMeterVariables vars) {
    StringBuilder script = new StringBuilder();
    JSR223PostProcessor jSR223PostProcessor = new JSR223PostProcessor();
    jSR223PostProcessor.setProperty(JSR223PostProcessor.GUI_CLASS, TestBeanGUI.class.getName());
    jSR223PostProcessor.setName("Parse Array Values");
    script.append("import com.blazemeter.jmeter.correlation.siebel.SiebelArrayFunction;\n\n");
    script.append("String stringToSplit = \"\";\n");
    script.append("String rowId = \"\";");
    int matchNumber = 1;
    for (String match : new RegexMatcher(regex, groupNr)
        .findMatches(target.getField(result))) {
      if (match == null) {
        continue;
      }
      try {
        String varNamePrefix = variableName + context.getNextRowPrefixId();
        SiebelArrayFunction.split(match, varNamePrefix, vars);
        int numberOfVariables = Integer.parseInt(vars.get(varNamePrefix + "_n"));
        script
            .append(String.format("\n\n// Parsing Star Array parameter(s) using match number %1$d\n"
                    + "stringToSplit = vars.get(\"%2$s_%1$d\");\n"
                    + "if (stringToSplit != null) {\n"
                    + "\tSiebelArrayFunction.split(stringToSplit, \"%3$s\", vars);\n"
                    + "\trowIdValue = vars.get(\"%3$s_%4$d\");\n"
                    + "\tvars.put(\"%3$s_rowId\", rowIdValue);"
                    + "\n}", matchNumber, variableName, varNamePrefix,
                numberOfVariables));
        String rowId = vars.get(varNamePrefix + "_" + numberOfVariables);
        context.addRowVar(rowId, varNamePrefix);
        vars.put(varNamePrefix + "_rowId", rowId);
        matchNumber++;
      } catch (IllegalArgumentException e) {
        LOG.warn(e.getMessage());
      }
    }
    jSR223PostProcessor.setProperty("script", script.toString());
    jSR223PostProcessor.setProperty("cacheKey", UUID.randomUUID().toString());
    return matchNumber > 1 ? jSR223PostProcessor : null;
  }

  @Override
  public String toString() {
    return "SiebelRowCorrelationExtractor{" +
        "paramValues=" + getParams() +
        '}';
  }

  public List<ParameterDefinition> getParamsDefinition() {
    return Arrays.asList(new TextParameterDefinition(EXTRACTOR_REGEX_NAME,
            EXTRACTOR_REGEX_DESCRIPTION,
            REGEX_DEFAULT_VALUE),
        new TextParameterDefinition(GROUP_NUMBER_NAME, GROUP_NUMBER_DESCRIPTION, String.valueOf(1)),
        new ComboParameterDefinition(TARGET_FIELD_NAME, TARGET_FIELD_DESCRIPTION,
            ResultField.BODY.name(),
            ResultField.getNamesToCodesMapping()));
  }

  @Override
  public List<String> getParams() {
    return Arrays
        .asList(regex, Integer.toString(groupNr), target.name());
  }

  @Override
  public void setParams(List<String> params) {
    regex = !params.isEmpty() ? params.get(0) : REGEX_DEFAULT_VALUE;
    setGroupNr(params.size() > 1 ? parseInteger(params.get(1), DEFAULT_MATCH_GROUP_NAME, 
        DEFAULT_MATCH_GROUP) : DEFAULT_MATCH_GROUP);
    //This means it applies to all the matches
    matchNr = -1;
    int targetParamIndex = 3;
    target =
        params.size() > targetParamIndex && !params.get(targetParamIndex).isEmpty() ? ResultField
            .valueOf(params.get(targetParamIndex)) : DEFAULT_TARGET_VALUE;
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setExtractorClass(getClass());
  }

  public void setRegex(String regex) {
    this.regex = regex;
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return SiebelContext.class;
  }

  @Override
  public boolean equals(Object o) {
    if (o.getClass() != getClass()) {
      return false;
    }
    return super.equals(o);
  }
}
