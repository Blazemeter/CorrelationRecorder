package com.blazemeter.jmeter.correlation.core.extractors;

import static com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils.jsonFindMatches;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.analysis.AnalysisReporter;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonCorrelationExtractor<T extends BaseCorrelationContext> extends
    CorrelationExtractor<T> {
  private static final String DEFAULT_MATCH_NUMBER_NAME = "match number";
  private static final int DEFAULT_MATCH_NUMBER = 1;
  private static final String EXTRACTOR_PATH_NAME = EXTRACTOR_PREFIX + "jsonpath";
  private static final String MATCH_NUMBER_NAME = EXTRACTOR_PREFIX + "matchNr";
  private static final String MULTIVALUED_NAME = EXTRACTOR_PREFIX + "multiValued";
  private static final String EXTRACTOR_DESCRIPTION = "JSONPath expression";
  private static final String MATCH_NUMBER_DESCRIPTION = "Match number";
  private static final String MULTIVALUED_DESCRIPTION = "Multivalued";
  private static final boolean DEFAULT_MULTIVALUED = false;
  private static final String PATH_DEFAULT_VALUE = "$.jsonpath.expression";
  private static final ResultField DEFAULT_TARGET_VALUE = ResultField.BODY;

  private static final Logger LOG = LoggerFactory.getLogger(JsonCorrelationExtractor.class);
  private static final String JSON_EXTRACTOR_GUI_CLASS =
      org.apache.jmeter.extractor.json.jsonpath.gui.JSONPostProcessorGui.class.getName();
  protected String jsonpath;
  protected int matchNr;
  protected boolean multiValued;

  private transient JMeterVariables currentVars;
  private transient List<TestElement> currentSamplersChild;

  public JsonCorrelationExtractor() {
    matchNr = DEFAULT_MATCH_NUMBER;
    jsonpath = PATH_DEFAULT_VALUE;
    target = ResultField.BODY;
    multiValued = DEFAULT_MULTIVALUED;
  }

  @VisibleForTesting
  public JsonCorrelationExtractor(String path, String variableName) {
    this.jsonpath = path;
    this.variableName = variableName;
    target = ResultField.BODY;
    matchNr = DEFAULT_MATCH_NUMBER;
  }

  @VisibleForTesting
  public JsonCorrelationExtractor(String path, int matchNr, ResultField target) {
    this.jsonpath = path;
    this.target = target;
    this.matchNr = matchNr;
  }

  @VisibleForTesting
  public JsonCorrelationExtractor(String path) {
    this.jsonpath = path;
    this.matchNr = DEFAULT_MATCH_NUMBER;
    this.target = ResultField.BODY;
    this.multiValued = DEFAULT_MULTIVALUED;
  }

  @VisibleForTesting
  public JsonCorrelationExtractor(String path, int matchNr, String target, String multiValued) {
    this.jsonpath = path;
    this.matchNr = matchNr;
    this.target = ResultField.valueOf(target);
    this.multiValued = Boolean.parseBoolean(multiValued);
  }

  public void setPath(String path) {
    this.jsonpath = path;
  }

  @Override
  public String getDisplayName() {
    return "JSON";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList(jsonpath, Integer.toString(matchNr), target.name(),
        Boolean.toString(multiValued));
  }

  @Override
  public void setParams(List<String> params) {
    jsonpath = params.size() > 0 ? params.get(0) : "$.jsonpath.expression";
    matchNr = params.size() > 1 ? parseInteger(params.get(1), DEFAULT_MATCH_NUMBER_NAME,
        DEFAULT_MATCH_NUMBER) : DEFAULT_MATCH_NUMBER;
    target = params.size() > 2 && !params.get(2).isEmpty() ? ResultField.valueOf(params.get(2))
        : DEFAULT_TARGET_VALUE;
    multiValued = params.size() >= 3 && Boolean.parseBoolean(params.get(3));
  }

  @Override
  public List<ParameterDefinition> getParamsDefinition() {
    HashMap<String, String> options = new HashMap<String, String>();
    options.put(ResultField.BODY.name(), ResultField.BODY.getCode());
    return Arrays.asList(new ParameterDefinition.TextParameterDefinition(EXTRACTOR_PATH_NAME,
            EXTRACTOR_DESCRIPTION,
            PATH_DEFAULT_VALUE),
        new ParameterDefinition.TextParameterDefinition(MATCH_NUMBER_NAME, MATCH_NUMBER_DESCRIPTION,
            String.valueOf(DEFAULT_MATCH_NUMBER), true),
        new ParameterDefinition.ComboParameterDefinition(TARGET_FIELD_NAME,
            TARGET_FIELD_DESCRIPTION,
            ResultField.BODY.name(), options, true),
        new ParameterDefinition.CheckBoxParameterDefinition(MULTIVALUED_NAME,
            MULTIVALUED_DESCRIPTION,
            DEFAULT_MULTIVALUED, true));
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setProperty(EXTRACTOR_PATH_NAME, jsonpath);
    testElem.setProperty(MATCH_NUMBER_NAME, String.valueOf(matchNr));
    testElem.setProperty(TARGET_FIELD_NAME,
        target != null ? target.name() : ResultField.BODY.name());
    testElem.setProperty(MULTIVALUED_NAME, multiValued);
  }

  @Override
  public void update(CorrelationRuleTestElement testElem) {
    super.update(testElem);
    jsonpath = testElem.getPropertyAsString(EXTRACTOR_PATH_NAME);
    matchNr = getMatchNumber(testElem);
  }

  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
                      JMeterVariables vars) {

    // If the response starts with a BOM, we need to add a PostProcessor to remove it
    // If the response can't be parsed as JSON, we short-circuit and don't process it
    // If we can successfully parse the JSON and apply the path, we add a PostProcessor
    // to extract the value

    if (jsonpath.isEmpty()) {
      return;
    }
    if (matchNr == 0) {
      LOG.warn("Extracting random appearances is not supported. Returning null instead.");
      return;
    }
    String field = target.getField(result);
    if (field == null || field.isEmpty()) {
      return;
    }

    this.currentVars = vars;
    this.currentSamplersChild = children;

    String varName = multiValued ? generateVariableName() : variableName;
    String matchedValue = null;
    String matchedVariable = varName;
    String matchedVariableChildPP = varName;
    String matchedVariablePP = varName;
    int matchedMatchNr = matchNr;
    if (matchNr >= 0) {
      String match = findMatch(field, matchNr);
      if (match != null && !match.equals(vars.get(varName))) {
        matchedValue = match;
      }
    } else {
      Pair<Class, ArrayList<String>> resultMatches = jsonFindMatches(field, jsonpath);
      ArrayList<String> matches = resultMatches.getRight();
      if (matches.size() == 1) {
        matchedValue = matches.get(0);
        matchedMatchNr = 1;
      } else if (matches.size() > 1) {
        if (!multiValued) {
          clearJMeterVariables(vars);
        }
        matchedValue = String.valueOf(matches.size());
        matchedVariableChildPP = matchedVariable + "_matchNr";
        int matchNr = 1;
        for (String match : matches) {
          vars.put(varName + "_" + matchNr, match);
          matchNr++;
        }
      }
    }
    if (matchedValue != null) {
      analyze(matchedValue, sampler, variableName);
      addVarAndChildPostProcessor(matchedValue, matchedVariableChildPP,
          createPostProcessor(matchedVariablePP, matchedMatchNr));
    }
  }

  public String findMatch(String input, int matchNumber) {
    if (!input.isEmpty()) {
      Pair<Class, ArrayList<String>> resultMatches = jsonFindMatches(input, jsonpath);
      ArrayList<String> matches = resultMatches.getRight();
      if (matches.size() == 0) {
        return null;
      }
      if (matches.size() >= matchNumber) {
        return matches.get(matchNumber - 1);
      }
      LOG.warn("Match number {} is bigger than actual matches {}",
          matchNumber, matches.size());
    }
    return null;
  }

  public AbstractTestElement createPostProcessor(String varName, int matchNr) {
    JSONPostProcessor jsonPostProcessor = new JSONPostProcessor();
    jsonPostProcessor.setProperty(TestElement.GUI_CLASS, JSON_EXTRACTOR_GUI_CLASS);
    jsonPostProcessor.setName("JSON Path - " + varName);
    jsonPostProcessor.setRefNames(varName);
    jsonPostProcessor.setJsonPathExpressions(jsonpath);
    jsonPostProcessor.setComputeConcatenation(true);
    jsonPostProcessor.setMatchNumbers(String.valueOf(matchNr));
    jsonPostProcessor.setDefaultValues(varName + DEFAULT_EXTRACTOR_SUFFIX);
    jsonPostProcessor.setScopeAll();
    return jsonPostProcessor;
  }

  public void setRefName(String name) {
    super.setVariableName(name);
  }

  private static int getMatchNumber(CorrelationRuleTestElement testElem) {
    return parseInteger(testElem.getPropertyAsString(MATCH_NUMBER_NAME),
        DEFAULT_MATCH_NUMBER_NAME,
        1);
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return BaseCorrelationContext.class;
  }

  public String getPath() {
    return jsonpath;
  }

  public int getMatchNr() {
    return matchNr;
  }

  public void setMatchNr(int matchNr) {
    this.matchNr = matchNr;
  }

  @Override
  public String toString() {
    return "JsonCorrelationExtractor{"
        + "path='" + jsonpath + '\''
        + ", matchNr=" + matchNr
        + ", variableName='" + variableName + '\''
        + ", target=" + target
        + '}';
  }

  private void addVarAndChildPostProcessor(String match, String variableName,
                                           AbstractTestElement postProcessor) {
    if (AnalysisReporter.canCorrelate()) {
      currentSamplersChild.add(postProcessor);
    }
    currentVars.put(variableName, match);
  }

  private String generateVariableName() {
    return variableName + "#" + context.getNextVariableNr(variableName);
  }

  public void setMultiValued(boolean multiValued) {
    this.multiValued = multiValued;
  }

  @Override
  public List<AbstractTestElement> createPostProcessors(String variableName, int i) {
    List<AbstractTestElement> extractors = new ArrayList<>();
    AbstractTestElement extractor = createPostProcessor(variableName, i);
    extractors.add(extractor);
    return extractors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JsonCorrelationExtractor<?> that = (JsonCorrelationExtractor<?>) o;
    return matchNr == that.matchNr
        && Objects.equals(jsonpath, that.jsonpath)
        && Objects.equals(variableName, that.variableName)
        && target == that.target;
  }

  @Override
  public int hashCode() {
    return Objects.hash(jsonpath, matchNr, variableName, target);
  }
}
