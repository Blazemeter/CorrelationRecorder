package com.blazemeter.jmeter.correlation.core.templates.repository.pluggable;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.LOCAL_REPOSITORY_NAME;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.repository.FileRepository;
import com.blazemeter.jmeter.correlation.core.templates.repository.PluggableRepository;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalRepository extends FileRepository implements PluggableRepository {

  private static final Logger LOG = LoggerFactory.getLogger(LocalConfiguration.class);

  @Override
  public String getName() {
    return LOCAL_REPOSITORY_NAME;
  }

  @Override
  public void setEndPoint(String endPoint) {
    // Not allowed
  }

  @Override
  public void setup() {
    File localRepositoryFile = getRepositoryFile();
    try {
      if (!localRepositoryFile.exists() &&
          localRepositoryFile.createNewFile()) {
        initEmptyLocalRepository(localRepositoryFile);
        LOG.info("Created the local repository file {}", localRepositoryFile);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private void initEmptyLocalRepository(File localRepositoryFile) throws IOException {
    Map<String, CorrelationTemplateVersions> empty = new HashMap<>();
    this.getConfig().writeValue(localRepositoryFile, empty);
  }

  @Override
  public boolean autoLoad() {
    return true;
  }

  @Override
  public boolean disableConfig() {
    return true;
  }

  @Override
  public CorrelationTemplatesRepository getRepository() {
    return null;
  }

  @Override
  public Map<String, CorrelationTemplateVersions> getTemplateVersions() {
    return null;
  }

  @Override
  public List<Template> getTemplates(List<TemplateVersion> filter) {
    return null;
  }

  @Override
  public Map<Template, TemplateProperties> getTemplatesAndProperties() {
    return null;
  }

  @Override
  public Map<Template, TemplateProperties> getTemplatesAndProperties(List<TemplateVersion> filter) {
    return null;
  }

  @Override
  public void upload(Template template) throws IOException {
    // Left empty intentionally for development purposes
  }

  @Override
  public String getDisplayName() {
    return LOCAL_REPOSITORY_NAME;
  }

  @Override
  public void setDisplayName(String displayName) {
    // Left empty intentionally for development purposes
  }

}
