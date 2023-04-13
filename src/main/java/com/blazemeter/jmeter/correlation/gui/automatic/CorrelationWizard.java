package com.blazemeter.jmeter.correlation.gui.automatic;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.analysis.AnalysisReporter;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.automatic.ReplayReport;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.gui.analysis.CorrelationTemplatesSelectionPanel;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jorphan.gui.ComponentUtil;

/**
 * This class will be the main class for the correlation wizard.
 * It will display the different steps to configure the correlation:
 * 1. Display the "Select the method of correlation" JPanel
 * 2. Display the CorrelationRulesSelectionPanel for the Template's Selection
 * (this should be the AnalysisPanel)
 * 3. Display the CorrelationSuggestionsPanel so the User can select which suggestions to apply
 */
public class CorrelationWizard extends JDialog {

  private static final long serialVersionUID = 1L;
  private static final String TITLE = "Correlation Wizard";
  private WizardStepPanel replayStep;
  private ReplayReport replayReport;
  private CorrelationMethodPanel selectMethodPanel;
  private CorrelationSuggestionsPanel suggestionsPanel;
  private CorrelationTemplatesSelectionPanel templateSelectionPanel;
  private CorrelationHistory history;

  private Supplier<List<TemplateVersion>> versionsSupplier;
  private Consumer<List<CorrelationRule>> exportRulesConsumer;

  public CorrelationWizard() {
    super();
    setTitle(TITLE);
    getContentPane().setLayout(new GridLayout());
  }

  public void init() {
    replayStep = new WizardStepPanel();
    selectMethodPanel = new CorrelationMethodPanel();
    this.setAlwaysOnTop(true);
    initCorrelateByComparisonPanels();
    initCorrelateByRulesPanels();
    initWizardStepsCallbacks();
    ComponentUtil.centerComponentInWindow(this);
  }

  private void initCorrelateByComparisonPanels() {
    suggestionsPanel = new CorrelationSuggestionsPanel();
  }

  private void initCorrelateByRulesPanels() {
    templateSelectionPanel = new CorrelationTemplatesSelectionPanel(versionsSupplier);
    if (history != null) {
      templateSelectionPanel.setGetCorrelationHistorySupplier(() -> history);
    }

    templateSelectionPanel.setStartNonCorrelatedAnalysis(startNonCorrelatedAnalysis());
  }

  private BiConsumer<List<TemplateVersion>, String> startNonCorrelatedAnalysis() {
    return (selectedTemplates, tracePath) -> {
      history.addAnalysisStep("Before applying Rules Analysis (Non-Correlated)",
          JMeterElementUtils.saveTestPlanSnapshot(), tracePath);

      templateSelectionPanel.runNonCorrelatedAnalysis(selectedTemplates, tracePath);
      List<CorrelationSuggestion> suggestions = AnalysisReporter.generateCorrelationSuggestions();
      if (suggestions.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "The analysis was completed successfully, but we did not find any suggestions."
                + System.lineSeparator()
                + "Please review the rules you are applying and try again.");
        hideWizard();
        return;
      }

      suggestionsPanel.loadSuggestions(suggestions);

      suggestionsPanel.setReplaySelectionMethod(() -> {
        displayMethodSelection();
      });

      //We set the "Correlate Method" for the SuggestionsPanel because apply the Suggestions
      //using the same method as the one used for the Analysis.
      suggestionsPanel.setAutoCorrelateMethod(() -> {
        history.addAnalysisStep("Before applying Rules Analysis (Correlated)",
            JMeterElementUtils.saveTestPlanSnapshot(), tracePath);
        templateSelectionPanel.runCorrelatedAnalysis(selectedTemplates, tracePath);
        JOptionPane.showMessageDialog(this,
            "The suggestions were applied successfully to your Test Plan."
                + System.lineSeparator()
                + "Please review the changes and, when you are ready, "
                + "replay to review the results.");
        hideWizard();
      });
      displaySuggestions();
      suggestionsPanel.displaySuggestionsTab();
      JOptionPane.showMessageDialog(this, "The analysis was completed successfully!"
          + System.lineSeparator()
          + "We generated the following suggestions for you. "
          + System.lineSeparator() + System.lineSeparator()
          + "Please review them and apply them if you agree with them.");
    };
  }

  private void hideWizard() {
    setVisible(false);
  }

  private void initWizardStepsCallbacks() {
    List<WizardStepPanel> steps = Arrays.asList(replayStep, selectMethodPanel, suggestionsPanel,
        templateSelectionPanel);
    for (WizardStepPanel step : steps) {
      step.setGetCorrelationHistorySupplier(() -> history);
      step.setDisplaySuggestionsPanel(this::loadAndDisplaySuggestions);
      step.setDisplayTemplateSelectionPanel(this::displayTemplateSelection);
      step.setLogStepConsumer(this::logStep);
      step.setGetCorrelationHistorySupplier(() -> history);
      step.setGetRecordingTraceSupplier(() -> history.getOriginalRecordingTrace());
      step.setGetReplayTraceSupplier(() -> history.getLastReplayTraceFilepath());
      step.setGetLastTestPlanSupplier(() -> history.getLastTestPlanFilepath());
      step.setToggleWizardVisibility(this::toggleWizardVisibility);
      step.setExportRulesConsumer(exportRulesConsumer);
    }
  }

  private void toggleWizardVisibility() {
    setVisible(!isVisible());
  }

  public void displayMethodSelection() {
    getContentPane().removeAll();
    getContentPane().add(selectMethodPanel);
    updateView(17);
  }

  private void updateView(int percentage) {
    if (isNonGui()) {
      return;
    }

    pack();
    ComponentUtil.centerComponentInWindow(this, percentage == 0 ? 80 : percentage);
    setVisible(true);
  }

  private boolean isNonGui() {
    return GuiPackage.getInstance() == null;
  }

  public void loadAndDisplaySuggestions() {
    if (replayReport != null) {
      int totalNewErrors = replayReport.getTotalNewErrors();
      suggestionsPanel.triggerSuggestionsGeneration(totalNewErrors);
    }
    suggestionsPanel.setReplaySelectionMethod(() -> {
      displayMethodSelection();
    });
    suggestionsPanel.setAutoCorrelateMethod(() -> {
      suggestionsPanel.applySuggestions();
      JOptionPane.showMessageDialog(this,
          "The suggestions were applied successfully to your Test Plan."
              + System.lineSeparator()
              + "Please review the changes and, when you are ready, "
              + "replay to review the results.");
      hideWizard();
    });
    displaySuggestions();
  }

  public void displaySuggestions() {
    getContentPane().removeAll();
    getContentPane().add(suggestionsPanel);
    updateView(60);
  }

  public void displayTemplateSelection() {
    getContentPane().removeAll();
    getContentPane().add(templateSelectionPanel);
    templateSelectionPanel.reloadCorrelationTemplates();
    templateSelectionPanel.setRecordingTrace();
    updateView(60);
  }

  @VisibleForTesting
  public void displayTemplateSelection(String recordingTrace) {
    getContentPane().removeAll();
    getContentPane().add(templateSelectionPanel);
    templateSelectionPanel.reloadCorrelationTemplates();
    templateSelectionPanel.setRecordingTrace(recordingTrace);
    updateView(60);
  }

  @VisibleForTesting
  public CorrelationTemplatesSelectionPanel getTemplateSelectionPanel() {
    return templateSelectionPanel;
  }

  public void logStep(String message) {
    CorrelationHistory.Step step = new CorrelationHistory.Step(message);
    step.addCurrentTestPlan();
    history.addStep(step);
  }

  public void addStep(CorrelationHistory.Step step) {
    history.addStep(step);
  }

  public void setHistory(CorrelationHistory history) {
    this.history = history;
  }

  public void setVersionsSupplier(Supplier<List<TemplateVersion>> versionsSupplier) {
    this.versionsSupplier = versionsSupplier;
  }

  public void requestPermissionToReplay() {
    int confirmReplay = JOptionPane.showConfirmDialog(this,
        "Start detection of dynamic values for auto correlation.\n"
            + "The recording will be replayed in the background as part of this process.\n"
            + "\n\nDo you want to continue?",
        TITLE, JOptionPane.YES_NO_OPTION);

    if (confirmReplay != JOptionPane.YES_OPTION) {
      hideWizard();
      return;
    }

    SwingWorker replay = replayStep.setupReplayWorker();
    replay.addPropertyChangeListener(evt -> {
      String name = evt.getPropertyName();
      if ("state".equals(name)) {
        SwingWorker.StateValue state = (SwingWorker.StateValue) evt
            .getNewValue();
        if (state.equals(SwingWorker.StateValue.DONE)) {
          showReport();
        }
      }
    });
    replayStep.displayWaitingScreen("We are replaying the test plan, please wait...");
    replayStep.startReplayWorker();

  }

  private void showReport() {
    replayReport = replayStep.getReplayReport();
    if (replayReport == null) {
      JOptionPane.showMessageDialog(this,
          "Replay failed, please check the logs for more details.",
          TITLE, JOptionPane.ERROR_MESSAGE);
      return;
    }

    replayStep.disposeWaitingDialog();
    if (!wantsToAutoCorrelate(replayReport, this)) {
      return;
    }
    displayMethodSelection();
  }

  public static boolean wantsToAutoCorrelate(ReplayReport report, Component parent) {
    return wantToAutoCorrelate(report, parent) == JOptionPane.YES_OPTION;
  }

  public static int wantToAutoCorrelate(ReplayReport report, Component parent) {
    int totalErrors = report.getTotalNewErrors();
    if (totalErrors == 0) {
      JOptionPane.showMessageDialog(parent,
          "After replaying the test plan, no new errors were found.",
          TITLE, JOptionPane.INFORMATION_MESSAGE);
      return JOptionPane.CANCEL_OPTION;
    }

    return requestPermissionToCorrelate(totalErrors, parent);
  }

  public static int requestPermissionToCorrelate(int totalErrors, Component parent) {
    return JOptionPane.showConfirmDialog(parent,
        getAfterReplayErrorMessage(totalErrors),
        TITLE, JOptionPane.YES_NO_OPTION);
  }

  public static String getAfterReplayErrorMessage(int totalErrors) {
    boolean manyErrors = totalErrors > 1;
    return "After replaying the test plan, " + totalErrors
        + " request" + (manyErrors ? "s" : "")
        + " failed. We will try to generate correlation suggestions to fix "
        + (manyErrors ? "them." : "it.")
        + "\n\nDo you want to continue?";
  }

  public void setAddRuleConsumer(Consumer<List<CorrelationRule>> exportRulesConsumer) {
    this.exportRulesConsumer = exportRulesConsumer;
  }

  public void setLocationRelativeTo(Component component) {
    super.setLocationRelativeTo(component);
  }
}
