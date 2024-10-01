package com.blazemeter.jmeter.correlation.custom.extension;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.google.common.annotations.VisibleForTesting;

/**
 * Dummy class to be used only for testing proposes
 */
@VisibleForTesting
public class CustomCorrelationReplacement extends RegexCorrelationReplacement<CustomContext> {

  @Override
  public String getDisplayName() {
    return "Custom Correlation Replacement";
  }

  @Override
  public Class<? extends CorrelationContext> getSupportedContext() {
    return CustomContext.class;
  }
}
