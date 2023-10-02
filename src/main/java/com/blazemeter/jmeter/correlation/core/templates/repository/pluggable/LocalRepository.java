package com.blazemeter.jmeter.correlation.core.templates.repository.pluggable;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.LOCAL_REPOSITORY_NAME;

import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst;
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
  private static final String SIEBEL_TEMPLATE_NAME = "siebel";
  private static final String DEFAULT_SIEBEL_TEMPLATE_VERSION = "1.0";
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
        LOG.info("Created the local repository file {}", localRepositoryFile);
        this.getConfig()
            .writeValue(localRepositoryFile, new HashMap<String, CorrelationTemplateVersions>() {
              {
                CorrelationTemplateVersions value =
                    new CorrelationTemplateVersions(DEFAULT_SIEBEL_TEMPLATE_VERSION);
                value.setRepositoryDisplayName(LOCAL_REPOSITORY_NAME);
                put(SIEBEL_TEMPLATE_NAME, value);
              }
            });
        LOG.info("Saved local repository file");
        if (!isSiebelInstalled()) {
          installSiebel();
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

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

  private boolean isSiebelInstalled() {
    return this.getConfig().isInstalled(getName(), SIEBEL_TEMPLATE_NAME);
  }

  private void installSiebel() {
    if (!isSiebelInstalled()) {
      try {
        this.getConfig().installTemplate(RepositoryGeneralConst.LOCAL_REPOSITORY_NAME,
            SIEBEL_TEMPLATE_NAME, DEFAULT_SIEBEL_TEMPLATE_VERSION);
      } catch (ConfigurationException e) {
        LOG.error("Error installing Siebel template", e);
      }
    }
    this.getConfig().saveLocalConfiguration();
  }

}
