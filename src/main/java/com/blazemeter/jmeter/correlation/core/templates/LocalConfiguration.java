package com.blazemeter.jmeter.correlation.core.templates;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalConfiguration {

  public static final String CENTRAL_REPOSITORY_ID = "central";
  //this value will change for real repository on SIP-170
  public static final String CENTRAL_REPOSITORY_URL = "https://raw.githubusercontent"
      + ".com/Blazemeter/CorrelationsRecorderTemplates/master/central/central-repository.json";
  public static final String CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER = "/correlation-templates/";
  public static final String INSTALL = "install";
  public static final String UNINSTALL = "uninstall";
  public static final String LOCAL_REPOSITORY_NAME = "local";
  protected static final String JSON_FILE_EXTENSION = ".json";
  protected static final String LOCAL_CONFIGURATION_FILE_NAME = "repositories";
  protected static final String REPOSITORY_NAME_SUFFIX = "-repository";
  protected static final String JAR_FILE_SUFFIX = ".jar";
  private static final Logger LOG = LoggerFactory.getLogger(LocalConfiguration.class);
  private static final String SIEBEL_TEMPLATE_NAME = "siebel";
  private static final String DEFAULT_SIEBEL_TEMPLATE_VERSION = "1.0";
  private transient String rootFolder;
  private transient String configurationFolder;

  private transient ObjectMapper mapper;
  private transient ObjectWriter writer;

  private List<CorrelationTemplatesRepositoryConfiguration> repositories = new ArrayList<>();

  //Constructor added to avoid issues with the serialization
  public LocalConfiguration() {

  }

  public LocalConfiguration(String jmeterRootFolder) {
    rootFolder = jmeterRootFolder;

    createDefaultCorrelationTemplateFolder();
    setJsonConfigurations();
    loadLocalConfigurationFile();

    try {
      createCentralRemoteRepository();
      createLocalRepository();
    } catch (IOException e) {
      LOG.info("There was a problem trying to create the local repository file. ", e);
    }

    if (!isSiebelInstalled()) {
      installSiebel();
    }
  }

  private void createCentralRemoteRepository() {
    addRepository(LocalConfiguration.CENTRAL_REPOSITORY_ID,
        LocalConfiguration.CENTRAL_REPOSITORY_URL);
  }

  private void createDefaultCorrelationTemplateFolder() {
    File directory = new File(rootFolder);
    if (!directory.exists() && directory.mkdir()) {
      LOG.debug("Created the root folder {}", rootFolder);
    }

    configurationFolder = rootFolder + CORRELATIONS_TEMPLATE_INSTALLATION_FOLDER;
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
        configurationFolder
            + LOCAL_CONFIGURATION_FILE_NAME + JSON_FILE_EXTENSION);
    if (localConfigurationFile.exists()) {
      try {
        repositories = readValue(localConfigurationFile, LocalConfiguration.class).repositories;
        LOG.info("Successfully loaded configuration file. ");
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

  public Map<String, CorrelationTemplateReference> readTemplatesReferences(File source)
      throws IOException {
    return mapper.readValue(source, new TypeReference<Map<String, CorrelationTemplateReference>>() {
    });
  }

  private boolean isSiebelInstalled() {
    return repositories.stream().anyMatch(
        r -> r.getName().equals(LOCAL_REPOSITORY_NAME) && r.getInstalledTemplates()
            .containsKey(SIEBEL_TEMPLATE_NAME));
  }

  private void installSiebel() {
    repositories.stream().filter(r -> r.getName().equals(LOCAL_REPOSITORY_NAME)).findFirst().get()
        .installTemplate(SIEBEL_TEMPLATE_NAME, DEFAULT_SIEBEL_TEMPLATE_VERSION);
    saveLocalConfiguration();
  }

  private void createLocalRepository() throws IOException {
    addRepository(LOCAL_REPOSITORY_NAME,
        configurationFolder + LOCAL_REPOSITORY_NAME
            + REPOSITORY_NAME_SUFFIX + JSON_FILE_EXTENSION);
    saveLocalRepository();
  }

  private void saveLocalRepository() throws IOException {
    File localRepositoryFile = new File(
        configurationFolder + LOCAL_REPOSITORY_NAME + REPOSITORY_NAME_SUFFIX + JSON_FILE_EXTENSION);
    if (!localRepositoryFile.exists() && localRepositoryFile.createNewFile()) {
      LOG.info("Created the local repository file {}", localRepositoryFile);
      writer.writeValue(localRepositoryFile, new HashMap<String, CorrelationTemplateReference>() {
        {
          put(SIEBEL_TEMPLATE_NAME,
              new CorrelationTemplateReference(DEFAULT_SIEBEL_TEMPLATE_VERSION));
        }
      });
      LOG.info("Saved local repository file");
    }
  }

  public void writeValue(File resultFile, Object value) throws IOException {
    writer.writeValue(resultFile, value);
  }

  public void addRepository(String name, String url) {
    if (repositories.stream().noneMatch(r -> r.getName().equals(name))) {
      repositories.add(new CorrelationTemplatesRepositoryConfiguration(name, url));
      saveLocalConfiguration();
    }
  }

  public void saveLocalConfiguration() {
    String localConfigurationPath =
        configurationFolder
            + LOCAL_CONFIGURATION_FILE_NAME
            + JSON_FILE_EXTENSION;

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

  public List<String> getRepositoriesNames() {
    return repositories.stream()
        .map(CorrelationTemplatesRepositoryConfiguration::getName)
        .collect(Collectors.toList());
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
    List<String> errors = new ArrayList<>();
    if (!repositoryURL.endsWith(".json")) {
      String error = "URL should lead to .json file";
      LOG.warn("There was an error on the repository {}'s URL={}. Error: {}",
          repositoryId, repositoryURL, error);
      errors.add(
          "- There was and error on the repository " + repositoryId + "'s URL " + repositoryURL +
              ".\n   Error: "
              + error);
      return errors;
    }
    try {
      URL parsedURL = new URL(repositoryURL.replace(" ", "/%20"));
      URLConnection uc = parsedURL.openConnection();
      if (uc instanceof HttpURLConnection) {
        HttpURLConnection huc = (HttpURLConnection) parsedURL.openConnection();
        //Just sending the headers to validate the URL exists
        huc.setRequestMethod("HEAD");
        int responseCode = huc.getResponseCode();
        if (HttpURLConnection.HTTP_OK != responseCode) {
          String error = huc.getResponseCode() + (huc.getResponseMessage().isEmpty() ? ""
              : ": " + huc.getResponseMessage());
          LOG.warn("There was an error trying to reach the repository {}'s URL={}. Error: {}",
              repositoryId, repositoryURL, error);
          errors.add(
              "- We couldn't reach " + repositoryId + "'s url " + repositoryURL + ".\n   Error: "
                  + error);
        }
      } else if (!Files.exists(Paths.get(repositoryURL.replace("file://", "")))) {
        String error = "File doesn't exist";
        LOG.warn("There was an error trying to reach the repository {}'s Path={}. Error: {}",
            repositoryId, repositoryURL, error);
        errors.add(
            "- We couldn't reach " + repositoryId + "'s Path " + repositoryURL + ".\n   Error: "
                + error);
      }
    } catch (IOException e) {
      if (Files.exists(Paths.get(repositoryURL))) {
        LOG.warn("There was and error parsing the repository {}'s Path={}.", repositoryId,
            repositoryURL, e);
        errors.add(
            "- We couldn't parse " + repositoryId + "'s Path " + repositoryURL + ".\n"
                + "   Path should start with protocol 'file://'\n"
                + "   Error: " + e.getMessage());
      } else {
        LOG.warn("There was and error parsing the repository {}'s URL={}.", repositoryId,
            repositoryURL, e);
        errors.add(
            "- We couldn't parse " + repositoryId + "'s url " + repositoryURL + ".\n"
                + "   Error: " + e.getMessage());
      }
    }
    return errors;
  }
}
