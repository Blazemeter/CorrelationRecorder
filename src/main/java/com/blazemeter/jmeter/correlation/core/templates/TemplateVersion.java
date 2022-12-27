package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TemplateVersion {

  private static final String SNAPSHOT_FILE_EXTENSION = ".png";
  private static final String SNAPSHOT_SUFFIX = "-snapshot";
  private String id;
  private String description;
  private String version;
  private String author;
  private String url;
  private String components;
  private String responseFilters;
  //To allows Backward Compatibility
  @JsonProperty(access = Access.WRITE_ONLY)
  private final List<CorrelationRule> rules = new ArrayList<>();
  private List<RulesGroup> groups = new ArrayList<>();
  private List<CorrelationTemplateDependency> dependencies = new ArrayList<>();
  private String repositoryId = "";
  private String changes;
  private transient String snapshotPath;
  private transient BufferedImage snapshot;
  private transient boolean isInstalled;

  //Default constructor to meet serialization
  public TemplateVersion() {
  }

  private TemplateVersion(Builder builder) {
    id = builder.id;
    description = builder.description;
    groups = builder.groups;
    snapshot = builder.snapShot;
    responseFilters = builder.responseFilters;
    components = builder.components;
    repositoryId = builder.repositoryId;
    version = builder.version;
    changes = builder.changes;
    dependencies = builder.dependencies;
    author = builder.author;
    url = builder.url;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public String getAuthor() {
    return author;
  }

  public String getUrl() {
    return url;
  }

  //Left for backward compatibility with version 1.1
  public List<CorrelationRule> getRules() {
    return rules;    
  }
  
  public List<RulesGroup> getGroups() {
    return groups;
  }

  //To allows Backward Compatibility
  public void setRules(List<CorrelationRule> rules) {
    this.groups.add(new RulesGroup.Builder().withId(id).withRules(rules).build());
  }

  public String getVersion() {
    return version;
  }

  public BufferedImage getSnapshot() {
    return snapshot;
  }

  void setSnapshot(BufferedImage snapshot) {
    this.snapshot = snapshot;
  }

  public String getSnapshotName() {
    return getId() + "-" + getVersion() + SNAPSHOT_SUFFIX + SNAPSHOT_FILE_EXTENSION;
  }

  public String getSnapshotPath() {
    return snapshotPath;
  }

  void setSnapshotPath(String snapshotPath) {
    this.snapshotPath = snapshotPath;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public boolean isInstalled() {
    return isInstalled;
  }

  public void setInstalled(boolean installed) {
    isInstalled = installed;
  }

  public String getComponents() {
    return components;
  }

  public String getResponseFilters() {
    return responseFilters;
  }

  public List<CorrelationTemplateDependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<CorrelationTemplateDependency> dependencies) {
    this.dependencies = dependencies;
  }

  public String getChanges() {
    return changes;
  }

  @Override
  public String toString() {
    return "CorrelationTemplate{" +
        "id='" + id + '\'' +
        ", description='" + description + '\'' +
        ", version='" + version + '\'' +
        ", author='" + author + '\'' +
        ", url='" + url + '\'' +
        ", components='" + components + '\'' +
        ", responseFilters='" + responseFilters + '\'' +
        ", groups=" + groups.toString() +
        ", dependencies=" + dependencies +
        ", repositoryId='" + repositoryId + '\'' +
        ", changes='" + changes + '\'' +
        ", snapshotPath='" + snapshotPath + '\'' +
        ", snapshot=" + snapshot +
        ", isInstalled=" + isInstalled +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TemplateVersion)) {
      return false;
    }
    TemplateVersion that = (TemplateVersion) o;
    return getId().equals(that.getId()) &&
        Objects.equals(getDescription(), that.getDescription()) &&
        Objects.equals(getVersion(), that.getVersion()) &&
        Objects.equals(getComponents(), that.getComponents()) &&
        Objects.equals(getResponseFilters(), that.getResponseFilters()) &&
        Objects.equals(getGroups(), that.getGroups()) &&
        Objects.equals(getRepositoryId(), that.getRepositoryId());
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(getId(), getDescription(), getVersion(), components, responseFilters, getGroups(),
            getRepositoryId());
  }

  public static final class Builder {

    private String description;
    private String id;
    private List<RulesGroup> groups;
    private BufferedImage snapShot;
    private String responseFilters;
    private String components;
    private String repositoryId;
    private String version;
    private String changes;
    private List<CorrelationTemplateDependency> dependencies;
    private String author;
    private String url;

    public Builder() {
    }

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withGroups(List<RulesGroup> groups) {
      this.groups = groups;
      return this;
    }

    public Builder withSnapshot(BufferedImage snapshot) {
      this.snapShot = snapshot;
      return this;
    }

    public Builder withComponents(String components) {
      this.components = components;
      return this;
    }

    public Builder withResponseFilters(String responseFilters) {
      this.responseFilters = responseFilters;
      return this;
    }

    public Builder withRepositoryId(String repositoryId) {
      this.repositoryId = repositoryId;
      return this;
    }

    public Builder withVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder withChanges(String changes) {
      this.changes = changes;
      return this;
    }

    public TemplateVersion build() {
      return new TemplateVersion(this);
    }

    public Builder withDependencies(List<CorrelationTemplateDependency> dependencies) {
      this.dependencies = dependencies;
      return this;
    }

    public Builder withAuthor(String author) {
      this.author = author;
      return this;
    }

    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    @Override
    public String toString() {
      return "Builder{" +
          "description='" + description + '\'' +
          ", id='" + id + '\'' +
          ", author='" + author + '\'' +
          ", url='" + url + '\'' +
          ", rules=" + groups.toString() +
          ", snapShot=" + snapShot +
          ", responseFilters='" + responseFilters + '\'' +
          ", components='" + components + '\'' +
          ", version='" + version + '\'' +
          ", repositoryId='" + repositoryId + '\'' +
          ", changes='" + changes + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Builder)) {
        return false;
      }
      Builder builder = (Builder) o;
      return Objects.equals(description, builder.description) &&
          Objects.equals(id, builder.id) &&
          Objects.equals(groups, builder.groups) &&
          Objects.equals(snapShot, builder.snapShot) &&
          Objects.equals(responseFilters, builder.responseFilters) &&
          Objects.equals(components, builder.components) &&
          Objects.equals(version, builder.version) &&
          Objects.equals(repositoryId, builder.repositoryId) &&
          Objects.equals(changes, builder.changes) &&
          Objects.equals(dependencies, builder.dependencies) &&
          Objects.equals(author, builder.author) &&
          Objects.equals(url, builder.url);
    }

    @Override
    public int hashCode() {
      return Objects
          .hash(description, id, groups, snapShot, responseFilters, components, repositoryId,
              changes, version, dependencies, author, url);
    }
  }
}
