package com.blazemeter.jmeter.correlation.gui.automatic;

import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.MultiProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;

/**
 * Which parts of the suggestions are already in the test plan, for the "Obtained from" and "Used
 * on" columns: a sampler is applied when it has the extractor of the variable, or already takes the
 * value from it ({@code ${var}}, or {@code [var]} in a name, where the correlation keeps it fixed).
 * The rows themselves stay in the table, so they can be exported.
 */
class SuggestionAppliedStatus {

  private static final String PENDING = "pending";
  private static final String APPLIED = "applied";
  private final Map<String, JMeterTreeNode> samplers = new HashMap<>();

  SuggestionAppliedStatus(JMeterTreeModel model) {
    if (model == null) {
      return;
    }
    for (JMeterTreeNode node : model.getNodesOfType(HTTPSamplerBase.class)) {
      TestElement sampler = node.getTestElement();
      samplers.putIfAbsent(sampler.getName(), node);
      samplers.putIfAbsent(JMeterElementUtils.getRecordedName(sampler), node);
    }
  }

  static SuggestionAppliedStatus ofCurrentPlan() {
    GuiPackage gui = GuiPackage.getInstance();
    return new SuggestionAppliedStatus(gui == null ? null : gui.getTreeModel());
  }

  Cell obtainedFrom(CorrelationSuggestion suggestion) {
    Pattern extractor = Pattern.compile(".+ - " + variable(suggestion));
    return Cell.of(suggestion.getObtainedFromNames(), name -> children(name).stream()
        .anyMatch(child -> extractor.matcher(child.getName()).matches()));
  }

  Cell usedOn(CorrelationSuggestion suggestion) {
    Pattern reference = Pattern.compile("\\$\\{" + variable(suggestion) + "}");
    Pattern nameReference = Pattern.compile("\\[" + variable(suggestion) + "]|"
        + reference.pattern());
    return Cell.of(suggestion.getUsedOnNames(), name -> {
      JMeterTreeNode node = samplers.get(name);
      return node != null && (refers(node.getTestElement(), reference, nameReference)
          || children(name).stream().anyMatch(child -> refers(child, reference, nameReference)));
    });
  }

  // The suggestion's variable, and the one of each of its values when it is multivalued (var#3)
  private static String variable(CorrelationSuggestion suggestion) {
    return Pattern.quote(suggestion.getParamName()) + "(?:#\\d+)?";
  }

  private List<TestElement> children(String samplerName) {
    JMeterTreeNode node = samplers.get(samplerName);
    if (node == null) {
      return Collections.emptyList();
    }
    List<TestElement> children = new ArrayList<>();
    for (int i = 0; i < node.getChildCount(); i++) {
      children.add(((JMeterTreeNode) node.getChildAt(i)).getTestElement());
    }
    return children;
  }

  private static boolean refers(TestElement element, Pattern reference, Pattern nameReference) {
    return refers(element.propertyIterator(), reference, nameReference);
  }

  private static boolean refers(PropertyIterator properties, Pattern reference,
      Pattern nameReference) {
    while (properties.hasNext()) {
      JMeterProperty property = properties.next();
      boolean found = property instanceof MultiProperty
          ? refers(((MultiProperty) property).iterator(), reference, nameReference)
          : (TestElement.NAME.equals(property.getName()) ? nameReference : reference)
              .matcher(property.getStringValue()).find();
      if (found) {
        return true;
      }
    }
    return false;
  }

  /**
   * A cell's text, "pending(2)[a, b ×2] applied(1)[c]", and its tooltip with a sampler per line.
   * Pending goes first because the cell cuts its end; an empty group is left out.
   */
  static final class Cell {

    private final String text;
    private final String tooltip;

    private Cell(String text, String tooltip) {
      this.text = text;
      this.tooltip = tooltip;
    }

    static Cell of(List<String> names, Predicate<String> applied) {
      Map<String, Map<String, Integer>> groups = new LinkedHashMap<>();
      groups.put(PENDING, new LinkedHashMap<>());
      groups.put(APPLIED, new LinkedHashMap<>());
      for (String name : names) {
        groups.get(applied.test(name) ? APPLIED : PENDING).merge(name, 1, Integer::sum);
      }
      List<String> texts = new ArrayList<>();
      StringBuilder tooltip = new StringBuilder("<html>");
      groups.forEach((state, counts) -> {
        if (counts.isEmpty()) {
          return;
        }
        List<String> entries = counts.entrySet().stream()
            .map(e -> e.getValue() > 1 ? e.getKey() + " ×" + e.getValue() : e.getKey())
            .collect(Collectors.toList());
        int count = counts.values().stream().mapToInt(Integer::intValue).sum();
        texts.add(state + "(" + count + ")[" + String.join(", ", entries) + "]");
        tooltip.append("<b>").append(state).append(" (").append(count).append(")</b>");
        entries.forEach(entry -> tooltip.append("<br>&nbsp;&nbsp;").append(escape(entry)));
        tooltip.append("<br>");
      });
      return texts.isEmpty() ? new Cell("", null)
          : new Cell(String.join(" ", texts), tooltip.append("</html>").toString());
    }

    private static String escape(String text) {
      return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    String getText() {
      return text;
    }

    String getTooltip() {
      return tooltip;
    }
  }
}
