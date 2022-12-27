package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.TestUtils.getFileContent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalConfigurationTest {

  private static final String LOCAL_REPOSITORY_NAME = "local";
  private static final String VERSION_ONE = "1.0";
  private static final String DUMMY_FILE_VERSION_ONE = "dummy-1.0.jar";
  private static final String SIEBEL_REPO_NAME = "siebel";
  private static final String FIRST_REPO_NAME = "repository1";
  private static final String SECOND_REPO_NAME = "repository2";
  private static final String firstRepositoryURL = "localhost/firstRepository.json";
  private static final String secondRepositoryURL = "localhost/secondRepository.json";
  private static final String DEFAULT_ULR = "http://localhost.com/1";
  private static final String JAR_SUFFIX = ".jar";
  private static final String firstTemplateID = "firstTemplate";
  private static final String firstTemplateVersion = "1.0";
  private static final String REPOSITORY_ID = "R1";

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Mock
  private CorrelationTemplateDependency firstDependency;
  @Mock
  private CorrelationTemplatesRepository localRepository;
  @Mock
  private CorrelationTemplatesRepository firstRepository;
  @Mock
  private CorrelationTemplatesRepository secondRepository;
  @Mock
  private CorrelationTemplatesRepositoryConfiguration repositoryWithInstalledTemplates;
  @Mock
  private CorrelationTemplatesRepositoryConfiguration localRepositoryConfiguration;
  private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
  private LocalConfiguration configuration;
  private String dependenciesFolder;

  @Before
  public void setUp() {
    configuration = new LocalConfiguration(folder.getRoot().getPath());
    when(firstRepository.getName()).thenReturn(FIRST_REPO_NAME);
    when(secondRepository.getName()).thenReturn(SECOND_REPO_NAME);
    when(localRepository.getName()).thenReturn(LOCAL_REPOSITORY_NAME);
    configuration.addRepository(firstRepository.getName(), firstRepositoryURL);
    dependenciesFolder = folder.getRoot().getAbsolutePath() + "/lib/";
  }

  @After
  public void tearDown() {
    if (wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  public void configureWireMockServer() {
    wireMockServer.start();
    configureFor("localhost", wireMockServer.port());
  }

  @Test
  public void shouldReturnRepositoryNameWhenAdded() {
    assertEquals(Arrays.asList(LocalConfiguration.CENTRAL_REPOSITORY_ID, localRepository.getName(),
        firstRepository.getName()),
        configuration.getRepositoriesNames());
  }

  @Test
  public void shouldNotAddRepositoryWhenRepositoryAlreadyExists() {
    configuration.addRepository(firstRepository.getName(), firstRepositoryURL);
    assertEquals(Arrays.asList(LocalConfiguration.CENTRAL_REPOSITORY_ID, localRepository.getName(),
        firstRepository.getName()),
        configuration.getRepositoriesNames());
  }

  @Test
  public void shouldDeleteRepositoryWhenDeleteAddedRepository() {
    configuration.removeRepository(firstRepository.getName());
    assertEquals(Arrays.asList(LocalConfiguration.CENTRAL_REPOSITORY_ID,
        localRepository.getName()),
        configuration.getRepositoriesNames());
  }

  @Test
  public void shouldNotDeleteRepositoryWhenDeleteRepositoryNotAdded() {
    configuration.removeRepository(secondRepository.getName());
    assertEquals(Arrays.asList(LocalConfiguration.CENTRAL_REPOSITORY_ID,
        localRepository.getName(), firstRepository.getName()),
        configuration.getRepositoriesNames());
  }

  @Test
  public void shouldReturnOnlyRepositoriesWithInstalledTemplateWhenGetRepositoriesWithInstalledTemplates()
      throws ConfigurationException {

    prepareExpectedRepositoryWithInstalledTemplates();

    configuration.addRepository(secondRepository.getName(), secondRepositoryURL);
    configuration
        .manageTemplate(LocalConfiguration.INSTALL, firstRepository.getName(), firstTemplateID,
            firstTemplateVersion);

    List<CorrelationTemplatesRepositoryConfiguration> expected = Arrays
        .asList(localRepositoryConfiguration, repositoryWithInstalledTemplates);

    List<CorrelationTemplatesRepositoryConfiguration> actual = configuration
        .getRepositoriesWithInstalledTemplates();

    //We are taking the second since the first it's the default Siebel
    assertTrue(expected.size() == actual.size() && actual.get(1).equals(expected.get(1)));
  }

  private void prepareExpectedRepositoryWithInstalledTemplates() {
    when(repositoryWithInstalledTemplates.getName()).thenReturn(FIRST_REPO_NAME);
    when(repositoryWithInstalledTemplates.getUrl()).thenReturn(firstRepositoryURL);
    when(repositoryWithInstalledTemplates.getInstalledTemplates())
        .thenReturn(new HashMap<String, String>() {{
          put(firstTemplateID, firstTemplateVersion);
        }});
  }

  @Test
  public void shouldReturnTrueWhenIsInstalledWithInstalledTemplate() throws ConfigurationException {
    configuration
        .manageTemplate(LocalConfiguration.INSTALL, firstRepository.getName(), firstTemplateID,
            firstTemplateVersion);
    assertTrue(configuration
        .isInstalled(firstRepository.getName(), firstTemplateID, firstTemplateVersion));
  }

  @Test
  public void shouldReturnFalseWhenIsInstalledWithInstalledTemplate() {
    assertFalse(
        configuration
            .isInstalled(firstRepository.getName(), firstTemplateID, firstTemplateVersion));
  }

  @Test
  public void shouldReturnListWithRepositoriesWhenGetRepositoriesNames() {
    configuration.addRepository(secondRepository.getName(), secondRepositoryURL);
    assertEquals(Arrays.asList(LocalConfiguration.CENTRAL_REPOSITORY_ID,
        localRepository.getName(), FIRST_REPO_NAME,
        SECOND_REPO_NAME),
        configuration.getRepositoriesNames());
  }

  @Test
  public void shouldReturnURLWhenGetRepositoryURL() {
    assertEquals(firstRepositoryURL, configuration.getRepositoryURL(firstRepository.getName()));
  }

  @Test
  public void shouldInstallLocalRepositoryOnStart() {
    assertTrue(configuration.getRepositoriesWithInstalledTemplates().stream()
        .anyMatch(r -> r.getInstalledTemplates().containsKey(SIEBEL_REPO_NAME)));
  }

  @Test
  public void shouldDownloadDependenciesWhenDownloadDependencies() throws IOException {
    prepareMockServer();
    prepareDependenciesFolder();
    prepareDependency(firstDependency, mockURL("/" + DUMMY_FILE_VERSION_ONE), 1);

    configuration.downloadDependencies(Collections.singletonList(firstDependency));

    List<String> expectedDependencies = Collections.singletonList(DUMMY_FILE_VERSION_ONE);
    List<String> actualDependencies = getInstalledDependencies();

    softly.assertThat(expectedDependencies.size()).isEqualTo(actualDependencies.size());
    softly.assertThat(expectedDependencies).containsAll(actualDependencies);
  }

  private void prepareMockServer() throws IOException {
    wireMockServer.start();
    configureFor("localhost", wireMockServer.port());
    mockingResponse("/" + DUMMY_FILE_VERSION_ONE, HttpURLConnection.HTTP_OK,
        getFileContent("/" + DUMMY_FILE_VERSION_ONE, getClass()), "get");
  }

  public void mockingResponse(String URL, int status, String body, String method) {
    MappingBuilder routeMapping;
    if (method.equals("head")) {
      routeMapping = head(urlEqualTo(URL));
    } else {
      routeMapping = get(urlEqualTo(URL));
    }

    wireMockServer.stubFor(routeMapping.willReturn(aResponse()
        .withStatus(status)
        .withBody(body)
        .withHeader("Cache-Control", "no-cache")
        .withHeader("Content-Type", "application/json")));
  }

  private void prepareDependenciesFolder() {
    new File(dependenciesFolder).mkdir();
  }

  private void prepareDependency(CorrelationTemplateDependency dependency, String url, int count) {
    when(dependency.getName()).thenReturn("dummy");
    when(dependency.getUrl()).thenReturn(url);
    when(dependency.getVersion()).thenReturn(count + ".0");
  }

  private String mockURL(String resourceName) {
    return "http://localhost:" + wireMockServer.port() + resourceName;
  }

  private List<String> getInstalledDependencies() {
    return Arrays.stream(Objects.requireNonNull(
        new File(dependenciesFolder).list()))
        .filter(f -> f.toLowerCase().endsWith(JAR_SUFFIX))
        .collect(Collectors.toList());
  }

  @Test
  public void shouldDeleteJarsWhenDeleteConflicts() throws IOException {
    prepareDependenciesFolder();
    prepareDependency(firstDependency, DEFAULT_ULR, 1);
    List<File> conflictingDependencies = createConflictingDependencies();

    List<String> previousDependencies = getInstalledDependencies();
    configuration.deleteConflicts(conflictingDependencies);
    List<String> actualDependencies = getInstalledDependencies();

    softly.assertThat(previousDependencies.size()).isEqualTo(actualDependencies.size());
    softly.assertThat(actualDependencies.isEmpty()).isTrue();
  }

  private ArrayList<File> createConflictingDependencies() throws IOException {
    ArrayList<File> conflictingDependencies = new ArrayList<>();
    conflictingDependencies.add(createSingleDependency(2));
    conflictingDependencies.add(createSingleDependency(3));
    return conflictingDependencies;
  }

  public File createSingleDependency(int count) throws IOException {
    File dependency = new File(dependenciesFolder + "dummy-" + count + ".0" + JAR_SUFFIX);
    dependency.createNewFile();
    return dependency;
  }

  @Test
  public void shouldFindJarsWhenGetJarFileByConditionWithNameContains() throws IOException {
    prepareDependenciesFolder();
    createSingleDependency(1);
    assertArrayEquals(buildExpectedDependencies(), configuration
        .getJarFileByCondition(dependenciesFolder, (fileName) -> fileName.contains(VERSION_ONE)));
  }

  private File[] buildExpectedDependencies() {
    return new File[]{new File(dependenciesFolder + DUMMY_FILE_VERSION_ONE)};
  }

  @Test
  public void shouldReturnEmptyWhenHasConflictingDependenciesWithDependencyInstalled()
      throws IOException {
    prepareDependenciesFolder();
    createSingleDependency(1);
    List<File> conflictingDependencies = createConflictingDependencies();
    prepareDependency(firstDependency, DEFAULT_ULR, 1);
    assertNotEquals(
        configuration.findConflictingDependencies(Collections.singletonList(firstDependency)),
        conflictingDependencies);
  }

  @Test
  public void shouldReturnConflictingFilesWhenFindConflictingDependenciesWithDifferentJarsVersions()
      throws IOException {
    prepareDependenciesFolder();
    List<File> conflictingDependencies = createConflictingDependencies();
    prepareDependency(firstDependency, DEFAULT_ULR, 1);
    assert (configuration.findConflictingDependencies(Collections.singletonList(firstDependency))
        .containsAll(conflictingDependencies));
  }

  @Test
  public void shouldReturnFalseWhenHasConflictingDependenciesWithDifferentJarsVersions() {
    prepareDependenciesFolder();
    prepareDependency(firstDependency, DEFAULT_ULR, 1);
    assert (configuration.findConflictingDependencies(Collections.singletonList(firstDependency))
        .isEmpty());
  }

  @Test
  public void shouldReturnErrorWhenCheckRepositoryUrlWithUrlWithoutProtocol() {
    String repositoryURL = "test.com/firstRepository.json";
    List<String> errors = configuration.checkRepositoryURL(REPOSITORY_ID, repositoryURL);
    assertEquals(Collections.singletonList(
        "- We couldn't parse " + REPOSITORY_ID + "'s url " + repositoryURL
            + ".\n   Error: no protocol: test.com/firstRepository.json"), errors);
  }

  @Test
  public void shouldReturnErrorWhenCheckRepositoryURlWithUrlWithNotFoundError() {
    configureWireMockServer();
    String path = "/firstRepository.json";
    String url = getBaseURL() + path;
    mockingResponse(path, HttpURLConnection.HTTP_NOT_FOUND, "", "head");

    List<String> errors = configuration.checkRepositoryURL(REPOSITORY_ID, url);
    assertEquals(Collections.singletonList(
        "- We couldn't reach " + REPOSITORY_ID + "'s url " + url
            + ".\n   Error: 404: Not Found"), errors);
  }

  private String getBaseURL() {
    return "http://localhost:" + wireMockServer.port();
  }

  @Test
  public void shouldReturnErrorWhenCheckRepositoryURlWithNotExistentLocalUrl() {
    String repositoryURL = "file://C:/test/firstRepository.json";
    List<String> errors = configuration.checkRepositoryURL(REPOSITORY_ID, repositoryURL);
    assertEquals(Collections.singletonList(
        "- We couldn't reach " + REPOSITORY_ID + "'s Path " + repositoryURL + ".\n"
            + "   Error: File doesn't exist"), errors);
  }

  @Test
  public void shouldReturnErrorWhenCheckRepositoryURlWithNoJsonFile() {
    String repositoryURL = "file://C:/test/";
    List<String> errors = configuration.checkRepositoryURL(REPOSITORY_ID, repositoryURL);
    assertEquals(Collections.singletonList(
        "- There was and error on the repository " + REPOSITORY_ID + "'s URL " + repositoryURL + ".\n"
            + "   Error: URL should lead to .json file"), errors);
  }
}
