package com.blazemeter.jmeter.correlation.core.templates;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
  private WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

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

    remote.save(REPOSITORY_NAME,
        "http://localhost:" + wireMockServer.port() + "/test-repository.json");

    List<String> expectedFiles = Arrays
        .asList("first-1.0-template.json", "base-repository.json", "second-1.0-template.json",
            "first-1.1-template.json");

    List<String> actualFiles = getGeneratedFilesNames();

    //with this we avoid the test failing because of the order or the files
    assertTrue(expectedFiles.size() == actualFiles.size() &&
        expectedFiles.containsAll(actualFiles) && actualFiles.containsAll(expectedFiles));
  }

  private void prepareURL(String URL) {
    wireMockServer.stubFor(get(urlEqualTo(URL)).willReturn(aResponse()
        .withStatus(200)
        .withHeader("Cache-Control", "no-cache")
        .withHeader("Content-Type", "application/json")
        .withBody(getFileContent(URL))
    ));
  }

  private String getFileContent(String filePath) {

    try {
      return Resources.toString(getClass().getResource(filePath), Charset.defaultCharset());
    } catch (IOException e) {
      return "";
    }
  }

  private void skipURL(String URL) {
    stubFor(get(urlEqualTo(URL)).willReturn(aResponse()
        .withStatus(304)
    ));
  }

  private List<String> getGeneratedFilesNames() {
    return Arrays.stream(Objects.requireNonNull(
        new File(
            folder.getRoot().getAbsolutePath() + "/" + TEMPLATES_FOLDER + "/" + REPOSITORY_NAME)
            .list()))
        .filter(f -> f.toLowerCase().endsWith("repository.json") || f.toLowerCase()
            .endsWith("template.json"))
        .collect(Collectors.toList());
  }
}
