package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.LOCAL_REPOSITORY_NAME;

import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationTemplatesRepositoriesConfiguration {

  private static final Logger LOG = LoggerFactory
      .getLogger(CorrelationTemplatesRepositoriesConfiguration.class);
  private final LocalConfiguration localConfig;

  public CorrelationTemplatesRepositoriesConfiguration(LocalConfiguration localConfiguration) {
    localConfig = localConfiguration;
  }

  public String getLocalRootFolder() {
    return localConfig.getRootFolder();
  }

  public RepositoryManager getRepositoryManager(String name) {
    return localConfig.getRepositoryManager(name);
  }

  public RepositoryManager getRepositoryManager(String name, String url) {
    RepositoryManager manager = localConfig.getRepositoryManager(name, url);
    if (manager == null) {
      manager = localConfig.getRepositoryManagerFromFolderOrUrl(name, url);
    }
    return manager;
  }

  /*
  public void save(String name, String url) throws IOException {
    getTemplateRegistry(name, url).save(name, url);
  }
  */

  private CorrelationTemplatesRepositoriesRegistry getTemplateRegistry(String name) {
    return ((CorrelationTemplatesRepositoriesRegistry) localConfig.getRepositoryManager(name)
        .getTemplateRegistry());
  }

  private CorrelationTemplatesRepositoriesRegistry getTemplateRegistry(String name, String url) {
    RepositoryManager manager = getRepositoryManager(name, url);
    return (CorrelationTemplatesRepositoriesRegistry) manager.getTemplateRegistry();
  }

  public void saveRepository(String name, String url) throws IOException {
    RepositoryManager manager = getRepositoryManager(name, url);
    ((CorrelationTemplatesRepositoriesRegistry) manager.getTemplateRegistry()).save(name, url);
    localConfig.addRepositoryManager(manager);
  }

  public void deleteRepository(String name) throws IOException {
    getTemplateRegistry(name).delete(name);
  }

  public List<CorrelationTemplatesRepository> getCorrelationRepositories() {
    return localConfig.getRepositories();
  }

  public Map<String, CorrelationTemplateVersions> getCorrelationTemplateVersionsByRepositoryName(
      String name, boolean useLocal) {
    Map<String, CorrelationTemplateVersions> versions = null;
    if (!useLocal) {
      RepositoryManager repManager = localConfig.getRepositoryManager(name);
      versions = repManager.getTemplateVersions();
    }
    if (versions == null) {
      return getTemplateRegistry(
          LOCAL_REPOSITORY_NAME).getCorrelationTemplateVersionsByRepositoryId(name);
    } else {
      return versions;
    }
  }

  public void installTemplate(String repositoryName, String templateName, String version)
      throws ConfigurationException {
    localConfig.installTemplate(repositoryName, templateName,
        version);
  }

  public void uninstallTemplate(String repositoryName, String name, String version)
      throws ConfigurationException {
    localConfig.uninstallTemplate(repositoryName, name, version);
  }

  public String getRepositoryURL(String name) {
    return getTemplateRegistry(LOCAL_REPOSITORY_NAME).getRepositoryURL(name);
  }

  public boolean isLocalTemplateVersionSaved(String templateId, String templateVersion) {
    return getTemplateRegistry(LOCAL_REPOSITORY_NAME).isLocalTemplateVersionSaved(templateId,
        templateVersion);
  }

  public Map<Template, TemplateProperties> getCorrelationTemplatesAndPropertiesByRepositoryName(
      String name,
      boolean useLocal) {
    Map<Template, TemplateProperties> templatesAndProperties = null;
    if (!useLocal) {
      RepositoryManager repManager = localConfig.getRepositoryManager(name);
      try {
        templatesAndProperties = repManager.getTemplatesAndProperties();
      } catch (Exception ex) {
        LOG.error("Unknown error getting templates on " + name, ex);
      }
    }

    if (templatesAndProperties == null) {
      return getTemplateRegistry(
          LOCAL_REPOSITORY_NAME).getCorrelationTemplatesAndPropertiesByRepositoryId(name);
    } else {
      return templatesAndProperties;
    }
  }
}
