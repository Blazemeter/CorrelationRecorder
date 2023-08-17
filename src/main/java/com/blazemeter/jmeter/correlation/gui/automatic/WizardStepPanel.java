package com.blazemeter.jmeter.correlation.gui.automatic;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will hold the common methods for the different steps of the Correlation Wizard.
 */
public class WizardStepPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(WizardStepPanel.class);
  protected Supplier<CorrelationHistory> getCorrelationHistorySupplier;
  protected Runnable displaySuggestionsPanel;
  protected Runnable displayTemplateSelectionPanel;

  protected Runnable displaySelectMethodPanel;
  protected Runnable toggleWizardVisibility;
  protected Supplier<String> getRecordingTraceSupplier;
  protected Supplier<String> getReplayTraceSupplier;
  protected Supplier<String> getLastTestPlanSupplier;
  protected Consumer<String> logStepConsumer;
  protected Consumer<List<CorrelationRule>> exportRulesConsumer;
  protected CorrelationTemplatesRepositoriesConfiguration manager;
  protected CorrelationWizard wizard;
  private Runnable replayTestPlan;

  public WizardStepPanel(CorrelationWizard wizard) {
    super();
    this.wizard = wizard;
  }

  public void setDisplaySuggestionsPanel(Runnable displaySuggestionsPanel) {
    this.displaySuggestionsPanel = displaySuggestionsPanel;
  }

  public void setDisplayTemplateSelectionPanel(Runnable displayTemplateSelectionPanel) {
    this.displayTemplateSelectionPanel = displayTemplateSelectionPanel;
  }

  public void setLogStepConsumer(Consumer<String> logStepConsumer) {
    this.logStepConsumer = logStepConsumer;
  }

  public void setGetRecordingTraceSupplier(Supplier<String> getRecordingTraceSupplier) {
    this.getRecordingTraceSupplier = getRecordingTraceSupplier;
  }

  public void setGetReplayTraceSupplier(Supplier<String> getReplayTraceSupplier) {
    this.getReplayTraceSupplier = getReplayTraceSupplier;
  }

  public void setGetCorrelationHistorySupplier(
      Supplier<CorrelationHistory> getCorrelationHistorySupplier) {
    this.getCorrelationHistorySupplier = getCorrelationHistorySupplier;
  }

  public void setGetLastTestPlanSupplier(Supplier<String> getLastTestPlanSupplier) {
    this.getLastTestPlanSupplier = getLastTestPlanSupplier;
  }

  public void setToggleWizardVisibility(Runnable toggleWizardVisibility) {
    this.toggleWizardVisibility = toggleWizardVisibility;
  }

  public void toggleWizardVisibility() {
    toggleWizardVisibility.run();
  }

  public void displaySuggestionsPanel() {
    displaySuggestionsPanel.run();
  }

  public void displayTemplateSelectionPanel() {
    displayTemplateSelectionPanel.run();
  }

  public Consumer<List<CorrelationRule>> getExportRulesConsumer() {
    return exportRulesConsumer;
  }

  public void setExportRulesConsumer(
      Consumer<List<CorrelationRule>> exportRulesConsumer) {
    this.exportRulesConsumer = exportRulesConsumer;
  }

  public void exportRules(List<CorrelationRule> rules) {
    exportRulesConsumer.accept(rules);
  }

  public void setDisplayMethodSelectionPanel(Runnable displayMethodSelection) {
    this.displaySelectMethodPanel = displayMethodSelection;
  }

  public void displayMethodSelectionPanel() {
    displaySelectMethodPanel.run();
  }

  public void setReplayTestPlan(Runnable replayTestPlan) {
    this.replayTestPlan = replayTestPlan;
  }

  public void replayTestPlan() {
    replayTestPlan.run();
  }
}
