package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.gui.TestPlanTemplatesRepository;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class TemplateUtils {

  public static final String TEST_TEMPLATE_NAME = "test";
  public static final String TEST_TEMPLATE_VERSION = "1.0";
  public static final String TEST_CORRELATION_TEMPLATE = "test-1.0-template.json";
  public static final String TEST_TEMPLATE_DESCRIPTION = "This is a testing template used to "
      + "verify product integrity";

  public static void installTestTemplate(String rootFolder, LocalConfiguration configuration) throws IOException,
      ConfigurationException {
    TestPlanTemplatesRepository templateRepository = new TestPlanTemplatesRepository(Paths
        .get(rootFolder,
            LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER)
        .toAbsolutePath() + File.separator);
    templateRepository.addCorrelationTemplate(TEST_CORRELATION_TEMPLATE,
        LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER);
    addTestRepositoryToLocal(rootFolder, configuration);
    configuration.manageTemplate(LocalConfiguration.INSTALL, "local",
        TEST_TEMPLATE_NAME, TEST_TEMPLATE_VERSION);
  }

  private static void addTestRepositoryToLocal(String rootFolder, LocalConfiguration configuration) throws IOException {
    Path localRepositoryPath = Paths.get(rootFolder,
        LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER, "local-repository"
            + ".json");
    HashMap<String, CorrelationTemplateVersions> template = new HashMap<String,
        CorrelationTemplateVersions>() {
      {
        CorrelationTemplateVersions value =
            new CorrelationTemplateVersions(TEST_TEMPLATE_VERSION);
        value.setRepositoryDisplayName("local");
        put(TEST_TEMPLATE_NAME, value);
      }
    };
    configuration.writeValue(localRepositoryPath.toFile(), template);
  }

}
