package com.blazemeter.jmeter.correlation.gui.templates.validations;

import javax.swing.text.JTextComponent;

public interface ComponentValidation<T extends JTextComponent> {
  void updateValidationStates();

  void applyFormat();

  void clearFormat();

  boolean isValid();

  T getField();

  void setErrorMessage(String message);

  String getErrorMessage();

  boolean getErrorVisible();

  void reset();
}

