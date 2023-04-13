package com.blazemeter.jmeter.correlation.core.analysis;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;

public class CorrelationRuleReport {
  private CorrelationRule rule;
  private boolean extractorApplied;
  private boolean replacementApplied;
  private AnalysisReporter.Report extractorReport;
  private AnalysisReporter.Report replacementReport;

  public CorrelationRule getRule() {
    return rule;
  }

  public void setRule(CorrelationRule rule) {
    this.rule = rule;
  }

  public boolean isExtractorApplied() {
    return extractorApplied;
  }

  public void setExtractorApplied(boolean extractorApplied) {
    this.extractorApplied = extractorApplied;
  }

  public boolean isReplacementApplied() {
    return replacementApplied;
  }

  public void setReplacementApplied(boolean replacementApplied) {
    this.replacementApplied = replacementApplied;
  }

  public AnalysisReporter.Report getExtractorReport() {
    return extractorReport;
  }

  public void setExtractorReport(
      AnalysisReporter.Report extractorReport) {
    this.extractorReport = extractorReport;
  }

  public AnalysisReporter.Report getReplacementReport() {
    return replacementReport;
  }

  public void setReplacementReport(
      AnalysisReporter.Report replacementReport) {
    this.replacementReport = replacementReport;
  }

  public boolean didApply() {
    return extractorApplied || replacementApplied;
  }
}
