package com.blazemeter.jmeter.correlation.gui.automatic;

import static org.slf4j.LoggerFactory.getLogger;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.analysis.AnalysisReporter;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.automatic.ReplayReport;
import com.blazemeter.jmeter.correlation.core.automatic.ReplayWorker;
import com.blazemeter.jmeter.correlation.core.automatic.ReplayWorker.ReplayWorkerArrivalContext;
import com.blazemeter.jmeter.correlation.core.suggestions.SuggestionGenerator;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Repository;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.Template.Builder;
import com.blazemeter.jmeter.correlation.gui.analysis.CorrelationTemplatesSelectionPanel;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jorphan.gui.ComponentUtil;
import org.slf4j.Logger;

/**
 * This class will be the main class for the correlation wizard.
 * It will display the different steps to configure the correlation:
 * 1. Display the "Select the method of correlation" JPanel
 * 2. Display the CorrelationRulesSelectionPanel for the Template's Selection
 * (this should be the AnalysisPanel)
 * 3. Display the CorrelationSuggestionsPanel so the User can select which suggestions to apply
 */
public class CorrelationWizard extends JDialog {

  private static final Logger LOG = getLogger(CorrelationWizard.class);
  private static final long serialVersionUID = 1L;
  private static final String TITLE = "Correlation Wizard";
  protected ReplayWorker replayWorker;

  protected JDialog runDialog;
  private CorrelationMethodPanel selectMethodPanel;
  private CorrelationSuggestionsPanel suggestionsPanel;
  private CorrelationTemplatesSelectionPanel templateSelectionPanel;
  private CorrelationHistory history;
  private Supplier<List<Template>> versionsSupplier;
  private Supplier<Map<String, Repository>> repositoriesSupplier;
  private Consumer<List<CorrelationRule>> exportRulesConsumer;

  private CorrelationTemplatesRepositoriesConfiguration repositoriesConfiguration;

  private LocalConfiguration configuration;

  private SuggestionGenerator suggestionGenerator;
  private Function<Builder, Template> buildTemplate;

  public CorrelationWizard() {
    super();
    setTitle(TITLE);
    getContentPane().setLayout(new GridLayout());
  }

  public void init() {
    selectMethodPanel = new CorrelationMethodPanel(this);
    this.setAlwaysOnTop(true);
    initCorrelateByComparisonPanels();
    initCorrelateByRulesPanels();
    initWizardStepsCallbacks();
    ComponentUtil.centerComponentInWindow(this);
  }

  public void setRepositoriesConfiguration(
      CorrelationTemplatesRepositoriesConfiguration repositoriesConfiguration) {
    this.repositoriesConfiguration = repositoriesConfiguration;
  }

  private void initCorrelateByComparisonPanels() {
    suggestionsPanel = new CorrelationSuggestionsPanel(this);
  }

  private void initCorrelateByRulesPanels() {
    templateSelectionPanel = new CorrelationTemplatesSelectionPanel(this);
    if (history != null) {
      templateSelectionPanel.setGetCorrelationHistorySupplier(() -> history);
    }
    templateSelectionPanel.setBuildTemplate(buildTemplate);

    templateSelectionPanel.setStartNonCorrelatedAnalysis(startNonCorrelatedAnalysis());
  }

  private BiConsumer<List<Template>, String> startNonCorrelatedAnalysis() {
    return (selectedTemplates, tracePath) -> {
      history.addAnalysisStep("Before applying rules analysis",
          JMeterElementUtils.saveTestPlanSnapshot(), tracePath);

      List<CorrelationSuggestion> suggestions = new ArrayList<>();
      for (Template version : selectedTemplates) {
        templateSelectionPanel.runNonCorrelatedAnalysis(Collections.singletonList(version),
            tracePath);
        List<CorrelationSuggestion> generatedSuggestions =
            AnalysisReporter.generateCorrelationSuggestions();

        for (CorrelationSuggestion suggestion : generatedSuggestions) {
          suggestion.setSource(version);
          suggestions.add(suggestion);
        }
      }

      if (suggestions.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "The analysis was completed successfully, but we did not find any suggestions." +
                System.lineSeparator() + "Please review the rules you are applying and try again.");
        hideWizard();
        return;
      }
      suggestionsPanel.loadSuggestions(suggestions);
      suggestionsPanel.setReplaySelectionMethod(this::displayMethodSelection);

      // Reminder: This is called when the Suggestions were generated by Analysis

      displaySuggestions();
      suggestionsPanel.displaySuggestionsTab();
      JOptionPane.showMessageDialog(this,
          "The analysis was completed successfully!" + System.lineSeparator() +
              "We generated the following suggestions for you. " + System.lineSeparator() +
              System.lineSeparator() + "Please review them and apply them if you agree with them.");
    };
  }

  private void applySuggestionsFromTemplate(List<Template> selectedTemplates, String tracePath) {
    history.addAnalysisStep("Before applying correlations",
        JMeterElementUtils.saveTestPlanSnapshot(), tracePath);
    templateSelectionPanel.runCorrelatedAnalysis(selectedTemplates, tracePath);
    JOptionPane.showMessageDialog(this,
        "The suggestions were applied successfully to your Test Plan." +
            System.lineSeparator() + "Please review the changes and, when you are ready, " +
            "replay to review the results.");
    history.addAnalysisStep("After applying correlations",
            JMeterElementUtils.saveTestPlanSnapshot(), tracePath);
    hideWizard();
  }

  private void hideWizard() {
    setVisible(false);
  }

  private void initWizardStepsCallbacks() {
    List<WizardStepPanel> steps =
        Arrays.asList(selectMethodPanel, suggestionsPanel, templateSelectionPanel);
    for (WizardStepPanel step : steps) {
      step.setGetCorrelationHistorySupplier(() -> history);
      step.setDisplaySuggestionsPanel(this::loadAndDisplaySuggestions);
      step.setDisplayTemplateSelectionPanel(this::displayTemplateSelection);
      step.setDisplayMethodSelectionPanel(this::displayMethodSelection);
      step.setLogStepConsumer(this::logStep);
      step.setGetRecordingTraceSupplier(() -> history.getOriginalRecordingTrace());
      step.setGetReplayTraceSupplier(() -> history.getLastReplayTraceFilepath());
      step.setGetLastTestPlanSupplier(() -> history.getLastTestPlanFilepath());
      step.setToggleWizardVisibility(this::toggleWizardVisibility);
      step.setReplayTestPlan(this::requestPermissionToReplay);
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

    suggestionsPanel.setReplaySelectionMethod(this::displayMethodSelection);
    suggestionsPanel.setAutoCorrelateMethod(() -> {
      displayApplyingSuggestionsWaitingScreen();
      SwingWorker swAnalysis = new SwingWorker() {
        @Override
        protected String doInBackground() {
          suggestionsPanel.applySuggestions();
          return null;
        }

        @Override
        protected void done() {
          disposeWaitingDialog();
          JOptionPane.showMessageDialog(CorrelationWizard.this,
              "The suggestions were applied successfully to your Test Plan." +
                  System.lineSeparator() + "Please review the changes and, when you are ready, " +
                  "replay to review the results.");
          hideWizard();
        }
      };
      swAnalysis.execute();
    });

    if (replayWorker == null) {
      setupReplayWorker();
      replayWorker.setReplayWorkerArrivalContext(ReplayWorkerArrivalContext.CORRELATION_METHOD);
      displayReplayWaitingScreen();
      startReplayWorker();
      return;
    }

    ReplayReport replayReport = replayWorker.getReplayReport();
    if (replayReport == null) {
      displayTemplateSelection();
      startReplayWorker();
    }

    if (replayReport != null) {
      int totalNewErrors = replayReport.getTotalNewErrors();
      displayGeneratingSuggestionsWaitingScreen();
      SwingWorker swSuggestionsGeneration = new SwingWorker() {
        @Override
        protected String doInBackground() {
          suggestionsPanel.triggerSuggestionsGeneration(totalNewErrors);
          return null;
        }

        @Override
        protected void done() {
          disposeWaitingDialog();
          displaySuggestions();
        }
      };
      swSuggestionsGeneration.execute();
    } else {
      LOG.warn("Replay report is null, cannot generate suggestions.");
    }
  }

  public void displaySuggestions() {
    getContentPane().removeAll();
    getContentPane().add(suggestionsPanel);
    updateView(60);
  }

  public void displayTemplateSelection() {
    getContentPane().removeAll();
    getContentPane().add(templateSelectionPanel);
    templateSelectionPanel.loadPanel();
    templateSelectionPanel.setRecordingTrace();
    updateView(60);
  }

  @VisibleForTesting
  public void displayTemplateSelection(String recordingTrace) {
    getContentPane().removeAll();
    getContentPane().add(templateSelectionPanel);
    templateSelectionPanel.loadCorrelationTemplates();
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

  public void setHistory(CorrelationHistory history) {
    this.history = history;
  }

  public void setVersionsSupplier(Supplier<List<Template>> versionsSupplier) {
    this.versionsSupplier = versionsSupplier;
  }

  public void setRepositoriesSupplier(Supplier<Map<String, Repository>> repositoriesSupplier) {
    this.repositoriesSupplier = repositoriesSupplier;
  }

  public void requestPermissionToReplay() {
    int confirmReplay = JOptionPane.showConfirmDialog(this,
        "Start detection of dynamic values for auto correlation.\n" +
            "The recording will be replayed in the background as part of this process.\n" +
            "\n\nDo you want to continue?", TITLE, JOptionPane.YES_NO_OPTION);

    if (confirmReplay != JOptionPane.YES_OPTION) {
      hideWizard();
      return;
    }

    replayTestPlan();
  }

  private void replayTestPlan() {
    setupReplayWorker();
    replayWorker.setReplayWorkerArrivalContext(ReplayWorkerArrivalContext.REPLAY_TEST_PLAN);
    displayReplayWaitingScreen();
    startReplayWorker();
  }

  public static int requestPermissionToCorrelate(int totalErrors, Component parent) {
    return JOptionPane.showConfirmDialog(parent, getAfterReplayErrorMessage(totalErrors), TITLE,
        JOptionPane.YES_NO_OPTION);
  }

  public static String getAfterReplayErrorMessage(int totalErrors) {
    boolean manyErrors = totalErrors > 1;
    return "After replaying the test plan, " + totalErrors + " request" + (manyErrors ? "s" : "") +
        " failed. We will try to generate correlation suggestions to fix " +
        (manyErrors ? "them." : "it.") + "\n\nDo you want to continue?";
  }

  public void setAddRuleConsumer(Consumer<List<CorrelationRule>> exportRulesConsumer) {
    this.exportRulesConsumer = exportRulesConsumer;
  }

  public void setLocationRelativeTo(Component component) {
    super.setLocationRelativeTo(component);
  }

  public void setupReplayWorker() {
    replayWorker = new ReplayWorker();
    replayWorker.setMethodToRun(this::getCurrentTestPlanReplayErrors);
    replayWorker.setOnDoneMethod(this::endReplayAndReport);
    replayWorker.setOnFailureMethod((exception) -> {
      replayWorker.cancel(true);
      onFailReplay(exception);
    });
  }

  protected ReplayReport getCurrentTestPlanReplayErrors() {
    String currentTestPlan = JMeterElementUtils.saveTestPlanSnapshot();
    String recordingTraceFilepath = history.getOriginalRecordingTrace();
    LOG.info("Current Test Plan: {}", currentTestPlan);
    LOG.info("Recording Trace Filepath: {}", recordingTraceFilepath);
    LOG.info("Correlation History: {}", history);

    if (isEmpty(currentTestPlan) || isEmpty(recordingTraceFilepath) || history == null) {
      LOG.error("Apparently, there are no steps to replay");
      return null;
    }

    return new JMeterElementUtils().getReplayErrors(currentTestPlan, recordingTraceFilepath,
        history);
  }

  private boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }

  public void endReplayAndReport() {
    disposeWaitingDialog();
    showReport();
  }

  public void showReport() {
    ReplayReport replayReport = replayWorker.getReplayReport();
    if (replayReport == null) {
      JOptionPane.showMessageDialog(this, "Replay failed, please check the logs for more details.",
          TITLE, JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (!wantsToAutoCorrelate(replayReport, this)) {
      return;
    }

    switch (replayWorker.getReplayWorkerArrivalContext()) {
      case REPLAY_TEST_PLAN:
        displayMethodSelection();
        break;
      case CORRELATION_METHOD:
        loadAndDisplaySuggestions();
        break;
      default:
        LOG.error("Unexpected arrival path to replay worker, please contact support");
    }
  }

  public static boolean wantsToAutoCorrelate(ReplayReport report, Component parent) {
    return wantToAutoCorrelate(report, parent) == JOptionPane.YES_OPTION;
  }

  public static int wantToAutoCorrelate(ReplayReport report, Component parent) {
    int totalErrors = report.getTotalNewErrors();
    if (totalErrors == 0) {
      JOptionPane.showMessageDialog(parent,
          "After replaying the test plan, no new errors were found.", TITLE,
          JOptionPane.INFORMATION_MESSAGE);
      return JOptionPane.CANCEL_OPTION;
    }

    return requestPermissionToCorrelate(totalErrors, parent);
  }

  public void onFailReplay(Exception ex) {
    LOG.error("Error while replaying the recording. {}", ex.getMessage(), ex);
    disposeWaitingDialog();
    toggleWizardVisibility();
    JOptionPane.showMessageDialog(this,
        "Error while replaying the recording. " + System.lineSeparator() +
            "Check the logs for more details. " + System.lineSeparator() + System.lineSeparator() +
            "Error message:  " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
  }

  public void displayReplayWaitingScreen() {
    displayWaitingScreen("We are replaying the test plan, please wait...");
  }

  protected void displayWaitingScreen(String message) {
    runDialog = JMeterElementUtils.makeWaitingFrame(message);
    runDialog.pack();
    runDialog.repaint();
    runDialog.setAlwaysOnTop(true);
    runDialog.setVisible(true);
    runDialog.toFront();
  }

  public void disposeWaitingDialog() {
    runDialog.dispose();
    runDialog.setVisible(false);
  }

  protected void startReplayWorker() {
    replayWorker.execute();
  }

  public void displayApplyingSuggestionsWaitingScreen() {
    displayWaitingScreen("We are applying the suggestions, please wait...");
  }

  public void displayGeneratingSuggestionsWaitingScreen() {
    displayWaitingScreen("We are generating suggestions, please wait...");
  }

  public CorrelationTemplatesRepositoriesConfiguration getRepositoriesConfiguration() {
    return this.repositoriesConfiguration;
  }

  public Supplier<Map<String, Repository>> getRepositoriesSupplier() {
    return repositoriesSupplier;
  }

  public LocalConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(LocalConfiguration configuration) {
    this.configuration = configuration;
  }

  public void setBuildTemplateProvider(Function<Builder, Template> buildTemplate) {
    this.buildTemplate = buildTemplate;
  }

  public Function<Builder, Template> getBuildTemplate() {
    return buildTemplate;
  }
}
