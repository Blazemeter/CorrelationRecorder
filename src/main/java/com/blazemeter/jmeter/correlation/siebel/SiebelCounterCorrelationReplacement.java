package com.blazemeter.jmeter.correlation.siebel;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.RegexMatcher;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import java.util.List;
import java.util.UUID;
import org.apache.jmeter.modifiers.JSR223PreProcessor;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;

public class SiebelCounterCorrelationReplacement extends
    RegexCorrelationReplacement<SiebelContext> {

  // Constructor added in order to satisfy json conversion
  public SiebelCounterCorrelationReplacement() {
  }

  public SiebelCounterCorrelationReplacement(String regex) {
    super(regex);
  }

  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
      JMeterVariables vars) {
    if (regex.isEmpty()) {
      return;
    }
    String match = new RegexMatcher(regex, 1).findMatch(result.getSamplerData(), 1);
    if (match != null) {
      int count = Integer.parseInt(match);
      Integer counter = context.getCounter();
      if (counter == null || counter != count) {
        children.add(createPreProcessor(counter, count));
        vars.put(getVariableName(), String.valueOf(count));
      }
      super.process(sampler, children, result, vars);
    }
  }

  private JSR223PreProcessor createPreProcessor(Integer counter, int count) {
    String variableName = getVariableName();
    JSR223PreProcessor jSR223PreProcessor = new JSR223PreProcessor();
    jSR223PreProcessor.setProperty(JSR223PreProcessor.GUI_CLASS, TestBeanGUI.class.getName());
    jSR223PreProcessor.setName(String.format("Calculate %s", variableName));
    StringBuilder script = new StringBuilder();
    if (counter == null) {
      script.append(String.format("int %s_var = %d;\n", variableName, count));
    } else if (counter < count) {
      script.append(String
          .format("int %1$s_var = Integer.valueOf(vars.get(\"%1$s\")) + %2$d;\n", variableName,
              count - counter));
    } else if (counter > count) {
      script.append(String
          .format("int %1$s_var = Integer.valueOf(vars.get(\"%1$s\")) - %2$d;\n", variableName,
              counter - count));
    }
    context.setCounter(count);
    script.append(String.format("vars.put(\"%1$s\", String.valueOf(%1$s_var));\n", variableName));
    jSR223PreProcessor.setProperty("script", script.toString());
    jSR223PreProcessor.setProperty("cacheKey", UUID.randomUUID().toString());
    return jSR223PreProcessor;
  }

  @Override
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    super.updateTestElem(testElem);
    testElem.setProperty(REPLACEMENT_REGEX_PROPERTY_NAME, regex);
  }

  @Override
  public void update(CorrelationRuleTestElement testElem) {
    super.update(testElem);
    regex = testElem.getPropertyAsString(REPLACEMENT_REGEX_PROPERTY_NAME);
  }

  @Override
  public String toString() {
    return "SiebelCounterCorrelationReplacement{" +
        "regex='" + regex + '\'' +
        '}';
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return SiebelContext.class;
  }
}
