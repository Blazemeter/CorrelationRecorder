package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.CENTRAL_REPOSITORY_ID;
import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.CENTRAL_REPOSITORY_URL;
import static com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry.TEMPLATE_FILE_SUFFIX;

import com.fasterxml.jackson.core.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCorrelationTemplatesRepositoriesRegistry extends
    LocalCorrelationTemplatesRepositoriesRegistry implements
    CorrelationTemplatesRepositoriesRegistry {

  private static final Logger LOG = LoggerFactory
      .getLogger(RemoteCorrelationTemplatesRepositoriesRegistry.class);

  private static final int CONNECT_TIMEOUT_MILLISECONDS = 3000;

  public RemoteCorrelationTemplatesRepositoriesRegistry(LocalConfiguration localConfiguration) {
    super(localConfiguration);
    setupCentralRemoteRepository();
  }

  private void setupCentralRemoteRepository() {
    File repositoryFile = new File(
        configuration.getCorrelationsTemplateInstallationFolder() +
            CENTRAL_REPOSITORY_ID + "/" + CENTRAL_REPOSITORY_ID
            + REPOSITORY_FILE_SUFFIX);
    if (!repositoryFile.exists()) {
      try {
        save(CENTRAL_REPOSITORY_ID, CENTRAL_REPOSITORY_URL);
      } catch (IOException e) {
        LOG.warn("Error while trying to setup remote central repository", e);
      }
    }
  }

  @Override
  public void save(String name, String url) throws IOException {
    String repositoryFolderName = name + "/";

    File repositoryFolder = new File(
        configuration.getCorrelationsTemplateInstallationFolder() + repositoryFolderName);
    if (!repositoryFolder.exists() && repositoryFolder.mkdir()) {
      LOG.info("Folder created for the repository {}", name);
    }

    String installationFolderPath =
        configuration.getCorrelationsTemplateInstallationFolder() + repositoryFolderName;
    String repositoryFilePath = installationFolderPath + name + REPOSITORY_FILE_SUFFIX;

    try {
      saveFileFromURL(url, repositoryFilePath);
      configuration.addRepository(name, url);

      String baseURL = getBaseURL(url);

      for (Map.Entry<String, CorrelationTemplateReference> templateReference
          : readTemplatesReferences(new File(repositoryFilePath)).entrySet()) {
        for (String templateVersion : templateReference.getValue().getVersions()) {
          String templateWithVersionName = templateReference.getKey() + "-" + templateVersion;

          String templateFileName = templateWithVersionName + TEMPLATE_FILE_SUFFIX;
          saveFileFromURL(baseURL + encodeSpecialCharacters(templateFileName),
              installationFolderPath + templateFileName);

          String snapshotFileName = templateWithVersionName + SNAPSHOT_FILE_SUFFIX;
          if (canDownload(baseURL + snapshotFileName)) {
            saveFileFromURL(baseURL + encodeSpecialCharacters(snapshotFileName),
                installationFolderPath + snapshotFileName);
          }
        }
      }
    } catch (JsonParseException e) {
      configuration.removeRepository(name);
      throw new IOException(
          "Content does not conform to JSON syntax. Please enter a URL that points to a "
              + "valid JSON.", e);
    }
  }

  private void saveFileFromURL(String fileURL, String fileFullPath) throws IOException {
    File templateFile = new File(fileFullPath);
    if (templateFile.exists() || templateFile.createNewFile()) {
      int connectionTimeout = CONNECT_TIMEOUT_MILLISECONDS * 10;
      FileUtils.copyURLToFile(new URL(fileURL), templateFile, connectionTimeout, connectionTimeout);
      LOG.info("Created the file {}", fileFullPath);
    }
  }

  private String encodeSpecialCharacters(String urlPart) throws UnsupportedEncodingException {
    /*
     * Implemented for backward compatibility with templates with IDs and versions with spaces
     * and '+'
     */
    return URLEncoder.encode(urlPart, StandardCharsets.UTF_8.toString()).replaceAll("[+ ]", "%20");
  }

  private String getBaseURL(String fullURL) {
    int index = fullURL.lastIndexOf('/');
    return fullURL.substring(0, index) + "/";
  }

  private boolean canDownload(String url) {
    try {
      URL siteURL = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
      connection.connect();

      return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    } catch (IOException e) {
      LOG.warn("There was an error trying to get the URL {}. ", url, e);
    }
    return false;
  }
}
