package com.blazemeter.jmeter.correlation.core.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a list of versions for a specific template.
 * For example, if we have a template for "WordPress" with versions 1.0 and 1.1,
 * this class will have name="WordPress" and versions=[1.0, 1.1]
 */
public class CorrelationTemplateVersions {

  private final List<String> versions = new ArrayList<>();
  private transient String name;
  private String repositoryDisplayName;

  //Constructor added to avoid issues with the serialization
  public CorrelationTemplateVersions() {
    this.repositoryDisplayName = "";
  }

  public CorrelationTemplateVersions(String name, List<String> versions) {
    this.name = name;
    this.versions.addAll(versions);
  }

  public CorrelationTemplateVersions(String version) {
    this.versions.add(version);
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public List<String> getVersions() {
    return versions;
  }

  public void addVersion(String version) {
    versions.add(version);
  }

  public String getRepositoryDisplayName() {
    return repositoryDisplayName;
  }

  public void setRepositoryDisplayName(String repositoryDisplayName) {
    this.repositoryDisplayName = repositoryDisplayName;
  }

  @Override
  public String toString() {
    return "CorrelationTemplateVersions {" +
        "versions=" + versions +
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
    CorrelationTemplateVersions that = (CorrelationTemplateVersions) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(versions, that.versions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, versions);
  }

}
