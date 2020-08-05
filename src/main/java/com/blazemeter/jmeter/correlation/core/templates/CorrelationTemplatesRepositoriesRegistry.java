package com.blazemeter.jmeter.correlation.core.templates;

import java.io.IOException;
import java.util.List;

public interface CorrelationTemplatesRepositoriesRegistry {

  void save(String name, String url) throws IOException;

  void delete(String name) throws IOException;

  CorrelationTemplatesRepository find(String id);

  List<CorrelationTemplatesRepository> getRepositories();
}
