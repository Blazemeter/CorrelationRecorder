package com.blazemeter.jmeter.correlation.gui.templates;

import static com.blazemeter.jmeter.correlation.gui.common.StringUtils.capitalize;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public abstract class BaseTest {
  protected static final String LOCAL_REPOSITORY_NAME = "local";
  protected static final String CENTRAL_REPOSITORY_NAME = "central";
  protected static final String CENTRAL_REPOSITORY_URL = "CENTRALURL";
  protected static final String FIRST_REPOSITORY_NAME = "R1";
  protected static final String FIRST_REPOSITORY_URL = "URL1";
  protected static final String SECOND_REPOSITORY_NAME = "R2";
  protected static final String SECOND_REPOSITORY_URL = "URL2";
  protected static final String NEW_REPO_ID = "R3";
  protected static final String NEW_REPO_URL = "URL3";
  protected static final String TEMPLATE_DESCRIPTION = "TestDescription";
  protected static final String TEMPLATE_CHANGES = "TestChanges";
  protected static final String TEMPLATE_VERSION = "1.0.0";
  protected static final String TEMPLATE_SUGGESTED_VERSION = "1.0.1";
  protected static final String TEMPLATE_ID = "TestID";
  protected static final String TEMPLATE_AUTHOR = "TestAuthor";
  protected static final String TEMPLATE_URL = "TestUrl";
  private static final List<String> templateVersions = Arrays.asList("1.0.0", "2.0.0", "3.0.0");
  @Mock
  protected CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistry;
  @Mock
  protected CorrelationTemplatesRegistryHandler templatesRegistry;
  @Mock
  protected Template lastLoadedTemplate;
  @Mock
  protected CorrelationTemplateDependency firstDependency;
  @Mock
  protected CorrelationTemplateDependency secondDependency;
  @Mock
  protected CorrelationTemplateDependency thirdDependency;
  @Mock
  protected CorrelationTemplatesRepository localRepository;
  @Mock
  protected CorrelationTemplatesRepository centralRepository;
  @Mock
  protected CorrelationTemplatesRepository firstRepository;
  @Mock
  protected CorrelationTemplatesRepository secondRepository;
  @Mock
  protected RepositoryManager centralRepositoryManager;
  @Mock
  protected RepositoryManager firstRepositoryManager;
  @Mock
  protected RepositoryManager secondRepositoryManager;

  @Before
  public void baseSetup() {
    MockitoAnnotations.initMocks(this);
    mockRepositories();
    mockRepositoriesManagers();
    mockRepositoriesRegistry();
    mockLastLoadedTemplate();
    mockLastLoadedTemplateDependencies();
  }

  private void mockRepositories() {
    prepareRepository(localRepository, LOCAL_REPOSITORY_NAME, capitalize(LOCAL_REPOSITORY_NAME));
    prepareRepository(centralRepository, CENTRAL_REPOSITORY_NAME, CENTRAL_REPOSITORY_NAME);
    prepareRepository(firstRepository, FIRST_REPOSITORY_NAME, FIRST_REPOSITORY_NAME);
    prepareRepository(secondRepository, SECOND_REPOSITORY_NAME, SECOND_REPOSITORY_NAME);
  }

  public void prepareRepository(CorrelationTemplatesRepository repository, String name,
                                String displayName) {
    when(repository.getName()).thenReturn(name);
    when(repository.getDisplayName()).thenReturn(displayName);
    prepareRepositoryTemplates(repository);
  }

  private void prepareRepositoryTemplates(CorrelationTemplatesRepository repository) {
    Map<String, CorrelationTemplateVersions> templates = new java.util.HashMap<>();
    templates.put(TEMPLATE_ID, new CorrelationTemplateVersions(TEMPLATE_ID, templateVersions));
    when(repository.getTemplates()).thenReturn(templates);
  }

  private void mockRepositoriesManagers() {
    when(centralRepositoryManager.disableConfig())
            .thenReturn(true);
    when(firstRepositoryManager.disableConfig())
            .thenReturn(false);
    when(secondRepositoryManager.disableConfig())
            .thenReturn(false);
  }

  private void mockRepositoriesRegistry() {
    when(repositoriesRegistry.getCorrelationRepositories())
        .thenReturn(Arrays.asList(localRepository, centralRepository, firstRepository, secondRepository));
    when(repositoriesRegistry.getRepositoryURL(CENTRAL_REPOSITORY_NAME))
            .thenReturn(CENTRAL_REPOSITORY_URL);
    when(repositoriesRegistry.getRepositoryURL(FIRST_REPOSITORY_NAME))
        .thenReturn(FIRST_REPOSITORY_URL);
    when(repositoriesRegistry.getRepositoryURL(SECOND_REPOSITORY_NAME))
        .thenReturn(SECOND_REPOSITORY_URL);
    when(repositoriesRegistry.getRepositoryManager(CENTRAL_REPOSITORY_NAME))
            .thenReturn(centralRepositoryManager);
    when(repositoriesRegistry.getRepositoryManager(FIRST_REPOSITORY_NAME))
            .thenReturn(firstRepositoryManager);
    when(repositoriesRegistry.getRepositoryManager(SECOND_REPOSITORY_NAME))
            .thenReturn(secondRepositoryManager);
  }

  private void mockLastLoadedTemplate() {
    when(lastLoadedTemplate.getId()).thenReturn(TEMPLATE_ID);
    when(lastLoadedTemplate.getVersion()).thenReturn(TEMPLATE_VERSION);
    when(lastLoadedTemplate.getDescription()).thenReturn(TEMPLATE_DESCRIPTION);
    when(lastLoadedTemplate.getDependencies()).thenReturn(
        Arrays.asList(firstDependency, secondDependency));
    when(lastLoadedTemplate.getRepositoryId()).thenReturn(FIRST_REPOSITORY_NAME);
    when(lastLoadedTemplate.getAuthor()).thenReturn(TEMPLATE_AUTHOR);
    when(lastLoadedTemplate.getUrl()).thenReturn(TEMPLATE_URL);
    when(lastLoadedTemplate.getChanges()).thenReturn(TEMPLATE_CHANGES);
    when(lastLoadedTemplate.getGroups()).thenReturn(new ArrayList<>());
    when(lastLoadedTemplate.getComponents()).thenReturn("");
    when(lastLoadedTemplate.getResponseFilters()).thenReturn("");
  }

  private void mockLastLoadedTemplateDependencies() {
    prepareDependencyAndUrlValidity(firstDependency, 1, true);
    prepareDependencyAndUrlValidity(secondDependency, 1, true);
  }

  private void prepareDependencyAndUrlValidity(CorrelationTemplateDependency dependency,
                                               int number,
                                               boolean urlValidity) {
    String dependencyName = "Dependency" + number;
    String dependencyVersion = number + ".0";
    String dependencyURL = "ulr" + number;

    when(dependency.getName()).thenReturn(dependencyName);
    when(dependency.getVersion()).thenReturn(dependencyVersion);
    when(dependency.getUrl()).thenReturn(dependencyURL);
    when(templatesRegistry
        .isValidDependencyURL(dependencyURL, dependencyName, dependencyVersion))
        .thenReturn(urlValidity);
  }

  protected Template.Builder buildTemplateBase(CorrelationTemplatesRepository repository,
                                               String templateId) {
    return new Template.Builder()
        .withId(templateId)
        .withAuthor(TEMPLATE_AUTHOR)
        .withUrl(TEMPLATE_URL)
        .withChanges(TEMPLATE_CHANGES)
        .withDescription(TEMPLATE_DESCRIPTION)
        .withRepositoryId(repository.getName())
        .withGroups(new ArrayList<>())
        .withDependencies(new ArrayList<>())
        .withResponseFilters("")
        .withComponents("");
  }

}

