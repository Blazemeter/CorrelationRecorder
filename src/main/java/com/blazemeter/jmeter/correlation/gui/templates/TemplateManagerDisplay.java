package com.blazemeter.jmeter.correlation.gui.templates;

import com.blazemeter.jmeter.correlation.core.templates.Template;
import java.util.ArrayList;
import java.util.List;

public class TemplateManagerDisplay {

  private final List<Template> versions;
  private String id;
  private boolean hasInstalled;

  private String repositoryName = "N/A";

  public TemplateManagerDisplay(String id, boolean hasInstalled) {
    this.id = id;
    this.hasInstalled = hasInstalled;
    this.versions = new ArrayList<>();
  }

  public void addTemplate(Template template) {
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

  public List<Template> getVersions() {
    return versions;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }
}
