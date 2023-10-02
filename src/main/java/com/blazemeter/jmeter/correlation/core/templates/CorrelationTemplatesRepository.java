package com.blazemeter.jmeter.correlation.core.templates;

import com.helger.commons.annotation.VisibleForTesting;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents a Repository with all the Templates associated to it.
 * For example, if we have a repository called "Local" with templates for "WordPress" and
 * "Joomla", this class will have name="Local" and templates=[WordPress, Joomla], also
 * each template will have a list of versions associated to them.
 */
public class CorrelationTemplatesRepository {

  private transient String name;
  private String displayName;
  private Map<String, CorrelationTemplateVersions> templatesVersions = new HashMap<>();

  //Constructor added to avoid issues with the serialization
  public CorrelationTemplatesRepository() {
  }

  public CorrelationTemplatesRepository(String name) {
    this.name = name;
  }

  public CorrelationTemplatesRepository(String name, Map<String, CorrelationTemplateVersions>
                                            templatesVersions) {
    this.name = name;
    this.templatesVersions = templatesVersions;
  }

  public void addTemplate(String name, String version) {
    if (templatesVersions.containsKey(name)) {
      //If the version already exists, no need to add it
      if (!templatesVersions.get(name).getVersions().contains(version)) {
        templatesVersions.get(name).addVersion(version);
      }
    } else {
      CorrelationTemplateVersions value = new CorrelationTemplateVersions(version);
      value.setRepositoryDisplayName(displayName);
      templatesVersions.put(name, value);
    }
  }

  public void addTemplate(String name, CorrelationTemplateVersions versions) {
    templatesVersions.put(name, versions);
  }

  public Map<String, CorrelationTemplateVersions> getTemplates() {
    return templatesVersions;
  }

  public void setTemplates(Map<String, CorrelationTemplateVersions> templatesVersions) {
    this.templatesVersions = templatesVersions;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String toString() {
    return "CorrelationTemplatesRepository{" +
        "name='" + name + '\'' +
        ", displayName='" + displayName + "'" +
        ", templatesVersions=" + templatesVersions +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CorrelationTemplatesRepository that = (CorrelationTemplatesRepository) o;
    return Objects.equals(getName(), that.getName()) &&
        Objects.equals(getTemplates(), that.getTemplates());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getTemplates());
  }

  @VisibleForTesting
  public String getValues() {
    return this.toString();
  }
}
