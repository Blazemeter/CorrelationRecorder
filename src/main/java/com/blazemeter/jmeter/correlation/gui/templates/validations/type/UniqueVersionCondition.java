package com.blazemeter.jmeter.correlation.gui.templates.validations.type;

import com.blazemeter.jmeter.correlation.gui.templates.validations.Condition;
import java.util.List;
import java.util.function.Supplier;

public class UniqueVersionCondition implements Condition {
  private boolean valid;
  private Supplier<List<String>> getVersions;

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void updateState(String input) {
    if (getVersions == null) {
      valid = false;
      return;
    }

    List<String> versions = getVersions.get();
    valid = versions.stream().noneMatch(version -> version.equals(input));
  }

  @Override
  public String getErrorMessage() {
    return "Version already exists.";
  }

  public void setVersionsSupplier(Supplier<List<String>> getVersions) {
    this.getVersions = getVersions;
  }
}
