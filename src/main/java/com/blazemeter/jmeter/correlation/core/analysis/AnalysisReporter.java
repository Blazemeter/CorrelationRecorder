package com.blazemeter.jmeter.correlation.core.analysis;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractionSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.ReplacementSuggestion;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;

public class AnalysisReporter {
  public static final String NO_REPORT = "The Analyzer was not active. No report available.";
  private static AnalysisReporter reporter;
  private static Map<CorrelationRulePartTestElement<?>, Report> reports
      = new HashMap<>();
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

  /**
   * Add a report entry to the AnalysisReporter for the given Correlation Rule Part.
   * @param part Correlation Rule Part that successfully applied.
   *             This is used to identify upon which part the report entry is for.
   * @param value Value that was affected by the Correlation Rule Part.
   * @param affectedElement Element that was affected by the Correlation Rule Part.
   * @param variableName Variable name that is used to store or extract the value from.
   * @param location Location within the affectedElement for the affected value.
   */
  public static void report(CorrelationRulePartTestElement<?> part, String value,
                            Object affectedElement, String variableName, String location) {

    ReportEntry reportEntry = generateReport(part, value, affectedElement, variableName, location);
    Report report = getReport(part);
    if (report == null) {
      report = new Report();
      report.part = part;
      report.entries = new ArrayList<>();
      reports.put(part, report);
    }

    report.entries.add(reportEntry);
  }

  /**
   * Similar to the other report method, but this one does not require a location.
   * Used to report when the Replacements are applied.
   */
  public static void report(CorrelationRulePartTestElement<?> part, String value,
                            Object affectedElement, String variableName) {
    if (!isCollectingReports) {
      return;
    }

    Report report = getReport(part);
    if (report == null) {
      report = new Report();
      report.part = part;
      report.entries = new ArrayList<>();
      reports.put(part, report);
    }

    ReportEntry entry = new ReportEntry();
    entry.value = value;
    entry.affectedElement = affectedElement;
    entry.variableName = variableName;
    entry.part = part;
    report.entries.add(entry);
  }

  private static ReportEntry generateReport(CorrelationRulePartTestElement<?> part, String value,
                                Object affectedElement, String variableName, String location) {
    ReportEntry entry = new ReportEntry();
    entry.value = value;
    entry.affectedElement = affectedElement;
    entry.variableName = variableName;
    entry.part = part;
    entry.location = location;
    return entry;
  }

  @VisibleForTesting
  public static Report getReport(CorrelationRulePartTestElement<?> part) {
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
    isCollectingReports = true;
    reports.clear();
  }

  public static boolean isCollecting() {
    return isCollectingReports;
  }

  private static String getParamName(CorrelationRulePartTestElement<?> part) {
    if (part instanceof CorrelationExtractor) {
      return ((CorrelationExtractor<?>) part).getVariableName();
    } else if (part instanceof CorrelationReplacement) {
      return ((CorrelationReplacement<?>) part).getVariableName();
    }
    return "";
  }

  public static List<CorrelationSuggestion> generateCorrelationSuggestions() {
    Map<String, CorrelationSuggestion> suggestionsMap = new HashMap<>();
    for (Report report : reports.values()) {
      CorrelationRulePartTestElement<?> part = report.part;
      String paramName = getParamName(part);
      CorrelationSuggestion suggestion = suggestionsMap.get(paramName);
      if (suggestion == null) {
        suggestion = new CorrelationSuggestion.Builder().fromRulesAnalysis()
            .withParamName(paramName)
            .build();
        suggestionsMap.put(paramName, suggestion);
      }

      if (part instanceof CorrelationExtractor) {
        RegexCorrelationExtractor<?> extractor = (RegexCorrelationExtractor<?>) part;
        for (ReportEntry entry : report.entries) {
          ExtractionSuggestion extraction = new ExtractionSuggestion(extractor, entry.getSampler());
          extraction.setValue(entry.value);
          extraction.setName(entry.variableName);
          extraction.setSource(entry.location);
          suggestion.setOriginalValue(entry.value);
          suggestion.addExtractionSuggestion(extraction);
        }
      } else {
        RegexCorrelationReplacement<?> replacement = (RegexCorrelationReplacement<?>) part;
        for (ReportEntry entry : report.entries) {
          ReplacementSuggestion replacementSuggestion
              = new ReplacementSuggestion(replacement,
              entry.getSampler());
          replacementSuggestion.setValue(entry.value);
          replacementSuggestion.setName(entry.variableName);
          replacementSuggestion.setSource(entry.location);
          suggestion.setOriginalValue(entry.value);
          suggestion.addReplacementSuggestion(replacementSuggestion);
        }
      }
    }
    return new ArrayList<>(suggestionsMap.values());
  }

  public static List<CorrelationSuggestion> generateReplacementSuggestions() {
    List<CorrelationSuggestion> suggestions = new ArrayList<>();
    for (Report report : reports.values()) {
      CorrelationRulePartTestElement<?> rulePart = report.part;
      if (!(rulePart instanceof CorrelationReplacement)) {
        continue;
      }

      if (!((rulePart instanceof RegexCorrelationReplacement))) {
        continue;
      }

      CorrelationSuggestion suggestion = new CorrelationSuggestion();
      RegexCorrelationReplacement<?> replacement = (RegexCorrelationReplacement<?>) rulePart;
      for (ReportEntry entry : report.entries) {
        if (!(entry.affectedElement instanceof TestElement)) {
          System.out.println("Replacement affect element is not an TestElement");
          continue;
        }
        TestElement usage = (TestElement) entry.affectedElement;
        ReplacementSuggestion replacementSuggestion
            = new ReplacementSuggestion(replacement, usage);
        replacementSuggestion.setSource("Correlation Analysis");
        replacementSuggestion.setName(entry.variableName);
        replacementSuggestion.setValue(entry.value);
        suggestion.setParamName(entry.variableName);
        suggestion.setOriginalValue(entry.value);
        suggestion.setNewValue("");
        suggestion.addReplacementSuggestion(replacementSuggestion);
      }
      suggestions.add(suggestion);
    }
    return suggestions;
  }

  private static List<CorrelationSuggestion> generateExtractorSuggestions() {
    List<CorrelationSuggestion> suggestions = new ArrayList<>();
    for (Report report : reports.values()) {
      CorrelationRulePartTestElement<?> rulePart = report.part;
      if (!(rulePart instanceof RegexCorrelationExtractor)) {
        continue;
      }

      RegexCorrelationExtractor<?> extractor = (RegexCorrelationExtractor<?>) rulePart;
      CorrelationSuggestion suggestion = new CorrelationSuggestion();
      for (ReportEntry entry : report.entries) {
        if (!(entry.affectedElement instanceof HTTPSamplerBase)) {
          System.out.println("Extractor affect element is not an SampleResult");
          continue;
        }

        HTTPSamplerBase request = (HTTPSamplerBase) entry.affectedElement;
        ExtractionSuggestion extractionSuggestion = new ExtractionSuggestion(extractor, request);
        extractionSuggestion.setSource("Correlation Analysis");
        extractionSuggestion.setValue(entry.value);
        extractionSuggestion.setName(entry.variableName);
        suggestion.addExtractionSuggestion(extractionSuggestion);
        suggestion.setParamName(entry.variableName);
        suggestion.setOriginalValue(entry.value);
        suggestion.setNewValue("");
        suggestions.add(suggestion);
      }
    }

    return suggestions;
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
    Report extractorReport = getReport(rule.getCorrelationExtractor());
    Report replacementReport = getReport(rule.getCorrelationReplacement());

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
    private CorrelationRulePartTestElement part;
    private List<ReportEntry> entries;

    @Override
    public String toString() {
      return "Part: " + part + " can be applied to " + entries.size() + " elements.\n"
          + "Entries: " + entries + "\n";
    }
  }

  private static class ReportEntry {
    public String variableName;
    private String value;
    private Object affectedElement;
    private CorrelationRulePartTestElement<?> part;
    private String location = "Correlation Analysis";

    private String getOperation() {
      if (part instanceof CorrelationExtractor) {
        return "extracted";
      } else if (part instanceof CorrelationReplacement) {
        return "replaced";
      } else {
        return "Correlation Analysis";
      }
    }

    public String getReportString() {
      return "{ value='" + value + "' " + getOperation()
          + " at '" + location + "'"
          + " in '" + getAffectedElementName()
          + "' with variable name '" + variableName + "'}";
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
    for (Report report : reports.values()) {
      sb.append(getIndentation(2))
          .append("Type='").append(report.part.getClass().getSimpleName()).append("'.")
          .append(separator).append(getIndentation(2))
          .append("Rule Part=").append(report.part).append(separator);
      sb.append(getIndentation(3))
          .append("  Can be applied to ").append(report.entries.size()).append(" elements.")
          .append(separator);
      sb.append(getIndentation(4)).append("  Entries: ").append(separator);
      for (ReportEntry entry : report.entries) {
        sb.append(getIndentation(5))
            .append("- ").append(entry.getReportString()).append(separator);
      }
    }
    return sb.toString();
  }

  private String getIndentation(int i) {
    return String.format("%" + i + "s", "");
  }
}
