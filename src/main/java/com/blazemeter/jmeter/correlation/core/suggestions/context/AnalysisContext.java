package com.blazemeter.jmeter.correlation.core.suggestions.context;

import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import java.util.ArrayList;
import java.util.List;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisContext implements CorrelationContext {
  private static final Logger LOG = LoggerFactory.getLogger(AnalysisContext.class);
  private String recordingTraceFilePath;
  private String recordingTestPlanPath;
  private HashTree recordingTestPlan;
  private Template template;

  private CorrelationComponentsRegistry registry;

  public AnalysisContext() {

  }

  /**
   * Fetches the recording sample results from the specified recording trace file path.
   *
   * @return A list of SampleResult objects. If the recording trace file path is null
   * or no sample results are found,
   * an empty list is returned.
   */
  @Override
  public List<SampleResult> getRecordingSampleResults() {
    if (recordingTraceFilePath == null) {
      LOG.error("Recording trace file path cannot be null");
      return new ArrayList<>();
    }

    JMeterElementUtils.getSampleResults(recordingTraceFilePath);
    List<SampleResult> sampleResults = JMeterElementUtils.getSampleResults(recordingTraceFilePath);
    if (sampleResults == null || sampleResults.isEmpty()) {
      LOG.error("No sample results found in the path '{}'", recordingTraceFilePath);
    }

    return sampleResults;
  }

  /**
   * Fetches the HTTPSamplerProxy objects from the recording test plan.
   *
   * @return A list of HTTPSamplerProxy objects. If the recording test
   * plan path is null or no test plan is found,
   * an empty list is returned.
   */
  @Override
  public List<HTTPSamplerProxy> getRecordingSamplers() {
    HashTree testPlan = getRecordingHashTree();
    if (testPlan == null) {
      return new ArrayList<>();
    }

    List<TestElement> samplerList = new ArrayList<>();
    JMeterElementUtils.extractHttpSamplers(testPlan, samplerList);
    return samplerList.stream()
        .map(sampler -> (HTTPSamplerProxy) sampler)
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
  }

  private HashTree getRecordingHashTree() {
    if (recordingTestPlanPath == null && recordingTestPlan == null) {
      LOG.error("Recording test plan path cannot be null");
      return null;
    }

    HashTree testPlan;
    // When running non-gui, the test plan is sent as a parameter
    if (recordingTestPlan != null) {
      testPlan = recordingTestPlan;
    } else {
      testPlan = JMeterElementUtils.getTestPlan(recordingTestPlanPath);
    }

    if (testPlan == null) {
      LOG.error("No test plan found in the path '{}'", recordingTestPlanPath);
      return null;
    }
    return testPlan;
  }

  /**
   * Fetches the JMeterTreeNode objects from the recording test plan.
   *
   * @return A list of JMeterTreeNode objects. If the recording test plan path is null,
   * no test plan is found, or an error occurs while adding the test plan to the GUI,
   * an empty list is returned.
   */
  public List<JMeterTreeNode> getSamplerNodes() {
    HashTree testPlan = getRecordingHashTree();
    if (testPlan == null) {
      return new ArrayList<>();
    }

    try {
      return JMeterElementUtils.getSamplerNodes(testPlan);
    } catch (Exception e) {
      LOG.error("Error while adding the test plan to the GUI", e);
    }

    return new ArrayList<>();
  }

  public String getRecordingTraceFilePath() {
    return recordingTraceFilePath;
  }

  public void setRecordingTraceFilePath(String recordingTraceFilePath) {
    this.recordingTraceFilePath = recordingTraceFilePath;
  }

  public String getRecordingTestPlanPath() {
    return recordingTestPlanPath;
  }

  public void setRecordingTestPlanPath(String recordingTestPlanPath) {
    this.recordingTestPlanPath = recordingTestPlanPath;
  }

  public CorrelationComponentsRegistry getRegistry() {
    return registry;
  }

  public void setRegistry(CorrelationComponentsRegistry registry) {
    this.registry = registry;
  }

  public Template getTemplate() {
    return template;
  }

  public void setTemplate(Template template) {
    this.template = template;
  }

  public HashTree getRecordingTestPlan() {
    return recordingTestPlan;
  }

  public void setRecordingTestPlan(HashTree recordingTestPlan) {
    this.recordingTestPlan = recordingTestPlan;
  }
}
