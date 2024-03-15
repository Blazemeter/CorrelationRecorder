package com.blazemeter.jmeter.correlation.core.templates;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RemoteCorrelationTemplatesRepositoriesRegistryTest extends WiredBaseTest{

  private static final String TEMPLATES_FOLDER = "correlation-templates";
  private static final String WIRED_HOST = "localhost";
  private static final String REPOSITORY_NAME = "base";
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  private RemoteCorrelationTemplatesRepositoriesRegistry remote;


  @Before
  public void setup() {
    remote = new RemoteCorrelationTemplatesRepositoriesRegistry(
        new LocalConfiguration(
            Paths.get(folder.getRoot().getPath(), File.separator).toAbsolutePath().toString()));
    startWiredMock("localhost");
  }

  @After
  public void tearDown() {
    stopWiredMock();
  }

  @Test
  public void shouldSaveTemplatesAndRepositoryFromURL() throws IOException, InterruptedException {
    mockRequestsToRepoFiles();
    remote.save(REPOSITORY_NAME, getBaseURL() + TEST_REPOSITORY_URL);

    List<String> expectedFiles = Arrays
        .asList("first-1.0-template.json", "base-repository.json", "second-1.0-template.json",
            "first-1.1-template.json");

    List<String> actualFiles = getGeneratedFilesNames();
    assertGeneratedFiles(expectedFiles, actualFiles);

  }

  private void assertGeneratedFiles(List<String> expectedFiles, List<String> actualFiles) {
    //with this we avoid the test failing because of the order or the files
    assertTrue(expectedFiles.size() == actualFiles.size() &&
        expectedFiles.containsAll(actualFiles) && actualFiles.containsAll(expectedFiles));
  }

  private List<String> getGeneratedFilesNames() {
    return Arrays.stream(Objects.requireNonNull(
            new File(Paths.get(
                    folder.getRoot().getAbsolutePath(), TEMPLATES_FOLDER, REPOSITORY_NAME).toAbsolutePath()
                .toString())
                .list()))
        .filter(f -> f.toLowerCase().endsWith("repository.json") ||
            f.toLowerCase().endsWith("template.json"))
        .collect(Collectors.toList());
  }

  @Test
  public void shouldEncodeUrlWhenSaveWithSpecialCharacters() throws IOException {
    prepareURL("/repository-with-spaced-templates.json");
    prepareURL("/first%20spaced%20template-1.0-template.json");
    skipURL("/first-1.0-snapshot.png");
    remote.save(REPOSITORY_NAME, getBaseURL() + "/repository-with-spaced-templates.json");
    List<String> expectedFiles = Arrays
        .asList("base-repository.json", "first spaced template-1.0-template.json");

    List<String> actualFiles = getGeneratedFilesNames();

    assertGeneratedFiles(expectedFiles, actualFiles);
  }

  @Test(expected = IOException.class)
  public void shouldThrowIOExceptionWhenMalformedJSON() throws IOException {
    remote.save("repo", "https://www.google.com");
  }
}
