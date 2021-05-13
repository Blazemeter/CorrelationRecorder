package com.blazemeter.jmeter.correlation.core.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CorrelationTemplateReference {

  private final List<String> versions = new ArrayList<>();
  private transient String name;

  //Constructor added to avoid issues with the serialization
  public CorrelationTemplateReference() {

  }

  public CorrelationTemplateReference(String version) {
    this.versions.add(version);
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getVersions() {
    return versions;
  }

  public void addVersion(String version) {
    versions.add(version);
  }

  @Override
  public String toString() {
    return "CorrelationTemplateReference{" +
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
    CorrelationTemplateReference that = (CorrelationTemplateReference) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(versions, that.versions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, versions);
  }

}
