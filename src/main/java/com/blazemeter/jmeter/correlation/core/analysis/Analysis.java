package com.blazemeter.jmeter.correlation.core.analysis;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.suggestions.method.AnalysisMethod;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Analysis {
  private static final Logger LOG = LoggerFactory.getLogger(Analysis.class);
  private CorrelationProxyControl proxyControl;
  private List<RulesGroup> rulesGroups;
  private String tracePath;
  private Supplier<List<JMeterTreeNode>> nodesSupplier;
  private Supplier<List<SampleResult>> resultsSupplier;
  private Supplier<List<HTTPSamplerProxy>> samplersSupplier;

  private Consumer<String> report;

  public Analysis() {
    samplersSupplier = JMeterElementUtils::getCurrentSamplerList;
    nodesSupplier = JMeterElementUtils::getCurrentSamplerNodes;
    report = LOG::trace;
  }

  public void setTracePath(String tracePath) {
    this.tracePath = tracePath;
  }

  /**
   * This method will start the analysis with the given rulesGroups, using the JTL file provided.
   * Note that the shouldCorrelate param wont avoid the analysis to be executed, but rather
   * prevent the Correlation Rules to modify the Test Plan elements.
   *
   * @param selectedTemplates List of templates to be used in the analysis
   * @param tracePath         Path to the JTL file
   * @param shouldCorrelate   Boolean indicating if the Correlation Rules should be applied
   * @return
   */
  public Map<Template, List<CorrelationSuggestion>> run(List<Template> selectedTemplates,
                                                        String tracePath, boolean shouldCorrelate) {
    setTracePath(tracePath);
    if (!shouldCorrelate) {
      disableCorrelation();
    } else {
      enableCorrelation();
    }

    setResultsSupplier(() -> JMeterElementUtils.getCurrentSampleResults(this.tracePath));

    Map<Template, List<CorrelationSuggestion>> suggestions = new HashMap<>();
    AnalysisReporter.startCollecting(); // Start collecting
    for (Template template : selectedTemplates) {
      startAnalysisWithGroupRules(template.getGroups());
      List<CorrelationSuggestion> correlationSuggestions =
          AnalysisReporter.generateCorrelationSuggestions();
      suggestions.put(template, correlationSuggestions);
      AnalysisReporter.clear(); // Clear the current template suggestions
    }
    AnalysisReporter.stopCollecting();

    enableCorrelation();
    LOG.trace("Analysis finished!");
    return suggestions;
  }

  public Map<Template, List<CorrelationSuggestion>> run() {
    List<Template> selectedTemplates = new ArrayList<>(); // populate this list as needed
    String tracePath = ""; // set the trace path as needed
    return run(selectedTemplates, tracePath, true);
  }

  private CorrelationProxyControl getProxyControl(JMeterTreeModel model) {
    List<JMeterTreeNode> proxyNodes = getCorrelationProxyControllers(model);
    if (proxyNodes.isEmpty()) {
      LOG.error("No Correlation Proxy Controller found in the Test Plan.");
      return null;
    }

    Optional<JMeterTreeNode> enabledProxy = proxyNodes.stream()
        .filter(JMeterTreeNode::isEnabled).findFirst();
    if (enabledProxy.isPresent()) {
      return (CorrelationProxyControl) enabledProxy.get().getTestElement();
    }
    return (CorrelationProxyControl) proxyNodes.get(0).getTestElement();
  }

  private static List<JMeterTreeNode> getCorrelationProxyControllers(JMeterTreeModel treeModel) {
    return treeModel.getNodesOfType(CorrelationProxyControl.class);
  }

  public String getReportText() {
    return AnalysisReporter.getReporter().toString();
  }

  public void disableCorrelation() {
    AnalysisReporter.disableCorrelation();
  }

  //With this method we will attempt to make the Analysis without triggering the recording
  public void startAnalysisWithGroupRules(List<RulesGroup> rulesGroups) {
    if (rulesGroups == null || rulesGroups.isEmpty()) {
      LOG.warn("No rules found. Using the default ones");
    }

    this.rulesGroups = rulesGroups;
    proxyControl = getProxyControl(getCurrentJMeterTreeModel());
    updateRefVariable();

    CorrelationEngine engine = new CorrelationEngine();
    /*
    * Question: Do I really need the proxyControl or just the CorrelationComponentsRegistry?
    * */
    CorrelationComponentsRegistry componentsRegistry =
        proxyControl.getCorrelationComponentsRegistry();
    engine.setCorrelationRules(this.rulesGroups, componentsRegistry);
    engine.setEnabled(true);
    engine.reset();

    List<HTTPSamplerProxy> samplers = samplersSupplier.get();
    List<SampleResult> sampleResults = resultsSupplier.get();
    List<JMeterTreeNode> samplerNodes = nodesSupplier.get();

    AnalysisMethod.run(sampleResults, samplerNodes, engine, samplers);
  }

  private static JMeterTreeModel getCurrentJMeterTreeModel() {
    return GuiPackage.getInstance().getTreeModel();
  }

  //This method updates the variable name (called Ref Name for rules) in every part of the
  // loaded Rule. This issue only occurs when the rules are loaded for the Analysis.
  private void updateRefVariable() {
    for (RulesGroup rulesGroup : this.rulesGroups) {
      for (CorrelationRule rule : rulesGroup.getRules()) {
        String referenceName = rule.getReferenceName();
        if (rule.getCorrelationExtractor() != null) {
          rule.getCorrelationExtractor().setVariableName(referenceName);
        }

        if (rule.getCorrelationReplacement() == null) {
          continue;
        }

        rule.getCorrelationReplacement().setVariableName(referenceName);
      }
    }
  }

  public void setNodesSupplier(Supplier<List<JMeterTreeNode>> nodesSupplier) {
    this.nodesSupplier = nodesSupplier;
  }

  public void setResultsSupplier(Supplier<List<SampleResult>> resultsSupplier) {
    this.resultsSupplier = resultsSupplier;
  }

  public void setSamplersSupplier(Supplier<List<HTTPSamplerProxy>> samplersSupplier) {
    this.samplersSupplier = samplersSupplier;
  }

  public void setProxyControl(CorrelationProxyControl proxyControl) {
    this.proxyControl = proxyControl;
  }

  public void setRealTimeReportConsumer(Consumer<String> appendToReport) {
    this.report = appendToReport;
  }

  public void enableCorrelation() {
    AnalysisReporter.enableCorrelation();
  }
}
