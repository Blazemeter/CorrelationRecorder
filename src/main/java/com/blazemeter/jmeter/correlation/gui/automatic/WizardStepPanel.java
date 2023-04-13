package com.blazemeter.jmeter.correlation.gui.automatic;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.automatic.ReplayReport;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will hold the common methods for the different steps of the Correlation Wizard.
 */
public class WizardStepPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(WizardStepPanel.class);
  protected JDialog runDialog;
  protected Supplier<CorrelationHistory> getCorrelationHistorySupplier;
  protected Runnable displaySuggestionsPanel;
  protected Runnable displayTemplateSelectionPanel;
  protected Runnable toggleWizardVisibility;
  protected Supplier<String> getRecordingTraceSupplier;
  protected Supplier<String> getReplayTraceSupplier;
  protected Supplier<String> getLastTestPlanSupplier;
  protected Consumer<String> logStepConsumer;
  protected Consumer<List<CorrelationRule>> exportRulesConsumer;
  protected SwingWorker<ReplayReport, Void> replay;

  public WizardStepPanel() {
    super();
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

  public CorrelationHistory getCorrelationHistory() {
    return getCorrelationHistorySupplier.get();
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

  protected void displayWaitingScreen(String message) {
    runDialog = JMeterElementUtils.makeWaitingFrame(message);
    runDialog.pack();
    runDialog.repaint();
    runDialog.setAlwaysOnTop(true);
    runDialog.setVisible(true);
    runDialog.toFront();
  }

  protected SwingWorker setupReplayWorker() {
    replay = new SwingWorker<ReplayReport, Void>() {
      @Override
      public ReplayReport doInBackground() {
        try {
          return getCurrentTestPlanReplayErrors();
        } catch (Exception ex) {
          LOG.error("Error while processing the Replay", ex);
          replay.cancel(true);
          disposeWaitingDialog();
          toggleWizardVisibility();
          return null;
        }
      }

      @Override
      public void done() {
        runDialog.dispose();
      }
    };
    return replay;
  }

  protected void startReplayWorker() {
    replay.execute();
  }

  protected ReplayReport getReplayReport() {
    try {
      return replay.get();
    } catch (Exception ex) {
      LOG.error("Error while replaying the recording. ", ex);
      return null;
    }
  }

  protected ReplayReport getCurrentTestPlanReplayErrors() {
    String currentTestPlan = JMeterElementUtils.saveTestPlanSnapshot();
    LOG.info("Current Test Plan: {}", currentTestPlan);
    String recordingTraceFilepath = getRecordingTraceSupplier.get();
    LOG.info("Recording Trace Filepath: {}", recordingTraceFilepath);
    CorrelationHistory history = getCorrelationHistorySupplier.get();
    LOG.info("Correlation History: {}", history);

    if (isEmpty(currentTestPlan) || isEmpty(recordingTraceFilepath) || history == null) {
      LOG.error("Apparently, there is no steps to replay");
      return null;
    }

    return new JMeterElementUtils()
        .getReplayErrors(currentTestPlan, recordingTraceFilepath, history);
  }

  private boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }

  public void disposeWaitingDialog() {
    runDialog.dispose();
    runDialog.setVisible(false);
  }
}
