package com.blazemeter.jmeter.correlation.core.analysis;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
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
   * @param selectedTemplates List of templates to be used in the analysis
   * @param tracePath Path to the JTL file
   * @param shouldCorrelate Boolean indicating if the Correlation Rules should be applied
   */
  public void run(List<TemplateVersion> selectedTemplates, String tracePath,
                  boolean shouldCorrelate) {
    setTracePath(tracePath);
    if (!shouldCorrelate) {
      disableCorrelation();
    } else {
      enableCorrelation();
    }

    setResultsSupplier(() -> JMeterElementUtils.getCurrentSampleResults(this.tracePath));
    AnalysisReporter.startCollecting();
    for (TemplateVersion template : selectedTemplates) {
      startAnalysisWithGroupRules(template.getGroups());
    }
    AnalysisReporter.stopCollecting();
    LOG.trace("Analysis finished!");
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
      LOG.error("No rules found. Using the default ones");
    }

    this.rulesGroups = rulesGroups;
    proxyControl = getProxyControl(GuiPackage.getInstance().getTreeModel());
    updateRefVariable();

    CorrelationEngine engine = new CorrelationEngine();
    engine.setCorrelationRules(this.rulesGroups, proxyControl.getCorrelationComponentsRegistry());
    engine.setEnabled(true);
    engine.reset();

    List<HTTPSamplerProxy> samplers = samplersSupplier.get();
    report.accept("Samplers: " + samplers.size());
    List<SampleResult> sampleResults = resultsSupplier.get();
    report.accept("Sample Results: " + sampleResults.size());
    List<JMeterTreeNode> samplerNodes = nodesSupplier.get();
    report.accept("Sampler Nodes: " + samplerNodes.size());
    for (int i = 0; i < sampleResults.size(); i++) {
      List<TestElement> children = new ArrayList<>();
      JMeterTreeNode node = samplerNodes.get(i);
      int initialChildCount = node.getChildCount();
      for (int j = 0; j < initialChildCount; j++) {
        children.add((TestElement) ((JMeterTreeNode) node.getChildAt(j)).getUserObject());
      }
      engine.process(samplers.get(i), children, sampleResults.get(i), "");
      if (initialChildCount == children.size()) {
        continue;
      }
      report.accept("Node '" + node.getName() + "' has been modified. Adding the new children. "
          + "Initial children: " + initialChildCount + ", new children: " + children.size() + ".");
      JMeterTreeModel model = GuiPackage.getInstance().getTreeModel();
      for (int j = initialChildCount; j < children.size(); j++) {
        TestElement child = children.get(j);
        try {
          model.addComponent(child, node);
        } catch (IllegalUserActionException e) {
          LOG.error("Error while adding the child '{}' to the element '{}'",
              child.getName(), node.getName(), e);
        }
      }
    }
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

  private List<RulesGroup> getLoadedRulesGroups() {
    List<RulesGroup> groups = proxyControl.getGroups();
    if (groups == null || groups.isEmpty()) {
      LOG.error("No rules found. Please, add some rules to the Test Plan.");
      return Collections.emptyList();
    }
    return groups;
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
