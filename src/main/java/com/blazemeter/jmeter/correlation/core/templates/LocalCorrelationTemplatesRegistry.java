package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.JSON_FILE_EXTENSION;
import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.LOCAL_REPOSITORY_NAME;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalCorrelationTemplatesRegistry implements CorrelationTemplatesRegistry {

  public static final String SNAPSHOT_FILE_TYPE = "png";
  private static final Logger LOG = LoggerFactory
      .getLogger(LocalCorrelationTemplatesRegistry.class);
  private static final String TEMPLATE_SUFFIX = "-template";
  private static final String PROPERTIES_SUFFIX = "-properties";
  public static final String TEMPLATE_FILE_SUFFIX = TEMPLATE_SUFFIX + JSON_FILE_EXTENSION;
  public static final String PROPERTIES_FILE_SUFFIX = PROPERTIES_SUFFIX + JSON_FILE_EXTENSION;

  private final LocalConfiguration localConfiguration;

  public LocalCorrelationTemplatesRegistry(LocalConfiguration localConfiguration) {
    this.localConfiguration = localConfiguration;
  }

  @Override
  public void save(Template template) throws IOException {
    saveTemplate(template);
    saveSnapshot(template);
  }

  private String getFileForTemplate(String repository, String templateId, String version) {
    return localConfiguration.getCorrelationsTemplateInstallationFolder()
        + getRepositoryFolderName(repository) + templateId + "-"
        + version
        + TEMPLATE_FILE_SUFFIX;
  }

  private void saveTemplate(Template template) throws IOException {
    localConfiguration
        .writeValue(new File(getFileForTemplate(template.getRepositoryId(), template.getId(),
            template.getVersion())), template);
  }

  private void saveSnapshot(Template template) throws IOException {
    File snapshotFile = getSnapshotFile(template);

    if (template.getSnapshot() != null) {
      localConfiguration.writeValue(snapshotFile, template);
      ImageIO.write(template.getSnapshot(), SNAPSHOT_FILE_TYPE,
          snapshotFile);
    }
  }

  private File getSnapshotFile(Template template) {
    return new File(localConfiguration.getCorrelationsTemplateInstallationFolder()
        + getRepositoryFolderName(template.getRepositoryId()),
        template.getSnapshotName());
  }

  @Override
  public Optional<Template> findByID(String repositoryOwner, String id,
                                     String templateVersion)
      throws IOException {
    Template correlationTemplate = localConfiguration.readValue(
        new File(getFileForTemplate(repositoryOwner, id, templateVersion)),
        Template.class);
    correlationTemplate.setSnapshot(getSnapshot(correlationTemplate));
    return Optional.of(correlationTemplate);
  }

  private BufferedImage getSnapshot(Template loadedTemplate) {
    try {
      File snapshotFile = getSnapshotFile(loadedTemplate);
      if (!snapshotFile.exists()) {
        loadedTemplate.setSnapshotPath("");
        LOG.warn("Couldn't find a snapshot file for the CorrelationTemplate {}.",
            loadedTemplate.getId());
        return null;
      }

      loadedTemplate.setSnapshotPath(snapshotFile.getAbsolutePath());
      return ImageIO.read(snapshotFile);
    } catch (IOException e) {
      LOG.warn("There was an issue trying to retrieve the image for the template '{}'",
          loadedTemplate.getId(), e);
      return null;
    }
  }

  private String getRepositoryFolderName(String name) {
    return name.equals(LOCAL_REPOSITORY_NAME) ? "" : name + File.separator;
  }

  @Override
  public List<Template> getInstalledTemplates() {
    ArrayList<Template> loadedTemplates = new ArrayList<>();
    localConfiguration.getRepositoriesWithInstalledTemplates()
        .forEach(r -> r.getInstalledTemplates().forEach((template, version) -> {
          String installedTemplateFile =
              getFileForTemplate(r.getName(), template, version);

          try {
            Template loadedTemplate = localConfiguration
                .readValue(new File(installedTemplateFile), Template.class);
            loadedTemplate.setRepositoryId(r.getName());
            loadedTemplate.setSnapshot(getSnapshot(loadedTemplate));
            loadedTemplates.add(loadedTemplate);
          } catch (IOException e) {
            LOG.warn("There was an error trying to get the CorrelationTemplate from the file {}",
                installedTemplateFile, e);
          }
        }));

    return loadedTemplates;
  }
}
