package com.blazemeter.jmeter.correlation.gui;

import com.helger.commons.annotation.VisibleForTesting;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPlanTemplatesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(TestPlanTemplatesRepository.class);
  private static final String DEPRECATED_TEMPLATE_NAME = "correlation-recorder.jmx";
  private String rootFolder;

  public TestPlanTemplatesRepository(String rootFolder) {
    try {
      Files.createDirectories(Paths.get(rootFolder));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.rootFolder = rootFolder;
  }

  @VisibleForTesting
  public void setRootFolder(String rootFolder) {
    this.rootFolder = rootFolder;
    try {
      Files.createDirectories(Paths.get(rootFolder));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void addCorrelationRecorderTemplate(String templateFileName, String templatesFolderPath,
                                             String descriptionFileName, String templateName) {
    copyTemplateFile(templateFileName, templatesFolderPath);
    addTemplateDescription(descriptionFileName, templateName);
    addFailExtractorAssertion(templateFileName);
  }

  private void copyTemplateFile(String fileName, String sourcePath) {
    try {
      removeDeprecatedTemplate(DEPRECATED_TEMPLATE_NAME);
      File dest = new File(Paths.get(rootFolder, fileName).toAbsolutePath().toString());
      String fileFromResources = getFileFromResources(sourcePath + fileName);
      if (!dest.exists() || !DigestUtils
          .md5Hex(new FileInputStream(dest.getPath()))
          .equals(DigestUtils.md5Hex(fileFromResources))) {
        try (FileWriter fileWriter = new FileWriter(dest)) {
          fileWriter.write(fileFromResources);
        }
      }
    } catch (IOException e) {
      LOG.error("Problem creating Correlation Recording template {}", fileName, e);
    }
  }

  private void removeDeprecatedTemplate(
      @SuppressWarnings("SameParameterValue") String templateName) {
    File oldTemplate =
        new File(Paths.get(rootFolder, templateName).toAbsolutePath().toString());
    if (!oldTemplate.exists()) {
      return;
    }
    LOG.info("[ACR] Removing old template: {}", oldTemplate.getAbsolutePath());
    if (!oldTemplate.delete()) {
      LOG.error("[ACR] Failed to remove old template: {}", oldTemplate.getAbsolutePath());
      return;
    }
    LOG.info("[ACR] Successfully removed old template: {}", oldTemplate.getAbsolutePath());
  }

  private String getFileFromResources(String fileName) throws IOException {
    InputStream inputStream = this.getClass().getResourceAsStream(fileName);
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
  }

  private void addTemplateDescription(String descTemplateName, String templateName) {
    try {
      String filePath = Paths.get(rootFolder, "templates.xml").toAbsolutePath().toString();
      removeDeprecatedTemplatesDescription(filePath);
      if (!checkIfStringExists(filePath, "<name>" + templateName + "</name>")) {
        Path path = Paths.get(filePath);
        List<String> replacedLines = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
          if (line.contains("</templates>")) {
            replacedLines.add(getFileFromResources(descTemplateName) + System.lineSeparator());
          }
          replacedLines.add(line);
        }

        if (!replacedLines.isEmpty()) {
          Files.write(path, replacedLines);
        }
      }

    } catch (IOException e) {
      LOG.warn("Problem adding {} template description {}", templateName, descTemplateName, e);
    }
  }

  private void removeDeprecatedTemplatesDescription(String filePath) {

    String content;
    try {
      content = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.error("Error trying to read the file {}", filePath);
      return;
    }
    List<Pattern> patterns =
        Stream.of("Correlation Recorder", "bzm - Correlation Recorder")
            .map(name -> Pattern.compile(
                "(<template isTestPlan=\"true\">[\\n\\s]*<name>" + name +
                    "</name>.*?</template>)", Pattern.DOTALL))
            .collect(Collectors.toList());
    boolean updated = false;
    for (Pattern pattern : patterns) {
      Matcher matcher = pattern.matcher(content);
      while (matcher.find()) {
        content = content.replace(matcher.group(), "");
        updated = true;
      }
    }
    if (updated) {
      try (FileWriter fileWriter = new FileWriter(filePath)) {
        fileWriter.write(content);
      } catch (IOException e) {
        LOG.error("Error trying to write the file {}", filePath);
      }
    } else {
      LOG.debug("Old Correlation Template not found.");
    }
  }

  private boolean checkIfStringExists(String filePath, String condition) throws IOException {
    return Files.lines(Paths.get(filePath)).anyMatch(line -> line.contains(condition));
  }

  public void addCorrelationTemplate(String templateName, String pathTemplateResource) {
    createCorrelationTemplatesFolder();
    copyTemplateFile(templateName, pathTemplateResource);
  }

  private void createCorrelationTemplatesFolder() {
    File folderContainer = new File(rootFolder);
    if (!folderContainer.exists() && folderContainer.mkdir()) {
      LOG.info("The container folder was created at {}", rootFolder);
    }
  }

  private void addFailExtractorAssertion(String templateName) {
    try {
      String filePath = rootFolder + templateName;
      String name = "Regex Extraction Assertion";
      if (!checkIfStringExists(filePath, name)) {
        List<String> replacedLines = new ArrayList<>();
        Path path = Paths.get(filePath);
        for (String line : Files.readAllLines(path)) {
          if (line.contains("testname=\"Correlation Recorder\"")) {
            String containingFolder = "/templates/components/";
            replacedLines.add(String.format(
                getFileFromResources(containingFolder + "JSR223AssertionTemplate.xml"), name,
                getFileFromResources(containingFolder + "ExtractingVariableAssertion.xml"))
                + System.lineSeparator());
          }
          replacedLines.add(line);
        }
        Files.write(path, replacedLines);
      }
    } catch (IOException e) {
      LOG.warn("Problem adding Custom components to Correlation Recorder template {}", templateName,
          e);
    }
  }
}
