package com.blazemeter.jmeter.correlation.core.extractors;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.jmeter.extractor.json.jmespath.JMESPathExtractor;
import org.apache.jmeter.extractor.json.jmespath.gui.JMESPathExtractorGui;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;

public class JsonCorrelationExtractor<T extends BaseCorrelationContext> extends
    CorrelationExtractor<T> {
  private static final String DEFAULT_MATCH_NUMBER_NAME = "match number";
  private static final int DEFAULT_MATCH_NUMBER = 1;
  private static final String EXTRACTOR_PATH_NAME = EXTRACTOR_PREFIX + "path";
  private static final String MATCH_NUMBER_NAME = EXTRACTOR_PREFIX + "matchNr";
  private static final String EXTRACTOR_DESCRIPTION = "JSON extractor";
  private static final String MATCH_NUMBER_DESCRIPTION = "Match number";
  private static final String PATH_DEFAULT_VALUE = "$.param";
  private static final ResultField DEFAULT_TARGET_VALUE = ResultField.BODY;
  private String path;
  private int matchNr;

  public JsonCorrelationExtractor() {
    matchNr = DEFAULT_MATCH_NUMBER;
    path = PATH_DEFAULT_VALUE;
    target = ResultField.BODY;
  }

  @VisibleForTesting
  public JsonCorrelationExtractor(String path, String variableName) {
    this.path = path;
    this.variableName = variableName;
    target = ResultField.BODY;
    matchNr = DEFAULT_MATCH_NUMBER;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String getDisplayName() {
    return "JSON";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList(path, Integer.toString(matchNr), target.name());
  }

  @Override
  public void setParams(List<String> params) {
    path = params.size() > 0 ? params.get(0) : "$.param";
    matchNr = params.size() > 1 ? parseInteger(params.get(1), DEFAULT_MATCH_NUMBER_NAME,
        DEFAULT_MATCH_NUMBER) : DEFAULT_MATCH_NUMBER;
    target = params.size() > 3 && !params.get(3).isEmpty() ? ResultField.valueOf(params.get(3))
        : DEFAULT_TARGET_VALUE;
  }

  @Override
  public List<ParameterDefinition> getParamsDefinition() {
    return Arrays.asList(new ParameterDefinition.TextParameterDefinition(EXTRACTOR_PATH_NAME,
            EXTRACTOR_DESCRIPTION,
            REGEX_DEFAULT_VALUE),
        new ParameterDefinition.TextParameterDefinition(MATCH_NUMBER_NAME, MATCH_NUMBER_DESCRIPTION,
            String.valueOf(DEFAULT_MATCH_NUMBER), true),
        new ParameterDefinition.ComboParameterDefinition(TARGET_FIELD_NAME,
            TARGET_FIELD_DESCRIPTION,
            ResultField.BODY.name(), ResultField.getNamesToCodesMapping(), true));
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setProperty(EXTRACTOR_PATH_NAME, path);
    testElem.setProperty(MATCH_NUMBER_NAME, String.valueOf(matchNr));
    testElem.setProperty(TARGET_FIELD_NAME,
        target != null ? target.name() : ResultField.BODY.name());
  }

  @Override
  public void update(CorrelationRuleTestElement testElem) {
    super.update(testElem);
    path = testElem.getPropertyAsString(EXTRACTOR_PATH_NAME);
    matchNr = getMatchNumber(testElem);
  }

  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
                      JMeterVariables vars) {

    // If the response starts with a BOM, we need to add a PostProcessor to remove it
    // If the response can't be parsed as JSON, we short-circuit and don't process it
    // If we can successfully parse the JSON and apply the path, we add a PostProcessor
    // to extract the value

    JSONPostProcessor jsonPostProcessor = new JSONPostProcessor();
    jsonPostProcessor.setJsonPathExpressions(path);
    jsonPostProcessor.setComputeConcatenation(true);
  }

  public void setRefName(String name) {
    super.setVariableName(name);
  }

  private static int getMatchNumber(CorrelationRuleTestElement testElem) {
    return parseInteger(testElem.getPropertyAsString(MATCH_NUMBER_NAME), DEFAULT_MATCH_NUMBER_NAME,
        1);
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return BaseCorrelationContext.class;
  }

  public String getPath() {
    return path;
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
        + "path='" + path + '\''
        + ", matchNr=" + matchNr
        + ", variableName='" + variableName + '\''
        + ", target=" + target
        + '}';
  }

  @Override
  public List<AbstractTestElement> createPostProcessors(String variableName, int i) {
    List<AbstractTestElement> extractors = new ArrayList<>();
    JMESPathExtractor extractor = new JMESPathExtractor();
    extractor.setProperty(TestElement.GUI_CLASS, JMESPathExtractorGui.class.getName());
    extractor.setName("JSON Extractor (" + variableName + ")");
    extractor.setRefName(variableName);
    extractor.setJmesPathExpression(path);
    extractor.setMatchNumber(String.valueOf(matchNr));
    extractor.setDefaultValue(variableName + "_NOT_FOUND");
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
        && Objects.equals(path, that.path)
        && Objects.equals(variableName, that.variableName)
        && target == that.target;
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, matchNr, variableName, target);
  }
}
