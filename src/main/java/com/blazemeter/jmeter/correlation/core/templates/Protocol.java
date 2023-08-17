package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.util.HashMap;
import java.util.Map;

public class Protocol {
  private String name;
  private Map<Template, TemplateProperties> templatesAndProperties = new HashMap<>();

  public Protocol(String name) {
    this.name = name;
  }

  public void addTemplate(Template template, TemplateProperties properties) {
    templatesAndProperties.put(template, properties);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<Template, TemplateProperties> getTemplatesAndProperties() {
    return templatesAndProperties;
  }

  public void setTemplatesAndProperties(
      Map<Template, TemplateProperties> templatesAndProperties) {
    this.templatesAndProperties = templatesAndProperties;
  }
}
