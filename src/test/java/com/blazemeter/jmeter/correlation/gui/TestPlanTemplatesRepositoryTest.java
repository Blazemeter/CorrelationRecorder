package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestPlanTemplatesRepositoryTest {

  private static final String CORRELATION_RECORDER_TEMPLATE_FILE_PATH =
      "template-correlation-recorder.xml";
  private static final String CORRELATION_RECORDER_TEST_PLAN = "recording-correlations.jmx";
  private static final String CORRELATION_RECORDER_TEMPLATE_NAME = "Recording";
  private static final String TEST_PLAN_TEMPLATE_FOLDER = "/test-plan-template/";
  private static final String TEMPLATES_LIST_PATH = "templates.xml";
  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  public TestPlanTemplatesRepository repository;

  @Before
  public void setup() throws IOException {
    repository = new TestPlanTemplatesRepository(
        tempFolder.getRoot().getPath() + File.separator);
    String dstFolder = Paths.get(
        tempFolder.getRoot().getPath(), TEST_PLAN_TEMPLATE_FOLDER
    ).toAbsolutePath().toString();
    (new File(dstFolder)).mkdirs();
    copyFile(TEST_PLAN_TEMPLATE_FOLDER + TEMPLATES_LIST_PATH,
        Paths.get(
            tempFolder.getRoot().getPath(), TEMPLATES_LIST_PATH
        ).toAbsolutePath().toString());
  }

  private void copyFile(String sourcePath, String destPath) throws IOException {
    try (FileWriter fileWriter = new FileWriter(new File(destPath))) {
      fileWriter.write(TestUtils.getFileContent(sourcePath, this.getClass()));
    }
  }

  @Test
  public void shouldAddATemplateAndATemplateDescriptionWhenNotExistTheTemplate()
      throws IOException {
    addTemplate(CORRELATION_RECORDER_TEST_PLAN);
    assertionCorrelationRecorderOnTemplates();
  }

  private void addTemplate(String templateTestPlan) {
    repository.addCorrelationRecorderTemplate(templateTestPlan, TEST_PLAN_TEMPLATE_FOLDER,
        Paths.get(
            TEST_PLAN_TEMPLATE_FOLDER, "correlation-recorder-template-description.xml"
        ).toAbsolutePath().toString(),
        CORRELATION_RECORDER_TEMPLATE_NAME);
  }

  private void assertionCorrelationRecorderOnTemplates() throws IOException {
    File resultTemplate = new File(
        Paths.get(
            tempFolder.getRoot().getPath(), CORRELATION_RECORDER_TEST_PLAN
        ).toAbsolutePath().toString()
    );
    File resultTemplatesList = new File(
        Paths.get(
            tempFolder.getRoot().getPath(), TEMPLATES_LIST_PATH
        ).toAbsolutePath().toString()
    );

    String expected = TEST_PLAN_TEMPLATE_FOLDER + CORRELATION_RECORDER_TEST_PLAN;
    assertFiles(expected, resultTemplate);
    expected = TEST_PLAN_TEMPLATE_FOLDER + CORRELATION_RECORDER_TEMPLATE_FILE_PATH;
    assertFiles(expected, resultTemplatesList);
  }

  private void assertFiles(String expectedFilePath, File actualFile) throws IOException {
    String actualContent = FileUtils.readFileToString(actualFile, "utf-8");

    softly.assertThat(actualContent)
        .isEqualToNormalizingWhitespace(
            TestUtils.getFileContent(expectedFilePath, this.getClass()));
  }

  @Test
  public void shouldNotAddTemplateDescriptionWhenItWasAlreadyAdded() throws IOException {
    addTemplate(CORRELATION_RECORDER_TEST_PLAN);

    String templatesListFile = Paths.get(
        tempFolder.getRoot().getPath(), TEMPLATES_LIST_PATH
    ).toAbsolutePath().toString();
    long lastModifiedExpected = new File(templatesListFile).lastModified();
    addTemplate(CORRELATION_RECORDER_TEST_PLAN);

    assertFileNotModified(templatesListFile, lastModifiedExpected);
    assertionCorrelationRecorderOnTemplates();
  }

  private void assertFileNotModified(String correlationRecorderTemplate,
                                     long lastModifiedExpected) {
    softly.assertThat(new File(correlationRecorderTemplate).lastModified())
        .isEqualTo(lastModifiedExpected);
  }

  @Test
  public void shouldAddAssertionsOnCorrelationRecorderTemplateWhenStart() throws IOException {
    String expectedCorrelationRecorderTemplate = copyTemplateWithAssertions();

    String correlationRecorderTemplate = "correlation-recorder-base-template.jmx";
    addTemplate(correlationRecorderTemplate);
    File resultTemplate = new File(
        Paths.get(
            tempFolder.getRoot().getPath(), correlationRecorderTemplate
        ).toAbsolutePath().toString()
    );
    assertFiles(expectedCorrelationRecorderTemplate, resultTemplate);
  }

  private String copyTemplateWithAssertions() throws IOException {
    String expectedCorrelationRecorderTemplate =
        TEST_PLAN_TEMPLATE_FOLDER + "correlation-recorder-with-assertions.jmx";
    copyFile(expectedCorrelationRecorderTemplate,
        Paths.get(
            tempFolder.getRoot().getPath(), TEMPLATES_LIST_PATH
        ).toAbsolutePath().toString()
    );
    return expectedCorrelationRecorderTemplate;
  }
}
