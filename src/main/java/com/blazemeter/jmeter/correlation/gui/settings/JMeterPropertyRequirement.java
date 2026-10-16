package com.blazemeter.jmeter.correlation.gui.settings;

import org.apache.jmeter.util.JMeterUtils;

/**
 * A JMeter property that recording and replaying need at a given value, so the correlation
 * analyses the same flow that a correctly configured JMeter would produce.
 */
public final class JMeterPropertyRequirement {

  private final String name;
  private final String expectedValue;
  private final String jmeterDefault;
  private final String effectWhenNotExpected;

  /**
   * @param name                  the JMeter property
   * @param expectedValue         the value the correlation needs
   * @param jmeterDefault         the value JMeter uses when the property is not set
   * @param effectWhenNotExpected what goes wrong when the property has another value
   */
  public JMeterPropertyRequirement(String name, String expectedValue, String jmeterDefault,
      String effectWhenNotExpected) {
    this.name = name;
    this.expectedValue = expectedValue;
    this.jmeterDefault = jmeterDefault;
    this.effectWhenNotExpected = effectWhenNotExpected;
  }

  public String getName() {
    return name;
  }

  public String getExpectedValue() {
    return expectedValue;
  }

  public String getEffectWhenNotExpected() {
    return effectWhenNotExpected;
  }

  /**
   * @return the value set for the property, or null when it is not set
   */
  public String getConfiguredValue() {
    return JMeterUtils.getProperty(name);
  }

  /**
   * @return the value JMeter uses: the configured one, or JMeter's default when not set
   */
  public String getEffectiveValue() {
    String configured = getConfiguredValue();
    return configured == null ? jmeterDefault : configured.trim();
  }

  public boolean isMet() {
    return expectedValue.equalsIgnoreCase(getEffectiveValue());
  }

  /**
   * @return the effective value as a user reads it, saying when it is JMeter's default
   */
  public String describeEffectiveValue() {
    return getConfiguredValue() == null ? jmeterDefault + " (JMeter's default)"
        : getEffectiveValue();
  }
}
