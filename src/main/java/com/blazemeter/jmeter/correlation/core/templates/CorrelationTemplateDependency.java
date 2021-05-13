package com.blazemeter.jmeter.correlation.core.templates;

import com.helger.commons.annotation.VisibleForTesting;
import java.util.Objects;

public class CorrelationTemplateDependency {

  private String name;
  private String version;
  private String url;

  public CorrelationTemplateDependency() {

  }

  @VisibleForTesting
  public CorrelationTemplateDependency(String name, String version, String url) {
    this.name = name;
    this.version = version;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CorrelationTemplateDependency)) {
      return false;
    }
    CorrelationTemplateDependency that = (CorrelationTemplateDependency) o;
    return name.equals(that.name) &&
        version.equals(that.version) &&
        url.equals(that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, url);
  }

  @Override
  public String toString() {
    return "CorrelationTemplateDependency{" +
        "name='" + name + '\'' +
        ", version='" + version + '\'' +
        ", url='" + url + '\'' +
        '}';
  }
}
