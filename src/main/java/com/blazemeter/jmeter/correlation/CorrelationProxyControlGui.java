package com.blazemeter.jmeter.correlation;

import com.blazemeter.jmeter.correlation.core.analysis.AnalysisReporter;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory;
import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Repository;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.Template.Builder;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import com.blazemeter.jmeter.correlation.gui.BlazemeterLabsLogo;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.gui.RulesContainer;
import com.blazemeter.jmeter.correlation.gui.automatic.CorrelationWizard;
import com.google.common.annotations.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import org.apache.jmeter.protocol.http.proxy.gui.ProxyControlGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationProxyControlGui extends ProxyControlGui
    implements CorrelationTemplatesRepositoriesRegistryHandler,
    CorrelationTemplatesRegistryHandler {

  public static Component mainParentGuiComponent;

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationProxyControlGui.class);
  private final RulesContainer rulesContainer;
  private CorrelationProxyControl model;
  private CorrelationHistory history;
  private CorrelationWizard wizard;

  static {
    LocalConfiguration.installDefaultFiles(JMeterUtils.getJMeterBinDir());
  }

  public CorrelationProxyControlGui() {
    JTabbedPane siebelPane = findTabbedPane();
    rulesContainer = new RulesContainer(this, () -> modifyTestElement(model));
    Objects.requireNonNull(siebelPane).add("Correlation", rulesContainer);
    add(new BlazemeterLabsLogo(), BorderLayout.SOUTH);

    wizard = new CorrelationWizard();
    wizard.setRepositoriesSupplier(this::getRepositories);
    wizard.setAddRuleConsumer(rulesContainer.obtainRulesExporter());
    wizard.init();

    rulesContainer.setOnWizardDisplayMethod(() -> wizard.displayMethodSelection());
    rulesContainer.setOnSuggestionsDisplayMethod(() -> wizard.displaySuggestions());
    rulesContainer.setEnableCorrelationConsumer((enableCorrelation)
        -> model.enableCorrelation(enableCorrelation));
    mainParentGuiComponent = getParent();

  }

  @VisibleForTesting
  public CorrelationProxyControlGui(CorrelationProxyControl model, RulesContainer container) {
    this.model = model;
    this.rulesContainer = container;
  }

  public void actionPerformed(ActionEvent action) {
    String command = action.getActionCommand();
    if ("start".equals(command)) {
      List<String> issues = new ArrayList<>();
      if (!model.isProperlyConfigured()) {
        issues.add(
            "- You are missing some configurations in your properties file (more info in the "
                + "logs)");
      }

      if (model.isLegacyEnabled()) {
        issues.add("- Legacy mode is enabled and you will not be able to " +
            "generate correlation suggestions.");
        if (!model.hasLoadedRules()) {
          issues.add("- Legacy mode is enabled and has no rules loaded, " +
              "no correlation will be performed.");
        }
      }

      if (!issues.isEmpty() && !model.areWarningsDisabled()) {
        String issuesConsolidatedMessage = "We noticed a few things before you start: "
            + System.lineSeparator() + System.lineSeparator()
            + String.join("\n", issues)
            + System.lineSeparator() + System.lineSeparator();
        LOG.info(issuesConsolidatedMessage);

        int response = JOptionPane.showConfirmDialog(null,
            issuesConsolidatedMessage
                + "Do you wish to continue regardless?",
            "Before we start recording", JOptionPane.YES_NO_OPTION);

        if (response != JOptionPane.YES_OPTION) {
          return;
        }

        AnalysisReporter.enableCorrelation();
      }
    }
    super.actionPerformed(action);
  }

  private JTabbedPane findTabbedPane() {
    LinkedList<Component> queue = new LinkedList<>(Arrays.asList(this.getComponents()));
    while (!queue.isEmpty()) {
      Component component = queue.removeFirst();
      if (component instanceof JTabbedPane) {
        return (JTabbedPane) component;
      } else if (component instanceof Container) {
        queue.addAll(Arrays.asList(((Container) component).getComponents()));
      }
    }
    return null;
  }

  @Override
  public String getStaticLabel() {
    return "bzm - Correlation Recorder";
  }

  @Override
  public void modifyTestElement(TestElement el) {
    super.modifyTestElement(el);
    if (el instanceof CorrelationProxyControl) {
      model = (CorrelationProxyControl) el;
      history = model.configHistory();
      model.update(rulesContainer.getCorrelationComponents(),
          rulesContainer.getRulesGroups(),
          rulesContainer.getResponseFilter());
      //model.update(analysisPanel.getAnalysis());
      model.setCorrelationHistory(history);
      model.setCorrelationHistoryId(history.getHistoryId());
      model.setOnStopRecordingMethod(() -> {
        updateHistory(history);
        wizard.requestPermissionToReplay();
      });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TestElement createTestElement() {
    CorrelationProxyControl model = new CorrelationProxyControl();
    configure(model);
    return model;
  }

  @Override
  public void configure(TestElement el) {
    // Don't allow to update UI on recording, because add test element on the tree fire configure
    if (el instanceof CorrelationProxyControl) {
      CorrelationProxyControl correlationProxyControl = (CorrelationProxyControl) el;
      model = correlationProxyControl;
      history = model.configHistory();
      if (wizard != null) {
        wizard.setRepositoriesConfiguration(model.getTemplateRepositoryConfig());
        updateHistory(history);
      }

      CorrelationComponentsRegistry.getInstance().reset();
      rulesContainer.configure(correlationProxyControl);
      model.setOnStopRecordingMethod(() -> {
        if (history != null) {
          updateHistory(history);
        }
        wizard.requestPermissionToReplay();
      });
    }
    super.configure(el);
  }

  @Override
  public void onSaveTemplate(Builder builder) throws IOException, ConfigurationException {
    modifyTestElement(model);
    model.onSaveTemplate(builder);
    configure(model);
  }

  @Override
  public void onLoadTemplate(String repositoryOwner, String id, String templateVersion)
      throws IOException {
    model.onLoadTemplate(repositoryOwner, id, templateVersion);
    rulesContainer.configure(model);
  }

  @Override
  public List<Template> getInstalledCorrelationTemplates() {
    return model.getInstalledCorrelationTemplates();
  }

  @Override
  public void saveRepository(String name, String url) throws IOException {
    model.saveRepository(name, url);
  }

  @Override
  public void deleteRepository(String name) throws IOException {
    model.deleteRepository(name);
  }

  @Override
  public List<CorrelationTemplatesRepository> getCorrelationRepositories() {
    return model.getCorrelationRepositories();
  }

  @Override
  public String getConfigurationRoute() {
    return model.getConfigurationRoute();
  }

  @Override
  public Map<Template, TemplateProperties> getCorrelationTemplatesAndPropertiesByRepositoryName(
      String name, boolean useLocal) {
    return model.getCorrelationTemplatesAndPropertiesByRepositoryName(name, useLocal);
  }

  @Override
  public Map<String, CorrelationTemplateVersions> getCorrelationTemplateVersionsByRepositoryName(
      String name, boolean useLocal) {
    return model.getCorrelationTemplateVersionsByRepositoryName(name, useLocal);
  }

  @Override
  public void installTemplate(String repositoryName, String id, String version)
      throws ConfigurationException {
    model.installTemplate(repositoryName, id, version);
  }

  @Override
  public void uninstallTemplate(String repositoryName, String id, String version)
      throws ConfigurationException {
    model.uninstallTemplate(repositoryName, id, version);
  }

  @Override
  public String getRepositoryURL(String name) {
    return model.getRepositoryURL(name);
  }

  @Override
  public RepositoryManager getRepositoryManager(String name) {
    return model.getRepositoryManager(name);
  }

  @Override
  public RepositoryManager getRepositoryManager(String name, String url) {
    return model.getRepositoryManager(name, url);
  }

  @Override
  public List<File> getConflictingInstalledDependencies(
      List<CorrelationTemplateDependency> dependencies) {
    return model.getConflictingInstalledDependencies(dependencies);
  }

  @Override
  public void deleteConflicts(List<File> dependencies) {
    model.deleteConflicts(dependencies);
  }

  @Override
  public void downloadDependencies(List<CorrelationTemplateDependency> dependencies)
      throws IOException {
    model.downloadDependencies(dependencies);
  }

  @Override
  public boolean isLocalTemplateVersionSaved(String templateId, String templateVersion) {
    return model.isLocalTemplateVersionSaved(templateId, templateVersion);
  }

  @Override
  public boolean isValidDependencyURL(String url, String name, String version) {
    return model.isValidDependencyURL(url, name, version);
  }

  @Override
  public void resetJMeter() {
    ArrayList<String> command = new ArrayList<>();
    command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
    command.add("-jar");
    command.add(JMeterUtils.getJMeterBinDir() + File.separator + "ApacheJMeter.jar");
    final ProcessBuilder builder = new ProcessBuilder(command);

    try {
      File cleanerLog = File.createTempFile("correlation-recorder-", ".log");
      builder.redirectError(cleanerLog);
      builder.redirectOutput(cleanerLog);
      builder.start();
    } catch (IOException e) {
      LOG.warn("Error trying to restart JMeter");
    }

    System.exit(0);
  }

  @Override
  public List<String> checkURL(String id, String url) {
    return model.checkURL(id, url);
  }

  @Override
  public boolean refreshRepositories(String localConfigurationRoute,
                                     Consumer<Integer> setProgressConsumer,
                                     Consumer<String> setStatusConsumer) {
    return model.refreshRepositories(localConfigurationRoute, setProgressConsumer,
        setStatusConsumer);
  }

  @VisibleForTesting
  protected RulesContainer getRulesContainer() {
    return rulesContainer;
  }

  @VisibleForTesting
  protected CorrelationProxyControl getCorrelationProxyControl() {
    return model;
  }

  public Map<String, Repository> getRepositories() {
    Map<String, Repository> repositoryMap = new HashMap<>();
    List<CorrelationTemplatesRepository> repositories = getCorrelationRepositories();

    boolean useLocal = true; // Force to use the local storage cache
    for (CorrelationTemplatesRepository repositoryEntry : repositories) {
      String repositoryName = repositoryEntry.getName();
      Repository repository = repositoryMap.get(repositoryName);
      if (repository == null) {
        repository = new Repository(repositoryName);
        repository.setDisplayName(repositoryEntry.getDisplayName());
        repositoryMap.put(repositoryName, repository);
      }

      Map<Template, TemplateProperties> templatesAndProperties =
          getCorrelationTemplatesAndPropertiesByRepositoryName(repositoryName, useLocal);
      if (templatesAndProperties == null) {
        LOG.warn("No templates found for repository " + repositoryName);
        continue;
      }

      for (Map.Entry<Template, TemplateProperties> entry : templatesAndProperties.entrySet()) {
        Template key = entry.getKey();
        TemplateProperties value = entry.getValue();
        repository.addTemplate(key, value);
      }
    }

    return repositoryMap;
  }

  public void updateHistory(CorrelationHistory history) {
    wizard.setHistory(history);
    rulesContainer.setHistory(history);
  }

}
