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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPlanTemplatesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(TestPlanTemplatesRepository.class);
  private String rootFolder;

  public TestPlanTemplatesRepository(String rootFolder) {
    this.rootFolder = rootFolder;
  }

  @VisibleForTesting
  public void setRootFolder(String rootFolder) {
    this.rootFolder = rootFolder;
  }

  public void addCorrelationRecorderTemplate(String templateFileName, String templatesFolderPath,
      String descriptionFileName, String templateName) {
    copyTemplateFile(templateFileName, templatesFolderPath);
    addTemplateDescription(descriptionFileName, templateName);
    addFailExtractorAssertion(templateFileName);
  }

  private void copyTemplateFile(String fileName, String sourcePath) {
    try {
      File dest = new File(rootFolder + fileName);
      String fileFromResources = getFileFromResources(sourcePath + fileName);
      if (!dest.exists() || !DigestUtils
          .md5Hex(new FileInputStream(dest.getPath()))
          .equals(DigestUtils.md5Hex(fileFromResources))) {
        try (FileWriter fileWriter = new FileWriter(dest)) {
          fileWriter.write(fileFromResources);
        }
      }
    } catch (IOException e) {
      LOG.warn("Problem creating Correlation Recording template {}", fileName, e);
    }
  }

  private String getFileFromResources(String fileName) throws IOException {
    InputStream inputStream = this.getClass().getResourceAsStream(fileName);
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
  }

  private void addTemplateDescription(String descTemplateName, String templateName) {
    try {
      String filePath = rootFolder + "templates.xml";
      removeOldTemplate(filePath);
      if (!checkIfStringExists(filePath, "<name>" + templateName + "</name>")) {
        Path path = Paths.get(filePath);
        List<String> replacedLines = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
          if (line.contains("</templates>")) {
            line = getFileFromResources(descTemplateName) + System.lineSeparator() + line;
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

  private void removeOldTemplate(String filePath) {

    String content;
    try {
      content = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.error("Error trying to read the file {}", filePath);
      return;
    }
    
    Matcher matcher = Pattern.compile(
        "(<template isTestPlan=\"true\">[\\n ]*<name>Correlation Recorder</name>.*</template>)",
        Pattern.DOTALL).matcher(content);
    if (!matcher.find()) {
      LOG.debug("Old Correlation Template not found.");
      return;
    }

    try (FileWriter fileWriter = new FileWriter(filePath)) {
      fileWriter.write(content.replace(matcher.group(), ""));
    } catch (IOException e) {
      LOG.error("Error trying to write the file {}", filePath);
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
            line = String.format(
                getFileFromResources(containingFolder + "JSR223AssertionTemplate.xml"), name,
                getFileFromResources(containingFolder + "ExtractingVariableAssertion.xml"))
                + System.lineSeparator() + line;
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
