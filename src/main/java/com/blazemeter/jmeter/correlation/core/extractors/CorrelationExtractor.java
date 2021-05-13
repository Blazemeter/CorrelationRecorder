package com.blazemeter.jmeter.correlation.core.extractors;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.DescriptionContent;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationRuleSerializationPropertyFilter;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeInfo(use = Id.CLASS, property = "type")
@JsonFilter(CorrelationRuleSerializationPropertyFilter.FILTER_ID)
/**
 * Serves as a base for all the Correlation Extractors.
 *
 * <p>Contains all the methods from loading, saving and processing responses from the server.
 *
 * <p>For a more detailed explanation on Correlation Extractors, their usages and methods, please 
 * read the 
 * <a href="https://github.com/Blazemeter/CorrelationRecorder/blob/master/README.md">readme</a>.
 *
 * <p>Along side {@link com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement}
 * and the Reference Variable, it is a part of the
 * {@link com.blazemeter.jmeter.correlation.core.CorrelationRule}
 */
public abstract class CorrelationExtractor<T extends CorrelationContext> extends
    CorrelationRulePartTestElement<T> {

  @JsonIgnore
  protected static final String EXTRACTOR_PREFIX = "CorrelationRule.CorrelationExtractor.";
  protected static final String TARGET_FIELD_NAME = EXTRACTOR_PREFIX + "target";
  protected static final String TARGET_FIELD_DESCRIPTION = "Target";
  protected static final String REGEX_DEFAULT_VALUE = "param=\"(.+?)\"";
  private static final Logger LOG = LoggerFactory
      .getLogger(CorrelationRuleSerializationPropertyFilter.class);
  public transient String variableName;
  protected ResultField target;

  /**
   * Default constructor added in order to satisfy the JSON conversion.
   *
   * <p>Implementing a Custom Correlation Extractor requires to always define a default constructor
   */
  public CorrelationExtractor() {
  }

  public CorrelationExtractor(ResultField target) {
    this.target = target;
  }

  protected static int parseInteger(String parsingValue, String name, int defaultValue) {
    int parsedValue = defaultValue;
    if (parsingValue != null && !parsingValue.isEmpty()) {
      try {
        parsedValue = Integer.parseInt(parsingValue);
      } catch (NumberFormatException e) {
        LOG.warn("Wrong format for {}={}, using default value '{}' instead.", name, parsingValue,
            defaultValue, e);
      }
    } else {
      LOG.warn("Wrong format {} for '{}', using default value '{}' instead.", name, defaultValue,
          parsingValue == null ? null : "empty");
    }

    return parsedValue;
  }

  @Override
  public String getDescription() {
    return DescriptionContent.getFromClass(getClass());
  }

  /**
   * Handles the saving of values of the Correlation Extractor into {@link
   * CorrelationRuleTestElement} for later storage in Test Plans and CorrelationTemplates.
   *
   * <p>This method has to be overwritten when implementing custom Correlation Extractors, 
   * otherwise, only the target field will be saved
   *
   * @param testElem CorrelationRuleTestElement where the fields will be stored
   */
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    testElem.setProperty(TARGET_FIELD_NAME,
        getTarget() != null ? getTarget().name() : ResultField.BODY.name());
    testElem.setExtractorClass((Class<? extends CorrelationExtractor<?>>) getClass());
  }

  /**
   * Handles the loading of values from Test Plans and CorrelationTemplates.
   *
   * <p>Gets the values using the property names used to store it on the <code>updateTestElem
   * (CorrelationRuleTestElement testElem)</code> method. This method has to be overwritten when 
   * implementing custom Correlation Extractors, otherwise, only target field will be loaded
   *
   * @param testElem CorrelationRuleTestElement from which the values are obtained
   */
  public void update(CorrelationRuleTestElement testElem) {
    target = ResultField.valueOf(testElem.getPropertyAsString(TARGET_FIELD_NAME));
  }

  /**
   * Process every response obtained from the server, after a request is made.
   *
   * <p>The logic for extracting the desired values will be contained here. This method has to be
   * implemented when creating custom Correlation Extractors. This method is expected to add 
   * children to the sampler to the Test Plan, in order to get the desire information from the 
   * SampleResult, during the replay as well.
   *
   * @param sampler recorded sampler containing the information of the request
   * @param children list of children added to the sampler (if the condition is matched, a component
   * will be added to it)
   * @param result result containing information about request and associated response from server
   * @param vars stored variables shared between requests during recording
   * @see
   * <a href="https://github.com/Blazemeter/CorrelationRecorder/examples/CustomCorrelationExtractor#process">
   * Process Example Explanation</a>
   */
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
    return "CorrelationReplacement{"
        + " replacementClass=" + getClass().getName()
        + " , paramValues='" + getParams() + "'}";
  }
}
