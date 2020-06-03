package com.blazemeter.jmeter.correlation.core;

import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Objects;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;

public class CorrelationRule {

  private final String referenceName;
  private final CorrelationExtractor correlationExtractor;
  private final CorrelationReplacement correlationReplacement;

  public CorrelationRule(String referenceName, String extractorRegex, String replacementRegex) {
    this(referenceName, new RegexCorrelationExtractor(extractorRegex),
        new RegexCorrelationReplacement(replacementRegex));
  }

  public CorrelationRule(String referenceName, CorrelationExtractor correlationExtractor,
      CorrelationReplacement correlationReplacement) {
    this.referenceName = referenceName;
    this.correlationExtractor = correlationExtractor;
    if (correlationExtractor != null) {
      correlationExtractor.setVariableName(referenceName);
    }
    this.correlationReplacement = correlationReplacement;
    if (correlationReplacement != null) {
      correlationReplacement.setVariableName(referenceName);
    }
  }

  // Constructor added in order to satisfy json conversion
  public CorrelationRule() {
    referenceName = "";
    correlationExtractor = null;
    correlationReplacement = null;
  }

  public CorrelationRuleTestElement buildTestElement() {
    CorrelationRuleTestElement testElem = new CorrelationRuleTestElement();
    testElem.setReferenceName(referenceName);

    if (correlationExtractor != null) {
      correlationExtractor.updateTestElem(testElem);
    }
    if (correlationReplacement != null) {
      correlationReplacement.updateTestElem(testElem);
    }
    return testElem;
  }

  public void applyReplacements(HTTPSamplerBase sampler, List<TestElement> children,
      SampleResult result,
      JMeterVariables vars) {
    if (correlationReplacement != null) {
      correlationReplacement.process(sampler, children, result, vars);
    }
  }

  public void addExtractors(HTTPSamplerBase sampler, List<TestElement> children,
      SampleResult result,
      JMeterVariables vars) {
    if (correlationExtractor != null) {
      correlationExtractor.process(sampler, children, result, vars);
    }
  }

  public CorrelationExtractor getCorrelationExtractor() {
    return correlationExtractor;
  }

  public CorrelationReplacement getCorrelationReplacement() {
    return correlationReplacement;
  }

  public String getReferenceName() {
    return referenceName;
  }

  @VisibleForTesting
  @Override
  public String toString() {
    return "CorrelationRule{" +
        "referenceName='" + referenceName + '\'' +
        ", correlationExtractor=" + correlationExtractor +
        ", correlationReplacement=" + correlationReplacement +
        '}';
  }

  @VisibleForTesting
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CorrelationRule that = (CorrelationRule) o;
    return Objects.equals(referenceName, that.referenceName) &&
        Objects.equals(correlationExtractor, that.correlationExtractor) &&
        Objects.equals(correlationReplacement, that.correlationReplacement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(referenceName, correlationExtractor, correlationReplacement);
  }
}
