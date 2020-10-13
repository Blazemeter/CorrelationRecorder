package com.blazemeter.jmeter.correlation.core.templates;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DescriptionContent {
  private static final Logger LOG = LoggerFactory.getLogger(DescriptionContent.class);

  public static String getFromClass(Class<?> thisClass) {
    String descriptionFilePath = String
        .format("/correlation-descriptions/%1$s.html", thisClass.getSimpleName());
    return getStringFromFile(thisClass, descriptionFilePath);
  }

  private static String getStringFromFile(Class<?> thisClass, String descriptionFilePath) {
    try (InputStream inputStream = thisClass.getResourceAsStream(descriptionFilePath)) {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.warn("Error trying to get '{}' file.", descriptionFilePath, e);
    }
    return "N/A";
  }

  public static String getFromName(Class<?> thisClass, String name) {
    String descriptionFilePath = String
        .format("/correlation-descriptions/%1$s.html", name);
    return getStringFromFile(thisClass, descriptionFilePath);
  }
  
}
