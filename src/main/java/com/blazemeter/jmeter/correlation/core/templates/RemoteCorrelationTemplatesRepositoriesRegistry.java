package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry.PROPERTIES_FILE_SUFFIX;
import static com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry.TEMPLATE_FILE_SUFFIX;

import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import com.fasterxml.jackson.core.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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
  }

  @Override
  public void save(String name, String url) throws IOException {
    String installationFolderPath = RepositoryUtils.createRepositoryFolder(configuration, name);

    String repositoryFilePath =
        Paths.get(installationFolderPath, RepositoryUtils.getRepositoryFileName(name))
            .toAbsolutePath()
            .toString();

    try {
      saveFileFromURL(url, repositoryFilePath);
      configuration.addRepository(name, url);

      String baseURL = getBaseURL(url);

      for (Map.Entry<String, CorrelationTemplateVersions> templateReference
          : readTemplatesVersions(new File(repositoryFilePath)).entrySet()) {
        for (String templateVersion : templateReference.getValue().getVersions()) {
          String templateWithVersionName = templateReference.getKey() + "-" + templateVersion;

          String templateFileName = templateWithVersionName + TEMPLATE_FILE_SUFFIX;
          saveFileFromURL(baseURL + encodeSpecialCharacters(templateFileName),
              Paths.get(installationFolderPath, templateFileName).toAbsolutePath().toString());

          String snapshotFileName = templateWithVersionName + SNAPSHOT_FILE_SUFFIX;
          if (canDownload(baseURL + snapshotFileName)) {
            saveFileFromURL(baseURL + encodeSpecialCharacters(snapshotFileName),
                Paths.get(installationFolderPath, snapshotFileName).toAbsolutePath().toString());
          }

          String propertiesFileName = templateWithVersionName + PROPERTIES_FILE_SUFFIX;
          File propertiesFile = Paths.get(installationFolderPath, propertiesFileName).toFile();
          configuration.writeValue(propertiesFile, new TemplateProperties());
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
