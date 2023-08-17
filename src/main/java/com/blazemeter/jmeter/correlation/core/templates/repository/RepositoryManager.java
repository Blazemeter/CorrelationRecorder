package com.blazemeter.jmeter.correlation.core.templates.repository;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RepositoryManager implements RepositoryRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryManager.class);

  private LocalConfiguration localConfig;
  private Object templateRegistry = null;

  private String name = null;

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getEndPoint() {
    return null;
  }

  @Override
  public void setup() {

  }

  @Override
  public LocalConfiguration getConfig() {
    return localConfig;
  }

  @Override
  public void setConfig(LocalConfiguration localConfig) {
    this.localConfig = localConfig;
  }

  @Override
  public void setTemplateRegistry(Object templateRegistry) {
    this.templateRegistry = templateRegistry;
  }

  @Override
  public Object getTemplateRegistry() {
    return templateRegistry;
  }

  @Override
  public CorrelationTemplatesRepository getRepository() {
    return null;
  }

  @Override
  public List<Template> getTemplates() {
    return null;
  }

  public List<Template> getTemplates(Map<String, TemplateVersion> filter) {
    return null;
  }

  @Override
  public Collection<String> checkRepositoryURL(String url) {
    List<String> errors = new ArrayList<>();
    if (!url.endsWith(".json")) {
      String error = "URL should lead to .json file";
      LOG.warn("There was an error on the repository {}'s URL={}. Error: {}",
          this.getName(), url, error);
      errors.add(
          "- There was and error on the repository " + this.getName() + "'s URL " + url +
              ".\n   Error: "
              + error);
    }
    return errors;
  }
}
