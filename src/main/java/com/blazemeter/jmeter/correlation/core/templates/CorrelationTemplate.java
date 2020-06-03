package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CorrelationTemplate {

  private static final String SNAPSHOT_FILE_EXTENSION = ".png";
  private static final String SNAPSHOT_SUFFIX = "-snapshot";
  private String id;
  private String description;
  private String version;
  private String components;
  private String responseFilters;
  private List<CorrelationRule> rules;
  private List<CorrelationTemplateDependency> dependencies = new ArrayList<>();
  private String repositoryId = "";
  private String changes;

  private transient String snapshotPath;
  private transient BufferedImage snapshot;
  private transient boolean isInstalled;

  //Default constructor to meet serialization
  public CorrelationTemplate() {
  }

  private CorrelationTemplate(Builder builder) {
    id = builder.id;
    description = builder.description;
    rules = builder.rules;
    snapshot = builder.snapShot;
    responseFilters = builder.responseFilters;
    components = builder.components;
    repositoryId = builder.repositoryId;
    version = builder.version;
    changes = builder.changes;
    dependencies = builder.dependencies;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public List<CorrelationRule> getRules() {
    return rules;
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

  boolean isInstalled() {
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
        ", components='" + components + '\'' +
        ", responseFilters='" + responseFilters + '\'' +
        ", rules=" + rules +
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
    if (!(o instanceof CorrelationTemplate)) {
      return false;
    }
    CorrelationTemplate that = (CorrelationTemplate) o;
    return getId().equals(that.getId()) &&
        Objects.equals(getDescription(), that.getDescription()) &&
        Objects.equals(getVersion(), that.getVersion()) &&
        Objects.equals(getComponents(), that.getComponents()) &&
        Objects.equals(getResponseFilters(), that.getResponseFilters()) &&
        Objects.equals(getRules(), that.getRules()) &&
        Objects.equals(getRepositoryId(), that.getRepositoryId());
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(getId(), getDescription(), getVersion(), components, responseFilters, getRules(),
            getRepositoryId());
  }

  public static final class Builder {

    private String description;
    private String id;
    private List<CorrelationRule> rules;
    private BufferedImage snapShot;
    private String responseFilters;
    private String components;
    private String repositoryId;
    private String version;
    private String changes;
    private List<CorrelationTemplateDependency> dependencies;

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

    public Builder withRules(List<CorrelationRule> rules) {
      this.rules = rules;
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

    public CorrelationTemplate build() {
      return new CorrelationTemplate(this);
    }

    public Builder withDependencies(List<CorrelationTemplateDependency> dependencies) {
      this.dependencies = dependencies;
      return this;
    }

    @Override
    public String toString() {
      return "Builder{" +
          "description='" + description + '\'' +
          ", id='" + id + '\'' +
          ", rules=" + rules +
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
          Objects.equals(rules, builder.rules) &&
          Objects.equals(snapShot, builder.snapShot) &&
          Objects.equals(responseFilters, builder.responseFilters) &&
          Objects.equals(components, builder.components) &&
          Objects.equals(version, builder.version) &&
          Objects.equals(repositoryId, builder.repositoryId) &&
          Objects.equals(changes, builder.changes) &&
          Objects.equals(dependencies, builder.dependencies);
    }

    @Override
    public int hashCode() {
      return Objects
          .hash(description, id, rules, snapShot, responseFilters, components, repositoryId,
              changes, version, dependencies);
    }
  }
}
