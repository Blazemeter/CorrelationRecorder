package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CorrelationTemplatesRepositoriesRegistry {

  void save(String name, String url) throws IOException;

  void delete(String name) throws IOException;

  CorrelationTemplatesRepository find(String id);

  Map<String, CorrelationTemplateVersions> getCorrelationTemplateVersionsByRepositoryId(
      String name);

  String getRepositoryURL(String name);

  boolean isLocalTemplateVersionSaved(String templateId, String templateVersion);

  Map<Template, TemplateProperties> getCorrelationTemplatesAndPropertiesByRepositoryId(String name);

  Map<Template, TemplateProperties> getCorrelationTemplatesAndPropertiesByRepositoryId(
      String name, List<TemplateVersion> filter);

  void upload(String name, Template template) throws IOException;
}
