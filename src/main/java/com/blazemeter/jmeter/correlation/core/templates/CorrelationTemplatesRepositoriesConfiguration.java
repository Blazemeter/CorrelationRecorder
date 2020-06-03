package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.JSON_FILE_EXTENSION;
import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.LOCAL_REPOSITORY_NAME;
import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.REPOSITORY_NAME_SUFFIX;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationTemplatesRepositoriesConfiguration {

  private static final Logger LOG = LoggerFactory
      .getLogger(CorrelationTemplatesRepositoriesConfiguration.class);
  private LocalCorrelationTemplatesRepositoriesRegistry local;
  private RemoteCorrelationTemplatesRepositoriesRegistry remote;

  public CorrelationTemplatesRepositoriesConfiguration(LocalConfiguration localConfiguration) {
    local = new LocalCorrelationTemplatesRepositoriesRegistry(localConfiguration);
    remote = new RemoteCorrelationTemplatesRepositoriesRegistry(localConfiguration);
  }

  public void save(String name, String url) throws IOException {
    if (isURL(url)) {
      remote.save(name, url);
    } else {
      local.save(name, url);
    }
  }

  private static boolean isURL(String text) {
    return text.toLowerCase().contains("http") || text.toLowerCase().contains("ftp");
  }

  public void deleteRepository(String name) throws IOException {
    local.delete(name);
  }

  public List<CorrelationTemplatesRepository> getCorrelationRepositories() {
    return local.getRepositories();
  }

  public List<CorrelationTemplate> getCorrelationTemplatesByRepositoryName(String name) {
    return local.getCorrelationTemplatesByRepositoryId(name);
  }

  public void installTemplate(String repositoryName, String templateName, String version)
      throws ConfigurationException {
    local.installTemplate(repositoryName, templateName, version);
  }

  public void uninstallTemplate(String repositoryName, String name, String version)
      throws ConfigurationException {
    local.uninstallTemplate(repositoryName, name, version);
  }

  public String getRepositoryURL(String name) {
    return local.getRepositoryURL(name);
  }

  public void updateLocalRepository(String templateId, String templateVersion) {
    local.updateLocalRepository(templateId, templateVersion);
  }

  public boolean isLocalTemplateVersionSaved(String templateId, String templateVersion) {
    return local.isLocalTemplateVersionSaved(templateId, templateVersion);
  }

  public void refreshRepositories(String configurationRoute,
      Consumer<Integer> setProgressConsumer) {
    String correlationTemplateInstallationPath = configurationRoute +
        LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER;
    List<CorrelationTemplatesRepository> repositories = getCorrelationRepositories();

    for (CorrelationTemplatesRepository repo : repositories) {
      if (repo.getName().equals(LOCAL_REPOSITORY_NAME)) {
        continue;
      }
      setProgressConsumer.accept((repositories.indexOf(repo) + 1) * 100 / repositories.size());
      String url = getRepositoryURL(repo.getName());
      String localFilePath =
          correlationTemplateInstallationPath + repo.getName()
              + "/" + repo.getName() + REPOSITORY_NAME_SUFFIX + JSON_FILE_EXTENSION;
      try {
        if (DigestUtils.md5Hex(getInputStream(url)).equals(DigestUtils.md5Hex(localFilePath))) {
        continue;
        }
        save(repo.getName(), url);
      } catch (IOException e) {
        LOG.error("Error while comparing MD5 url: {} localPath: {} ", url, localFilePath, e);
      }
    }
  }

  private static InputStream getInputStream(String path) throws IOException {
    return isURL(path) ? new URL(path).openStream() : new FileInputStream(path);
  }
}
