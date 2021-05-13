package com.blazemeter.jmeter.correlation.gui.templates;

import java.util.function.Predicate;

public class Validation {

  private final Predicate<String> condition;
  private final String errorMessageTemplate;

  public Validation(Predicate<String> condition, String errorMessageTemplate) {
    this.condition = condition;
    this.errorMessageTemplate = errorMessageTemplate;
  }

  public boolean failsValidation(String value) {
    return condition.test(value);
  }

  public String formatErrorMessage(String owner) {
    return String.format(errorMessageTemplate, owner);
  }
}
