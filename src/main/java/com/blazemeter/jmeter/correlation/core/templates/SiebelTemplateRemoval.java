package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.gui.TestPlanTemplatesRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class was added in version 3.0. The sole propose of this class is to encapsulate the logic
 * that deletes the siebel template and testplan from an upgraded version of ACR. Since the
 * creation, the deprecation was added in order to warn deletion once considered everyone is
 * migrated to newer versions of ACR. It's possible to see code duplication or raw manipulation of
 * files. The idea is to add this removal modifying the less. Encapsulating everything here without
 * dependency, makes the deletion of this class easier in the future.
 */
@Deprecated
public class SiebelTemplateRemoval {

  private static final String SIEBEL_TESTPLAN_NAME = "siebel-template.jmx";
  private static final String SIEBEL_TEMPLATE_NAME = "siebel-1.0-template.json";
  private static final String CORRELATION_TEMPLATES_PATH = "/correlation-templates/";
  private static final String SIEBEL_TEMPLATE_MD5_VALUE = "e1de737f84e2081b26b51d04acecd71a";
  private static final Logger LOG = LoggerFactory.getLogger(SiebelTemplateRemoval.class);

  public static void delete(String rootFolder) {
    deleteTestPlan(rootFolder);
    try {
      deleteTemplate(rootFolder);
    } catch (IOException e) {
      LOG.debug("Failed to delete old siebel template", e);
    }
  }

  private static void deleteTestPlan(String rootFolder) {
    TestPlanTemplatesRepository templateRepository = new TestPlanTemplatesRepository(
        Paths.get(rootFolder, "/templates/").toAbsolutePath()
            + File.separator);
    templateRepository.removeDeprecatedTemplate(SIEBEL_TESTPLAN_NAME);
  }

  private static void deleteTemplate(String rootFolder) throws IOException {
    String acrTemplatePath = rootFolder + CORRELATION_TEMPLATES_PATH;
    if (!isSiebelTemplateAdded(acrTemplatePath)) {
      return;
    }
    deleteJsonTemplate(acrTemplatePath);
    removeSiebelTemplateFromLocalRepository(acrTemplatePath);
  }

  private static boolean isSiebelTemplateAdded(String acrTemplatePath)
      throws IOException {
    File siebelTemplate = new File(
        Paths.get(acrTemplatePath, SIEBEL_TEMPLATE_NAME).toAbsolutePath().toString());
    return siebelTemplate.exists() && DigestUtils
        .md5Hex(Files.newInputStream(Paths.get(siebelTemplate.getPath())))
        .equals(SIEBEL_TEMPLATE_MD5_VALUE);
  }

  private static void removeSiebelTemplateFromLocalRepository(String acrTemplatePath)
      throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    File localRepository = new File(acrTemplatePath + "local-repository.json");
    JsonNode rootNode = objectMapper.readTree(localRepository);
    ObjectNode rootObjectNode = (ObjectNode) rootNode;
    rootObjectNode.remove("siebel");
    objectMapper.writerWithDefaultPrettyPrinter().writeValue(localRepository, rootNode);
  }

  private static void deleteJsonTemplate(String acrTemplatePath) {
    File siebelTemplate = new File(acrTemplatePath, SIEBEL_TEMPLATE_NAME);
    if (siebelTemplate.delete()) {
      LOG.info("Siebel template was deleted successfully");
      return;
    }
    LOG.error("Siebel template couldn't be deleted. Which could lead into several issues on newer "
        + "versions of ACR. Change correlation-templates folder and files inside permissions. "
        + "Restart Jmeter");
  }

}
