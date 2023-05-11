package com.blazemeter.jmeter.correlation.core.automatic;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.tree.TreeNode;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for the modification of JMeter Elements when applying a
 * {@link CorrelationSuggestion}.
 */
public class ElementsModification {

  private static final Logger LOG = LoggerFactory.getLogger(ElementsModification.class);

  public static Map<JMeterTreeNode, ModificationResult> applySelectedSuggestions(
      List<CorrelationSuggestion> suggestions) {
    LOG.info("Applying {} suggestions.", suggestions.size());
    HashTree testPlan = JMeterElementUtils.getNormalizedTestPlan();
    ModificationReport report = new ModificationReport();
    try {
      report = applySuggestions(testPlan, suggestions);
      JMeterElementUtils.refreshJMeter();
    } catch (IllegalUserActionException e) {
      LOG.error("Error applying suggestions", e);
      e.printStackTrace();
    }
    LOG.info("Suggestions applied");
    return report.getModificationResults();
  }

  @VisibleForTesting
  public static ModificationReport applySuggestions(HashTree testPlan,
                                                    List<CorrelationSuggestion> suggestions)
      throws IllegalUserActionException {
    ModificationReport report = new ModificationReport();
    JMeterTreeModel model = JMeterElementUtils.convertToTreeModel(testPlan);
    List<JMeterTreeNode> recordedSamplers = model.getNodesOfType(HTTPSamplerBase.class);
    Map<JMeterTreeNode, ModificationResult> modificationResults = new HashMap<>();
    for (CorrelationSuggestion suggestion : suggestions) {
      for (JMeterTreeNode sampler : recordedSamplers) {
        ModificationResult modificationResult = modificationResults.get(sampler);
        if (modificationResult == null) {
          modificationResult = new ModificationResult(sampler);
          modificationResults.put(sampler, modificationResult);
        }
        addSuggestedExtractors(suggestion, sampler, modificationResult, model);
        addSuggestedReplacements(suggestion, sampler, modificationResult, model);
      }
    }

    report.setModificationResults(modificationResults);
    report.setOriginalTestPlan(testPlan);
    //We need this to have a valid Test Plan (at least for the tests)
    HashTree modifiedTestPlan = model.getTestPlan();
    JMeter.convertSubTree(modifiedTestPlan);
    report.setModifiedTestPlan(modifiedTestPlan);
    return report;
  }

  /**
   * Adds the suggested extractors to the sampler if the sampler matches the element
   * where the suggestion was made.
   *
   * @param suggestion  Suggestion to be applied.
   * @param samplerNode HTTPSamplerProxy where the {@link CorrelationExtractor} will be added.
   * @param model       JMeterTreeModel of the test plan.
   * @see CorrelationSuggestion
   */
  private static void addSuggestedExtractors(CorrelationSuggestion suggestion,
                                             JMeterTreeNode samplerNode,
                                             ModificationResult result, JMeterTreeModel model) {

    for (ExtractionSuggestion extraction : suggestion.getExtractionSuggestions()) {
      if (JMeterElementUtils.areEquivalent(samplerNode, extraction.getSampleResult())) {
        RegexCorrelationExtractor<?> extractor = extraction.getExtractor();
        String variableName = extractor.getVariableName();
        if (JMeterElementUtils.isExtractorRepeated(samplerNode, variableName)) {
          continue;
        }

        RegexExtractor postProcessor = extractor.createPostProcessor(variableName, 1);
        postProcessor.setName(postProcessor.getName() + " (" + extraction.getSource() + ")");
        postProcessor.setScopeAll();
        JMeterElementUtils.addPostProcessorToNode(samplerNode,
            postProcessor, model);
        result.addExtraction("- Extractor: Added '" + variableName
            + "' ('" + extraction.getValue() + "')'. Regex: '" + postProcessor.getRegex() + "'");
      }
    }
  }

  /**
   * Adds the suggested replacements to the sampler if the sampler matches the element
   * where the suggestion was made.
   *
   * @param suggestion  Suggestion to be applied.
   * @param samplerNode HTTPSamplerProxy whom its arguments will be modified.
   * @param model       JMeterTreeModel of the test plan.
   * @see CorrelationSuggestion
   */
  private static void addSuggestedReplacements(CorrelationSuggestion suggestion,
                                               JMeterTreeNode samplerNode,
                                               ModificationResult result, JMeterTreeModel model) {

    for (ReplacementSuggestion replacementSuggestion : suggestion.getReplacementSuggestions()) {
      TestElement usage = replacementSuggestion.getUsage();
      if (JMeterElementUtils.areEquivalentRequests(samplerNode, usage)) {
        RegexCorrelationReplacement<?> replacement =
            replacementSuggestion.getReplacementSuggestion();
        String source = replacementSuggestion.getSource();
        String variableName = replacement.getVariableName();
        switch (source) {
          case "Header":
          case "Header Request (Fields)":
            replaceHttpHeaders(replacementSuggestion, getHeaderManagerOf(samplerNode, model),
                samplerNode, result);
            break;
          case "Body Data (JSON)":
            JMeterProperty body =
                samplerNode.getTestElement().getProperty("HTTPSampler.postBodyRaw");
            if (body != null) {
              replaceHttpArguments(replacementSuggestion, samplerNode, result);
            }
            break;
          case "URL":
            JMeterProperty url = samplerNode.getTestElement().getProperty("HTTPSampler.path");
            if (url != null) {
              result.addReplacement("Failed to replace " + variableName
                  + " (" + source + ")");
            }
            break;
          case "HTTP arguments":
            replaceHttpArguments(replacementSuggestion, samplerNode, result);
            break;
          case "Request Path":
            replaceRequestPath(replacementSuggestion, samplerNode, result);
            break;
          default:
            LOG.error("The source '{}' is not supported."
                    + "Replacement's value '{}' in the suggestion {}",
                source, replacementSuggestion.getValue(), replacementSuggestion);
            break;
        }
      }
    }
  }

  private static String replaceStartEnd(String value, String replace, String start, String end,
                                        String text) {
    String safeSearch = Pattern.quote(start + value + end);
    String safeReplacement = Matcher.quoteReplacement(start + replace + end);
    return text.replaceAll(safeSearch, safeReplacement);
  }

  @VisibleForTesting
  public static String replacePath(String value, String valueReplace, String urlPath) {
    String sp = urlPath;
    String eol = "[EOL]";
    if (sp.contains(value)) { // Path
      try {
        // Use multiple strategies
        String spRpl = sp + eol; // Add End of line to use in replace
        spRpl = replaceStartEnd(value, valueReplace, "/", "/", spRpl);
        spRpl = replaceStartEnd(value, valueReplace, "/", "?", spRpl);
        spRpl = replaceStartEnd(value, valueReplace, "/", eol, spRpl);
        spRpl = spRpl.replace(eol, "");
        sp = spRpl;
      } catch (Exception ex) {
        LOG.error("Unknown exception", ex);
      }
    }
    String sq = sp.indexOf("?") > 0 ? sp.split("\\?")[1] : "";
    if (sq.contains(value)) { // Query
      try {
        // Use multiple strategies
        String sqRpl = sq + eol; // sq.replace(value, variableName);
        sqRpl = replaceStartEnd(value, valueReplace, "=", "&", sqRpl);
        sqRpl = replaceStartEnd(value, valueReplace, "'", "'", sqRpl);
        sqRpl = replaceStartEnd(value, valueReplace, "\"", "\"", sqRpl);
        sqRpl = replaceStartEnd(value, valueReplace, "=", eol, sqRpl);
        sqRpl = sqRpl.replace(eol, "");
        sp = sp.split("\\?")[0] + "?" + sqRpl;
      } catch (Exception ex) {
        LOG.error("Unknown exception", ex);
      }
    }
    return sp;
  }

  private static void replaceRequestPath(ReplacementSuggestion suggestion,
                                         JMeterTreeNode requestElement, ModificationResult result) {
    HTTPSamplerBase sampler = (HTTPSamplerBase) requestElement.getTestElement();
    String name = suggestion.getName();
    String variableName = "${" + suggestion.getReplacementSuggestion().getVariableName() + "}";
    String value = suggestion.getValue();
    String sp = sampler.getPath();
    String spRpl = replacePath(value, variableName, sp);
    if (!sp.equals(spRpl)) {
      sampler.setPath(sp.replace(sp, spRpl));
      result.addReplacement(
          "- Replacement: URL Path '" + name + "' ('" + value + "')' with '" + variableName + "'");
    }
  }

  private static void replaceHttpArguments(ReplacementSuggestion suggestion,
                                           JMeterTreeNode requestElement,
                                           ModificationResult result) {
    for (JMeterProperty variable : JMeterElementUtils.getHttpArguments(requestElement)) {
      if (variable.getStringValue().contains(suggestion.getValue())) {
        String variableName = "${" + suggestion.getReplacementSuggestion().getVariableName() + "}";
        HTTPArgument argument = JMeterElementUtils.getHttpArgument(variable);
        argument.setValue(suggestion.getSource().equals("Body Data (JSON)")
            ? argument.getValue().replace(suggestion.getValue(), variableName)
            : variableName);

        result.addReplacement("- Replacement: Argument '" + suggestion.getName()
            + "' ('" + suggestion.getValue() + "')' with '" + variableName + "'");
      }
    }
  }

  private static List<HeaderManager> getHeaderManagerOf(JMeterTreeNode samplerNode,
                                                        JMeterTreeModel model) {
    List<HeaderManager> headerManagers = new ArrayList<>();
    JMeterTreeNode nodeOf = model.getNodeOf(samplerNode.getTestElement());
    //If the node is a leaf, it means that it is not a parent of any other node.
    if (nodeOf.isLeaf()) {
      return headerManagers;
    }

    for (int i = 0; i < nodeOf.getChildCount(); i++) {
      TreeNode child = nodeOf.getChildAt(i);
      if (child instanceof JMeterTreeNode) {
        JMeterTreeNode node = (JMeterTreeNode) child;
        if (node.getTestElement() instanceof HeaderManager) {
          headerManagers.add((HeaderManager) node.getTestElement());
        }
      }
    }
    return headerManagers;
  }

  private static void replaceHttpHeaders(ReplacementSuggestion suggestion,
                                         List<HeaderManager> headerManagers,
                                         JMeterTreeNode requestElement,
                                         ModificationResult result) {

    for (HeaderManager manager : headerManagers) {
      CollectionProperty headers = manager.getHeaders();
      for (int i = 0; i < headers.size(); i++) {
        JMeterProperty header = headers.get(i);
        // The getStringValue return header[\t]value, split and use the second part
        String rawHeaderValue = header.getStringValue();
        String headerValue = rawHeaderValue.substring(rawHeaderValue.indexOf("\t") + 1);
        String value = suggestion.getValue();
        if (containsIgnoreCase(headerValue, value)) {
          String variableName =
              "${" + suggestion.getReplacementSuggestion().getVariableName() + "}";
          manager.get(i).setValue(headerValue.replace(value, variableName));
          result.addReplacement("- Replacement: Header '" + suggestion.getName()
              + "' ('" + suggestion.getValue() + "')' with '" + variableName + "'");
        }
        headers.set(i, header);
      }
    }

  }

  public static class ModificationReport {
    private HashTree originalTestPlan;
    private HashTree modifiedTestPlan;
    private Map<JMeterTreeNode, ModificationResult> results = new HashMap<>();

    public ModificationReport() {

    }

    public ModificationReport(HashTree originalTestPlan, HashTree modifiedTestPlan) {
      this.originalTestPlan = originalTestPlan;
      this.modifiedTestPlan = modifiedTestPlan;
    }

    public void addResult(JMeterTreeNode samplerNode, ModificationResult result) {
      results.put(samplerNode, result);
    }

    public HashTree getOriginalTestPlan() {
      return originalTestPlan;
    }

    public HashTree getModifiedTestPlan() {
      return modifiedTestPlan;
    }

    public void setOriginalTestPlan(HashTree originalTestPlan) {
      this.originalTestPlan = originalTestPlan;
    }

    public void setModifiedTestPlan(HashTree modifiedTestPlan) {
      this.modifiedTestPlan = modifiedTestPlan;
    }

    public Map<JMeterTreeNode, ModificationResult> getResults() {
      return results;
    }

    public Map<JMeterTreeNode, ModificationResult> getModificationResults() {
      return results.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
              LinkedHashMap::new));
    }

    public void setModificationResults(Map<JMeterTreeNode, ModificationResult> applySuggestions) {
      this.results = applySuggestions;
    }
  }
}
