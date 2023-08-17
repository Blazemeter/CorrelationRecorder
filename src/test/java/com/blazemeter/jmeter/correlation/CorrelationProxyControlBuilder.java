package com.blazemeter.jmeter.correlation;

import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistry;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import org.apache.jmeter.gui.tree.JMeterTreeNode;

public class CorrelationProxyControlBuilder {

  private CorrelationEngine engine;
  private CorrelationComponentsRegistry registry;
  private LocalConfiguration configuration;
  private CorrelationTemplatesRegistry templatesRegistry;
  private CorrelationTemplatesRepositoriesConfiguration repositoriesRegistry;
  private JMeterTreeNode target;
  private String configurationPath;

  public CorrelationProxyControlBuilder() {
  }

  public CorrelationProxyControlBuilder withCorrelationEngine(CorrelationEngine engine) {
    this.engine = engine;
    return this;
  }

  public CorrelationProxyControlBuilder withLocalConfiguration(LocalConfiguration configuration) {
    this.configuration = configuration;
    return this;
  }

  public CorrelationProxyControlBuilder withCorrelationTemplatesRegistry(
      CorrelationTemplatesRegistry templatesRegistry) {
    this.templatesRegistry = templatesRegistry;
    return this;
  }

  public CorrelationProxyControlBuilder withCorrelationTemplatesRepositoriesConfiguration(
      CorrelationTemplatesRepositoriesConfiguration repositoriesRegistry) {
    this.repositoriesRegistry = repositoriesRegistry;
    return this;
  }

  public CorrelationProxyControlBuilder withTarget(JMeterTreeNode target) {
    this.target = target;
    return this;
  }

  public CorrelationProxyControlBuilder withLocalConfigurationPath(String path) {
    this.configurationPath = path;
    return this;
  }

  public CorrelationProxyControl build() {
    engine = engine != null ? engine : new CorrelationEngine();
    registry = registry != null ? registry : CorrelationComponentsRegistry.getInstance();
    configuration = configuration != null ? configuration
        : new LocalConfiguration(configurationPath);
    templatesRegistry = templatesRegistry != null ? templatesRegistry :
        new LocalCorrelationTemplatesRegistry(configuration);
    repositoriesRegistry = repositoriesRegistry != null ?
        repositoriesRegistry
        : new CorrelationTemplatesRepositoriesConfiguration(configuration);
    CorrelationProxyControl model = new CorrelationProxyControl(registry, repositoriesRegistry,
        configuration, engine, templatesRegistry);
    model.setName("bzm - Correlation Recorder");

    if (target != null) {
      model.setTarget(target);
    }
    return model;
  }
}
