package com.blazemeter.jmeter.correlation.core.templates.repository;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.JSON_FILE_EXTENSION;
import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.REPOSITORY_NAME_SUFFIX;

import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryUtils {

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryUtils.class);
  private static final String REPOSITORY_FILE_SUFFIX = REPOSITORY_NAME_SUFFIX + JSON_FILE_EXTENSION;

  public static String getRepositoryFileName(String repositoryName) {
    return repositoryName + REPOSITORY_FILE_SUFFIX;
  }

  public static String removeRepositoryNameFromFile(String fileName) {
    return fileName.replace(REPOSITORY_FILE_SUFFIX, "");
  }

  public static boolean isURL(String text) {
    return text.toLowerCase().contains("http") || text.toLowerCase().contains("ftp");
  }

  public static boolean checkTemplateVersionProperty(
      Map<TemplateVersion, Map<String, String>> properties,
      TemplateVersion templateVersion, String key,
      String expectedValue) {
    if (properties == null || properties.size() == 0) {
      return false;
    }
    if (properties.containsKey(templateVersion) &&
        properties.get(templateVersion).containsKey(key)) {
      return properties.get(templateVersion).get(key).equals(expectedValue);
    } else {
      return false;
    }
  }

  public static String createRepositoryFolder(LocalConfiguration configuration, String name) {
    File repositoryFolderFile = getRepositoryFolderFile(configuration, name);

    if (!repositoryFolderFile.exists() && repositoryFolderFile.mkdir()) {
      LOG.info("Folder created for the repository {}", name);
    }
    return repositoryFolderFile.getAbsolutePath();
  }

  public static File getRepositoryFolderFile(LocalConfiguration configuration, String name) {
    return new File(Paths
        .get(configuration.getCorrelationsTemplateInstallationFolder(), name)
        .toAbsolutePath()
        .toString());
  }

  public static String getTemplateInfo(Template template) {
    return template.getId() + " v" + template.getVersion()
        + " (" + template.getRepositoryId() + ")";
  }
}
