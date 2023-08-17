package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.LOCAL_REPOSITORY_NAME;
import static com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils.getRepositoryFileName;
import static com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils.isURL;

import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.commons.codec.digest.DigestUtils;
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

  public static InputStream getInputStream(String path) throws IOException {
    return isURL(path) ? new URL(path).openStream() : new FileInputStream(path.replace("file://",
        ""));
  }

  public RepositoryManager getRepositoryManager(String name) {
    return localConfig.getRepositoryManager(name);
  }

  public RepositoryManager getRepositoryManager(String name, String url) {
    return localConfig.getRepositoryManager(name, url);
  }

  public void save(String name, String url) throws IOException {
    getTemplateRegistry(name, url).save(name, url);
  }

  private CorrelationTemplatesRepositoriesRegistry getTemplateRegistry(String name) {
    return ((CorrelationTemplatesRepositoriesRegistry) localConfig.getRepositoryManager(name)
        .getTemplateRegistry());
  }

  private CorrelationTemplatesRepositoriesRegistry getTemplateRegistry(String name, String url) {
    return ((CorrelationTemplatesRepositoriesRegistry) localConfig.getRepositoryManager(name, url)
        .getTemplateRegistry());
  }

  public void deleteRepository(String name) throws IOException {
    getTemplateRegistry(name).delete(name);
  }

  public List<CorrelationTemplatesRepository> getCorrelationRepositories() {
    return getTemplateRegistry(LOCAL_REPOSITORY_NAME).getRepositories();
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
    getTemplateRegistry(LOCAL_REPOSITORY_NAME).installTemplate(repositoryName, templateName,
        version);
  }

  public void uninstallTemplate(String repositoryName, String name, String version)
      throws ConfigurationException {
    getTemplateRegistry(LOCAL_REPOSITORY_NAME).uninstallTemplate(repositoryName, name, version);
  }

  public String getRepositoryURL(String name) {
    return getTemplateRegistry(LOCAL_REPOSITORY_NAME).getRepositoryURL(name);
  }

  public void updateLocalRepository(String templateId, String templateVersion) {
    getTemplateRegistry(LOCAL_REPOSITORY_NAME).updateLocalRepository(templateId, templateVersion);
  }

  public boolean isLocalTemplateVersionSaved(String templateId, String templateVersion) {
    return getTemplateRegistry(LOCAL_REPOSITORY_NAME).isLocalTemplateVersionSaved(templateId,
        templateVersion);
  }

  public boolean refreshRepositories(String configurationRoute,
                                     Consumer<Integer> setProgressConsumer,
                                     Consumer<String> setStatusConsumer) {
    int progress = 0;
    boolean isUpToDate = true;
    String correlationTemplateInstallationPath = configurationRoute +
        LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER;

    setProgressConsumer.accept(progress);

    AbstractMap<String, RepositoryManager> repositoriesManagers =
        localConfig.getRepositoriesManagers();

    List<CorrelationTemplatesRepository> repositories = getCorrelationRepositories();

    int total = 0;

    Iterator<String> managersIterator = repositoriesManagers.keySet().iterator();
    int progressAdvance = 0;
    while (managersIterator.hasNext()) {
      progressAdvance += 1;

      // Recalculate, can increase dynamically on each iteration
      total = repositoriesManagers.size() + repositories.size();

      progress = (progressAdvance * 100) / total;

      String managerId = managersIterator.next();
      RepositoryManager manager = repositoriesManagers.get(managerId);
      try {
        setStatusConsumer.accept("Checking " + manager.getName() + "...");
        setProgressConsumer.accept(progress);

        manager.setup();
      } catch (Exception e) {
        LOG.error("Unknown Exception setting up {} with implementation:{}",
            managerId, manager.getClass().getName(), e);
      }
    }

    // After setup, update the list of repositories and the total
    repositories = getCorrelationRepositories();
    total = repositoriesManagers.size() + repositories.size();

    for (CorrelationTemplatesRepository repo : repositories) {
      progressAdvance += 1;
      progress = (progressAdvance * 100) / total;
      setStatusConsumer.accept("Updating " + repo.getName() + "...");
      setProgressConsumer.accept(progress);

      String url = getRepositoryURL(repo.getName());
      String localFilePath =
          correlationTemplateInstallationPath + repo.getName()
              + "/" + getRepositoryFileName(repo.getName());

      // Exclude if is Local
      if (repo.getName().equals(LOCAL_REPOSITORY_NAME)) {
        continue;
      }

      try {
        RepositoryManager repManager = localConfig.getRepositoryManager(repo.getName(), url);
        if (repManager.getRepository() == null) {
          if (DigestUtils.md5Hex(getInputStream(url))
              .equals(DigestUtils.md5Hex(getInputStream(localFilePath)))) {
            continue;
          }
        } else {
          // Compute the difference with the object
          if (DigestUtils.md5Hex(localConfig.getValueAsBytes(repManager.getTemplateVersions()))
              .equals(DigestUtils.md5Hex(getInputStream(localFilePath)))) {
            continue;
          }
        }
        save(repo.getName(), url);
        isUpToDate = false;
      } catch (IOException e) {
        LOG.error("Error while comparing MD5 url: {} localPath: {} ", url, localFilePath, e);
      }
    }
    progress = 100;
    setStatusConsumer.accept("Update finished");
    setProgressConsumer.accept(progress);

    return isUpToDate;
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
