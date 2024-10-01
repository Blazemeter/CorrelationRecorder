package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface CorrelationTemplatesRepositoriesRegistryHandler {

  void saveRepository(String id, String url) throws IOException;

  void deleteRepository(String name) throws IOException;

  List<CorrelationTemplatesRepository> getCorrelationRepositories();

  String getConfigurationRoute();

  Map<Template, TemplateProperties> getCorrelationTemplatesAndPropertiesByRepositoryName(
      String name, boolean useLocal);

  Map<String, CorrelationTemplateVersions> getCorrelationTemplateVersionsByRepositoryName(
      String name, boolean useLocal);

  void installTemplate(String repositoryName, String id, String version)
      throws ConfigurationException;

  void uninstallTemplate(String repositoryName, String id, String version)
      throws ConfigurationException;

  String getRepositoryURL(String name);

  RepositoryManager getRepositoryManager(String name);

  RepositoryManager getRepositoryManager(String name, String url);

  List<File> getConflictingInstalledDependencies(List<CorrelationTemplateDependency> dependencies);

  void deleteConflicts(List<File> dependencies);

  void downloadDependencies(List<CorrelationTemplateDependency> dependencies) throws IOException;

  boolean isLocalTemplateVersionSaved(String templateId, String templateVersion);

  void resetJMeter();

  List<String> checkURL(String id, String url);

  boolean refreshRepositories(String localConfigurationRoute,
                              Consumer<Integer> setProgressConsumer,
                              Consumer<String> setStatusConsumer);

  void setTemplatesIgnoreErrors(boolean b);
}
