package com.blazemeter.jmeter.correlation.gui.templates.validations.type;

import com.blazemeter.jmeter.correlation.gui.templates.validations.Condition;

public class NoNumbersCondition implements Condition {
  private boolean valid;

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void updateState(String input) {
    valid = input != null && !input.matches(".*\\d+.*");
  }

  @Override
  public String getErrorMessage() {
    return "Field cannot contain numbers.";
  }
}


