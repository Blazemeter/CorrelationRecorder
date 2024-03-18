package com.blazemeter.jmeter.correlation.core.automatic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import kg.apc.emulators.TestJMeterUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.collections.HashTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Use this class to obtain certain Samplers and Results from a JMX and JTL files.
 * Specially useful when you want to simulate some scenarios without the need of
 * opening JMeter GUI and making the removals manually.
 * <p>
 * Just like the NonGuiAcr class, you require to have the path to the files and
 * where you want to output them.
 */
@RunWith(MockitoJUnitRunner.class)
public class NonGuiFilesExtractor {
  //Remember to use the absolute path of the files.
  private static String jmxFilePath = "";
  private static String recordingTracePath = "";
  private static String finalTracePath = "";
  private static String finalJmxPath = "";

  @Before
  public void setUp() {
    TestJMeterUtils.createJmeterEnv();
  }

  /**
   * This method will use the list of desired elements to filter both the JMX and JTL files.
   */
  @Test
  public void shouldRemoveUndesiredElementsFromFiles()
      throws IllegalUserActionException, IOException {
    if (!hasNeededFiles(jmxFilePath, recordingTracePath, finalTracePath, finalJmxPath)) {
      return;
    }
    List<String> desiredElements = Arrays.asList("/wp-admin/-3",
        "/wp-admin/edit.php-9",
        "/wp-admin/post-new.php-11",
        "/wp-admin/edit.php-17",
        "/wp-login.php-21");
    String newTestPlanPath = removeUndesiredRequests(desiredElements);
    String newTracePath = removeUndesiredResponses(desiredElements);
    FileUtils.copyFile(new File(newTracePath), new File(finalTracePath));
  }

  private String removeUndesiredRequests(List<String> desiredElements)
      throws IllegalUserActionException {
    HashTree hashTree = JMeterElementUtils.getTestPlan(jmxFilePath);

    JMeterTreeModel model = JMeterElementUtils.convertToTreeModel(hashTree);
    List<JMeterTreeNode> undesiredNodes = model.getNodesOfType(HTTPSamplerProxy.class).stream()
        .filter(sampler -> !desiredElements.contains(sampler.getName()))
        .collect(Collectors.toList());

    for (JMeterTreeNode node : undesiredNodes) {
      hashTree.remove(node);
      model.removeNodeFromParent(node);
      node.getTestElement().removed();
    }

    HashTree modifiedTestPlan = model.getTestPlan();
    JMeter.convertSubTree(modifiedTestPlan);

    return JMeterElementUtils.saveTestPlan(modifiedTestPlan, finalJmxPath);
  }

  private String removeUndesiredResponses(List<String> desiredElements) {
    Configuration configuration = new Configuration();
    ResultFileParser parser = new ResultFileParser(configuration);
    List<SampleResult> results = parser
        .loadFromFile(new File(recordingTracePath), true);
    List<SampleResult> cleanedResults = new ArrayList<>();
    for (SampleResult result : results) {
      if (!desiredElements.contains(result.getSampleLabel())) {
        continue;
      }
      cleanedResults.add(result);
    }
    return ResultFileParser.saveToFile(cleanedResults);
  }

  protected boolean hasNeededFiles(String... files) {
    for (String file : files) {
      if (file == null) {
        return false;
      }

      if (file.isEmpty()) {
        return false;
      }

      if (!new File(file).exists()) {
        return false;
      }
    }
    return true;
  }
}
