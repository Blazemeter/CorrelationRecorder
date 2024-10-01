package com.blazemeter.jmeter.correlation.core.templates;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SiebelTemplateRemovalTest {


  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private File siebelJsonFile;
  private File localRepoFile;


  @Before
  public void setUp() throws Exception {
    File binDir = temporaryFolder.newFolder("bin");
    File templatesDir = new File(binDir, "templates");
    File correlationTemplatesDir = new File(binDir, "correlation-templates");
    templatesDir.mkdirs();
    correlationTemplatesDir.mkdirs();
    File siebelTemplateFile = new File(templatesDir, "siebel-template.jmx");
    siebelTemplateFile.createNewFile();
    siebelJsonFile = new File(correlationTemplatesDir, "siebel-1.0-template.json");
    siebelJsonFile.createNewFile();
    writeSiebelJsonTemplate(siebelJsonFile);

    localRepoFile = new File(correlationTemplatesDir, "local-repository.json");
    localRepoFile.createNewFile();
    writeLocalRepository(localRepoFile);
  }

  private void writeLocalRepository(File localRepoFile) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode rootNode = objectMapper.createObjectNode();
    ObjectNode siebelNode = objectMapper.createObjectNode();
    ArrayNode versionsNode = objectMapper.createArrayNode();

    versionsNode.add("1.0");
    siebelNode.put("repositoryDisplayName", "local");
    siebelNode.set("versions", versionsNode);
    rootNode.set("siebel", siebelNode);
    objectMapper.writerWithDefaultPrettyPrinter().writeValue(localRepoFile, rootNode);
  }

  private void writeSiebelJsonTemplate(File siebelJsonFile) throws IOException {
    try (InputStream resourceStream =
        getClass().getResourceAsStream("/templates/siebel-template-removal.json")) {
      if (resourceStream == null) {
        throw new IOException("Resource not found");
      }
      Path path = Paths.get(siebelJsonFile.toURI());
      Files.copy(resourceStream, path, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public static File findFile(File directory, String fileName) {
    if (directory.getName().equals(fileName)) {
      return directory;
    }
    File[] files = directory.listFiles();
    File ret = null;
    if (files != null) {
      for (File file : files) {
        ret = findFile(file, fileName);
      }
    }
    return ret;
  }

  public static boolean fileExists(File directory, String fileName) {
    return findFile(directory, fileName) != null;
  }

  @Test
  public void shouldRemoveSiebelTestPlanWhenDelete() throws Exception {
    SiebelTemplateRemoval.delete(temporaryFolder.getRoot().getAbsolutePath() + "/bin");
    assertFalse(fileExists(temporaryFolder.getRoot(), "siebel-template.jmx"));
  }

  @Test
  public void shouldDeleteJsonTemplateWhenDelete() throws Exception {
    SiebelTemplateRemoval.delete(temporaryFolder.getRoot().getAbsolutePath() + "/bin");
    assertFalse(fileExists(temporaryFolder.getRoot(), "siebel-1.0-template.json"));
  }

  @Test
  public void shouldRemoveSiebelFromLocalRepositoryWhenDelete() throws Exception {
    SiebelTemplateRemoval.delete(temporaryFolder.getRoot().getAbsolutePath() + "/bin");
    File file = findFile(temporaryFolder.getRoot(), "local-repository.json");
    List<String> lines = Files.readAllLines(file.toPath().toAbsolutePath());
    assertTrue(lines.size() == 1 && lines.get(0).equals("{ }"));
  }

}
