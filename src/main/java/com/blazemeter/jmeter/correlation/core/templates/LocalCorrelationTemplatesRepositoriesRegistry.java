package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils.removeRepositoryNameFromFile;
import static org.apache.commons.io.FileUtils.copyFile;

import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

  private static final Logger LOG = LoggerFactory
      .getLogger(LocalCorrelationTemplatesRepositoriesRegistry.class);

  protected LocalConfiguration configuration;

  public LocalCorrelationTemplatesRepositoriesRegistry(LocalConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void save(String name, String url) throws IOException {
    String path = url.replace("file://", "");
    if (Files.exists(Paths.get(path))) {
      File repositoryFolder = new File(configuration.getRepositoryFolderPath(name));
      if (!repositoryFolder.exists() && repositoryFolder.mkdir()) {
        LOG.info("Created the folder for the repository {}", name);
        configuration.addRepository(name, url);
      }
      copyFileFromPath(path, configuration.getRepositoryFilePath(name));

      String basePath = getBasePath(path);
      Map<String, CorrelationTemplateVersions> templatesReferences =
          configuration.readTemplatesVersions(configuration.getRepositoryFile(name));

      for (Map.Entry<String, CorrelationTemplateVersions> entry : templatesReferences.entrySet()) {
        for (String version : entry.getValue().getVersions()) {
          String templateFilename = configuration.getTemplateFilename(entry.getKey(), version);
          String snapshotFilename = configuration.getTemplateSnapshotFilename(entry.getKey(),
              version);

          copyFileFromPath(basePath + templateFilename,
              Paths.get(configuration.getRepositoryFolderPath(name), templateFilename).toString());

          File templateSnapshotFile = new File(basePath + snapshotFilename);
          if (templateSnapshotFile.exists()) {
            copyFileFromPath(basePath + snapshotFilename, snapshotFilename);
          }
        }
      }
    } else {
      throw new IOException(url + " file does not exists");
    }
  }

  private String getBasePath(String path) {
    return Paths.get(path).getParent().toAbsolutePath() + File.separator;
  }

  private void copyFileFromPath(String source, String templateFileName) throws IOException {
    File localTemplateFile = new File(templateFileName);
    if (!localTemplateFile.exists() && localTemplateFile.createNewFile()) {
      copyFile(new File(source), new File(templateFileName));
      LOG.info("Created the file {}", localTemplateFile);
    }
  }

  @Override
  public CorrelationTemplatesRepository find(String id) {
    Optional<String> foundRepository = configuration.getRepositoriesNames().stream()
        .filter(r -> r.equals(id))
        .findAny();

    if (!foundRepository.isPresent()) {
      return null;
    }

    try {
      File source = configuration.getRepositoryFile(id);
      Map<String, CorrelationTemplateVersions> templatesReferences =
          configuration.readTemplatesVersions(source);

      return new CorrelationTemplatesRepository(
          removeRepositoryNameFromFile(source.getName()),
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

  @Override
  public Map<Template, TemplateProperties> getCorrelationTemplatesAndPropertiesByRepositoryId(
      String id) {
    List<File> templates = getTemplatesFilesByRepositoryId(id);
    Map<Template, TemplateProperties> relatedTemplates = new HashMap<>();
    templates.forEach(templateFile -> {
      try {
        Template template = loadTemplateFromFile(id, templateFile);
        relatedTemplates.put(template, loadTemplatePropertiesFromFile(templateFile));
      } catch (IOException e) {
        LOG.warn("There was an issue trying to get the Template from {}.", templateFile, e);
      }
    });

    return relatedTemplates;
  }

  @Override
  public Map<Template, TemplateProperties> getCorrelationTemplatesAndPropertiesByRepositoryId(
      String id, List<TemplateVersion> filter) {
    List<File> templatesFiles = getTemplatesFilesByRepositoryId(id);

    Map<Template, TemplateProperties> relatedTemplates = new HashMap<>();
    templatesFiles.forEach(file -> {
      try {
        Template template = loadTemplateFromFile(id, file);
        filter.forEach(f -> {
          if (template.getId().equals(f.getName())
              && template.getVersion().equals(f.getVersion())) {
            try {
              relatedTemplates.put(template, loadTemplatePropertiesFromFile(file));
            } catch (IOException e) {
              LOG.warn("There was an issue trying to get the Template from {}.", file, e);
            }
          }
        });
      } catch (IOException e) {
        LOG.warn("There was an issue trying to get the Template from {}.", file, e);
      }
    });
    return relatedTemplates;
  }

  @Override
  public void upload(String name, Template template) throws IOException {
    // Do nothing
  }

  private Template loadTemplateFromFile(String repositoryId, File templateFile) throws IOException {
    Template template = configuration.readTemplateFromPath(templateFile.getAbsolutePath());
    template.setRepositoryId(repositoryId);
    template.setInstalled(
        configuration.isInstalled(repositoryId, template.getId(), template.getVersion()));
    return template;
  }

  private List<File> getTemplatesFilesByRepositoryId(String id) {
    File repositoryFolder = configuration.getRepositoryFolder(id);
    return Stream.of(Objects.requireNonNull((repositoryFolder).listFiles()))
        .filter(LocalCorrelationTemplatesRepositoriesRegistry::isJsonFile)
        .collect(Collectors.toList());
  }

  private static boolean isJsonFile(File f) {
    return f.getName().endsWith(RepositoryGeneralConst.TEMPLATE_FILE_SUFFIX);
  }

  private TemplateProperties loadTemplatePropertiesFromFile(File templateFile) throws IOException {
    String propertiesFilepath = configuration.getTemplatePropertiesFromFilepath(templateFile);
    if (Files.exists(Paths.get(propertiesFilepath))) {
      return configuration.readTemplatePropertiesFromPath(propertiesFilepath);
    }
    return new TemplateProperties();
  }

  @Override
  public Map<String, CorrelationTemplateVersions> getCorrelationTemplateVersionsByRepositoryId(
      String name) {
    File repositoryFile = configuration.getRepositoryFile(name);
    try {
      return configuration.readTemplatesVersions(repositoryFile);
    } catch (IOException e) {
      LOG.error("There was an issue trying to read the file {}.", repositoryFile.getName(), e);
    }
    return null;
  }

  public boolean isLocalTemplateVersionSaved(String templateId, String templateVersion) {
    return Stream.of(Objects.requireNonNull((new File(
            configuration.getCorrelationsTemplateInstallationFolder()))
            .listFiles()))
        .anyMatch(f -> f.getName().toLowerCase()
            .startsWith(templateId.toLowerCase() + "-" + templateVersion.toLowerCase())
            && isJsonFile(f));
  }

  public String getRepositoryURL(String name) {
    return configuration.getRepositoryURL(name);
  }
  
}
