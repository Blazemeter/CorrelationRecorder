package com.blazemeter.jmeter.correlation.core.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class LocalCorrelationTemplatesRepositoriesRegistryTest {

  private static final String TEMPLATES_FOLDER = "/correlation-templates/";
  private static final String TEMPLATE_FILE_SUFFIX = "template.json";
  private static final String REPOSITORY_FILE_SUFFIX = "repository.json";
  private static final String EXTERNAL_REPOSITORY_NAME = "base";
  private static final String LOCAL_REPOSITORY_NAME = "local";
  private static final String CENTRAL_REPOSITORY_NAME = "central";
  private static final String SIEBEL_TEMPLATE_REFERENCE_NAME = "siebel";
  private static final String WORDPRESS_TEMPLATE_REFERENCE_NAME = "wordpress";

  private static final String BASE_REPOSITORY_NAME =
      EXTERNAL_REPOSITORY_NAME + "-" + REPOSITORY_FILE_SUFFIX;

  private static final String TEMPLATE_VERSION_TWO = "1.1";
  private static final String TEMPLATE_VERSION_ONE = "1.0";

  private static final String TEMPLATE_VERSION_THREE = "0.1-alpha";

  private static final String SIEBEL_TEMPLATE_VERSION_TWO_NAME =
      SIEBEL_TEMPLATE_REFERENCE_NAME + "-" + TEMPLATE_VERSION_TWO + "-" + TEMPLATE_FILE_SUFFIX;
  private static final String SIEBEL_TEMPLATE_VERSION_ONE_NAME =
      SIEBEL_TEMPLATE_REFERENCE_NAME + "-" + TEMPLATE_VERSION_ONE + "-" + TEMPLATE_FILE_SUFFIX;

  private static final String WORDPRESS_TEMPLATE_VERSION_ONE_NAME =
      WORDPRESS_TEMPLATE_REFERENCE_NAME + "-" + TEMPLATE_VERSION_ONE + "-" + TEMPLATE_FILE_SUFFIX;
  private static final String CORRELATION_TEMPLATES_REPOSITORY_NAME =
      "CorrelationTemplatesRepository";

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  @Mock
  private CorrelationTemplatesRepository expectedBaseRepository;
  @Mock
  private CorrelationTemplatesRepository expectedSiebelRepository;
  @Mock
  private CorrelationTemplatesRepository expectedCentralRepository;
  private LocalCorrelationTemplatesRepositoriesRegistry local;

  @Before
  public void setup() throws IOException {
    LocalConfiguration localConfiguration =
        new LocalConfiguration(folder.getRoot().getPath(), true);
    localConfiguration.setupRepositoryManagers();
    local = new LocalCorrelationTemplatesRepositoriesRegistry(localConfiguration);
    String localRepository = Paths.get(new File(getClass().getResource("/").getFile()).toPath().
        toAbsolutePath().toString(), BASE_REPOSITORY_NAME).toAbsolutePath().toString();

    local.save(EXTERNAL_REPOSITORY_NAME, localRepository);
    prepareExpectedLocalRepository();
  }

  private void prepareExpectedLocalRepository() {
    when(expectedSiebelRepository.getValues()).thenReturn(
        CORRELATION_TEMPLATES_REPOSITORY_NAME + "{name='" + LOCAL_REPOSITORY_NAME
            + "', templatesVersions={" + SIEBEL_TEMPLATE_REFERENCE_NAME +
            "=CorrelationTemplateReference{versions=[" + TEMPLATE_VERSION_ONE + "]}}}");

    when(expectedCentralRepository.getValues()).thenReturn(
        CORRELATION_TEMPLATES_REPOSITORY_NAME + "{name='" + CENTRAL_REPOSITORY_NAME
            + "', templatesVersions={" + WORDPRESS_TEMPLATE_REFERENCE_NAME +
            "=CorrelationTemplateReference{versions=[" + TEMPLATE_VERSION_THREE + "]}}}");

    when(expectedBaseRepository.getName()).thenReturn(EXTERNAL_REPOSITORY_NAME);
    when(expectedBaseRepository.getValues()).thenReturn(
        CORRELATION_TEMPLATES_REPOSITORY_NAME + "{name='" + EXTERNAL_REPOSITORY_NAME
            + "', templatesVersions={"
            + SIEBEL_TEMPLATE_REFERENCE_NAME
            + "=CorrelationTemplateReference{versions=[" + TEMPLATE_VERSION_ONE
            + ", " + TEMPLATE_VERSION_TWO + "]}, "
            + WORDPRESS_TEMPLATE_REFERENCE_NAME
            + "=CorrelationTemplateReference{versions=[" + TEMPLATE_VERSION_ONE
            + "]}}}");
  }

  @Test
  public void shouldSaveRepositoryAndTemplatesWhenSaveFromLocalFileSystem() {
    List<String> expectedGeneratedFilesNames = Arrays
        .asList(BASE_REPOSITORY_NAME, SIEBEL_TEMPLATE_VERSION_TWO_NAME,
            SIEBEL_TEMPLATE_VERSION_ONE_NAME,
            WORDPRESS_TEMPLATE_VERSION_ONE_NAME);

    List<String> actualGeneratedFilesNames = getGeneratedFilesNames(EXTERNAL_REPOSITORY_NAME);
    //Avoiding failing because of the order
    assertTrue(expectedGeneratedFilesNames.size() == actualGeneratedFilesNames.size() &&
        expectedGeneratedFilesNames.containsAll(actualGeneratedFilesNames)
        && actualGeneratedFilesNames.containsAll(expectedGeneratedFilesNames));
  }

  private List<String> getGeneratedFilesNames(String repositoryName) {
    File repositoryFolder = new File(
        folder.getRoot().getAbsolutePath() + TEMPLATES_FOLDER + repositoryName);
    if (!repositoryFolder.exists()) {
      return new ArrayList<>();
    }

    return Arrays.stream(Objects.requireNonNull(
            repositoryFolder.list()))
        .filter(f -> f.toLowerCase().endsWith(REPOSITORY_FILE_SUFFIX) || f.toLowerCase()
            .endsWith(TEMPLATE_FILE_SUFFIX))
        .collect(Collectors.toList());
  }

  @Test
  public void shouldReturnLocalCorrelationTemplatesRepositoriesWhenGetRepositories() {
    assertEquals(getRepositoriesNames(prepareExpectedLocalRepositories()),
        getRepositoriesNames(local.getRepositories()));
  }

  private List<String> getRepositoriesNames(List<CorrelationTemplatesRepository> repositories) {
    return repositories.stream().map(CorrelationTemplatesRepository::getValues)
        .collect(Collectors.toList());
  }

  private List<CorrelationTemplatesRepository> prepareExpectedLocalRepositories() {
    List<CorrelationTemplatesRepository> expectedLocalRepositories = new ArrayList<>();
    expectedLocalRepositories.add(expectedCentralRepository);
    expectedLocalRepositories.add(expectedSiebelRepository);
    expectedLocalRepositories.add(expectedBaseRepository);

    return expectedLocalRepositories;
  }

  @Test
  public void shouldFindRepositoryByIdWhenFind() {
    assertEquals(expectedBaseRepository.getValues(),
        local.find(EXTERNAL_REPOSITORY_NAME).getValues());
  }

  public String getValuesForCompare(CorrelationTemplatesRepository repository) {
    return repository.getTemplates().keySet().stream()
        .map(key -> key + "=" + repository.getTemplates().get(key))
        .collect(Collectors.joining(", ", "{", "}"));
  }

  @Test
  public void shouldDeleteRepositoryAndTemplatesWhenDeleteRepository() throws IOException {
    local.delete(expectedBaseRepository.getName());
    assert (getGeneratedFilesNames(EXTERNAL_REPOSITORY_NAME).isEmpty());
  }
}