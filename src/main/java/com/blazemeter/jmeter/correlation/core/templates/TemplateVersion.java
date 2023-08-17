package com.blazemeter.jmeter.correlation.core.templates;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;

@JsonDeserialize(keyUsing = TemplateVersionKeyDeserializer.class)
public class TemplateVersion implements Serializable {
  private static final long serialVersionUID = 1L;
  private String name;
  private String version;

  public TemplateVersion() {
  }

  public TemplateVersion(String name, String version) {
    this.name = name;
    this.version = version;
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

  public String toString() {
    return name + " " + version;
  }
}
