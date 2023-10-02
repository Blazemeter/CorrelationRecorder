package com.blazemeter.jmeter.correlation.core.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalCorrelationTemplatesRegistry implements CorrelationTemplatesRegistry {
  private static final Logger LOG = LoggerFactory
      .getLogger(LocalCorrelationTemplatesRegistry.class);
  private final LocalConfiguration localConfiguration;

  public LocalCorrelationTemplatesRegistry(LocalConfiguration localConfiguration) {
    this.localConfiguration = localConfiguration;
  }

}
