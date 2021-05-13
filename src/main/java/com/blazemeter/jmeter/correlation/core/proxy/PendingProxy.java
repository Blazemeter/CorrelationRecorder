package com.blazemeter.jmeter.correlation.core.proxy;

import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;

public class PendingProxy {

  private final JMeterTreeNode target;
  private HTTPSamplerBase sampler;
  private TestElement[] testElements;
  private SampleResult result;
  private boolean complete;

  public PendingProxy(JMeterTreeNode target) {
    this.target = target;
  }

  public void update(HTTPSamplerBase sampler, TestElement[] testElements,
      SampleResult result) {
    this.sampler = sampler;
    this.testElements = testElements;
    this.result = result;
  }

  public SampleResult getResult() {
    return result;
  }

  public boolean isComplete() {
    return complete;
  }

  public void setComplete(boolean complete) {
    this.complete = complete;
  }

  public HTTPSamplerBase getSampler() {
    return sampler;
  }

  public JMeterTreeNode getTarget() {
    return target;
  }

  public TestElement[] getTestElements() {
    return testElements;
  }

  public void setTestElements(TestElement[] testElements) {
    this.testElements = testElements;
  }
}
