package com.blazemeter.jmeter.correlation.gui.templates.validations;

import javax.swing.text.JTextComponent;

public interface ComponentValidation<T extends JTextComponent> {
  void updateValidationStates();

  void applyFormat();

  boolean isValid();

  T getField();

  void setErrorMessage(String message);

  String getErrorMessage();

  void reset();
}

