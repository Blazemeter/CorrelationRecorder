package com.blazemeter.jmeter.correlation.gui.templates.validations;

public interface Condition {
  boolean isValid();

  void updateState(String input);

  String getErrorMessage();
}

