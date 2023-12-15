package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationRuleSerializationPropertyFilter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileManagementUtils {
  private static final Logger LOG = LoggerFactory.getLogger(FileManagementUtils.class);
  private static final String SNAPSHOT_FILENAME_PROPERTY_NAME
      = "correlation.snapshot.filename.format";
  private static final String REPLAY_FILENAME_PROPERTY_NAME = "correlation.replay.filename.format";
  private static final String RECORDING_FILENAME_PROPERTY_NAME
      = "correlation.record.filename.format";
  private static final String HISTORY_FILENAME_PROPERTY_NAME
      = "correlation.history.filename.format";
  private static final String RECORDING_FOLDER = "Recording";
  private static final String REPLAY_FOLDER = "Replay";
  private static final String HISTORY_FOLDER = "History";

  private transient ObjectMapper mapper;
  private transient ObjectWriter writer;

  public FileManagementUtils() {
    setJsonConfigurations();
  }

  public static void makeRecordingFolder() {
    makeFolderAtBin(RECORDING_FOLDER);
  }

  private static void makeFolderAtBin(String name) {
    File folder = getPathInBin(name).toFile();
    if (folder.mkdirs()) {
      LOG.info("Folder {} created", name);
    } else {
      LOG.info("Folder {} already exists", name);
    }
  }

  private static Path getPathInBin(String name) {
    return Paths.get(JMeterUtils.getJMeterBinDir(), name);
  }

  public static void makeReplayResultsFolder() {
    makeFolderAtBin(REPLAY_FOLDER);
  }

  /**
   * Returns the path to the snapshot file for the given test plan.
   *
   * @return the path to the snapshot file.
   */
  public static String getSnapshotFileName() {
    return applyFormat(JMeterUtils.getPropDefault(SNAPSHOT_FILENAME_PROPERTY_NAME,
        getPathInBin(RECORDING_FOLDER) + File.separator + "snapshot-%s.jmx"));
  }

  private static String applyFormat(String format) {
    return String.format(format, System.currentTimeMillis());
  }

  public static String getReplayResultFileName() {
    return applyFormat(JMeterUtils.getPropDefault(REPLAY_FILENAME_PROPERTY_NAME,
        getPathInBin(REPLAY_FOLDER) + File.separator + "replay-%s.jtl"));
  }

  public static String getRecordingResultFileName() {
    return applyFormat(JMeterUtils.getPropDefault(RECORDING_FILENAME_PROPERTY_NAME,
        getPathInBin(RECORDING_FOLDER) + File.separator + "recording-%s.jtl"));
  }

  public static void makeHistoryFolder() {
    makeFolderAtBin(HISTORY_FOLDER);
  }

  public String saveCorrelationHistoryFile(CorrelationHistory history, String filepath) {
    return saveObjectToFile(history, filepath.isEmpty()
        ? getHistoryFilenamePropertyName() : filepath);
  }

  public static String getHistoryFilenamePropertyName() {
    return applyFormat(JMeterUtils.getPropDefault(HISTORY_FILENAME_PROPERTY_NAME,
        getPathInBin(HISTORY_FOLDER) + File.separator + "history-%s.json"));
  }

  public String saveObjectToFile(Object object, String filepath) {
    File file = new File(filepath);
    try {
      if (!file.exists() && file.createNewFile()) {
        LOG.info("The local configuration file was created at {}", file);
      }
      writer.writeValue(file, object);
    } catch (IOException e) {
      LOG.warn("There was an error trying to save the configuration file {}.",
          file, e);
      return "";
    }
    return filepath;
  }

  private void setJsonConfigurations() {
    mapper = new ObjectMapper();
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
    FilterProvider filters = new SimpleFilterProvider()
        .addFilter(CorrelationRuleSerializationPropertyFilter.FILTER_ID,
            new CorrelationRuleSerializationPropertyFilter());
    mapper.setFilterProvider(filters);
    mapper.writerWithDefaultPrettyPrinter().with(filters);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    writer = mapper.writerWithDefaultPrettyPrinter().with(filters);
  }

  public CorrelationHistory loadCorrelationHistoryFile(String correlationHistoryPath) {
    return loadObjectFromFile(correlationHistoryPath, CorrelationHistory.class);
  }

  public <T> T loadObjectFromFile(String filePath, Class<T> clazz) {
    File file = new File(filePath);
    try {
      return mapper.readValue(file, clazz);
    } catch (IOException e) {
      LOG.warn("There was an error trying to load the configuration file {}.",
          file, e);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Stores the response in the given folder. The filename will be the current time in milliseconds.
   *
   * @param response The Response
   * @param folderPath The folder path
   * @return
   */
  public static String storeResponse(String response, String folderPath) {
    Path pathToFolder = Paths.get(folderPath);
    Path pathToResponse = pathToFolder.resolve(System.currentTimeMillis() + ".json");

    try (FileWriter file = new FileWriter(pathToResponse.toString())) {
      file.write(response);
    } catch (IOException e) {
      LOG.warn("There was an error trying to store the response in the folder {}.",
          folderPath, e);
      e.printStackTrace();
      return null;
    }

    return pathToResponse.toString();
  }

  /**
   * Store the object in the given folder with the given filename. Uses the JMeter SaveService to
   * store the object in XML format.
   * Note: This method uses the JMeter SaveService to store the object in XML format, you must
   * load the configuration for JMeter ensure that the SaveService is initialized.
   * This method is created for debugging purposes.
   * @param object The object to store
   * @param folderPath The folder where the object will be stored
   * @param fileName The name of the file where the object will be stored
   * @return The path to the file where the object was stored. Empty if there was an error.
   */
  public static Optional<String> saveObjectToXml(Object object, String folderPath,
                                                 String fileName) {
    Path pathToDestinationFile = Paths.get(folderPath).resolve(fileName);

    try {
      OutputStream outputStream = Files.newOutputStream(pathToDestinationFile.toAbsolutePath());
      System.out.println("Saving object to " + pathToDestinationFile.toString());
      SaveService.saveElement(object, outputStream);
      return Optional.of(pathToDestinationFile.toString());
    } catch (IOException e) {
      LOG.warn("There was an error trying to store the object in the folder {}.",
          folderPath, e);
    }
    return Optional.empty();
  }

  private String getTimeStamp() {
    return String.valueOf(System.currentTimeMillis());
  }

  /**
   * This method loads an object from an XML file located at the given path.
   * It uses the JMeter SaveService to load the object from the XML file.
   * If an IOException occurs while trying to load the object, it logs a warning and
   * returns an empty Optional.
   * This method is created for debugging purposes.
   *
   * @param path the path to the directory containing the XML file.
   * @param filename the name of the XML file.
   * @return an Optional containing the loaded object if successful, or an empty Optional if
   * an error occurred.
   */
  public static Optional<Object> loadObjectFromXmlPath(String path, String filename) {
    try {
      Path path1 = Paths.get(path).resolve(filename);
      InputStream inputStream = Files.newInputStream(path1);
      return Optional.ofNullable(SaveService.loadElement(inputStream));
    } catch (IOException e) {
      LOG.warn("There was an error trying to load the object from the file {}.",
          filename, e);
    }
    return Optional.empty();
  }
}
