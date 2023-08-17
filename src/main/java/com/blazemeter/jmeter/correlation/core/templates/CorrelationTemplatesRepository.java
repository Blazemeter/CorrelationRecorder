package com.blazemeter.jmeter.correlation.core.templates;

import com.helger.commons.annotation.VisibleForTesting;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CorrelationTemplatesRepository {

  private transient String name;
  private Map<String, CorrelationTemplateVersions> templatesVersions = new HashMap<>();

  //Constructor added to avoid issues with the serialization
  public CorrelationTemplatesRepository() {
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
      templatesVersions.put(name, new CorrelationTemplateVersions(version));
    }
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

  @Override
  public String toString() {
    return "CorrelationTemplatesRepository{" +
        "name='" + name + '\'' +
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
