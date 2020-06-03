package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.CENTRAL_REPOSITORY_ID;
import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.CENTRAL_REPOSITORY_URL;
import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.LOCAL_REPOSITORY_NAME;
import static com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry.TEMPLATE_FILE_SUFFIX;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteCorrelationTemplatesRepositoriesRegistry extends
    LocalCorrelationTemplatesRepositoriesRegistry implements
    CorrelationTemplatesRepositoriesRegistry {

  private static final Logger LOG = LoggerFactory
      .getLogger(RemoteCorrelationTemplatesRepositoriesRegistry.class);

  private static final int CONNECT_TIMEOUT = 3000;

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

    saveFileFromURL(url, repositoryFilePath);
    configuration.addRepository(name, url);

    String baseURL = getBaseURL(url);
    for (Map.Entry<String, CorrelationTemplateReference> templateReference :
        readTemplatesReferences(
        new File(repositoryFilePath)).entrySet()) {
      for (String templateVersion : templateReference.getValue().getVersions()) {
        String templateWithVersionName = templateReference.getKey() + "-" + templateVersion;

        String templateFileName = templateWithVersionName + TEMPLATE_FILE_SUFFIX;
        saveFileFromURL(baseURL + templateFileName, installationFolderPath + templateFileName);

        String snapshotFileName = templateWithVersionName + SNAPSHOT_FILE_SUFFIX;
        if (canDownload(baseURL + snapshotFileName)) {
          saveFileFromURL(baseURL + snapshotFileName, installationFolderPath + snapshotFileName);
        }
      }
    }
  }

  private String getBaseURL(String fullURL) {
    int index = fullURL.lastIndexOf('/');
    return fullURL.substring(0, index) + "/";
  }

  private static void saveFileFromURL(String fileURL, String fileFullPath) throws IOException {
    File templateFile = new File(fileFullPath);
    if (templateFile.exists() || templateFile.createNewFile()) {
      /*
       * If the URL contains spaces, we need to fill them with %20
       * Some IDs might contain spaces, and the generated URL to
       * download them might contain spaces as well.
       * */
      String parsedURL = fileURL.replace(" ", "%20");
      FileUtils.copyURLToFile(new URL(parsedURL), templateFile,
          Math.multiplyExact(CONNECT_TIMEOUT, 10),
          Math.multiplyExact(CONNECT_TIMEOUT, 10));
      LOG.info("Created the file {}", fileFullPath);
    }
  }

  private boolean canDownload(String url) {
    try {
      URL siteURL = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(CONNECT_TIMEOUT);
      connection.connect();

      return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    } catch (IOException e) {
      LOG.warn("There was an error trying to get the URL {}. ", url, e);
    }
    return false;
  }
}
