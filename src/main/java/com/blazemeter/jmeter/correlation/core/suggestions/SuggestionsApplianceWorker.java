package com.blazemeter.jmeter.correlation.core.suggestions;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.automatic.WaitingDialog;
import com.blazemeter.jmeter.correlation.core.suggestions.context.AnalysisContext;
import com.blazemeter.jmeter.correlation.core.suggestions.method.AnalysisMethod;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.gui.automatic.CorrelationSuggestionsPanel;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jorphan.collections.HashTree;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestionsApplianceWorker extends SwingWorker<Void, String> implements
    InterruptibleWorkerAgreement {

  private static final Logger LOG = LoggerFactory.getLogger(SuggestionsApplianceWorker.class);
  private final List<CorrelationSuggestion> suggestions;
  private final CorrelationSuggestionsPanel suggestionPanel;
  private final String recordingTracePath;
  private final Consumer<String> historyLogStep;
  private boolean isWorkerRunning;
  private Timer timer;
  private HashTree recoveryTestPlan;

  public SuggestionsApplianceWorker(CorrelationSuggestionsPanel panel) {
    this.suggestions = panel.exportSelectedSuggestions();
    this.suggestionPanel = panel;
    this.recordingTracePath = panel.getRecordingTraceSupplier();
    this.historyLogStep = panel.getHistoryLogStep();
    this.addPropertyChangeListener(this);
  }

  @Override
  protected void done() {
    WaitingDialog.disposeWaitingDialog();
    suggestionPanel.hideWizard();
  }

  @Override
  protected Void doInBackground() {
    Void result = null;
    WaitingDialog.displayWaitingScreen("Applying suggestions",
        "We are applying the suggestions, please wait...", suggestionPanel);
    WaitingDialog.addWindowAdapter(getWindowAdapter());
    saveTestPlanSnapShotForRecoveryPlan();
    isWorkerRunning = true;
    suggestions.clear();
    suggestions.addAll(suggestionPanel.exportSelectedSuggestions());
    if (suggestions.isEmpty()) {
      isWorkerRunning = false;
      WaitingDialog.disposeWaitingDialog();
      JOptionPane.showMessageDialog(suggestionPanel,
          "No suggestions selected. Please select at least one suggestion to " + "apply",
          "No suggestions selected", JOptionPane.INFORMATION_MESSAGE);
      return result;
    }

    historyLogStep.accept("(Save) Before apply suggestions");
    applySuggestions();
    historyLogStep.accept("(Save) After apply suggestions");

    isWorkerRunning = false;
    WaitingDialog.disposeWaitingDialog();

    JOptionPane.showMessageDialog(suggestionPanel,
        "The suggestions were applied successfully to your Test Plan." +
            System.lineSeparator() + "Please review the changes and, when you are ready, " +
            "replay to review the results.");

    JMeterElementUtils.refreshJMeter();

    return result;
  }

  private void saveTestPlanSnapShotForRecoveryPlan() {
    this.recoveryTestPlan = JMeterElementUtils.getTreeModel().getTestPlan();
  }

  private void applySuggestions() {
    /* When a set of suggestions is presented to be applied to the test as form of
    CorrelationRules it does not matter where were generated such suggestions: Analysis Method
    (based on template) or Comparison Method (automatic generated). It will always use the
    Analysis Method to apply them.
    * */
    AnalysisContext context = buildAnalysisContext();
    AnalysisMethod analysisMethod = new AnalysisMethod(context);
    List<RulesGroup> rulesGroups = getRulesGroups();
    analysisMethod.runAnalysis(rulesGroups, true);
  }

  private @NotNull AnalysisContext buildAnalysisContext() {
    AnalysisContext context = new AnalysisContext();
    context.setRecordingTraceFilePath(recordingTracePath);
    context.setRecordingTestPlan(JMeterElementUtils.getNormalizedTestPlan());
    context.setRegistry(CorrelationComponentsRegistry.getInstance());
    return context;
  }

  private @NotNull List<RulesGroup> getRulesGroups() {
    List<CorrelationRule> correlationRules = SuggestionsUtils.parseSuggestionsToRules(suggestions);
    return SuggestionsUtils.parseToGroup(correlationRules);
  }

  @Override
  public void onInterruption() {
    if (!isWorkerRunning) {
      return;
    }
    SuggestionsApplianceWorker.this.cancel(true);
    suggestionPanel.hideWizard();
    WaitingDialog.displayWaitingScreen("Abort operation",
        "Waiting for suggestions application to be terminated",
        SuggestionsApplianceWorker.this.suggestionPanel);
    timer = new Timer(3000, event -> {
      if (SuggestionsApplianceWorker.this.isWorkerRunning) {
        timer.restart();
      } else {
        WaitingDialog.disposeWaitingDialog();
        timer.stop();
        JOptionPane.showMessageDialog(SuggestionsApplianceWorker.this.suggestionPanel,
            "Suggestion application stopped");
        SuggestionsApplianceWorker.this.firePropertyChange(ON_FAILURE_ENDED_PROPERTY,
            false,
            true);
      }
    });
    timer.setRepeats(false);
    timer.start();
  }

  @Override
  public void onWorkerPropertyChange(PropertyChangeEvent evt) {
    if (evt.getPropertyName().equals("state") && evt.getNewValue().equals(StateValue.DONE)
        && isDone()) {
      try {
        Void ignore = this.get();
      } catch (InterruptedException | ExecutionException e) {
        WaitingDialog.disposeWaitingDialog();
        JOptionPane.showMessageDialog(suggestionPanel,
            "There was an unexpected error while applying suggestions");
        LOG.error("Error while applying suggestions", e);
        Thread.currentThread().interrupt();
      } catch (CancellationException e) {
        // Exception captured and ignored since this case is handled by the worker itself as
        // recovery plan when user cancels the generation of suggestions see below recovery plan
        // when ON_FAILURE_ENDED_PROPERTY
      }
    } else if (evt.getPropertyName().equals(ON_FAILURE_ENDED_PROPERTY)) {
      executeRecoveryPlan();
    }

  }

  private void executeRecoveryPlan() {
    JOptionPane.showMessageDialog(suggestionPanel, "Since application of suggestions was "
        + "interrupted Test Plan will be recovered as it was before appliance");
    GuiPackage guiPackage = GuiPackage.getInstance();
    guiPackage.clearTestPlan();
    try {
      JMeterElementUtils.convertSubTree(recoveryTestPlan);
      guiPackage.addSubTree(recoveryTestPlan);
    } catch (IllegalUserActionException e) {
      LOG.error("Error while attempting to recover TestPlan to before suggestions were applied", e);
    }
  }
}
