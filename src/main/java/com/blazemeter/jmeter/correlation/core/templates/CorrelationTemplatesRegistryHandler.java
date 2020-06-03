package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate.Builder;
import java.io.IOException;
import java.util.List;

public interface CorrelationTemplatesRegistryHandler {

  void onSaveTemplate(Builder builder) throws IOException, ConfigurationException;

  void onLoadTemplate(String repositoryOwner, String id, String templateVersion) throws IOException;

  List<CorrelationTemplate> getInstalledCorrelationTemplates();

  boolean isLocalTemplateVersionSaved(String templateId, String templateVersion);

  boolean isValidDependencyURL(String url, String name, String version);
}


