package com.blazemeter.jmeter.correlation.core.analysis;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractionSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ReplacementSuggestion;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisReporter {

  public static final String NO_REPORT = "The Analyzer was not active. No report available.";
  private static final Logger LOG = LoggerFactory.getLogger(AnalysisReporter.class);
  private static AnalysisReporter reporter;
  private static Map<VariablePartElement, Report> reports
      = new HashMap<>();
  private static int globalSequence = 0;
  private static boolean isCollectingReports = true;
  private static boolean canCorrelate = true;

  private AnalysisReporter() {

  }

  public static AnalysisReporter getReporter() {
    if (reporter == null) {
      reporter = new AnalysisReporter();
    }
    return reporter;
  }

  /* For ACR > v3.0 the AnalysisReported is now grouping every CorrelationRulePartTestElement
  with a variable reference name for better reporting findings. Therefore, the signature of these
  report methods changed due to parameters being able to be deductive rather than passing. These
  (deprecated) methods remain due to API compatibility such as
  Siebel Extension usage.
   */
  @Deprecated
  public static void report(CorrelationRulePartTestElement<?> part, String value,
      Object affectedElement, String variableName, String location) {
    report(part, affectedElement, location, value);
  }

  @Deprecated
  public static void report(CorrelationRulePartTestElement<?> part, String value,
      Object affectedElement, String variableName) {
    report(part, affectedElement, value);
  }

  /**
   * Add a report entry to the AnalysisReporter for the given Correlation Rule Part.
   *
   * @param part Correlation Rule Part that successfully applied. This is used to identify upon
   * which part the report entry is for.
   * @param affectedElement Element that was affected by the Correlation Rule Part.
   * @param location Location within the affectedElement for the affected value.
   * @param value Value that was affected by the Correlation Rule Part.
   */
  public static void report(CorrelationRulePartTestElement<?> part, Object affectedElement,
      String location, String value) {
    if (!isCollectingReports) {
      return;
    }
    ReportEntry reportEntry = generateReport(value, affectedElement, location);
    VariablePartElement variablePartElement = VariablePartElement.getFrom(part);
    Report report = getReport(variablePartElement);
    if (report == null) {
      report = new Report();
      report.entries = new ArrayList<>();
      reports.put(variablePartElement, report);
    }

    report.entries.add(reportEntry);
  }

  /**
   * Similar to the other report method, but this one does not require a location. Used to report
   * when the Replacements are applied.
   */
  public static void report(CorrelationRulePartTestElement<?> part, Object affectedElement,
      String value) {
    report(part, affectedElement, "Correlation Analysis", value);
  }

  private static ReportEntry generateReport(String value, Object affectedElement, String location) {
    ReportEntry entry = new ReportEntry();
    entry.sequence = getNextSequence();
    entry.value = value;
    entry.affectedElement = affectedElement;
    entry.location = location;
    return entry;
  }

  private static int getNextSequence() {
    globalSequence += 1;
    return globalSequence;
  }

  @VisibleForTesting
  public static Report getReport(VariablePartElement part) {
    return getReporter().reports.get(part);
  }

  /**
   * Stops collecting reports. This is used when recordings run without the Analysis.
   */
  public static void stopCollecting() {
    isCollectingReports = false;
  }

  /**
   * Starts collecting reports. This is used when processing recordings with Analysis.
   */
  public static void startCollecting() {
    globalSequence = 0;
    isCollectingReports = true;
    clear();
  }

  public static void clear() {
    reports.clear();
  }

  public static boolean isCollecting() {
    return isCollectingReports;
  }

  public static List<CorrelationSuggestion> generateCorrelationSuggestions() {
    Map<String, CorrelationSuggestion> suggestionsMap = new HashMap<>();
    for (Entry<VariablePartElement, Report> reportsMap : reports.entrySet()) {

      CorrelationSuggestion suggestion = suggestionsMap.get(
          reportsMap.getKey().referenceVariableName);
      String referenceVariableName = reportsMap.getKey().referenceVariableName;
      if (suggestion == null) {
        suggestion = new CorrelationSuggestion.Builder().fromRulesAnalysis()
            .withParamName(referenceVariableName)
            .build();
        suggestionsMap.put(referenceVariableName, suggestion);
      }

      CorrelationRulePartTestElement<?> part = reportsMap.getKey().partTestElement;

      if (part instanceof CorrelationExtractor) {
        CorrelationExtractor<?> extractor = (CorrelationExtractor<?>) part;
        for (ReportEntry entry : reportsMap.getValue().entries) {
          ExtractionSuggestion extraction = new ExtractionSuggestion(extractor, entry.getSampler());
          extraction.setSequence(entry.sequence);
          extraction.setValue(entry.value);
          extraction.setName(referenceVariableName);
          extraction.setSource(entry.location);
          suggestion.setOriginalValue(entry.value);
          suggestion.addExtractionSuggestion(extraction);
        }
      } else {
        CorrelationReplacement<?> replacement = (CorrelationReplacement<?>) part;
        for (ReportEntry entry : reportsMap.getValue().entries) {
          ReplacementSuggestion replacementSuggestion
              = new ReplacementSuggestion(replacement,
              entry.getSampler());
          replacementSuggestion.setSequence(entry.sequence);
          replacementSuggestion.setValue(entry.value);
          replacementSuggestion.setName(referenceVariableName);
          replacementSuggestion.setSource(entry.location);
          suggestion.setOriginalValue(entry.value);
          suggestion.addReplacementSuggestion(replacementSuggestion);
        }
      }
    }
    return new ArrayList<>(suggestionsMap.values());
  }

  public static void enableCorrelation() {
    canCorrelate = true;
  }

  public static void disableCorrelation() {
    canCorrelate = false;
  }

  public static boolean canCorrelate() {
    return canCorrelate;
  }

  public CorrelationRuleReport getRuleReport(CorrelationRule rule) {
    Report extractorReport =
        getReport(VariablePartElement.getFrom(rule.getCorrelationExtractor()));
    Report replacementReport =
        getReport(VariablePartElement.getFrom(rule.getCorrelationReplacement()));
    boolean extractorApplied = extractorReport != null;
    boolean replacementApplied = replacementReport != null;

    CorrelationRuleReport report = new CorrelationRuleReport();
    report.setRule(rule);
    report.setExtractorApplied(extractorApplied);
    report.setReplacementApplied(replacementApplied);
    report.setExtractorReport(extractorReport);
    report.setReplacementReport(replacementReport);
    return report;
  }

  public static class Report {

    private List<ReportEntry> entries;

    @Override
    public String toString() {
      return "Part can be applied to " + entries.size() + " elements.\n"
          + "Entries: " + entries + "\n";
    }
  }

  private static class ReportEntry {

    private int sequence;
    private String value;
    private Object affectedElement;
    private String location = "Correlation Analysis";

    public String getReportString() {
      return "{ value='" + value + "' "
          + " at '" + location + "'"
          + " in '" + getAffectedElementName()
          + "'}";
    }

    private String getAffectedElementName() {
      if (affectedElement == null) {
        return "null";
      }

      if (affectedElement instanceof HTTPSamplerBase) {
        return ((HTTPSamplerBase) affectedElement).getName();
      } else if (affectedElement instanceof SampleResult) {
        return ((SampleResult) affectedElement).getSampleLabel();
      } else {
        return affectedElement.getClass().getSimpleName();
      }
    }

    public HTTPSamplerBase getSampler() {
      if (affectedElement == null) {
        return null;
      }

      return (HTTPSamplerBase) affectedElement;
    }
  }

  @VisibleForTesting
  public String getReportAsString() {
    if (!isCollectingReports) {
      return NO_REPORT;
    }

    StringBuilder sb = new StringBuilder();
    String separator = System.lineSeparator();
    sb.append("Correlations Report:").append(separator)
        .append(getIndentation(1))
        .append(" Total rules appliances=").append(reports.entrySet().size())
        .append(".").append(separator);
    if (reports.isEmpty()) {
      sb.append(getIndentation(1))
          .append("No rules were applied successfully. Review them and try again.");
      return sb.toString();
    }
    sb.append(getIndentation(1)).append("Details by rule part:").append(separator);
    for (Entry<VariablePartElement, Report> map : reports.entrySet()) {
      sb.append(getIndentation(2))
          .append("Type=").append(map.getKey().partTestElement.getClass().getSimpleName())
          .append(separator).append(getIndentation(2))
          .append("Reference Name=").append(map.getKey().referenceVariableName)
          .append(separator).append(getIndentation(2))
          .append("Rule Part=").append(map.getKey().partTestElement).append(separator);
      sb.append(getIndentation(3))
          .append("  Can be applied to ").append(map.getValue().entries.size()).append(" elements.")
          .append(separator);
      sb.append(getIndentation(4)).append("  Entries: ").append(separator);
      for (ReportEntry entry : map.getValue().entries) {
        sb.append(getIndentation(5))
            .append("- ").append(entry.getReportString()).append(separator);
      }
    }
    return sb.toString();
  }

  private String getIndentation(int i) {
    return String.format("%" + i + "s", "");
  }

  public static class VariablePartElement {

    private final String referenceVariableName;
    private final CorrelationRulePartTestElement<?> partTestElement;

    public VariablePartElement(String referenceVariableName,
        CorrelationRulePartTestElement<?> partTestElement) {
      this.referenceVariableName = referenceVariableName;
      this.partTestElement = partTestElement;
    }

    public static VariablePartElement getFrom(CorrelationRulePartTestElement<?> partTestElement) {
      return new VariablePartElement(getRefVarNameFrom(partTestElement), partTestElement);
    }

    private static String getRefVarNameFrom(CorrelationRulePartTestElement<?> partTestElement) {
      if (partTestElement instanceof CorrelationExtractor) {
        return ((CorrelationExtractor<?>) partTestElement).getVariableName();
      } else if (partTestElement instanceof CorrelationReplacement) {
        return ((CorrelationReplacement<?>) partTestElement).getVariableName();
      }
      return "";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      VariablePartElement that = (VariablePartElement) o;
      return Objects.equals(referenceVariableName, that.referenceVariableName) && Objects.equals(
          partTestElement, that.partTestElement);
    }

    @Override
    public int hashCode() {
      return Objects.hash(referenceVariableName, partTestElement);
    }
  }
}
