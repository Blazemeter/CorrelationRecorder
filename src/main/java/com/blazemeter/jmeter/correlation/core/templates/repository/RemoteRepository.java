package com.blazemeter.jmeter.correlation.core.templates.repository;

import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.RemoteCorrelationTemplatesRepositoriesRegistry;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RemoteRepository extends RepositoryManager {
  private static final Logger LOG = LoggerFactory.getLogger(LocalConfiguration.class);

  @Override
  public void init() {
    setTemplateRegistry(new RemoteCorrelationTemplatesRepositoriesRegistry(this.getConfig()));
  }

  @Override
  public void setup() {
    try {
      ((RemoteCorrelationTemplatesRepositoriesRegistry) getTemplateRegistry()).save(this.getName(),
          this.getEndPoint());
    } catch (IOException e) {
      LOG.warn("Error while trying to setup remote central repository", e);
    }
  }
}
