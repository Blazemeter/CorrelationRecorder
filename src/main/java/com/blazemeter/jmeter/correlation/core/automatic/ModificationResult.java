package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jmeter.gui.tree.JMeterTreeNode;

/**
 * Represents the result of a modifications done for a {@link JMeterTreeNode}.
 * Contains the list of replacements, extractors alongside the element they modified.
 */
public class ModificationResult {
  private final JMeterTreeNode node;
  private final List<String> replacementList = new ArrayList<>();
  private final List<String> extractionList = new ArrayList<>();

  public ModificationResult(JMeterTreeNode node) {
    this.node = node;
  }

  public JMeterTreeNode getNode() {
    return node;
  }

  public void addReplacement(String modification) {
    replacementList.add(modification);
  }

  public List<String> getReplacementList() {
    return replacementList;
  }

  public List<String> getExtractionList() {
    return extractionList;
  }

  public int getReplacementCount() {
    return replacementList.size();
  }

  public int getExtractionCount() {
    return extractionList.size();
  }

  public void addExtraction(String extraction) {
    this.extractionList.add(extraction);
  }

  public int getModificationCount() {
    return replacementList.size() + extractionList.size();
  }

  @Override
  public String toString() {
    return node.getName() + "(" + (replacementList.size() + extractionList.size()) + ") \n"
        + replacementList.stream().collect(Collectors.joining("\n", "[", "]"))
        + extractionList.stream().collect(Collectors.joining("\n", "[", "]"));
  }
}
