package com.blazemeter.jmeter.correlation.core.suggestions.method;

import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.analysis.AnalysisReporter;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.suggestions.context.AnalysisContext;
import com.blazemeter.jmeter.correlation.core.suggestions.context.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AnalysisMethod class implements the CorrelationMethod interface and provides methods for
 * generating correlation suggestions by analyzing the recording and replay traces using the
 * Correlation Rules in the Correlation Templates.
 */
public class AnalysisMethod implements CorrelationMethod {

  private static final Logger LOG = LoggerFactory.getLogger(AnalysisMethod.class);
  private AnalysisContext context;

  public AnalysisMethod() {
  }

  public AnalysisMethod(AnalysisContext context) {
    this.context = context;
  }

  @Override
  public List<CorrelationSuggestion> generateSuggestions(CorrelationContext context) {
    this.context = (AnalysisContext) context;

    Template template = this.context.getTemplate();
    if (template == null) {
      LOG.error("No template found. Skipping analysis.");
      return new ArrayList<>();
    }

    List<RulesGroup> templateRulesGroups = template.getGroups();
    if (isNullOrEmpty(templateRulesGroups)) {
      LOG.error("No rule groups found in the template '{}'. Skipping analysis.", template.getId());
      return new ArrayList<>();
    }

    boolean hasAnyRules = false;
    for (RulesGroup rulesGroup : templateRulesGroups) {
      if (!isNullOrEmpty(rulesGroup.getRules())) {
        hasAnyRules = true;
        break;
      } else {
        LOG.error("No rules found in the group '{}'.", rulesGroup.getId());
      }
    }

    if (!hasAnyRules) {
      LOG.error("No rules found in the groups for template '{}'. Skipping analysis.",
          template.getId());
      return new ArrayList<>();
    }

    runAnalysis(templateRulesGroups, false);
    return generateSuggestions();
  }

  private static List<CorrelationSuggestion> generateSuggestions() {
    List<CorrelationSuggestion> correlationSuggestions =
        AnalysisReporter.generateCorrelationSuggestions();
    LOG.info("Correlation suggestions generated: {}", correlationSuggestions.size());
    return correlationSuggestions;
  }

  public static synchronized void run(List<SampleResult> sampleResults,
      List<JMeterTreeNode> samplerNodes,
      CorrelationEngine engine,
      List<HTTPSamplerProxy> samplers) {
    JMeterTreeModel model = getCurrentJMeterTreeModel();
    Map<String, Integer> indexedSamplers = getSamplersIndexedByName(samplers);

    for (SampleResult sampleResult : sampleResults) {
      if (Thread.currentThread().isInterrupted()) {
        break;
      }
      // Search the sample that match with sample result
      // Get indexed sampler if exist
      if (indexedSamplers.containsKey(sampleResult.getSampleLabel())) {
        Integer samplerIndex = indexedSamplers.get(sampleResult.getSampleLabel());
        HTTPSamplerProxy samplerProxy = samplers.get(samplerIndex);

        List<TestElement> children = new ArrayList<>();
        JMeterTreeNode node = samplerNodes.get(samplerIndex);
        Map<String, Integer> indexedChildren = getChildrenNodeByName(node);

        int initialChildCount = node.getChildCount();
        for (int j = 0; j < initialChildCount; j++) {
          children.add((TestElement) ((JMeterTreeNode) node.getChildAt(j)).getUserObject());
        }
        engine.process(samplerProxy, children, sampleResult, "");
        if (initialChildCount == children.size()) {
          continue;
        }
        // When children was added, propagate the child to the tree node
        for (int j = initialChildCount; j < children.size(); j++) {
          if (Thread.currentThread().isInterrupted()) {
            break;
          }
          TestElement child = children.get(j);
          // check if that children not exist in the previous node data
          // this is to avoid generating duplicate extractors
          if (!indexedChildren.containsKey(child.getName())) {

            try {
              model.addComponent(child, node);
            } catch (IllegalUserActionException e) {
              LOG.error("Error while adding the child '{}' to the element '{}'",
                  child.getName(), node.getName(), e);
            }
          }
        }
      }
    }
  }

  public static Map<String, Integer> getSamplersIndexedByName(
      List<HTTPSamplerProxy> samplers) {
    // The method is used to optimize the way how to get the index of a specific sampler by name
    Map<String, Integer> indexedSamplers = new HashMap<>();
    int index = 0;
    for (HTTPSamplerProxy sampler : samplers) {
      indexedSamplers.put(getCrName(sampler), index);
      index += 1;
    }
    return indexedSamplers;
  }

  public static Map<String, Integer> getChildrenNodeByName(JMeterTreeNode node) {
    int childCount = node.getChildCount();
    Map<String, Integer> indexedChildren = new HashMap<>();
    for (int index = 0; index < childCount; index++) {
      indexedChildren.put(getCrName(((TestElement) (((JMeterTreeNode)
          node.getChildAt(index)).getUserObject()))), index);
    }
    return indexedChildren;
  }

  private static String getCrName(TestElement te) {
    String comment = te.getComment();
    String crName = comment.substring(comment.lastIndexOf(";") + 1);
    if (crName.contains("ORIGINAL_NAME")) {
      return crName.substring(crName.indexOf("=") + 1);
    }
    return te.getName();
  }

  private static JMeterTreeModel getCurrentJMeterTreeModel() {
    JMeterTreeModel model;
    if (!JMeterElementUtils.isNotRunningWithGui()) {
      model = GuiPackage.getInstance().getTreeModel();
    } else {
      model = new JMeterTreeModel();
    }
    return model;
  }

  private boolean isNullOrEmpty(List<?> list) {
    return list == null || list.isEmpty();
  }

  public void runAnalysis(List<RulesGroup> rulesGroups, boolean shouldCorrelate) {
    if (!shouldCorrelate) {
      disableCorrelation();
    } else {
      enableCorrelation();
    }

    AnalysisReporter.startCollecting();
    startAnalysisWithGroupRules(rulesGroups);
    AnalysisReporter.stopCollecting();
    enableCorrelation();
  }

  public void disableCorrelation() {
    AnalysisReporter.disableCorrelation();
  }

  public void enableCorrelation() {
    AnalysisReporter.enableCorrelation();
  }

  private void startAnalysisWithGroupRules(List<RulesGroup> rulesGroups) {
    updateRefVariable(rulesGroups);

    CorrelationEngine engine = new CorrelationEngine();
    engine.setCorrelationRules(rulesGroups, context.getRegistry());
    engine.setEnabled(true);
    engine.reset();

    List<HTTPSamplerProxy> samplers = context.getRecordingSamplers();
    List<SampleResult> sampleResults = context.getRecordingSampleResults();
    List<JMeterTreeNode> samplerNodes = context.getSamplerNodes();

    if (isNullOrEmpty(samplers) || isNullOrEmpty(sampleResults) || isNullOrEmpty(samplerNodes)) {
      LOG.error("No recording data found. Skipping analysis.");
      return;
    }

    run(sampleResults, samplerNodes, engine, samplers);
  }

  private void updateRefVariable(List<RulesGroup> rulesGroups) {
    for (RulesGroup rulesGroup : rulesGroups) {
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

  public AnalysisContext getContext() {
    return context;
  }

  public void setContext(
      AnalysisContext context) {
    this.context = context;
  }
}
