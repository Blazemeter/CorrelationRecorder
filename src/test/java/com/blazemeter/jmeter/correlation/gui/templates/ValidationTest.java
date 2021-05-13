package com.blazemeter.jmeter.correlation.gui.templates;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.gui.templates.Validation;
import org.junit.Test;

public class ValidationTest {

  private Validation validation;

  @Test
  public void shouldReturnFalseWhenFailsValidationIsMeet() {
    validation = new Validation(text -> text.isEmpty(), "%s is empty");
    assertThat(validation.failsValidation("")).isTrue();
  }

  @Test
  public void shouldReturnFalseWhenFailsValidationNotMeet() {
    validation = new Validation(text -> text.isEmpty(), "%s is empty");
    validation.failsValidation("Not empty text");
    assertThat(validation.failsValidation("Not empty text")).isFalse();
  }

  @Test
  public void shouldFormatErrorMessageWhenFailsValidationIsMeet() {
    validation = new Validation(text -> text.isEmpty(), "%s is empty");
    validation.failsValidation("");
    assertThat(validation.formatErrorMessage("ID")).isEqualTo("ID is empty");
  }
}
