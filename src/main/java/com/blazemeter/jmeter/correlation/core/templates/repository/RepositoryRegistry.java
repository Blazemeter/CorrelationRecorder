package com.blazemeter.jmeter.correlation.core.templates.repository;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RepositoryRegistry {

  void init();

  void setName(String name);

  String getName();

  String getEndPoint();

  void setup();

  void setConfig(LocalConfiguration localConfiguration);

  LocalConfiguration getConfig();

  boolean autoLoad();

  boolean disableConfig();

  void setTemplateRegistry(Object templateRegistry);

  Object getTemplateRegistry();

  CorrelationTemplatesRepository getRepository();

  Map<String, CorrelationTemplateVersions> getTemplateVersions();

  List<Template> getTemplates();

  List<Template> getTemplates(List<TemplateVersion> filter);

  Collection<String> checkRepositoryURL(String url);

  Map<Template, TemplateProperties> getTemplatesAndProperties();

  Map<Template, TemplateProperties> getTemplatesAndProperties(List<TemplateVersion> filter);
}
