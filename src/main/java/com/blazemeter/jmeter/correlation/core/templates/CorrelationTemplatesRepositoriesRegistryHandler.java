package com.blazemeter.jmeter.correlation.core.templates;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public interface CorrelationTemplatesRepositoriesRegistryHandler {

  void saveRepository(String id, String url) throws IOException;

  void deleteRepository(String name) throws IOException;

  List<CorrelationTemplatesRepository> getCorrelationRepositories();

  String getConfigurationRoute();

  List<TemplateVersion> getCorrelationTemplatesByRepositoryName(String name);

  void installTemplate(String repositoryName, String id, String version)
      throws ConfigurationException;

  void uninstallTemplate(String repositoryName, String id, String version)
      throws ConfigurationException;

  String getRepositoryURL(String name);

  List<File> getConflictingInstalledDependencies(List<CorrelationTemplateDependency> dependencies);

  void deleteConflicts(List<File> dependencies);

  void downloadDependencies(List<CorrelationTemplateDependency> dependencies) throws IOException;

  boolean isLocalTemplateVersionSaved(String templateId, String templateVersion);

  void resetJMeter();

  List<String> checkURL(String id, String url);

  boolean refreshRepositories(String localConfigurationRoute,
      Consumer<Integer> setProgressConsumer);
}
