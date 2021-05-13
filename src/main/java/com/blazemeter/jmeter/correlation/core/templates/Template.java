package com.blazemeter.jmeter.correlation.core.templates;

import java.util.ArrayList;
import java.util.List;

public class Template {

  private final List<TemplateVersion> versions;
  private String id;
  private boolean hasInstalled;

  public Template(String id, boolean hasInstalled) {
    this.id = id;
    this.hasInstalled = hasInstalled;
    this.versions = new ArrayList<>();
  }

  public void addTemplate(TemplateVersion template) {
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

  public List<TemplateVersion> getVersions() {
    return versions;
  }
}
