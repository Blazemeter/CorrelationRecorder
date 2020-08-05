package com.blazemeter.jmeter.correlation.core.templates;

import java.util.ArrayList;
import java.util.List;

public class TemplateItem {

  private String id;
  private boolean hasInstalled;
  private List<CorrelationTemplate> versions;

  public TemplateItem(String id, boolean hasInstalled) {
    this.id = id;
    this.hasInstalled = hasInstalled;
    this.versions = new ArrayList<>();
  }

  public void addTemplate(CorrelationTemplate template) {
    versions.add(template);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean hasInstalledVersion() {
    return hasInstalled;
  }

  public void setHasInstalled(boolean hasInstalled) {
    this.hasInstalled = hasInstalled;
  }

  public List<CorrelationTemplate> getVersions() {
    return versions;
  }
}
