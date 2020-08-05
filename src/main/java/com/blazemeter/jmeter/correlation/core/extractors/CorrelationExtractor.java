package com.blazemeter.jmeter.correlation.core.extractors;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ResultField;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationRuleSerializationPropertyFilter;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeInfo(use = Id.CLASS, property = "type")
@JsonFilter(CorrelationRuleSerializationPropertyFilter.FILTER_ID)
public abstract class CorrelationExtractor<T extends CorrelationContext> extends
    CorrelationRulePartTestElement<T> {

  @JsonIgnore
  protected static final String EXTRACTOR_PREFIX = "CorrelationRule.CorrelationExtractor.";
  protected static final String TARGET_FIELD_NAME = EXTRACTOR_PREFIX + "target";
  protected static final String TARGET_FIELD_DESCRIPTION = "Target";
  protected static final String REGEX_DEFAULT_VALUE = "";
  private static final Logger LOG = LoggerFactory
      .getLogger(CorrelationRuleSerializationPropertyFilter.class);
  public transient String variableName;
  protected ResultField target;

  // Constructor added in order to satisfy json conversion
  public CorrelationExtractor() {
  }

  public CorrelationExtractor(ResultField target) {
    this.target = target;
  }

  protected static int parseInteger(String parsingValue, String name, int defaultValue) {
    int parsedValue = defaultValue;
    if (parsingValue != null && !parsingValue.isEmpty()) {
      try {
        parsedValue = Integer.valueOf(parsingValue);
      } catch (NumberFormatException e) {
        LOG.warn("Wrong format for {}={}, using default value '{}' instead.", name, parsingValue,
            e);
      }
    } else {
      LOG.warn("Wrong format {} for '{}', using default value '{}' instead.",
          parsingValue == null ? null : "empty", name, defaultValue);
    }

    return parsedValue;
  }

  public void updateTestElem(CorrelationRuleTestElement testElem) {
    testElem.setProperty(TARGET_FIELD_NAME,
        getTarget() != null ? getTarget().name() : ResultField.BODY.name());
    testElem.setExtractorClass(getClass());
  }

  public void update(CorrelationRuleTestElement testElem) {
    target = ResultField.valueOf(testElem.getPropertyAsString(TARGET_FIELD_NAME));
  }

  public abstract void process(HTTPSamplerBase sampler, List<TestElement> children,
      SampleResult result, JMeterVariables vars);

  public ResultField getTarget() {
    return target;
  }

  public String getVariableName() {
    return variableName;
  }

  @JsonIgnore
  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  @Override
  public String getType() {
    return getClass().getCanonicalName();
  }

  @Override
  public String toString() {
    List<ParameterDefinition> paramsDefinition = getParamsDefinition();
    return "CorrelationReplacement{"
        + " replacementClass=" + getClass().getName()
        + " , variableName='" + variableName + "'"
        + " , params={" + IntStream
        .range(0, paramsDefinition.size())
        .mapToObj(i -> paramsDefinition.get(i).getName() + "=" + getParams().get(i))
        .collect(Collectors.joining(",")) + "}}";
  }
}
