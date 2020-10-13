package com.blazemeter.jmeter.correlation;

import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate.Builder;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.gui.BlazemeterLabsLogo;
import com.blazemeter.jmeter.correlation.gui.RulesContainer;
import com.blazemeter.jmeter.correlation.gui.TestPlanTemplatesRepository;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
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

public class CorrelationProxyControlGui extends ProxyControlGui implements
    CorrelationTemplatesRepositoriesRegistryHandler, CorrelationTemplatesRegistryHandler {

  protected static final String TEMPLATES_FOLDER_PATH = "/templates/";
  protected static final String SIEBEL_CORRELATION_TEMPLATE = "siebel-1.0-template.json";
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationProxyControlGui.class);
  private static final String CORRELATION_RECORDER_TEST_PLAN = "correlation-recorder.jmx";
  private static final String CORRELATION_RECORDER_TEMPLATE_DESC = "correlation-recorder-template"
      + "-description.xml";
  private static final String CORRELATION_RECORDER_TEMPLATE_NAME = "bzm - Correlation Recorder";
  private final RulesContainer rulesContainer;
  private CorrelationProxyControl model;

  public CorrelationProxyControlGui() {
    JTabbedPane siebelPane = findTabbedPane();
    rulesContainer = new RulesContainer(this, () -> modifyTestElement(model));
    Objects.requireNonNull(siebelPane).add("Correlation Rules", rulesContainer);
    add(new BlazemeterLabsLogo(), BorderLayout.SOUTH);
    installDefaultFiles();
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
        getJMeterDirPath() + "/bin" + TEMPLATES_FOLDER_PATH);
    
    templateRepository
        .addCorrelationRecorderTemplate(CORRELATION_RECORDER_TEST_PLAN, TEMPLATES_FOLDER_PATH,
            TEMPLATES_FOLDER_PATH + CORRELATION_RECORDER_TEMPLATE_DESC,
            CORRELATION_RECORDER_TEMPLATE_NAME);
    LOG.info("bzm - Correlation Recorder Test Plan Template installed");
  }

  private String getJMeterDirPath() {
    String siebelPluginPath = getClass().getProtectionDomain().getCodeSource().getLocation()
        .getPath();

    /*This is done to obtain and remove the initial `/` from the path.
      i.e: In Windows the path would be something like `/C:`,
      so we check if the char at position 3 is ':' and if so, we remove the initial '/'.
    */
    char a_char = siebelPluginPath.charAt(2);
    if (a_char == ':') {
      siebelPluginPath = siebelPluginPath.substring(1);
    }
    int index = siebelPluginPath.indexOf("/lib/ext/");
    return siebelPluginPath.substring(0, index);
  }

  private void installSiebelCorrelationTemplate() {
    TestPlanTemplatesRepository templateRepository = new TestPlanTemplatesRepository(
        getJMeterDirPath() + LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER);
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
      model.update(rulesContainer.getCorrelationComponents(), rulesContainer.getCorrelationRules(),
          rulesContainer.getResponseFilter());
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
    LOG.debug("Configuring gui with {}", el);
    super.configure(el);
    if (el instanceof CorrelationProxyControl) {
      CorrelationProxyControl correlationProxyControl = (CorrelationProxyControl) el;
      model = correlationProxyControl;
      rulesContainer.configure(correlationProxyControl);
    }
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
  public List<CorrelationTemplate> getInstalledCorrelationTemplates() {
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
  public List<CorrelationTemplate> getCorrelationTemplatesByRepositoryName(String name) {
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
}
