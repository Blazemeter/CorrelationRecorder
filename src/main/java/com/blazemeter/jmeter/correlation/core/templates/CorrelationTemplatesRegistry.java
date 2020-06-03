package com.blazemeter.jmeter.correlation.core.templates;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CorrelationTemplatesRegistry {

  void save(CorrelationTemplate correlationTemplate) throws IOException;

  Optional<CorrelationTemplate> findByID(String repositoryOwner, String id,
      String templateVersion) throws IOException;

  List<CorrelationTemplate> getInstalledTemplates();
}
