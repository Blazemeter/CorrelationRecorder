package com.blazemeter.jmeter.correlation;

import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate.Builder;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistry;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.blazemeter.jmeter.correlation.gui.CorrelationRulesTestElement;
import com.helger.commons.annotation.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.jmeter.protocol.http.proxy.ProxyControl;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationProxyControl extends ProxyControl implements
    CorrelationTemplatesRegistryHandler, CorrelationTemplatesRepositoriesRegistryHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationProxyControl.class);
  private static final String CORRELATION_RULES = "CorrelationProxyControl.rules";
  private static final String CORRELATION_COMPONENTS = "CorrelationProxyControl.components";
  private static final String RESPONSE_FILTER = "CorrelationProxyControl.responseFilter";
  private static final String TEMPLATE_PATH = "CorrelationProxyControl.templatePath";
  private static final String RECORDER_NAME = "Correlation Recorder";

  private LocalConfiguration localConfiguration;

  private CorrelationEngine correlationEngine;
  private CorrelationComponentsRegistry componentsRegistry;
  private CorrelationTemplatesRegistry correlationTemplatesRegistry;
  private CorrelationTemplatesRepositoriesConfiguration correlationTemplatesRepositoriesConfiguration;

  public CorrelationProxyControl() {
    correlationEngine = new CorrelationEngine();
    componentsRegistry = new CorrelationComponentsRegistry();
    localConfiguration = new LocalConfiguration(getTemplateDirectoryPath());
    correlationTemplatesRegistry = new LocalCorrelationTemplatesRegistry(localConfiguration);
    correlationTemplatesRepositoriesConfiguration =
        new CorrelationTemplatesRepositoriesConfiguration(
        localConfiguration);

    setName(RECORDER_NAME);
  }

  @VisibleForTesting
  public CorrelationProxyControl(String configurationPath) {
    correlationEngine = new CorrelationEngine();
    componentsRegistry = new CorrelationComponentsRegistry();
    localConfiguration = new LocalConfiguration(configurationPath);
    correlationTemplatesRegistry = new LocalCorrelationTemplatesRegistry(localConfiguration);
    correlationTemplatesRepositoriesConfiguration =
        new CorrelationTemplatesRepositoriesConfiguration(
        localConfiguration);

    setName(RECORDER_NAME);
  }

  @VisibleForTesting
  public CorrelationProxyControl(CorrelationTemplatesRegistry correlationTemplatesRegistry,
      CorrelationTemplatesRepositoriesConfiguration configuration) {
    correlationEngine = new CorrelationEngine();
    componentsRegistry = new CorrelationComponentsRegistry();
    this.correlationTemplatesRegistry = correlationTemplatesRegistry;
    this.correlationTemplatesRepositoriesConfiguration = configuration;

    setName(RECORDER_NAME);
  }

  private static String getTemplateDirectoryPath() {
    return JMeterUtils.getPropDefault(TEMPLATE_PATH, JMeterUtils.getJMeterHome());
  }

  public void setCorrelationEngine(CorrelationEngine correlationEngine) {
    this.correlationEngine = correlationEngine;
  }

  public void setCorrelationTemplatesRegistry(
      CorrelationTemplatesRegistry correlationTemplatesRegistry) {
    this.correlationTemplatesRegistry = correlationTemplatesRegistry;
  }

  @Override
  public void startProxy() throws IOException {
    correlationEngine.reset();
    super.startProxy();
  }

  public CorrelationRulesTestElement getCorrelationRulesTestElement() {
    return (CorrelationRulesTestElement) getProperty(CORRELATION_RULES).getObjectValue();
  }

  private List<CorrelationRule> buildCorrelationRules(
      CorrelationRulesTestElement correlationRulesTestElement) {
    if (correlationRulesTestElement == null) {
      return new ArrayList<>();
    }

    return correlationRulesTestElement.getRules().stream()
        /*
         * We need the rules that contains at least 1 Extractor/Replacement and,
         * the selected ones, need to be pass the Components Validations
         * */
        .filter(t -> (t.getExtractorClass() != null || t.getReplacementClass() != null) &&
            (t.getExtractorClass() == null || componentsRegistry
                .containsExtractor(t.getExtractorClass())) &&
            (t.getReplacementClass() == null || componentsRegistry
                .containsReplacement(t.getReplacementClass()))
        )
        .map(t -> new CorrelationRule(t.getReferenceName(),
            t.getExtractorClass() != null ? buildCorrelationExtractor(t) : null,
            t.getReplacementClass() != null ? buildCorrelationReplacement(t) : null))
        .collect(Collectors.toList());
  }

  private CorrelationExtractor buildCorrelationExtractor(CorrelationRuleTestElement t) {
    CorrelationExtractor extractor = componentsRegistry
        .getCorrelationExtractor(t.getExtractorClass().getCanonicalName(), new ArrayList<>());
    extractor.update(t);
    return extractor;
  }

  private CorrelationReplacement buildCorrelationReplacement(CorrelationRuleTestElement t) {
    CorrelationReplacement replacement = componentsRegistry
        .getCorrelationReplacement(t.getReplacementClass().getCanonicalName(), new ArrayList<>());
    replacement.update(t);
    return replacement;
  }

  @Override
  public synchronized void deliverSampler(HTTPSamplerBase sampler, TestElement[] testElements,
      SampleResult result) {
    if (sampler != null) {
      List<TestElement> children = new ArrayList<>(Arrays.asList(testElements));
      correlationEngine.process(sampler, children, result, this.getContentTypeInclude());
      testElements = children.toArray(new TestElement[0]);
    }
    super.deliverSampler(sampler, testElements, result);
  }

  @Override
  public void onSaveTemplate(Builder builder) throws IOException, ConfigurationException {
    CorrelationTemplate template = builder
        .withRules(getCorrelationRules())
        .withComponents(getCorrelationComponents())
        .withResponseFilters(getResponseFilter())
        .build();

    correlationTemplatesRegistry.save(template);
    updateLocalRepository(template);
    installTemplate(template.getRepositoryId(), template.getId(), template.getVersion());
  }

  private List<CorrelationRule> getCorrelationRules() {
    CorrelationRulesTestElement correlationRulesTestElement =
        (CorrelationRulesTestElement) getProperty(
        CORRELATION_RULES).getObjectValue();

    return buildCorrelationRules(correlationRulesTestElement);
  }

  public void setCorrelationRules(List<CorrelationRule> correlationRules) {
    setProperty(new TestElementProperty(CORRELATION_RULES,
        correlationRules != null ? new CorrelationRulesTestElement(correlationRules.stream()
            .map(CorrelationRule::buildTestElement)
            .collect(Collectors.toList())) : new CorrelationRulesTestElement()));
    correlationEngine.setCorrelationRules(correlationRules, componentsRegistry);
  }

  public String getCorrelationComponents() {
    return getPropertyAsString(CORRELATION_COMPONENTS);
  }

  @VisibleForTesting
  public void setCorrelationComponents(String correlationComponents) {
    setProperty(CORRELATION_COMPONENTS, correlationComponents);
    List<String> components = parseComponentString(correlationComponents);
    components.forEach(c -> {
      try {
        componentsRegistry.addComponent(c);
      } catch (IllegalArgumentException | ClassNotFoundException e) {
        LOG.error("There was an issue trying to add the component {}. ", c, e);
      }
    });
  }

  public String getResponseFilter() {
    return getPropertyAsString(RESPONSE_FILTER);
  }

  @VisibleForTesting
  public void setResponseFilter(String responseFilter) {
    setProperty(RESPONSE_FILTER, responseFilter);
  }

  private void updateLocalRepository(CorrelationTemplate template) {
    correlationTemplatesRepositoriesConfiguration
        .updateLocalRepository(template.getId(), template.getVersion());
  }

  @Override
  public void onLoadTemplate(String repositoryOwner, String id, String templateVersion)
      throws IOException {
    Optional<CorrelationTemplate> correlationTemplate = correlationTemplatesRegistry
        .findByID(repositoryOwner, id, templateVersion);
    if (correlationTemplate.isPresent()) {
      CorrelationTemplate template = correlationTemplate.get();
      append(template.getComponents(), template.getRules(), template.getResponseFilters());
    } else {
      LOG.error("Template not found {}", id);
    }
  }

  public void append(String loadedComponents, List<CorrelationRule> loadedRules,
      String loadedFilters) {

    String actualComponents = getCorrelationComponents();
    setCorrelationComponents(loadedComponents.isEmpty() ? actualComponents
        : cleanRepeated(actualComponents, loadedComponents));

    String actualFilters = getResponseFilter();
    setResponseFilter(
        loadedFilters.isEmpty() ? actualFilters : cleanRepeated(actualFilters, loadedFilters));

    List<CorrelationRule> actualRules = getCorrelationRules();
    setCorrelationRules(
        loadedRules.isEmpty() ? actualRules : appendCorrelationRules(actualRules, loadedRules));
  }

  private String cleanRepeated(String actual, String loaded) {
    if (loaded == null || loaded.isEmpty()) {
      return actual;
    }

    Set<String> cleaned = actual.isEmpty() ? new HashSet<>()
        : new HashSet<>(Arrays.asList(actual.replace("\n", "").split(",")));
    cleaned.addAll(Arrays.asList(loaded.replace("\n", "").split(",")));

    return cleaned.stream().map(String::trim).collect(Collectors.joining(","));
  }

  public List<CorrelationRule> appendCorrelationRules(List<CorrelationRule> actualRules,
      List<CorrelationRule> loadedRules) {
    if (loadedRules.isEmpty()) {
      return actualRules;
    }

    LinkedHashSet<CorrelationRule> actualRulesSet = new LinkedHashSet<>(actualRules);
    actualRulesSet.addAll(loadedRules);

    return new ArrayList<>(actualRulesSet);
  }

  public String getContentTypeInclude() {
    return getPropertyAsString(RESPONSE_FILTER);
  }

  public List<CorrelationTemplate> getInstalledCorrelationTemplates() {
    return correlationTemplatesRegistry.getInstalledTemplates();
  }

  private List<String> parseComponentString(String correlationComponents) {
    if (correlationComponents.isEmpty()) {
      return new ArrayList<>();
    }

    return Arrays.asList(correlationComponents
        .replace("\n", "")
        .replace("\r", "")
        .split(","));
  }

  public void update(String correlationComponents, List<CorrelationRule> correlationRules,
      String responseFilter) {
    if (correlationComponents != null && !correlationComponents.isEmpty()) {
      setCorrelationComponents(correlationComponents);
    }
    setCorrelationRules(correlationRules);
    setResponseFilter(responseFilter);
  }

  @Override
  public void saveRepository(String id, String url) throws IOException {
    correlationTemplatesRepositoriesConfiguration.save(id, url);
  }

  @Override
  public void deleteRepository(String id) throws IOException {
    correlationTemplatesRepositoriesConfiguration.deleteRepository(id);
  }

  @Override
  public List<CorrelationTemplatesRepository> getCorrelationRepositories() {
    return correlationTemplatesRepositoriesConfiguration.getCorrelationRepositories();
  }

  @Override
  public String getConfigurationRoute() {
    return localConfiguration.getRootFolder();
  }

  @Override
  public List<CorrelationTemplate> getCorrelationTemplatesByRepositoryName(String name) {
    return correlationTemplatesRepositoriesConfiguration
        .getCorrelationTemplatesByRepositoryName(name);
  }

  @Override
  public void installTemplate(String repositoryName, String id, String version)
      throws ConfigurationException {
    correlationTemplatesRepositoriesConfiguration.installTemplate(repositoryName, id, version);
  }

  @Override
  public void uninstallTemplate(String repositoryName, String id, String version)
      throws ConfigurationException {
    correlationTemplatesRepositoriesConfiguration.uninstallTemplate(repositoryName, id, version);
  }

  @Override
  public String getRepositoryURL(String otherName) {
    return correlationTemplatesRepositoriesConfiguration.getRepositoryURL(otherName);
  }

  @Override
  public List<File> getConflictingInstalledDependencies(
      List<CorrelationTemplateDependency> dependencies) {
    return localConfiguration.findConflictingDependencies(dependencies);
  }

  @Override
  public void deleteConflicts(List<File> dependencies) {
    localConfiguration.deleteConflicts(dependencies);
  }

  @Override
  public void downloadDependencies(List<CorrelationTemplateDependency> dependencies)
      throws IOException {
    localConfiguration.downloadDependencies(dependencies);
  }

  @Override
  public boolean isLocalTemplateVersionSaved(String templateId, String templateVersion) {
    return correlationTemplatesRepositoriesConfiguration
        .isLocalTemplateVersionSaved(templateId, templateVersion);
  }

  @Override
  public void resetJMeter() {
    //Left empty
  }

  @Override
  public List<String> checkURL(String id, String url) {
    return localConfiguration.checkRepositoryURL(id, url);
  }

  @Override
  public void refreshRepositories(String localConfigurationRoute,
      Consumer<Integer> setProgressConsumer) {
    correlationTemplatesRepositoriesConfiguration
        .refreshRepositories(localConfigurationRoute, setProgressConsumer);
  }

  @Override
  public boolean isValidDependencyURL(String url, String name, String version) {
    return localConfiguration.isValidDependencyURL(url, name, version);
  }

  @VisibleForTesting
  public List<CorrelationRule> getRules() {
    CorrelationRulesTestElement correlationRulesTestElement =
        (CorrelationRulesTestElement) getProperty(
        CORRELATION_RULES).getObjectValue();

    return buildCorrelationRules(correlationRulesTestElement);
  }
}
