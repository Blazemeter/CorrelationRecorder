package com.blazemeter.jmeter.correlation.core.templates;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertTrue;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RemoteCorrelationTemplatesRepositoriesRegistryTest {

  private static final String TEMPLATES_FOLDER = "correlation-templates";
  private static final String REPOSITORY_NAME = "base";
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

  private RemoteCorrelationTemplatesRepositoriesRegistry remote;


  @Before
  public void setup() {
    remote = new RemoteCorrelationTemplatesRepositoriesRegistry(
        new LocalConfiguration(folder.getRoot().getPath() + "/"));
    wireMockServer.start();
    configureFor("localhost", wireMockServer.port());
  }

  @After
  public void tearDown() {
    if (wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  @Test
  public void shouldSaveTemplatesAndRepositoryFromURL() throws IOException {
    prepareURL("/test-repository.json");
    prepareURL("/first-1.0-template.json");
    skipURL("/first-1.0-snapshot.png");
    prepareURL("/first-1.1-template.json");
    skipURL("/first-1.1-snapshot.png");
    prepareURL("/second-1.0-template.json");
    skipURL("/second-1.0-snapshot.png");

    remote.save(REPOSITORY_NAME, getBaseURL() + "/test-repository.json");

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

  private void prepareURL(String URL) throws IOException {
    wireMockServer.stubFor(get(urlEqualTo(URL)).willReturn(aResponse()
        .withStatus(200)
        .withHeader("Cache-Control", "no-cache")
        .withHeader("Content-Type", "application/json")
        .withBody(TestUtils.getFileContent(URL, getClass()))
    ));
  }

  private void skipURL(String URL) {
    stubFor(get(urlEqualTo(URL)).willReturn(aResponse()
        .withStatus(304)
    ));
  }

  private String getBaseURL() {
    return "http://localhost:" + wireMockServer.port();
  }

  private List<String> getGeneratedFilesNames() {
    return Arrays.stream(Objects.requireNonNull(
        new File(
            folder.getRoot().getAbsolutePath() + "/" + TEMPLATES_FOLDER + "/" + REPOSITORY_NAME)
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
