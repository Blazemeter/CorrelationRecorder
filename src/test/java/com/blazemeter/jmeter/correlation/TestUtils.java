package com.blazemeter.jmeter.correlation;

import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;

/**
 * The TestUtils class provides utility methods for handling files and
 * extracting data from TestElements.
 */
public class TestUtils {

  /**
   * This method converts a list of TestElements into a list of maps.
   * Each map represents a TestElement and contains its properties, excluding the
   * 'cacheKey' property.
   *
   * @param children a list of TestElements to convert.
   * @return a list of maps representing the TestElements.
   */
  public static List<Map<String, String>> comparableFrom(List<TestElement> children) {
    return children.stream()
        .map(te -> {
          Map<String, String> props = new HashMap<>();
          PropertyIterator it = te.propertyIterator();
          while (it.hasNext()) {
            JMeterProperty prop = it.next();
            if (!"cacheKey".equals(prop.getName())) {
              props.put(prop.getName(), prop.getStringValue());
            }
          }
          return props;
        })
        .collect(Collectors.toList());
  }

  /**
   * This method reads a file from a given path and returns its content as a string.
   *
   * @param path the path of the file to read.
   * @param encoding the encoding to use when reading the file.
   * @return the content of the file as a string.
   */
  public static String readFile(String path, Charset encoding) {
    try {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, encoding);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This method retrieves the content of a file located at a given path
   * relative to a given class.
   *
   * @param filePath the path of the file relative to the class.
   * @param testClass the class relative to which the file path is defined.
   * @return the content of the file as a string.
   */
  public static String getFileContent(String filePath, Class<?> testClass) throws IOException {
    return Resources.toString(testClass.getResource(filePath), Charset.defaultCharset());
  }

  /**
   * This method retrieves the absolute path of a file located at a given path
   * relative to a given class.
   *
   * @param filePath the path of the file relative to the class.
   * @param testClass the class relative to which the file path is defined.
   * @return the absolute path of the file.
   */
  public static String getFilePath(String filePath, Class<?> testClass) throws IOException {
    return new File(URLDecoder.decode(testClass.getResource(filePath).getFile(), "UTF-8")).getAbsolutePath();
  }

  /**
   * This method retrieves the absolute path of a folder located at a given path
   * relative to a given class.
   *
   * @param folderPath the path of the folder relative to the class.
   * @param testClass the class relative to which the folder path is defined.
   * @return the absolute path of the folder.
   */
  public static String getFolderPath(String folderPath, Class<?> testClass) throws IOException {
    return new File(URLDecoder.decode(testClass.getResource(folderPath).getFile(), "UTF-8")).getAbsolutePath();
  }

  /**
   * This method finds a file with a given name and returns it as a File object.
   *
   * @param file the name of the file to find.
   * @return a File object representing the found file.
   */
  public static File findTestFile(String file) throws UnsupportedEncodingException {
    URL resource = Resources.getResource(file);
    return new File(URLDecoder.decode(resource.getFile(), "UTF-8"));
  }

  /**
   * This method retrieves the absolute path of the directory containing a given class.
   *
   * @param clazz the class for which to retrieve the directory path.
   * @return the absolute path of the directory containing the class.
   */
  public static String getCleanPath(Class clazz) throws UnsupportedEncodingException {
    return new File(URLDecoder.decode(clazz
        .getProtectionDomain().getCodeSource()
        .getLocation().getFile(), "UTF-8"))
        .getAbsolutePath();
  }

  /**
   * This method copies a file from a source path to a destination path.
   *
   * @param source the path of the file to copy.
   * @param destination the path to which to copy the file.
   * @return a File object representing the copied file.
   */
  public static File copyFile(String source, String destination) throws IOException {
    Files.copy(Paths.get(source), Paths.get(destination));
    return new File(destination);
  }

  /**
   * This method retrieves a Template object from a file located at a given path.
   *
   * @param filePath the path of the file containing the template.
   * @return a Template object representing the template in the file.
   */
  public static Optional<Template> getTemplateFromFilePath(String filePath) {
    try {
      LocalConfiguration configuration = new LocalConfiguration();
      Optional<String> path = getFilePathFromResources(filePath, TestUtils.class);
      if (!path.isPresent()) {
        return Optional.empty();
      }
      Template value = configuration.readTemplateFromPath(path.get());
      return Optional.of(value);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * This method retrieves the absolute path of a file located at a given path
   * relative to a given class. The path is returned wrapped in an Optional.
   * If any exception occurs during the retrieval of the file path, an empty Optional is returned.
   *
   * @param filePath the path of the file relative to the class.
   * @param testClass the class relative to which the file path is defined.
   * @return an Optional containing the absolute path of the file if successful,
   * or an empty Optional if an exception occurred.
   */
  public static Optional<String> getFilePathFromResources(String filePath, Class<?> testClass) {
    try {
      String path = getFolderPath(filePath, testClass);
      return Optional.of(path);
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
