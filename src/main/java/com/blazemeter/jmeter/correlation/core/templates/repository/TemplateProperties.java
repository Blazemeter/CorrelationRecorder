package com.blazemeter.jmeter.correlation.core.templates.repository;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.TEMPLATE_PROPERTY_DISALLOW_TO_USE;
import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.TEMPLATE_PROPERTY_NOT_ALLOW_EXPORT;

import java.util.HashMap;

public class TemplateProperties extends HashMap<String, String> {

  public TemplateProperties() {
    super();
    put(TEMPLATE_PROPERTY_NOT_ALLOW_EXPORT, "false");
    put(TEMPLATE_PROPERTY_DISALLOW_TO_USE, "false");
  }
}
