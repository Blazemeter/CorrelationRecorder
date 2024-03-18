package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import com.blazemeter.jmeter.correlation.core.templates.repository.pluggable.RemoteUrlRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CorrelationTemplatesRepositoriesConfigurationTest extends WiredBaseTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private CorrelationTemplatesRepositoriesConfiguration correlationTempRepoConfig;
    private LocalConfiguration localConfiguration;
    private static final String WIRED_HOST = "localhost";
    private static final String TEST_REPO_NAME = "test";
    private static final String FIRST_TEMPLATE_NAME = "first";
    private static final String SECOND_TEMPLATE_NAME = "second";
    private static final String SIEBEL_TEMPLATE_NAME = "siebel";
    private static final String TEMPLATE_VERSION_1_1 = "1.1";
    private static final String TEMPLATE_VERSION_1_0 = "1.0";
    private static final String TEMPLATE_DESCRIPTION = "Description1";
    private static final String LOCAL_REPO_NAME = "local";
    private static final String CENTRAL_REPO_NAME = "local";

    @Before
    public void setUp() throws IOException {
        String path = folder.getRoot().getPath();
        LocalConfiguration.installDefaultFiles(path);
        localConfiguration = new LocalConfiguration(path, true);
        localConfiguration.setupRepositoryManagers();
        correlationTempRepoConfig = new CorrelationTemplatesRepositoriesConfiguration(localConfiguration);
    }

    @Test
    public void shouldReturnRootPathWhenGetLocalRootFolder() {
        String rootFolder = correlationTempRepoConfig.getLocalRootFolder();
        assertEquals(folder.getRoot().getPath().toString(), rootFolder);
    }

    @Test
    public void shouldReturnRepositoryManagerWhenGetRepositoryManagerWithNoURL() {
        RepositoryManager repoManager = correlationTempRepoConfig.getRepositoryManager(LOCAL_REPO_NAME);
        assertEquals(localConfiguration.getRepositoriesManagers().get(LOCAL_REPO_NAME), repoManager);
    }

    @Test
    public void shouldReturnRepositoryManagerWhenGetRepositoryManager() {
        RepositoryManager repoManager = correlationTempRepoConfig.getRepositoryManager(LOCAL_REPO_NAME, "");
        assertEquals(localConfiguration.getRepositoriesManagers().get(LOCAL_REPO_NAME), repoManager);
    }

    @Test
    public void shouldReturnNewRepoWhenGetRepositoryManagerWithUnexistingName() {
        RepositoryManager expectedRepo = correlationTempRepoConfig.getRepositoryManager(TEST_REPO_NAME, "localhost");
        assertEquals(expectedRepo.getName(), TEST_REPO_NAME);
        assertEquals(expectedRepo.getEndPoint(), "localhost");
    }

    @Test
    public void shouldSaveRepositoryWhenSaveRepository() throws IOException {
        startWiredMock(WIRED_HOST);
        mockRequestsToRepoFiles();
        correlationTempRepoConfig.saveRepository(TEST_REPO_NAME, getBaseURL() + TEST_REPOSITORY_URL);
        RepositoryManager repoManager = localConfiguration.getRepositoriesManagers().get(TEST_REPO_NAME);
        assertEquals(TEST_REPO_NAME, repoManager.getName());
        assertEquals(getBaseURL() + TEST_REPOSITORY_URL, repoManager.getEndPoint());
    }

    @Test
    public void shouldDeleteRepositoryWhenDeleteRepository() throws IOException {
        installTestRepo();
        correlationTempRepoConfig.deleteRepository(TEST_REPO_NAME);
        List<CorrelationTemplatesRepository> repositories =  localConfiguration.getRepositories();
        assertTrue(repositories.stream().noneMatch(r -> r.getName().equals(TEST_REPO_NAME)));
    }

    private void installTestRepo() throws IOException {
        startWiredMock(WIRED_HOST);
        mockRequestsToRepoFiles();
        RepositoryManager testRepo = createRemoteRepository();
        ((CorrelationTemplatesRepositoriesRegistry) testRepo.getTemplateRegistry()).save(TEST_REPO_NAME, getBaseURL() + TEST_REPOSITORY_URL);
        localConfiguration.addRepositoryManager(testRepo);
        localConfiguration.setupRepositoryManagers();
    }
    private RemoteUrlRepository createRemoteRepository() {
        RemoteUrlRepository testRepo = new RemoteUrlRepository();
        testRepo.setName(TEST_REPO_NAME);
        testRepo.setEndPoint(getBaseURL() + TEST_REPOSITORY_URL);
        testRepo.setConfig(localConfiguration);
        testRepo.init();
        return testRepo;
    }

    @Test
    public void shouldReturnCorrelationRepositoriesWhenGetCorrelationRepositories() throws IOException {
        installTestRepo();
        List<CorrelationTemplatesRepository> repositories =  correlationTempRepoConfig.getCorrelationRepositories();
        assertTrue(repositories.stream().anyMatch(r -> r.getName().equals(TEST_REPO_NAME)));
        assertTrue(repositories.stream().anyMatch(r -> r.getName().equals(LOCAL_REPO_NAME)));
        assertTrue(repositories.stream().anyMatch(r -> r.getName().equals(CENTRAL_REPO_NAME)));
    }

    @Test
    public void shouldReturnCorrelationTemplateVersionsWhenGetCorrelationTemplateVersionsByRepositoryNameWithUseLocal() throws IOException {
        installTestRepo();
        String expectedFirstCorrelationTemplateVersions = "CorrelationTemplateVersions {versions=[1.0, 1.1]}";
        String expectedSecondCorrelationTemplateVersions = "CorrelationTemplateVersions {versions=[1.0]}";

        Map<String, CorrelationTemplateVersions> correlationTemplateVersions =
                correlationTempRepoConfig.getCorrelationTemplateVersionsByRepositoryName(TEST_REPO_NAME, true);

        assertEquals(expectedFirstCorrelationTemplateVersions, correlationTemplateVersions.get(FIRST_TEMPLATE_NAME).toString());
        assertEquals(expectedSecondCorrelationTemplateVersions, correlationTemplateVersions.get(SECOND_TEMPLATE_NAME).toString());
    }

    @Test
    public void shouldReturnCorrelationTemplateVersionsWhenGetCorrelationTemplateVersionsByRepositoryName() throws IOException {
        installTestRepo();
        String expectedFirstCorrelationTemplateVersions = "CorrelationTemplateVersions {versions=[1.0, 1.1]}";
        String expectedSecondCorrelationTemplateVersions = "CorrelationTemplateVersions {versions=[1.0]}";

        Map<String, CorrelationTemplateVersions> correlationTemplateVersions =
                correlationTempRepoConfig.getCorrelationTemplateVersionsByRepositoryName(TEST_REPO_NAME, false);

        assertEquals(expectedFirstCorrelationTemplateVersions, correlationTemplateVersions.get(FIRST_TEMPLATE_NAME).toString());
        assertEquals(expectedSecondCorrelationTemplateVersions, correlationTemplateVersions.get(SECOND_TEMPLATE_NAME).toString());
    }

    @Test
    public void shouldInstallTemplateWhenInstallTemplate() throws IOException, ConfigurationException {
        installTestRepo();
        correlationTempRepoConfig.installTemplate(TEST_REPO_NAME, FIRST_TEMPLATE_NAME, TEMPLATE_VERSION_1_1 );
        assertTrue(localConfiguration.isInstalled(TEST_REPO_NAME, FIRST_TEMPLATE_NAME, TEMPLATE_VERSION_1_1));
    }

    @Test
    public void shouldUninstallTemplateWhenUninstallTemplate() throws IOException, ConfigurationException {
        installTestRepo();
        localConfiguration.installTemplate(TEST_REPO_NAME, FIRST_TEMPLATE_NAME, TEMPLATE_VERSION_1_1);
        correlationTempRepoConfig.uninstallTemplate(TEST_REPO_NAME, FIRST_TEMPLATE_NAME, TEMPLATE_VERSION_1_1 );
        assertFalse(localConfiguration.isInstalled(TEST_REPO_NAME, FIRST_TEMPLATE_NAME, TEMPLATE_VERSION_1_1));
    }

    @Test
    public void shouldReturnRepositoryUrlWhenGetRepositoryURL() throws IOException {
        installTestRepo();
        String repoURL = correlationTempRepoConfig.getRepositoryURL(TEST_REPO_NAME);
        assertEquals(getBaseURL() + TEST_REPOSITORY_URL, repoURL);
    }

    @Test
    public void shouldReturnTrueWhenIisLocalTemplateVersionSavedWithSavedVersion() {
        assertTrue(correlationTempRepoConfig.isLocalTemplateVersionSaved(SIEBEL_TEMPLATE_NAME, TEMPLATE_VERSION_1_0));
    }

    @Test
    public void shouldReturnCorrelationTemplatesAndPropertiesWhenGetCorrelationTemplatesAndPropertiesByRepositoryNameWithUseLocal() throws IOException {
        installTestRepo();
        Map<Template, TemplateProperties> templatesAndProperties =
                correlationTempRepoConfig.getCorrelationTemplatesAndPropertiesByRepositoryName(TEST_REPO_NAME, true);
        Iterator<Map.Entry<Template, TemplateProperties>> iterator =  templatesAndProperties.entrySet().iterator();
        Map.Entry<Template, TemplateProperties> entry = iterator.next();
        checkTemplate(entry.getKey(),
                FIRST_TEMPLATE_NAME + "-" +TEMPLATE_VERSION_1_1,
                TEMPLATE_DESCRIPTION, TEMPLATE_VERSION_1_1, TEST_REPO_NAME);
        checkProperties(entry.getValue());
        entry = iterator.next();
        checkTemplate(entry.getKey(),
                FIRST_TEMPLATE_NAME + "-" +TEMPLATE_VERSION_1_0,
                TEMPLATE_DESCRIPTION, TEMPLATE_VERSION_1_0, TEST_REPO_NAME);
        checkProperties(entry.getValue());
        entry = iterator.next();
        checkTemplate(entry.getKey(),
                SECOND_TEMPLATE_NAME + "-" +TEMPLATE_VERSION_1_0,
                TEMPLATE_DESCRIPTION, TEMPLATE_VERSION_1_0, TEST_REPO_NAME);
        checkProperties(entry.getValue());
    }

    private void checkTemplate(Template temp, String id, String Description, String version, String repoId){
        assertEquals(temp.getId(), id);
        assertEquals(temp.getDescription(), Description);
        assertEquals(temp.getVersion(), version);
        assertEquals(temp.getRepositoryId(), repoId);
    }

    private void checkProperties(TemplateProperties props){
        Assert.assertEquals("false", props.get("not_allow_export"));
        Assert.assertEquals("false", props.get("disallow_to_use"));
    }

    @Test
    public void shouldReturnCorrelationTemplatesAndPropertiesWhenGetCorrelationTemplatesAndPropertiesByRepositoryName() throws IOException {
        installTestRepo();
        Map<Template, TemplateProperties> templatesAndProperties =
                correlationTempRepoConfig.getCorrelationTemplatesAndPropertiesByRepositoryName(TEST_REPO_NAME, false);
        Iterator<Map.Entry<Template, TemplateProperties>> iterator =  templatesAndProperties.entrySet().iterator();
        Map.Entry<Template, TemplateProperties> entry = iterator.next();
        checkTemplate(entry.getKey(),
                FIRST_TEMPLATE_NAME + "-" +TEMPLATE_VERSION_1_1,
                TEMPLATE_DESCRIPTION, TEMPLATE_VERSION_1_1, TEST_REPO_NAME);
        checkProperties(entry.getValue());
        entry = iterator.next();
        checkTemplate(entry.getKey(),
                FIRST_TEMPLATE_NAME + "-" +TEMPLATE_VERSION_1_0,
                TEMPLATE_DESCRIPTION, TEMPLATE_VERSION_1_0, TEST_REPO_NAME);
        checkProperties(entry.getValue());
        entry = iterator.next();
        checkTemplate(entry.getKey(),
                SECOND_TEMPLATE_NAME + "-" +TEMPLATE_VERSION_1_0,
                TEMPLATE_DESCRIPTION, TEMPLATE_VERSION_1_0, TEST_REPO_NAME);
        checkProperties(entry.getValue());
    }
}
