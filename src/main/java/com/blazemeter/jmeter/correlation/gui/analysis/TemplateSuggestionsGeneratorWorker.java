package com.blazemeter.jmeter.correlation.gui.analysis;

import static com.blazemeter.jmeter.correlation.gui.analysis.CorrelationTemplatesSelectionPanel.DRAFT_REPOSITORY_NAME;

import com.blazemeter.jmeter.correlation.core.analysis.Analysis;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.WaitingDialog;
import com.blazemeter.jmeter.correlation.core.suggestions.InterruptibleWorkerAgreement;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.repository.Properties;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateSuggestionsGeneratorWorker extends
    SwingWorker<List<CorrelationSuggestion>, String> implements InterruptibleWorkerAgreement {

  private static final Logger LOG = LoggerFactory.getLogger(
      TemplateSuggestionsGeneratorWorker.class);
  private final CorrelationTemplatesSelectionPanel templatePanel;
  private final Analysis analysis = new Analysis();
  private final TemplateBundle templateBundle;
  private final String traceFilePath;
  private boolean isRunning = false;
  private Timer timer;

  public TemplateSuggestionsGeneratorWorker(CorrelationTemplatesSelectionPanel panel) {
    this.templatePanel = panel;
    traceFilePath = templatePanel.getTraceFilePath();
    templateBundle = new TemplateBundle(
        templatePanel.getSelectedTemplateWithRepositoryMap(),
        templatePanel.getDraftTemplate(),
        templatePanel.getRepositoriesConfiguration());
    this.addPropertyChangeListener(this);
  }

  @Override
  protected void process(List<String> chunks) {
    String message = chunks.get(chunks.size() - 1);
    WaitingDialog.changeWaitingMessage(message);
  }

  @Override
  protected void done() {
    WaitingDialog.disposeWaitingDialog();
  }

  @Override
  protected List<CorrelationSuggestion> doInBackground() {
    WaitingDialog.displayWaitingScreen("Generating Suggestions from templates",
        "Analysing templates",
        templatePanel);
    WaitingDialog.addWindowAdapter(getWindowAdapter());
    isRunning = true;

    List<Template> canUseTemplates = templateBundle.getCanUseTemplates();
    List<CorrelationSuggestion> suggestions = new ArrayList<>();
    for (Template version : canUseTemplates) {
      if (isCancelled()) {
        break;
      }
      publish("Running Analysis for:\n\t-" + RepositoryUtils.getTemplateInfo(version));
      Map<Template, List<CorrelationSuggestion>> generatedSuggestions =
          analysis.run(Collections.singletonList(version),
              traceFilePath, false);
      for (Entry<Template, List<CorrelationSuggestion>> entry
          : generatedSuggestions.entrySet()) {
        for (CorrelationSuggestion suggestion : entry.getValue()) {
          suggestion.setSource(version);
          suggestions.add(suggestion);
        }
      }
    }
    isRunning = false;
    return suggestions;
  }

  public boolean isRunning() {
    return isRunning;
  }

  @Override
  public void onInterruption() {
    if (!isRunning) {
      return;
    }
    TemplateSuggestionsGeneratorWorker.this.cancel(true);
    WaitingDialog.displayWaitingScreen("Abort operation", "Waiting for Analysis to be "
        + "terminated", TemplateSuggestionsGeneratorWorker.this.templatePanel);
    timer = new Timer(3000, event -> {
      if (TemplateSuggestionsGeneratorWorker.this.isRunning()) {
        timer.restart();
      } else {
        WaitingDialog.disposeWaitingDialog();
        timer.stop();
        JOptionPane.showMessageDialog(TemplateSuggestionsGeneratorWorker.this.templatePanel,
            "Suggestion generation stopped");
        TemplateSuggestionsGeneratorWorker.this.firePropertyChange(ON_FAILURE_ENDED_PROPERTY,
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
      List<CorrelationSuggestion> suggestions;
      try {
        suggestions = this.get();
      } catch (InterruptedException | ExecutionException e) {
        JOptionPane.showMessageDialog(templatePanel,
            "There was an unexpected error while retrieving suggestions");
        LOG.error("Error while generating suggestions from Templates", e);
        Thread.currentThread().interrupt();
        return;
      } catch (CancellationException e) {
        // Exception captured and ignored since this case is handled by the worker itself as
        // recovery plan when user cancels the generation of suggestions see onInterruption()
        return;
      }
      templatePanel.sendSuggestionsToWizard(suggestions);
      templatePanel.enableContinueButton(true);
    } else if (evt.getPropertyName().equals(ON_FAILURE_ENDED_PROPERTY)) {
      templatePanel.enableContinueButton(true);
    }
  }

  public boolean isSelectedCanNotUseTemplates() {
    return templateBundle.isSelectedCanNotUseTemplates();
  }

  public String getCanNotUseTemplatesName() {
    return templateBundle.getCanNotUseTemplatesName();
  }

  private static class TemplateBundle {

    private final List<Template> canUseTemplates = new ArrayList<>();
    private final List<Template> cannotUseTemplates = new ArrayList<>();
    private final Map<String, List<TemplateVersion>> templateWithRepositoryMap;
    private final Template draftTemplate;
    private final CorrelationTemplatesRepositoriesConfiguration config;

    TemplateBundle(Map<String, List<TemplateVersion>> templateWithRepositoryMap,
                   Template draftTemplate,
                   CorrelationTemplatesRepositoriesConfiguration config) {

      this.templateWithRepositoryMap = templateWithRepositoryMap;
      this.draftTemplate = draftTemplate;
      this.config = config;
      resolveSelectedTemplates();
    }

    public List<Template> getCanUseTemplates() {
      return canUseTemplates;
    }

    public boolean isSelectedCanNotUseTemplates() {
      return !cannotUseTemplates.isEmpty();
    }

    public String getCanNotUseTemplatesName() {
      return cannotUseTemplates.stream().map(RepositoryUtils::getTemplateInfo)
          .collect(Collectors.joining("\n"));
    }

    public void resolveSelectedTemplates() {
      boolean isDraft = false;
      for (Entry<String, List<TemplateVersion>> entry : templateWithRepositoryMap.entrySet()) {
        String repositoryName = entry.getKey();
        if (repositoryName.equals(DRAFT_REPOSITORY_NAME)) {
          isDraft = !draftTemplate.getGroups().isEmpty();
          continue;
        }
        List<TemplateVersion> templates = entry.getValue();
        RepositoryManager repManager = config.getRepositoryManager(repositoryName);

        Map<Template, TemplateProperties> templatesAndProperties =
            repManager.getTemplatesAndProperties(
                templates);

        if (templatesAndProperties == null || templatesAndProperties.isEmpty()) {
          // Get all the templates and properties for the local repository and filter the selected
          templatesAndProperties =
              config.getCorrelationTemplatesAndPropertiesByRepositoryName(
                      repositoryName, true).entrySet().stream()
                  .filter(templateEntry -> templates.stream()
                      .anyMatch(
                          t -> templateEntry.getKey().getId().equals(t.getName())
                              && templateEntry.getKey().getVersion().equals(t.getVersion())))
                  .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }

        for (Entry<Template, TemplateProperties> templateEntry
            : templatesAndProperties.entrySet()) {
          TemplateProperties value = templateEntry.getValue();
          Properties properties = new Properties();
          properties.putAll(value);

          if (properties.canUse()) {
            canUseTemplates.add(templateEntry.getKey());
          } else {
            cannotUseTemplates.add(templateEntry.getKey());
          }
        }
      }
      if (isDraft) {
        canUseTemplates.add(draftTemplate);
      }
    }
  }
}
