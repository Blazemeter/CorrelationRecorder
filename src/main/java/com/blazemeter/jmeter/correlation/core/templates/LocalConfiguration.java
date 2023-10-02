package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.BZM_CENTRAL_REPOSITORY_NAME;
import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.CENTRAL_REPOSITORY_NAME;
import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.JSON_FILE_EXTENSION;
import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.LOCAL_REPOSITORY_NAME;
import static com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils.isURL;
import static com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils.removeRepositoryNameFromFile;

import com.blazemeter.jmeter.correlation.core.templates.repository.PluggableRepository;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import com.blazemeter.jmeter.correlation.core.templates.repository.pluggable.RemoteFolderRepository;
import com.blazemeter.jmeter.correlation.core.templates.repository.pluggable.RemoteUrlRepository;
import com.blazemeter.jmeter.correlation.gui.TestPlanTemplatesRepository;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.reflect.ClassFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalConfiguration {
  public static final String CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER = "/correlation-templates/";
  public static final String INSTALL = "install";
  public static final String UNINSTALL = "uninstall";
  private static final String LOCAL_CONFIGURATION_FILE_NAME = "repositories";
  private static final String SIEBEL_CORRELATION_TEMPLATE = "siebel-1.0-template.json";
  private static final String JAR_FILE_SUFFIX = ".jar";
  private static final String TEMPLATES_FOLDER_PATH = "/templates/";
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

  public static void installDefaultFiles(String rootFolder) {
    LOG.info("Installing default files");
    installCorrelationRecorderTemplateTestPlan(rootFolder);
    installSiebelCorrelationTemplate(rootFolder);
  }

  private static void installCorrelationRecorderTemplateTestPlan(String rootFolder) {
    TestPlanTemplatesRepository templateRepository = new TestPlanTemplatesRepository(
        Paths.get(rootFolder, TEMPLATES_FOLDER_PATH).toAbsolutePath().toString()
            + File.separator);

    templateRepository.addCorrelationRecorderTemplate(CORRELATION_RECORDER_TEST_PLAN,
        TEMPLATES_FOLDER_PATH,
        TEMPLATES_FOLDER_PATH + CORRELATION_RECORDER_TEMPLATE_DESC,
        CORRELATION_RECORDER_TEMPLATE_NAME);
    LOG.info("bzm - Correlation Recorder Test Plan Template installed");
  }

  private static void installSiebelCorrelationTemplate(String rootFolder) {
    TestPlanTemplatesRepository templateRepository = new TestPlanTemplatesRepository(Paths
        .get(rootFolder,
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
            repository.setEndPoint(getRepositoryURL(key));
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

    // Find repositories without manager for standard Remote File or URL repositories
    repositories.forEach(repo -> {
      // When pre-registered repository don't have a Manager,try to assign one based on the URL
      if (!repositoriesManagers.containsKey(repo.getName())) {
        RepositoryManager repository =
            getRepositoryManagerFromFolderOrUrl(repo.getName(), repo.getUrl());
        if (repository != null) {
          addRepositoryManager(repository);
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
    return repositoriesManagers.getOrDefault(name, null);
  }

  public RepositoryManager getRepositoryManager(String name) {
    if (!repositoriesManagers.containsKey(name)) {
      String url = getRepositoryURL(name);
      return getRepositoryManager(name, url);
    }
    return repositoriesManagers.getOrDefault(name, null);
  }

  public void createDefaultCorrelationTemplateFolder() {
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
    File localConfigurationFile = new File(getConfigurationFilePath());
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

  public Template readTemplateFromPath(String templatePath) throws IOException {
    return readValue(new File(templatePath), Template.class);
  }

  public Template readTemplateFromString(String templateContent) throws IOException {
    return readValue(templateContent, Template.class);
  }

  public TemplateProperties readTemplatePropertiesFromPath(String templatePropertiesPath)
      throws IOException {
    return readValue(new File(templatePropertiesPath), TemplateProperties.class);
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
    String localConfigurationPath = getConfigurationFilePath();

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

  private String getConfigurationFilePath() {
    return Paths.get(configurationFolder, LOCAL_CONFIGURATION_FILE_NAME + JSON_FILE_EXTENSION)
        .toAbsolutePath()
        .toString();
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

  public List<String> getRepositoriesNames() {
    return repositories.stream()
        .map(CorrelationTemplatesRepositoryConfiguration::getName)
        .collect(Collectors.toList());
  }


  /*
   * Returns the list of Repository's ID with their display name
   * */
  public Map<String, String> getRepositoriesDisplayName() {
    Map<String, String> repositoriesDisplayName = new HashMap<>();
    for (CorrelationTemplatesRepositoryConfiguration repository : repositories) {
      repositoriesDisplayName.put(repository.getName(), repository.getUrl());
    }
    repositoriesDisplayName.put(LOCAL_REPOSITORY_NAME, "Local");
    repositoriesDisplayName.put(CENTRAL_REPOSITORY_NAME, "GitHub's Central");
    repositoriesDisplayName.put(BZM_CENTRAL_REPOSITORY_NAME, "BlazeMeter's Central");
    return repositoriesDisplayName;
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
    } else {
      repositoryManager = getRepositoryManagerFromFolderOrUrl(repositoryId, repositoryURL);
      errors.addAll(repositoryManager.checkRepositoryURL(repositoryURL));
    }
    return errors;
  }

  public RepositoryManager getRepositoryManagerFromFolderOrUrl(String repositoryId,
                                                               String repositoryURL) {
    RepositoryManager manager;
    if (isURL(repositoryURL)) {
      manager = new RemoteUrlRepository();
    } else {
      manager = new RemoteFolderRepository();
    }
    manager.setName(repositoryId);
    manager.setEndPoint(repositoryURL);
    manager.setConfig(this);
    manager.init();
    return manager;
  }

  public static String getTemplateFilename(Template template) {
    return getTemplateFilename(template.getId(), template.getVersion());
  }

  public static String getTemplateFilename(String id, String version) {
    return id + "-" + version + RepositoryGeneralConst.TEMPLATE_FILE_SUFFIX;
  }

  public static String getTemplateSnapshotFilename(String id, String version) {
    return id + "-" + version + RepositoryGeneralConst.SNAPSHOT_FILE_SUFFIX;
  }

  public static String getTemplatePropertiesFilename(String id, String version) {
    return id + "-" + version + RepositoryGeneralConst.PROPERTIES_FILE_SUFFIX;
  }

  public File getRepositoryFolder(String repositoryName) {
    return new File(getRepositoryFolderPath(repositoryName));
  }

  public String getRepositoryFolderPath(String repositoryName) {
    String repositoryFolderPath = getRepositoryFolderName(repositoryName);
    return Paths.get(getCorrelationsTemplateInstallationFolder(), repositoryFolderPath)
        .toAbsolutePath() + File.separator;
  }

  public String getRepositoryFolderName(String name) {
    return name.equals(LOCAL_REPOSITORY_NAME) ? "" : name + File.separator;
  }

  public String getRepositoryFilePath(String repositoryName) {
    return Paths.get(getRepositoryFolderPath(repositoryName),
            RepositoryUtils.getRepositoryFileName(repositoryName))
        .toAbsolutePath()
        .toString();
  }

  public File getRepositoryFile(String repositoryName) {
    return new File(getRepositoryFilePath(repositoryName));
  }

  public String getTemplateFilePath(String repositoryName, String templateId,
                                    String templateVersion) {
    return Paths.get(getRepositoryFolderPath(repositoryName),
            getTemplateFilename(templateId, templateVersion))
        .toAbsolutePath()
        .toString();
  }

  public String getTemplateContentAsString(Template template) throws JsonProcessingException {
    return mapper.writeValueAsString(template);
  }

  public String getTemplatePropertiesFromFilepath(File templateFile) {
    return templateFile.getAbsolutePath()
        .replace(RepositoryGeneralConst.TEMPLATE_FILE_SUFFIX,
            RepositoryGeneralConst.PROPERTIES_FILE_SUFFIX);
  }

  public boolean isCloudStored(Template template) {
    RepositoryManager manager = getRepositoryManager(template.getRepositoryId());
    return isRemoteRepository(manager.getTemplateRegistry());
  }

  public boolean isRemoteRepository(Object templateRegistry) {
    return templateRegistry instanceof RemoteCorrelationTemplatesRepositoriesRegistry;
  }

  public void triggerCloudUpload(Template template) throws IOException {
    getRepositoryManager(template.getRepositoryId()).upload(template);
  }

  public Optional<Template> findById(String repository, String id, String version)
      throws IOException {
    String templateFilePath = getTemplateFilePath(repository, id, version);
    Template template = readTemplateFromPath(templateFilePath);
    template.setSnapshot(getSnapshot(template));
    return Optional.ofNullable(template);
  }

  public BufferedImage getSnapshot(Template loadedTemplate) {
    try {
      File snapshotFile = getSnapshotFile(loadedTemplate);
      if (!snapshotFile.exists()) {
        loadedTemplate.setSnapshotPath("");
        return null;
      }

      loadedTemplate.setSnapshotPath(snapshotFile.getAbsolutePath());
      return ImageIO.read(snapshotFile);
    } catch (IOException e) {
      LOG.warn("There was an issue trying to retrieve the image for the template '{}'",
          loadedTemplate.getId(), e);
      return null;
    }
  }

  public File getSnapshotFile(Template template) {
    return new File(getRepositoryFolderPath(template.getRepositoryId()),
        template.getSnapshotName());
  }

  public List<Template> getInstalledTemplates() {
    ArrayList<Template> loadedTemplates = new ArrayList<>();
    getRepositoriesWithInstalledTemplates()
        .forEach(r -> r.getInstalledTemplates().forEach((template, version) -> {
          String installedTemplateFile = getTemplateFilePath(r.getName(),
              template, version);

          try {
            Template loadedTemplate = readTemplateFromPath(installedTemplateFile);
            loadedTemplate.setRepositoryId(r.getName());
            loadedTemplate.setSnapshot(getSnapshot(loadedTemplate));
            loadedTemplates.add(loadedTemplate);
          } catch (IOException e) {
            LOG.warn("There was an error trying to get the CorrelationTemplate from the file {}",
                installedTemplateFile, e);
          }
        }));

    return loadedTemplates;
  }

  public void saveTemplate(Template template) throws IOException, ConfigurationException {
    if (isCloudStored(template)) {
      triggerCloudUpload(template);
    } else {
      writeFile(template);
      saveSnapshot(template);
      updateLocalRepository(template);
      installTemplate(template.getRepositoryId(), template.getId(), template.getVersion());
    }
  }

  private void writeFile(Template template) throws IOException {
    writeValue(new File(getFileNameForTemplate(template)), template);
  }

  public String getFileNameForTemplate(Template template) {
    return getTemplateFilePath(template.getRepositoryId(), template.getId(),
        template.getVersion());
  }

  private void saveSnapshot(Template template) throws IOException {
    File snapshotFile = getSnapshotFile(template);

    if (template.getSnapshot() != null) {
      writeValue(snapshotFile, template);
      ImageIO.write(template.getSnapshot(), RepositoryGeneralConst.SNAPSHOT_FILE_TYPE,
          snapshotFile);
    }
  }

  private void updateLocalRepository(Template template) {
    updateLocalRepository(template.getId(), template.getVersion());
  }

  public void updateLocalRepository(String templateId, String templateVersion) {
    File localRepositoryFile = getRepositoryFile(LOCAL_REPOSITORY_NAME);
    try {
      CorrelationTemplatesRepository localRepository;

      if (!localRepositoryFile.exists()) {
        localRepositoryFile.createNewFile();
        localRepository = new CorrelationTemplatesRepository();
        localRepository.setTemplates(new HashMap<String, CorrelationTemplateVersions>() {
        });
        writeValue(localRepositoryFile, localRepository.getTemplates());
        LOG.info("No local repository file found. Created a new one instead");
      } else {
        localRepository = new CorrelationTemplatesRepository("local",
            readTemplatesVersions(localRepositoryFile));
      }

      localRepository.addTemplate(templateId, templateVersion);
      writeValue(localRepositoryFile, localRepository.getTemplates());
      addRepository(LOCAL_REPOSITORY_NAME, localRepositoryFile.getAbsolutePath());
    } catch (IOException e) {
      LOG.warn("There was a problem trying to update the local repository file.", e);
    }
  }

  public void installTemplate(String repositoryName, String templateId, String templateVersion)
      throws ConfigurationException {
    File template = new File(getTemplateFilePath(repositoryName, templateId, templateVersion));
    if (!template.exists()) {
      LOG.error("The template {} doesn't exists", template.getName());
      throw new ConfigurationException(
          "The template " + template.getAbsolutePath() + " doesn't exists");
    }

    manageTemplate(LocalConfiguration.INSTALL, repositoryName, templateId, templateVersion);
  }

  public Map<String, CorrelationTemplateVersions> readTemplatesVersions(File source)
      throws IOException {
    return readTemplatesReferences(source);
  }

  public void uninstallTemplate(String repositoryName, String templateId, String templateVersion)
      throws ConfigurationException {
    manageTemplate(LocalConfiguration.UNINSTALL, repositoryName, templateId, templateVersion);
  }

  public boolean refreshRepositories(String configurationRoute,
                                     Consumer<Integer> setProgressConsumer,
                                     Consumer<String> setStatusConsumer) {
    int progress = 0;
    boolean isUpToDate = true;
    setProgressConsumer.accept(progress);

    AbstractMap<String, RepositoryManager> repositoriesManagers = getRepositoriesManagers();

    List<CorrelationTemplatesRepository> repositories = getRepositories();
    int total = 0;
    Iterator<String> managersIterator = repositoriesManagers.keySet().iterator();
    int progressAdvance = 0;
    while (managersIterator.hasNext()) {
      progressAdvance += 1;

      // Recalculate, can increase dynamically on each iteration
      total = repositoriesManagers.size() + repositories.size();

      progress = (progressAdvance * 100) / total;

      String managerId = managersIterator.next();
      RepositoryManager manager = repositoriesManagers.get(managerId);
      try {
        String repositoryName = getRepositoryName(manager);
        setStatusConsumer.accept("Syncing '" + repositoryName + "' repository.");
        setProgressConsumer.accept(progress);

        manager.setup();
      } catch (Exception e) {
        LOG.error("Unknown Exception setting up {} with implementation:{}",
            managerId, manager.getClass().getName(), e);
      }
    }

    // After setup, update the list of repositories and the total
    repositories = getRepositories();
    total = repositoriesManagers.size() + repositories.size();

    for (CorrelationTemplatesRepository repo : repositories) {
      progressAdvance += 1;
      progress = (progressAdvance * 100) / total;

      String repositoryDisplayName = getRepositoryName(repo);
      setStatusConsumer.accept("Updating '" + repositoryDisplayName + "' repository.");
      setProgressConsumer.accept(progress);

      // Exclude if is Local
      if (repo.getName().equals(LOCAL_REPOSITORY_NAME)) {
        continue;
      }

      String url = getRepositoryURL(repo.getName());
      String repositoryFilePath = getRepositoryFilePath(repo.getName());
      try {
        RepositoryManager repManager = getRepositoryManager(repo.getName(), url);
        if (repManager == null) {
          LOG.error("Internal error, repository without manager: {} ", repo.getName());
          continue;
        }
        //TODO: We need to rework this part, it's not working properly
        if (repManager.getRepository() == null) {
          try {
            if (DigestUtils.md5Hex(getInputStream(url))
                .equals(DigestUtils.md5Hex(getInputStream(repositoryFilePath)))) {
              continue;
            }
          } catch (IOException e) {
            LOG.error("Error while comparing MD5 url: {} localPath: {} ", url, repositoryFilePath,
                e);
            isUpToDate = false;
            continue;
          }
        } else {
          // Compute the difference with the object
          if (DigestUtils.md5Hex(getValueAsBytes(repManager.getTemplateVersions()))
              .equals(DigestUtils.md5Hex(getInputStream(repositoryFilePath)))) {
            continue;
          }
        }
        save(repo.getName(), url);
        isUpToDate = false;
      } catch (IOException e) {
        LOG.error("Error while comparing MD5 url: {} localPath: {} ", url, repositoryFilePath, e);
      }
    }
    progress = 100;
    setStatusConsumer.accept("Update finished");
    setProgressConsumer.accept(progress);

    return isUpToDate;
  }

  public List<CorrelationTemplatesRepository> getRepositories() {
    List<CorrelationTemplatesRepository> correlationRepositoryList = new ArrayList<>();
    List<String> repositoriesList = getRepositoriesNames();
    // We force the update of the proper Repository Display Name
    Map<String, String> repositoriesDisplayName = getRepositoriesDisplayName();

    repositoriesList.forEach(r -> {
      File repositoryFile = getRepositoryFile(r);
      String repoName = removeRepositoryNameFromFile(repositoryFile.getName());
      if (repositoryFile.exists()) {
        try {
          CorrelationTemplatesRepository loadedRepository =
              new CorrelationTemplatesRepository(repoName, readTemplatesVersions(repositoryFile));

          // refresh the version's protocol name (that currently is null)
          Map<String, CorrelationTemplateVersions> templates = loadedRepository.getTemplates();
          templates.forEach((key, value) -> value.setName(key));
          loadedRepository.setDisplayName(repositoriesDisplayName.get(repoName));
          correlationRepositoryList.add(loadedRepository);
        } catch (IOException e) {
          LOG.error("There was an issue trying to read the file {}.", repositoryFile.getName(), e);
        }
      } else {
        LOG.warn("Repository file not found {}.", repositoryFile);
      }
    });

    return correlationRepositoryList;
  }

  private static String getRepositoryName(RepositoryManager manager) {
    String repositoryName = manager.getDisplayName();
    if (repositoryName == null || repositoryName.isEmpty()) {
      repositoryName = manager.getName();
    }
    return repositoryName;
  }

  private static String getRepositoryName(CorrelationTemplatesRepository repo) {
    String repositoryDisplayName = repo.getDisplayName();
    if (repositoryDisplayName == null || repositoryDisplayName.isEmpty()) {
      repositoryDisplayName = repo.getName();
    }
    return repositoryDisplayName;
  }

  public static InputStream getInputStream(String path) throws IOException {
    return isURL(path) ? new URL(path).openStream() : new FileInputStream(path.replace("file://",
        ""));
  }

  public void save(String id, String url) throws IOException {
    getTemplateRegistry(id, url).save(id, url);
  }

  private CorrelationTemplatesRepositoriesRegistry getTemplateRegistry(String name, String url) {
    return ((CorrelationTemplatesRepositoriesRegistry) getRepositoryManager(name, url)
        .getTemplateRegistry());
  }
}
