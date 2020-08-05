package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestPlanTemplatesRepositoryTest {

  protected static final String SIEBEL_CORRELATION_TEMPLATE = "siebel-1"
      + ".0-template.json";
  private static final String CORRELATION_RECORDER_TEMPLATE_FILE_PATH = "/template"
      + "-correlation-recorder.xml";
  private static final String CORRELATIONS_RECORDER_TEST_PLAN = "recording"
      + "-correlations.jmx";
  private static final String CORRELATION_RECORDER_TEMPLATE_DESCRIPTION_FILE = "test-plan-template"
      + "/correlation"
      + "-recorder-template-description.xml";
  private static final String SIEBEL_RECORDING_TEMPLATE_NAME = "Recording";
  private static final String TEST_PLAN_TEMPLATE_FOLDER = "/test-plan-template";
  private static final String PATH_SEPARATOR = "/";
  private static String TEMPLATES_LIST_PATH = "templates.xml";
  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  private TestPlanTemplatesRepository testPlanTemplatesRepository;

  @Before
  public void setup() {
    testPlanTemplatesRepository = new TestPlanTemplatesRepository(
        tempFolder.getRoot().getPath() + PATH_SEPARATOR);
  }

  @Test
  public void shouldAddATemplateAndATemplateDescriptionWhenNotExistTheTemplate()
      throws IOException {
    copyFile(TEST_PLAN_TEMPLATE_FOLDER + PATH_SEPARATOR + TEMPLATES_LIST_PATH,
        tempFolder.getRoot().getPath() + TEMPLATES_LIST_PATH);
    addSiebelTemplate(TEST_PLAN_TEMPLATE_FOLDER + PATH_SEPARATOR + CORRELATIONS_RECORDER_TEST_PLAN);
    assertionCorrelationRecorderOnTemplates();
  }

  private void copyFile(String sourcePath, String destPath) throws IOException {
    File dstFile = new File(destPath);
    try (FileWriter fileWriter = new FileWriter(dstFile)) {
      fileWriter.write(getFileFromResources(sourcePath));
    }
  }

  private String getFileFromResources(String fileName) throws IOException {
    InputStream inputStream = this.getClass().getResourceAsStream(fileName);
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
  }

  private void addSiebelTemplate(String templateResourcePath) {
    testPlanTemplatesRepository
        .addSiebelTemplate(CORRELATIONS_RECORDER_TEST_PLAN, templateResourcePath,
            CORRELATION_RECORDER_TEMPLATE_DESCRIPTION_FILE, SIEBEL_RECORDING_TEMPLATE_NAME);
  }

  private void assertionCorrelationRecorderOnTemplates() throws IOException {
    File resultTemplate = new File(
        tempFolder.getRoot().getPath() + PATH_SEPARATOR + CORRELATIONS_RECORDER_TEST_PLAN);
    File resultTemplatesList = new File(tempFolder.getRoot().getPath() + TEMPLATES_LIST_PATH);

    softly.assertThat(FileUtils.readFileToString(resultTemplate, "utf-8"))
        .isEqualToNormalizingWhitespace(
            getFileFromResources(TEST_PLAN_TEMPLATE_FOLDER + PATH_SEPARATOR
                + CORRELATIONS_RECORDER_TEST_PLAN));
    softly.assertThat(FileUtils.readFileToString(resultTemplatesList, "utf-8"))
        .isEqualToNormalizingWhitespace(getFileFromResources(TEST_PLAN_TEMPLATE_FOLDER +
            CORRELATION_RECORDER_TEMPLATE_FILE_PATH));
  }

  @Test
  public void shouldNotAddTemplateDescriptionWhenItWasAlreadyAdded() throws IOException {
    copyFile(TEST_PLAN_TEMPLATE_FOLDER + CORRELATION_RECORDER_TEMPLATE_FILE_PATH,
        tempFolder.getRoot().getPath() + TEMPLATES_LIST_PATH);
    addSiebelTemplate(TEST_PLAN_TEMPLATE_FOLDER + PATH_SEPARATOR + CORRELATIONS_RECORDER_TEST_PLAN);
    assertionCorrelationRecorderOnTemplates();
  }

  @Test
  public void shouldNotAddATemplateWhenItWasAlreadyAdded() throws IOException {
    copyFile(TEST_PLAN_TEMPLATE_FOLDER + CORRELATION_RECORDER_TEMPLATE_FILE_PATH,
        tempFolder.getRoot().getPath() + TEMPLATES_LIST_PATH);
    addSiebelTemplate(TEST_PLAN_TEMPLATE_FOLDER + PATH_SEPARATOR + CORRELATIONS_RECORDER_TEST_PLAN);

    File result = new File(tempFolder.getRoot().getPath() + CORRELATIONS_RECORDER_TEST_PLAN);
    long lastModifiedExpected = result.lastModified();
    addSiebelTemplate(CORRELATIONS_RECORDER_TEST_PLAN);

    result = new File(tempFolder.getRoot().getPath() + CORRELATIONS_RECORDER_TEST_PLAN);
    long lastModifiedResult = result.lastModified();

    softly.assertThat(lastModifiedResult).isEqualTo(lastModifiedExpected);
    assertionCorrelationRecorderOnTemplates();
  }

  @Test
  public void shouldAddSiebelCorrelationTemplateWhenStart() throws IOException {
    String correlationTemplatesRootFolder = tempFolder.getRoot().getPath()
        + LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER;
    testPlanTemplatesRepository.setRootFolder(correlationTemplatesRootFolder);

    testPlanTemplatesRepository.addSiebelCorrelationTemplate(SIEBEL_CORRELATION_TEMPLATE,
        PATH_SEPARATOR + SIEBEL_CORRELATION_TEMPLATE);

    File resultCorrelationTemplate = new File(tempFolder.getRoot().getPath()
        + LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER
        + SIEBEL_CORRELATION_TEMPLATE);

    softly.assertThat(FileUtils.readFileToString(resultCorrelationTemplate, "utf-8"))
        .isEqualToNormalizingWhitespace(
            getFileFromResources(PATH_SEPARATOR + SIEBEL_CORRELATION_TEMPLATE));

  }
}
