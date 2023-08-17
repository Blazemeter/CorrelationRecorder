package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.JSON_FILE_EXTENSION;
import static com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils.isURL;

import com.blazemeter.jmeter.correlation.core.templates.repository.PluggableRepository;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.pluggable.RemoteFolderRepository;
import com.blazemeter.jmeter.correlation.core.templates.repository.pluggable.RemoteUrlRepository;
import com.blazemeter.jmeter.correlation.gui.TestPlanTemplatesRepository;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.reflect.ClassFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalConfiguration {

  public static final String CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER = "/correlation-templates/";
  public static final String INSTALL = "install";
  public static final String UNINSTALL = "uninstall";
  public static final String LOCAL_CONFIGURATION_FILE_NAME = "repositories";
  protected static final String SIEBEL_CORRELATION_TEMPLATE = "siebel-1.0-template.json";
  protected static final String JAR_FILE_SUFFIX = ".jar";
  protected static final String TEMPLATES_FOLDER_PATH = "/templates/";
  private static final String LOCAL_REPOSITORIES_MANAGERS_FILE_NAME = "managers";
  private static final String CORRELATION_RECORDER_TEST_PLAN = "correlation-recorder.jmx";
  private static final String CORRELATION_RECORDER_TEMPLATE_DESC = "correlation-recorder-template"
      + "-description.xml";
  private static final String CORRELATION_RECORDER_TEMPLATE_NAME = "bzm - Correlation Recorder";
  private static final Logger LOG = LoggerFactory.getLogger(LocalConfiguration.class);

  @JsonIgnore
  private static ConcurrentHashMap<String, RepositoryManager> repositoriesManagers =
      new ConcurrentHashMap<>();

  private transient String rootFolder;
  private transient String configurationFolder;

  private transient ObjectMapper mapper;
  private transient ObjectWriter writer;

  private List<CorrelationTemplatesRepositoryConfiguration> repositories = new ArrayList<>();

  //Constructor added to avoid issues with the serialization
  public LocalConfiguration() {

  }

  public LocalConfiguration(String jmeterRootFolder, boolean initStatic) {
    // This flag is mainly used for unit tests
    if (initStatic) {
      repositoriesManagers.clear();
    }
    rootFolder = jmeterRootFolder;
    createDefaultCorrelationTemplateFolder();
    setJsonConfigurations();
    loadLocalConfigurationFile();
    try {
      registerPreInstalledRepositories();
    } catch (IOException e) {
      LOG.error("There was a problem trying to create the local repository file. ", e);
    }
  }

  public LocalConfiguration(String jmeterRootFolder) {
    this(jmeterRootFolder, false);
  }

  public static void installDefaultFiles() {
    LOG.info("Installing default files");
    installCorrelationRecorderTemplateTestPlan();
    installSiebelCorrelationTemplate();
  }

  private static void installCorrelationRecorderTemplateTestPlan() {
    TestPlanTemplatesRepository templateRepository = new TestPlanTemplatesRepository(
        Paths.get(JMeterUtils.getJMeterBinDir(), TEMPLATES_FOLDER_PATH).toAbsolutePath().toString()
            + File.separator);

    templateRepository.addCorrelationRecorderTemplate(CORRELATION_RECORDER_TEST_PLAN,
        TEMPLATES_FOLDER_PATH,
        TEMPLATES_FOLDER_PATH + CORRELATION_RECORDER_TEMPLATE_DESC,
        CORRELATION_RECORDER_TEMPLATE_NAME);
    LOG.info("bzm - Correlation Recorder Test Plan Template installed");
  }

  private static void installSiebelCorrelationTemplate() {
    TestPlanTemplatesRepository templateRepository = new TestPlanTemplatesRepository(Paths
        .get(JMeterUtils.getJMeterBinDir(),
            LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER)
        .toAbsolutePath().toString() + File.separator);
    templateRepository.addCorrelationTemplate(SIEBEL_CORRELATION_TEMPLATE,
        LocalConfiguration.CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER);
    LOG.info("Siebel Correlation's Template installed");
  }

  private HashMap<String, String> loadRepositoriesManagers() {
    String localRepositoriesManagersPath =
        Paths.get(
            configurationFolder,
            LOCAL_REPOSITORIES_MANAGERS_FILE_NAME + JSON_FILE_EXTENSION
        ).toAbsolutePath().toString();

    File localRepositoriesManagersFile = new File(localRepositoriesManagersPath);
    if (localRepositoriesManagersFile.exists()) {
      try {
        HashMap<String, String> repositoriesManagersKV =
            readValue(localRepositoriesManagersFile, HashMap.class);
        return repositoriesManagersKV;
      } catch (IOException e) {
        LOG.warn("There was an error trying to save the configuration file {}.",
            localRepositoriesManagersFile, e);
      }
    }
    return new HashMap<>();
  }

  private void saveRepositoriesManagers() {
    String localRepositoriesManagersPath =
        Paths.get(
            configurationFolder,
            LOCAL_REPOSITORIES_MANAGERS_FILE_NAME + JSON_FILE_EXTENSION
        ).toAbsolutePath().toString();

    File localRepositoriesManagersFile = new File(localRepositoriesManagersPath);
    try {
      HashMap<String, String> repositoriesManagersKV = new HashMap<>();
      repositoriesManagers.forEach((key, value) -> {
        repositoriesManagersKV.put(key, value.getClass().getCanonicalName());
      });
      writer.writeValue(localRepositoriesManagersFile, repositoriesManagersKV);
    } catch (IOException e) {
      LOG.warn("There was an error trying to save the configuration file {}.",
          localRepositoriesManagersFile, e);
    }
  }

  public RepositoryManager findAndInstanceRepositoryClass(String strClassName)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
      InstantiationException, IllegalAccessException {
    Class<?> commandClass = Class.forName(strClassName);
    if (!Modifier.isAbstract(commandClass.getModifiers())) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Instantiating: {}", commandClass.getName());
      }
      return (RepositoryManager) commandClass.getDeclaredConstructor().newInstance();
    }
    return null;
  }

  private boolean isJUnitTest() {
    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
      if (element.getClassName().startsWith("org.junit.")) {
        return true;
      }
    }
    return false;
  }

  private List<String> findPluggableRepositoriesClasses() throws IOException {
    List<String> findPaths = new ArrayList<>();
    findPaths.addAll(Arrays.asList(JMeterUtils.getSearchPaths()));
    // WA for no correct jmeter env initialization like junit tests
    if (isJUnitTest()) {
      findPaths.add(new File(
          this.getClass().getProtectionDomain().getCodeSource().getLocation()
              .getFile()).getAbsolutePath());
    }
    String[] findPathsArr = new String[findPaths.size()];
    findPathsArr = findPaths.toArray(findPathsArr);
    return ClassFinder.findClassesThatExtend(
        findPathsArr,
        new Class[] {PluggableRepository.class});
  }

  private void registerPreInstalledRepositories() throws IOException {
    if (repositoriesManagers.size() > 0) {
      return; // Managers are not allowed to re-register if they have already run.
    }
    HashMap<String, String> repositoriesManagersIdMap = loadRepositoriesManagers();
    List<String> listClasses = findPluggableRepositoriesClasses();
    for (String strClassName : listClasses) {
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Loading class: {}", strClassName);
        }
        RepositoryManager repository = findAndInstanceRepositoryClass(strClassName);
        if (repository != null && repository.autoLoad()) {
          if (!repositoriesManagers.containsKey(repository.getName())) {
            repository.setConfig(this);
            repository.init();
            addRepository(repository.getName(), repository.getEndPoint());
            repositoriesManagers.put(repository.getName(), repository);
          }
        }
      } catch (Exception e) {
        LOG.error("Exception registering {} with implementation:{}",
            RepositoryManager.class.getName(), strClassName, e);
      }
    }
    // Register the repositories without autoload
    repositoriesManagersIdMap.forEach((key, value) -> {
      if (!repositoriesManagers.containsKey(key)) {
        RepositoryManager repository = null;
        try {
          repository = findAndInstanceRepositoryClass(value);
        } catch (Exception e) {
          LOG.error("Exception registering {} with implementation:{}",
              RepositoryManager.class.getName(), value, e);
        }
        if (repository != null) {
          try {
            repository.setConfig(this);
            repository.setName(key);
            repository.init();
            addRepository(repository.getName(), repository.getEndPoint());
            repositoriesManagers.put(repository.getName(), repository);
          } catch (Exception e) {
            LOG.error("Unknown Exception registering {} with implementation:{}",
                RepositoryManager.class.getName(), value, e);
          }
        } else {
          LOG.error("Instance class {} with implementation:{} not found!",
              RepositoryManager.class.getName(), value);
        }
      }
    });
    saveRepositoriesManagers();
  }

  public void setupRepositoryManagers() {
    Iterator<String> managersIterator = repositoriesManagers.keySet().iterator();
    while (managersIterator.hasNext()) {
      String managerId = managersIterator.next();
      RepositoryManager manager = repositoriesManagers.get(managerId);
      try {
        manager.setup();
      } catch (Exception e) {
        LOG.error("Unknown Exception setting up {} with implementation:{}",
            managerId, manager.getClass().getName(), e);
      }
    }
  }

  public void addRepositoryManager(RepositoryManager repository) {
    if (!repositoriesManagers.containsKey(repository.getName())) {
      repositoriesManagers.put(repository.getName(), repository);
      saveRepositoriesManagers();
    }
  }

  public AbstractMap<String, RepositoryManager> getRepositoriesManagers() {
    return repositoriesManagers;
  }

  public RepositoryManager getRepositoryManager(String name, String url) {
    if (!repositoriesManagers.containsKey(name)) {

      // Try to identify the manager based on the URL, for "Legacy" Repositories
      RepositoryManager repository;
      if (isURL(url)) {
        repository = new RemoteUrlRepository(name, url);
      } else {
        repository = new RemoteFolderRepository(name, url);
      }
      repository.setConfig(this);
      repository.init();
      addRepositoryManager(repository);
    }
    return repositoriesManagers.getOrDefault(name, null);
  }

  public RepositoryManager getRepositoryManager(String name) {
    if (!repositoriesManagers.containsKey(name)) {
      String url = getRepositoryURL(name);
      return getRepositoryManager(name, url);
    }
    return repositoriesManagers.getOrDefault(name, null);
  }

  private void createDefaultCorrelationTemplateFolder() {
    File directory = new File(rootFolder);
    if (!directory.exists() && directory.mkdir()) {
      LOG.debug("Created the root folder {}", rootFolder);
    }

    configurationFolder = Paths.get(
        rootFolder, CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER
    ).toAbsolutePath() + File.separator;
    directory = new File(configurationFolder);
    if (!directory.exists() && directory.mkdir()) {
      LOG.debug("Created the configuration folder {}", configurationFolder);
    }
  }

  private void setJsonConfigurations() {
    mapper = new ObjectMapper();
    mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    mapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
    FilterProvider filters = new SimpleFilterProvider()
        .addFilter(CorrelationRuleSerializationPropertyFilter.FILTER_ID,
            new CorrelationRuleSerializationPropertyFilter());
    mapper.setFilterProvider(filters);
    mapper.writerWithDefaultPrettyPrinter().with(filters);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    writer = mapper.writerWithDefaultPrettyPrinter().with(filters);
  }

  private void loadLocalConfigurationFile() {
    File localConfigurationFile = new File(
        Paths.get(
                configurationFolder,
                LOCAL_CONFIGURATION_FILE_NAME + JSON_FILE_EXTENSION)
            .toAbsolutePath().toString()
    );
    if (localConfigurationFile.exists()) {
      try {
        repositories = readValue(localConfigurationFile, LocalConfiguration.class).repositories;
      } catch (IOException e) {
        LOG.error("There was an error trying to read the local configuration file. ", e);
      }
    } else {
      LOG.info("No local configuration file was found.");
    }
  }

  public <T> T readValue(File source, Class<T> valueType) throws IOException {
    return mapper.readValue(source, valueType);
  }

  public <T> T readValue(String source, Class<T> valueType) throws IOException {
    return mapper.readValue(source, valueType);
  }

  public Map<String, CorrelationTemplateVersions> readTemplatesReferences(File source)
      throws IOException {
    return mapper.readValue(source, new TypeReference<Map<String, CorrelationTemplateVersions>>() {
    });
  }

  public void writeValue(File resultFile, Object value) throws IOException {
    writer.writeValue(resultFile, value);
  }

  public byte[] getValueAsBytes(Object value) throws IOException {
    return writer.writeValueAsBytes(value);
  }

  public void addRepository(String name, String url) {
    if (repositories.stream().noneMatch(r -> r.getName().equals(name))) {
      repositories.add(new CorrelationTemplatesRepositoryConfiguration(name, url));
      saveLocalConfiguration();
    }
  }

  public void saveLocalConfiguration() {
    String localConfigurationPath =
        Paths.get(
            configurationFolder,
            LOCAL_CONFIGURATION_FILE_NAME + JSON_FILE_EXTENSION
        ).toAbsolutePath().toString();

    File localConfigurationFile = new File(localConfigurationPath);

    try {
      if (!localConfigurationFile.exists() && localConfigurationFile.createNewFile()) {
        LOG.info("The local configuration file was created at {}", localConfigurationPath);
      }
      writer.writeValue(localConfigurationFile, this);
    } catch (IOException e) {
      LOG.warn("There was an error trying to save the configuration file {}.",
          localConfigurationPath, e);
    }
  }

  public void removeRepository(String repositoryName) {
    findRepositoryById(repositoryName).ifPresent(r -> repositories.remove(r));
    saveLocalConfiguration();
  }

  private Optional<CorrelationTemplatesRepositoryConfiguration> findRepositoryById(
      String repositoryName) {
    return repositories.stream().filter(r -> r.getName().equals(repositoryName)).findAny();
  }

  public void manageTemplate(String action, String repositoryName, String templateId,
                             String templateVersion) throws ConfigurationException {
    Optional<CorrelationTemplatesRepositoryConfiguration> repository = findRepositoryById(
        repositoryName);

    if (!repository.isPresent()) {
      throw new ConfigurationException(
          "The repository " + repositoryName + "doesn't exists. Template " + templateVersion
              + "couldn't be " + action + "ed");
    }

    if (action.equals(INSTALL)) {
      repository.get().installTemplate(templateId, templateVersion);
    } else {
      repository.get().uninstallTemplate(templateId);
    }
    saveLocalConfiguration();
  }

  public List<CorrelationTemplatesRepositoryConfiguration> getRepositories() {
    return repositories;
  }

  public List<String> getRepositoriesNames() {
    return repositories.stream()
        .map(CorrelationTemplatesRepositoryConfiguration::getName)
        .collect(Collectors.toList());
  }

  public boolean isInstalled(String repositoryName, String templateId) {
    return repositories.stream().anyMatch(
        r -> r.getName().equals(repositoryName) && r.getInstalledTemplates()
            .containsKey(templateId));
  }

  public boolean isInstalled(String repositoryName, String templateId, String templateVersion) {
    Optional<CorrelationTemplatesRepositoryConfiguration> foundRepository = repositories.stream()
        .filter(r -> r.getName().equals(repositoryName))
        .findFirst();

    return foundRepository.map(r -> r.isInstalled(templateId, templateVersion)).orElse(false);
  }

  public List<CorrelationTemplatesRepositoryConfiguration> getRepositoriesWithInstalledTemplates() {
    return repositories.stream()
        .filter(CorrelationTemplatesRepositoryConfiguration::hasInstalledTemplates)
        .collect(Collectors.toList());
  }

  public String getRepositoryURL(String name) {
    return repositories.stream()
        .filter(r -> r.getName().toLowerCase().equals(name.toLowerCase()))
        .map(CorrelationTemplatesRepositoryConfiguration::getUrl)
        .collect(Collectors.joining());
  }

  public String getRootFolder() {
    return rootFolder;
  }

  public String getCorrelationsTemplateInstallationFolder() {
    return configurationFolder;
  }

  public List<File> findConflictingDependencies(List<CorrelationTemplateDependency> dependencies) {
    List<File> conflictingDependencies = new ArrayList<>();
    for (CorrelationTemplateDependency dependency : dependencies) {
      String possibleJarFileName = getUrlFileName(dependency.getUrl());

      String dependenciesFolderPath = rootFolder + "/lib/";
      File[] possibleDependencies = getJarFileByCondition(dependenciesFolderPath,
          (fileName) -> fileName.toLowerCase().contains(dependency.getName().toLowerCase())
              || fileName.contains(possibleJarFileName.replace(JAR_FILE_SUFFIX, "")));
      if (possibleDependencies.length == 0) {
        continue;
      }

      Optional<File> installedDependency = Arrays.stream(possibleDependencies)
          .filter(f -> f.getName().equals(possibleJarFileName) || (
              f.getName().contains(dependency.getName()) && f.getName()
                  .contains(dependency.getVersion())))
          .findFirst();

      if (installedDependency.isPresent()) {
        continue;
      }

      LOG.warn(
          "The following dependencies might generate conflicts with {}, and will be deleted if "
              + "you proceed to install this Correlation Template.",
          dependency.getName());
      Arrays.stream(possibleDependencies).forEach(
          conflictingDependency -> {
            conflictingDependencies.add(conflictingDependency);
            LOG.warn("Dependency: {}.", conflictingDependency.getName());
          });
    }
    return conflictingDependencies;
  }

  private String getUrlFileName(String url) {
    int pos = url.lastIndexOf("/");
    return pos < 0 ? "" : url.substring(pos + 1);
  }

  public File[] getJarFileByCondition(String folderPath, Predicate<String> condition) {
    return new File(folderPath)
        .listFiles(((dir, name) -> condition.test(name) && name.endsWith(JAR_FILE_SUFFIX)));
  }

  public void deleteConflicts(List<File> dependencies) {
    for (File dependency : dependencies) {

      if (dependency.exists() && dependency.delete()) {
        LOG.info("The dependency {} was deleted.", dependency.getName());
      } else {
        LOG.info("The dependency {} couldn't be deleted", dependency.getName());
      }
    }
  }

  public void downloadDependencies(List<CorrelationTemplateDependency> dependencies)
      throws IOException {
    String dependenciesFolderPath = rootFolder + "/lib/";
    for (CorrelationTemplateDependency dependency : dependencies) {
      saveFileFromURL(dependency.getUrl(),
          dependenciesFolderPath + dependency.getName() + "-" + dependency.getVersion()
              + JAR_FILE_SUFFIX);
    }
  }

  private void saveFileFromURL(String fileURL, String fileFullPath) throws IOException {
    File templateFile = new File(fileFullPath);
    if (templateFile.createNewFile()) {
      FileUtils.copyURLToFile(new URL(fileURL), templateFile);
      LOG.info("Created the file {}", fileFullPath);
    }
  }

  public boolean isValidDependencyURL(String url, String name, String version) {
    boolean isValid = true;
    try {
      URL parsedURL = new URL(url);
      HttpURLConnection huc = (HttpURLConnection) parsedURL.openConnection();
      //Just sending the headers to validate the URL exists
      huc.setRequestMethod("HEAD");
      int responseCode = huc.getResponseCode();
      if (HttpURLConnection.HTTP_OK != responseCode) {
        LOG.warn(
            "There was an error trying to reach the URL for the dependency {} (version {}). "
                + "Error: {}. URL: {}",
            name, version,
            huc.getResponseMessage().isEmpty() ? huc.getResponseCode() : huc.getResponseMessage(),
            url);
        isValid = false;
      }
    } catch (IOException ex) {
      isValid = false;
      LOG.warn("There was an error with the URL for the dependency {} (version {}). URL: {}.", name,
          version, url, ex);
    }

    return isValid;
  }

  public List<String> checkRepositoryURL(String repositoryId, String repositoryURL) {
    RepositoryManager repositoryManager = getRepositoryManager(repositoryId, repositoryURL);
    List<String> errors = new ArrayList<>();
    if (repositoryManager != null) {
      errors.addAll(repositoryManager.checkRepositoryURL(repositoryURL));
    }
    return errors;
  }
}
