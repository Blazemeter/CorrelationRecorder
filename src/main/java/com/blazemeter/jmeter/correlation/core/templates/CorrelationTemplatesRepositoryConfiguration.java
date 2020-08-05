package com.blazemeter.jmeter.correlation.core.templates;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CorrelationTemplatesRepositoryConfiguration {

  private String name;
  private String url;
  private Map<String, String> installedTemplates = new HashMap<>();

  //Added to satisfy the deserialization process
  public CorrelationTemplatesRepositoryConfiguration() {
  }

  public CorrelationTemplatesRepositoryConfiguration(String name, String url) {
    this.name = name;
    this.url = url;
  }

  void installTemplate(String templateName, String templateVersion) {
    installedTemplates.put(templateName, templateVersion);
  }

  public void uninstallTemplate(String templateId) {
    installedTemplates.remove(templateId);
  }

  public boolean isInstalled(String templateName, String templateVersion) {
    return installedTemplates.containsKey(templateName) && installedTemplates.get(templateName)
        .equals(templateVersion);
  }

  public boolean hasInstalledTemplates() {
    return !installedTemplates.isEmpty();
  }

  public Map<String, String> getInstalledTemplates() {
    return installedTemplates;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return "CorrelationTemplatesRepositoryConfiguration{" +
        "name='" + name + '\'' +
        ", url='" + url + '\'' +
        ", installedTemplates=" + installedTemplates +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CorrelationTemplatesRepositoryConfiguration)) {
      return false;
    }

    CorrelationTemplatesRepositoryConfiguration that =
        (CorrelationTemplatesRepositoryConfiguration) o;

    return Objects.equals(getName(), that.getName()) &&
        Objects.equals(getUrl(), that.getUrl()) &&
        Objects.equals(getInstalledTemplates(), that.getInstalledTemplates());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getUrl(), getInstalledTemplates());
  }
}
