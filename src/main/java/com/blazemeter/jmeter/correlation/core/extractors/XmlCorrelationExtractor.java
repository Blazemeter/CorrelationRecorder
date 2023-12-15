package com.blazemeter.jmeter.correlation.core.extractors;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;

public class XmlCorrelationExtractor<T extends BaseCorrelationContext> extends
    CorrelationExtractor<T> {
  protected static final String XPATH_DEFAULT_VALUE = "$.param";
  protected static final String MATCH_NUMBER_DESCRIPTION = "Match number";

  private static final String MATCH_NUMBER_NAME = EXTRACTOR_PREFIX + "matchNr";
  private static final String EXTRACTOR_XPATH_NAME = EXTRACTOR_PREFIX + "xpath";
  private static final String DEFAULT_MATCH_NUMBER_NAME = "match number";
  private static final String EXTRACTOR_XPATH_DESCRIPTION = "XPath query";
  private static final int DEFAULT_MATCH_NUMBER = 1;
  private static final ResultField DEFAULT_TARGET_VALUE = ResultField.BODY;
  private String xpath;
  private int matchNr;

  public XmlCorrelationExtractor() {
    matchNr = DEFAULT_MATCH_NUMBER;
    xpath = XPATH_DEFAULT_VALUE;
    target = ResultField.BODY;
  }

  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
                      JMeterVariables vars) {
    System.out.println("XmlCorrelationExtractor.process");
  }

  public void setRefName(String name) {
    super.setVariableName(name);
  }

  public void setXPathQuery(String xmlPath) {
    this.xpath = xmlPath;
  }

  @Override
  public List<String> getParams() {
    return Arrays
        .asList(xpath, Integer.toString(matchNr), target.name());
  }

  @Override
  public void setParams(List<String> params) {
    xpath = params.size() > 0 ? params.get(0) : "$.param";
    matchNr = params.size() > 1 ? parseInteger(params.get(1), DEFAULT_MATCH_NUMBER_NAME,
        DEFAULT_MATCH_NUMBER) : DEFAULT_MATCH_NUMBER;
    target = params.size() > 3 && !params.get(3).isEmpty() ? ResultField.valueOf(params.get(3))
        : DEFAULT_TARGET_VALUE;
  }

  @Override
  public String getDisplayName() {
    return "XML";
  }

  public List<ParameterDefinition> getParamsDefinition() {
    return Arrays.asList(new ParameterDefinition.TextParameterDefinition(EXTRACTOR_XPATH_NAME,
            EXTRACTOR_XPATH_DESCRIPTION,
            REGEX_DEFAULT_VALUE),
        new ParameterDefinition.TextParameterDefinition(MATCH_NUMBER_NAME, MATCH_NUMBER_DESCRIPTION,
            String.valueOf(DEFAULT_MATCH_NUMBER), true),
        new ParameterDefinition.ComboParameterDefinition(TARGET_FIELD_NAME,
            TARGET_FIELD_DESCRIPTION, ResultField.BODY.name(),
            ResultField.getNamesToCodesMapping(), true));
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setProperty(EXTRACTOR_XPATH_NAME, xpath);
    testElem.setProperty(MATCH_NUMBER_NAME, String.valueOf(matchNr));
    testElem.setProperty(TARGET_FIELD_NAME,
        target != null ? target.name() : ResultField.BODY.name());
  }

  @Override
  public void update(CorrelationRuleTestElement testElem) {
    super.update(testElem);
    xpath = testElem.getPropertyAsString(EXTRACTOR_XPATH_NAME);
    matchNr = getMatchNumber(testElem);
  }

  private static int getMatchNumber(CorrelationRuleTestElement testElem) {
    return parseInteger(testElem.getPropertyAsString(MATCH_NUMBER_NAME), DEFAULT_MATCH_NUMBER_NAME,
        1);
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return BaseCorrelationContext.class;
  }

  @Override
  public String toString() {
    return "XmlCorrelationExtractor {"
        + "xmlPathExpression='" + xpath + '\''
        + ", variableName='" + variableName + '\'' +
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
    XmlCorrelationExtractor<?> that = (XmlCorrelationExtractor<?>) o;
    return matchNr == that.matchNr
        && Objects.equals(xpath, that.xpath)
        && Objects.equals(variableName, that.variableName)
        && target == that.target;
  }

  @Override
  public int hashCode() {
    return Objects.hash(xpath, matchNr, variableName, target);
  }
}
