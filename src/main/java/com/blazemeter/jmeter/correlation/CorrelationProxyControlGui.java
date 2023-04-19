package com.blazemeter.jmeter.correlation;

import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory;
import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion.Builder;
import com.blazemeter.jmeter.correlation.gui.BlazemeterLabsLogo;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.gui.RulesContainer;
import com.blazemeter.jmeter.correlation.gui.TestPlanTemplatesRepository;
import com.blazemeter.jmeter.correlation.gui.automatic.CorrelationWizard;
import com.google.common.annotations.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
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
  protected static final String TEMPLATES_FOLDER_PATH = "/templates/";
  protected static final String SIEBEL_CORRELATION_TEMPLATE = "siebel-1.0-template.json";
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationProxyControlGui.class);
  private static final String CORRELATION_RECORDER_TEST_PLAN = "correlation-recorder.jmx";
  private static final String CORRELATION_RECORDER_TEMPLATE_DESC = "correlation-recorder-template"
      + "-description.xml";
  private static final String CORRELATION_RECORDER_TEMPLATE_NAME = "bzm - Correlation Recorder";
  private final RulesContainer rulesContainer;
  private CorrelationProxyControl model;
  private CorrelationHistory history;
  private CorrelationWizard wizard;

  public CorrelationProxyControlGui() {
    JTabbedPane siebelPane = findTabbedPane();
    rulesContainer = new RulesContainer(this, () -> modifyTestElement(model));
    Objects.requireNonNull(siebelPane).add("Correlation", rulesContainer);
    add(new BlazemeterLabsLogo(), BorderLayout.SOUTH);
    installDefaultFiles();
    history = new CorrelationHistory();

    wizard = new CorrelationWizard();
    wizard.setHistory(history);
    wizard.setVersionsSupplier(this::getTemplateVersions);
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

  private void installDefaultFiles() {
    installCorrelationRecorderTemplateTestPlan();
    installSiebelCorrelationTemplate();
  }

  private void installCorrelationRecorderTemplateTestPlan() {
    TestPlanTemplatesRepository templateRepository = new TestPlanTemplatesRepository(
        Paths.get(getJMeterDirPath(), "bin", TEMPLATES_FOLDER_PATH).toAbsolutePath().toString()
            + File.separator);

    templateRepository.addCorrelationRecorderTemplate(CORRELATION_RECORDER_TEST_PLAN,
        TEMPLATES_FOLDER_PATH,
        TEMPLATES_FOLDER_PATH + CORRELATION_RECORDER_TEMPLATE_DESC,
        CORRELATION_RECORDER_TEMPLATE_NAME);
    LOG.info("bzm - Correlation Recorder Test Plan Template installed");
  }

  private String getJMeterDirPath() {
    String siebelPluginPath = getClass().getProtectionDomain().getCodeSource().getLocation()
        .getPath();

    /*
     * This is done to obtain and remove the initial `/` from the path. i.e: In
     * Windows the path would be something like `/C:`, so we check if the char at
     * position 3 is ':' and if so, we remove the initial '/'.
     */
    char aChar = siebelPluginPath.charAt(2);
    if (aChar == ':') {
      siebelPluginPath = siebelPluginPath.substring(1);
    }
    int index = siebelPluginPath.indexOf("/lib/ext/");
    return siebelPluginPath.substring(0, index);
  }

  private void installSiebelCorrelationTemplate() {
    TestPlanTemplatesRepository templateRepository = new TestPlanTemplatesRepository(Paths
        .get(getJMeterDirPath(), LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER)
        .toAbsolutePath().toString() + File.separator);
    templateRepository.addCorrelationTemplate(SIEBEL_CORRELATION_TEMPLATE,
        LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER);
    LOG.info("Siebel Correlation's Template installed");
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
      model.update(rulesContainer.getCorrelationComponents(),
          rulesContainer.getRulesGroups(),
          rulesContainer.getResponseFilter(),
          history.getHistoryPath());
      //model.update(analysisPanel.getAnalysis());
      model.setCorrelationHistory(history);
      model.setCorrelationHistoryPath(history.getHistoryPath());
      model.setOnStopRecordingMethod(() -> {
        wizard.setHistory(history);
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
    LOG.debug("creating/configuring model = {}", model);
    configure(model);
    return model;
  }

  @Override
  public void configure(TestElement el) {
    // Don't allow to update UI on recording, because add test element on the tree fire configure
    LOG.debug("Configuring gui with {}", el);
    if (el instanceof CorrelationProxyControl) {
      CorrelationProxyControl correlationProxyControl = (CorrelationProxyControl) el;
      // Check if the server of recording is running, if the server is running, no update the UI
      if (!correlationProxyControl.canRemove()) {
        return;
      }
      model = correlationProxyControl;
      CorrelationComponentsRegistry.getInstance().reset();
      rulesContainer.configure(correlationProxyControl);
      model.setOnStopRecordingMethod(() -> {
        if (history != null) {
          wizard.setHistory(history);
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
  public List<TemplateVersion> getInstalledCorrelationTemplates() {
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
  public List<TemplateVersion> getCorrelationTemplatesByRepositoryName(String name) {
    return model.getCorrelationTemplatesByRepositoryName(name);
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
                                     Consumer<Integer> setProgressConsumer) {
    return model.refreshRepositories(localConfigurationRoute, setProgressConsumer);
  }

  @VisibleForTesting
  protected RulesContainer getRulesContainer() {
    return rulesContainer;
  }

  @VisibleForTesting
  protected CorrelationProxyControl getCorrelationProxyControl() {
    return model;
  }

  public List<TemplateVersion> getTemplateVersions() {
    List<TemplateVersion> rawTemplates = new ArrayList<>();
    getCorrelationRepositories().forEach(repository -> rawTemplates
        .addAll(getCorrelationTemplatesByRepositoryName(repository.getName())));
    return rawTemplates;
  }
}
