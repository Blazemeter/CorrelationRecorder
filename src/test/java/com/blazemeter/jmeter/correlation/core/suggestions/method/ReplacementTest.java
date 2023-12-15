package com.blazemeter.jmeter.correlation.core.suggestions.method;

import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.util.List;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;

public class ReplacementTest {
  protected static final String REFERENCE_NAME = "x-csrf-token#1";
  protected static final String VARIABLE_NAME = "x-csrf-token";
  protected static final String REQUEST_REGEX = "=([^&]+)";
  protected static final String REQUEST_REGEX_WITHOUT_GROUP = "=[^&]+";
  protected static final String PARAM_NAME = "x-csrf-token";
  protected static final String PARAM_VALUE = "a6e9ad8f-cc16-42b0-b189-8dd1ea531cf4";
  protected static final String HEADER_REGEX_SUGGESTED = "x-csrf-token: ([^\n]+)";
  protected static final String HEADER_REGEX_GENERATED = "x-csrf-token: ([^&]+)";
  protected static HeaderManager headerManager;

  protected static List<TestElement> children;
  protected RegexCorrelationReplacement<BaseCorrelationContext> replacer;
  protected JMeterVariables vars;
  protected HTTPSampler sampler;
}
