package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.LOCAL_REPOSITORY_NAME;

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
  public static final String JSON_FILE_EXTENSION = ".json";
  private static final Logger LOG = LoggerFactory
      .getLogger(LocalCorrelationTemplatesRegistry.class);
  private static final String TEMPLATE_SUFFIX = "-template";
  public static final String TEMPLATE_FILE_SUFFIX = TEMPLATE_SUFFIX + JSON_FILE_EXTENSION;

  private final LocalConfiguration localConfiguration;

  public LocalCorrelationTemplatesRegistry(LocalConfiguration localConfiguration) {
    this.localConfiguration = localConfiguration;
  }

  @Override
  public void save(CorrelationTemplate correlationTemplate) throws IOException {
    saveTemplate(correlationTemplate);
    saveSnapshot(correlationTemplate);
  }

  private void saveTemplate(CorrelationTemplate template) throws IOException {
    localConfiguration
        .writeValue(new File(localConfiguration.getCorrelationsTemplateInstallationFolder()
            + (
            template.getRepositoryId().equals(LOCAL_REPOSITORY_NAME) ? "" : template
                .getRepositoryId() + "/") + template.getId() + "-" + template.getVersion()
            + TEMPLATE_FILE_SUFFIX), template);
  }

  private void saveSnapshot(CorrelationTemplate correlationTemplate) throws IOException {
    File snapshotFile = getSnapshotFile(correlationTemplate);

    if (correlationTemplate.getSnapshot() != null) {
      localConfiguration.writeValue(snapshotFile, correlationTemplate);
      ImageIO.write(correlationTemplate.getSnapshot(), SNAPSHOT_FILE_TYPE,
          snapshotFile);
    }
  }

  private File getSnapshotFile(CorrelationTemplate correlationTemplate) {
    return new File(localConfiguration.getCorrelationsTemplateInstallationFolder()
        + (
        correlationTemplate.getRepositoryId().equals(LOCAL_REPOSITORY_NAME) ? ""
            : correlationTemplate.getRepositoryId() + "/"),
        correlationTemplate.getSnapshotName());
  }

  @Override
  public Optional<CorrelationTemplate> findByID(String repositoryOwner, String id,
      String templateVersion)
      throws IOException {
    CorrelationTemplate correlationTemplate = localConfiguration.readValue(
        new File(localConfiguration.getCorrelationsTemplateInstallationFolder()
            + (
            repositoryOwner.equals(LOCAL_REPOSITORY_NAME) ? "" : repositoryOwner + "/") + id
            + "-" + templateVersion + TEMPLATE_FILE_SUFFIX),
        CorrelationTemplate.class);
    correlationTemplate.setSnapshot(getSnapshot(correlationTemplate));
    return Optional.of(correlationTemplate);
  }

  private BufferedImage getSnapshot(CorrelationTemplate loadedTemplate) {
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

  @Override
  public List<CorrelationTemplate> getInstalledTemplates() {
    ArrayList<CorrelationTemplate> loadedTemplates = new ArrayList<>();
    localConfiguration.getRepositoriesWithInstalledTemplates()
        .forEach(r -> r.getInstalledTemplates().forEach((template, version) -> {
          String installedTemplateName =
              localConfiguration.getCorrelationsTemplateInstallationFolder()
                  + (
                  r.getName().equals(LOCAL_REPOSITORY_NAME) ? "" : r.getName() + "/") + template
                  + "-"
                  + version + TEMPLATE_FILE_SUFFIX;

          try {
            CorrelationTemplate loadedTemplate = localConfiguration
                .readValue(new File(installedTemplateName), CorrelationTemplate.class);
            loadedTemplate.setRepositoryId(r.getName());
            loadedTemplate.setSnapshot(getSnapshot(loadedTemplate));
            loadedTemplates.add(loadedTemplate);
          } catch (IOException e) {
            LOG.warn("There was an error trying to get the CorrelationTemplate from the file {}",
                installedTemplateName, e);
          }
        }));

    return loadedTemplates;
  }
}
