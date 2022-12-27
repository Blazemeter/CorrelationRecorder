package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.LOCAL_REPOSITORY_NAME;
import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.REPOSITORY_NAME_SUFFIX;
import static com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry.JSON_FILE_EXTENSION;
import static com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry.SNAPSHOT_FILE_TYPE;
import static com.blazemeter.jmeter.correlation.core.templates.LocalCorrelationTemplatesRegistry.TEMPLATE_FILE_SUFFIX;
import static org.apache.commons.io.FileUtils.copyFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalCorrelationTemplatesRepositoriesRegistry implements
    CorrelationTemplatesRepositoriesRegistry {

  public static final String REPOSITORY_FILE_SUFFIX = REPOSITORY_NAME_SUFFIX + JSON_FILE_EXTENSION;
  private static final Logger LOG = LoggerFactory
      .getLogger(LocalCorrelationTemplatesRepositoriesRegistry.class);
  private static final String SNAPSHOT_SUFFIX = "-snapshot";
  private static final String SNAPSHOT_FILE_EXTENSION = "." + SNAPSHOT_FILE_TYPE;
  public static final String SNAPSHOT_FILE_SUFFIX = SNAPSHOT_SUFFIX + SNAPSHOT_FILE_EXTENSION;

  protected LocalConfiguration configuration;

  public LocalCorrelationTemplatesRepositoriesRegistry(LocalConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void save(String name, String url) throws IOException {
    String path = url.replace("file://", "");
    if (Files.exists(Paths.get(path))) {
      String repositoryFolderName = name + "/";

      String installationFolderPath =
          configuration.getCorrelationsTemplateInstallationFolder() + repositoryFolderName;

      File repositoryFolder = new File(installationFolderPath);
      if (!repositoryFolder.exists() && repositoryFolder.mkdir()) {
        LOG.info("Created the folder for the repository {}", name);
        configuration.addRepository(name, url);
      }
      String repositoryFilePath = installationFolderPath + name + REPOSITORY_FILE_SUFFIX;

      copyFileFromPath(path, repositoryFilePath);

      String basePath = getBasePath(path);
      Map<String, CorrelationTemplateReference> templatesReferences = readTemplatesReferences(
          new File(installationFolderPath + name + REPOSITORY_FILE_SUFFIX));

      for (Map.Entry<String, CorrelationTemplateReference> entry : templatesReferences.entrySet()) {
        for (String version : entry.getValue().getVersions()) {
          String templateFileName = entry.getKey() + "-" + version;

          copyFileFromPath(basePath + templateFileName + TEMPLATE_FILE_SUFFIX,
              installationFolderPath + templateFileName + TEMPLATE_FILE_SUFFIX);

          File templateSnapshotFile = new File(basePath + templateFileName + SNAPSHOT_FILE_SUFFIX);
          if (templateSnapshotFile.exists()) {
            copyFileFromPath(basePath + templateFileName + SNAPSHOT_FILE_SUFFIX,
                templateFileName + SNAPSHOT_FILE_SUFFIX);
          }
        }
      }
    } else {
      throw new IOException(url + " file does not exists");
    }
  }

  private String getBasePath(String path) {
    int index = path.lastIndexOf('/');
    return path.substring(0, index) + "/";
  }

  private void copyFileFromPath(String source, String templateFileName) throws IOException {
    File localTemplateFile = new File(templateFileName);
    if (!localTemplateFile.exists() && localTemplateFile.createNewFile()) {
      copyFile(new File(source), new File(templateFileName));
      LOG.info("Created the file {}", localTemplateFile);
    }
  }

  @Override
  public List<CorrelationTemplatesRepository> getRepositories() {
    List<CorrelationTemplatesRepository> correlationRepositoryList = new ArrayList<>();
    List<String> repositoriesList = configuration.getRepositoriesNames();
    repositoriesList.forEach(r -> {
      File repositoryFile = new File(
          configuration.getCorrelationsTemplateInstallationFolder() + (
              r.equals(LOCAL_REPOSITORY_NAME)
                  ? ""
                  : r + "/") + r
              + REPOSITORY_FILE_SUFFIX);
      try {
        CorrelationTemplatesRepository loadedRepository =
            new CorrelationTemplatesRepository(
                repositoryFile.getName().replace(REPOSITORY_FILE_SUFFIX, ""),
                readTemplatesReferences(repositoryFile));

        correlationRepositoryList.add(loadedRepository);
      } catch (IOException e) {
        LOG.warn("There was an issue trying to read the file {}.", repositoryFile.getName(), e);
      }
    });

    return correlationRepositoryList;
  }

  public Map<String, CorrelationTemplateReference> readTemplatesReferences(File source)
      throws IOException {
    return configuration.readTemplatesReferences(source);
  }

  @Override
  public CorrelationTemplatesRepository find(String id) {
    Optional<String> foundRepository = configuration.getRepositoriesNames().stream()
        .filter(r -> r.equals(id))
        .findAny();

    if (!foundRepository.isPresent()) {
      return null;
    }

    String repositoryFolderPath = (id.equals(LOCAL_REPOSITORY_NAME) ? "" : id) + "/";

    try {
      File source = new File(
          configuration.getCorrelationsTemplateInstallationFolder() + repositoryFolderPath + id
              + REPOSITORY_FILE_SUFFIX);
      Map<String, CorrelationTemplateReference> templatesReferences = readTemplatesReferences(
          source);

      return new CorrelationTemplatesRepository(
          source.getName().replace(REPOSITORY_FILE_SUFFIX, ""),
          templatesReferences);
    } catch (IOException e) {
      LOG.warn("There was and issue trying to get the templates from the repository.", e);
    }

    return null;
  }

  @Override
  public void delete(String name) throws IOException {
    configuration.removeRepository(name);

    File repositoryFolder = new File(
        configuration.getCorrelationsTemplateInstallationFolder() + name);
    if (!repositoryFolder.exists()) {
      LOG.error("The folder for the repository {} didn't exists. Only removed from configuration",
          name);
      return;
    }

    if (repositoryFolder.isDirectory()) {
      LOG.info("Removing {}'s repository folder at {}.", name, repositoryFolder.getAbsolutePath());
      FileUtils.deleteDirectory(repositoryFolder);
    } else {
      LOG.warn(
          "The repository {} doesn't seems to have a folder, {} was found instead. Only removed " 
              + "from the configuration.",
          name, repositoryFolder.getName());
    }
  }

  public List<TemplateVersion> getCorrelationTemplatesByRepositoryId(String id) {
    List<File> templates = Stream.of(Objects.requireNonNull((new File(
        configuration.getCorrelationsTemplateInstallationFolder() + (
            id.equals(LOCAL_REPOSITORY_NAME) ? "" : id)))
        .listFiles()))
        .filter(f -> f.getName().endsWith(TEMPLATE_FILE_SUFFIX))
        .collect(Collectors.toList());

    List<TemplateVersion> relatedTemplates = new ArrayList<>();
    templates.forEach(t -> {
      try {
        TemplateVersion template = configuration.readValue(t, TemplateVersion.class);

        template.setRepositoryId(id);
        template
            .setInstalled(configuration.isInstalled(id, template.getId(), template.getVersion()));

        relatedTemplates.add(template);
      } catch (IOException e) {
        LOG.warn("There was an issue trying to get the Template from {}.", t, e);
      }
    });

    return relatedTemplates;
  }

  public boolean isLocalTemplateVersionSaved(String templateId, String templateVersion) {
    return Stream.of(Objects.requireNonNull((new File(
        configuration.getCorrelationsTemplateInstallationFolder()))
        .listFiles()))
        .anyMatch(f -> f.getName().toLowerCase()
            .startsWith(templateId.toLowerCase() + "-" + templateVersion.toLowerCase()) &&
            f.getName().endsWith(TEMPLATE_FILE_SUFFIX));
  }

  public void installTemplate(String repositoryName, String templateId, String templateVersion)
      throws ConfigurationException {
    manageTemplate(LocalConfiguration.INSTALL, repositoryName, templateId, templateVersion);
  }

  private void manageTemplate(String action, String repositoryName, String templateId,
      String templateVersion) throws ConfigurationException {

    String repositoryFolderPath =
        repositoryName.equals(LOCAL_REPOSITORY_NAME) ? "" : repositoryName + "/";

    File template = new File(
        configuration.getCorrelationsTemplateInstallationFolder() + repositoryFolderPath
            + templateId
            + "-" + templateVersion + TEMPLATE_FILE_SUFFIX);

    if (!template.exists()) {
      LOG.error("The template {} doesn't exists", template.getName());
      throw new ConfigurationException(
          "The template " + template.getAbsolutePath() + " doesn't exists");
    }

    configuration.manageTemplate(action, repositoryName, templateId, templateVersion);
  }

  public void uninstallTemplate(String repositoryName, String templateId, String templateVersion)
      throws ConfigurationException {
    manageTemplate(LocalConfiguration.UNINSTALL, repositoryName, templateId, templateVersion);
  }

  String getRepositoryURL(String name) {
    return configuration.getRepositoryURL(name);
  }

  public void updateLocalRepository(String templateId, String templateVersion) {
    File localRepositoryFile = new File(
        configuration.getCorrelationsTemplateInstallationFolder() + LOCAL_REPOSITORY_NAME
            + REPOSITORY_FILE_SUFFIX);
    try {
      CorrelationTemplatesRepository localRepository;

      if (!localRepositoryFile.exists()) {
        localRepositoryFile.createNewFile();
        localRepository = new CorrelationTemplatesRepository();
        localRepository.setTemplates(new HashMap<String, CorrelationTemplateReference>() {
        });
        configuration.writeValue(localRepositoryFile, localRepository.getTemplates());
        LOG.info("No local repository file found. Created a new one instead");
      } else {
        localRepository = new CorrelationTemplatesRepository("local",
            readTemplatesReferences(localRepositoryFile));
      }

      localRepository.addTemplate(templateId, templateVersion);
      configuration.writeValue(localRepositoryFile, localRepository.getTemplates());
      configuration.addRepository(LOCAL_REPOSITORY_NAME, localRepositoryFile.getAbsolutePath());
    } catch (IOException e) {
      LOG.warn("There was a problem trying to update the local repository file.", e);
    }
  }
}
