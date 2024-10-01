package com.blazemeter.jmeter.correlation.custom.extension;

import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.ComboParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition.TextParameterDefinition;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterVariables;

/**
 * Dummy class to be used only for testing proposes
 */
@VisibleForTesting
public class CustomCorrelationExtractor extends RegexCorrelationExtractor<CustomContext> {

  private static final String FIELD_ONE_PROPERTY_NAME = EXTRACTOR_PREFIX + "fieldOne";
  private static final String FIELD_TWO_PROPERTY_NAME = EXTRACTOR_PREFIX + "fieldTwo";

  private static final String FIELD_ONE_DEFAULT_VALUE = "fieldOneDefault";

  private static final Map<String, String> fieldTwoValuesToDisplay =
      new HashMap<String, String>() {{
        put("Blue", "#3371FF ");
        put("Red", "#FF5733");
      }};

  private static final TextParameterDefinition fieldOneDefinition = new TextParameterDefinition(
      "fieldOne",
      "fieldOne", FIELD_ONE_DEFAULT_VALUE);


  private static final ComboParameterDefinition fieldTwoDefinition = new ComboParameterDefinition(
      "fieldTwo",
      "Field Two", "1", fieldTwoValuesToDisplay);

  private static final ParameterDefinition targetDefinition = new ParameterDefinition(
      TARGET_FIELD_NAME, TARGET_FIELD_DESCRIPTION, ResultField.BODY.getCode(),
      ResultField.getNamesToCodesMapping());


  private String fieldOne;
  private int fieldTwo;

  public CustomCorrelationExtractor() {
    fieldOne = FIELD_ONE_DEFAULT_VALUE;
  }

  public CustomCorrelationExtractor(String fieldOne, String fieldTwo, String target) {
    this.fieldOne = fieldOne;
    this.fieldTwo = Integer.parseInt(fieldTwo);
    this.target = ResultField.valueOf(target);
  }

  @Override
  public String getDisplayName() {
    return "Custom Correlation Extractor";
  }

  @Override
  public List<String> getParams() {
    return new ArrayList<>(Arrays.asList(fieldOne, Integer.toString(fieldTwo), target.getCode()));
  }

  @Override
  public void setParams(List<String> params) {
    fieldOne = !params.isEmpty() ? params.get(0) : FIELD_ONE_DEFAULT_VALUE;

    if (params.size() > 1) {
      fieldTwo = Integer.parseInt(params.get(1));
    }

    target = params.size() > 2 ? ResultField.valueOf(params.get(2)) : ResultField.BODY;
  }

  @Override
  public List<ParameterDefinition> getParamsDefinition() {
    return Arrays.asList(fieldOneDefinition, fieldTwoDefinition,
        targetDefinition);
  }

  public void updateTestElem(CorrelationRuleTestElement testElem) {
    testElem.setProperty(FIELD_ONE_PROPERTY_NAME, fieldOne);
    testElem.setProperty(FIELD_TWO_PROPERTY_NAME, fieldTwo);
    testElem.setProperty(TARGET_FIELD_NAME, target.getCode());
  }

  public void update(CorrelationRuleTestElement testElem) {
    fieldOne = testElem.getPropertyAsString(FIELD_ONE_PROPERTY_NAME);
    fieldTwo = testElem.getPropertyAsInt(FIELD_TWO_PROPERTY_NAME);
    target = ResultField
        .valueOf(testElem.getPropertyAsString(TARGET_FIELD_NAME, ResultField.BODY.getCode()));
  }

  @Override
  public void process(HTTPSamplerBase sampler, List children, SampleResult result,
      JMeterVariables vars) {
    super.process(sampler, children, result, vars);
  }
}
