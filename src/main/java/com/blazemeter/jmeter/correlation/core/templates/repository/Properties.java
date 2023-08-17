package com.blazemeter.jmeter.correlation.core.templates.repository;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.TEMPLATE_PROPERTY_DISALLOW_TO_USE;
import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.TEMPLATE_PROPERTY_NOT_ALLOW_EXPORT;

public class Properties extends TemplateProperties {
  private static final long serialVersionUID = 1L;

  public Properties() {
    put(TEMPLATE_PROPERTY_NOT_ALLOW_EXPORT, "false");
    put(TEMPLATE_PROPERTY_DISALLOW_TO_USE, "false");
  }

  public boolean canExport() {
    String exportString = get(TEMPLATE_PROPERTY_NOT_ALLOW_EXPORT);
    if (exportString == null) {
      return false;
    }

    return !Boolean.parseBoolean(exportString);
  }

  public boolean canUse() {
    String useString = get(TEMPLATE_PROPERTY_DISALLOW_TO_USE);
    if (useString == null) {
      return false;
    }

    return !Boolean.parseBoolean(useString);
  }

  @Override
  public String toString() {
    return "Capabilities{"
        + " canExport" + canExport()
        + ", canUse" + canUse()
        + '}';
  }
}
