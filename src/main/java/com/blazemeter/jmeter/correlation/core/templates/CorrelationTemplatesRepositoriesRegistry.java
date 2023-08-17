package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CorrelationTemplatesRepositoriesRegistry {

  void save(String name, String url) throws IOException;

  void delete(String name) throws IOException;

  CorrelationTemplatesRepository find(String id);

  List<CorrelationTemplatesRepository> getRepositories();

  Map<String, CorrelationTemplateVersions> getCorrelationTemplateVersionsByRepositoryId(
      String name);

  void installTemplate(String repositoryName, String templateName, String version)
      throws ConfigurationException;

  void uninstallTemplate(String repositoryName, String name, String version)
      throws ConfigurationException;

  String getRepositoryURL(String name);

  void updateLocalRepository(String templateId, String templateVersion);

  boolean isLocalTemplateVersionSaved(String templateId, String templateVersion);

  Map<Template, TemplateProperties> getCorrelationTemplatesAndPropertiesByRepositoryId(String name);

  Map<Template, TemplateProperties> getCorrelationTemplatesAndPropertiesByRepositoryId(
      String name, List<TemplateVersion> filter);
}
