package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeNode;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.TreeCloner;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for the creation of the JMeter test plan.
 */
public class JMeterElementUtils {
  protected static final String URL_PARAM_SEPARATOR = "&";
  protected static final String URL_PARAM_VALUE_SEPARATOR = "=";
  private static final Logger LOG = LoggerFactory.getLogger(JMeterElementUtils.class);
  private Configuration configuration;

  public JMeterElementUtils() {
    this.configuration = new Configuration();
  }

  public JMeterElementUtils(Configuration configuration) {
    this.configuration = configuration;
  }

  public static List<HTTPSamplerBase> getHttpSamplers(HashTree testPlanTree) {
    List<TestElement> samplers = new LinkedList<>();
    extractHttpSamplers(testPlanTree, samplers);
    return samplers.stream()
        .map(HTTPSamplerBase.class::cast)
        .collect(Collectors.toList());
  }

  public static void extractHttpSamplers(HashTree tree, List<TestElement> samplerList) {
    extractElementsByCondition(tree, samplerList, item -> item instanceof HTTPSamplerBase);
  }

  private static void extractElementsByCondition(HashTree tree, List<TestElement> elementsList,
                                                 Predicate<TestElement> condition) {
    for (Object o : new LinkedList<>(tree.list())) {
      TestElement item = (TestElement) o;
      if (condition.test(item)) {
        elementsList.add(item);
      }
      extractElementsByCondition(tree.getTree(item), elementsList, condition);
    }
  }

  protected static void extractHeaderManagers(HashTree tree, List<TestElement> headerManagerList) {
    extractElementsByCondition(tree, headerManagerList, item -> item instanceof HeaderManager);
  }

  public static String getRecordingFilePath() {
    List<JMeterTreeNode> recordingNodeList = getTreeModel()
        .getNodesOfType(CorrelationProxyControl.class);

    if (recordingNodeList.isEmpty()) {
      LOG.warn("No recording node found");
      return null;
    }

    JMeterTreeNode recordingNode = getTreeModel()
        .getNodeOf(recordingNodeList.get(0).getTestElement());

    int childCount = recordingNode.getChildCount();
    if (childCount == 0) {
      LOG.warn("No recording node found");
      return null;
    }

    JMeterTreeNode child = (JMeterTreeNode) recordingNode.getChildAt(0);
    if (child.getTestElement() instanceof ResultCollector) {
      return ((ResultCollector) child.getTestElement()).getFilename();
    }

    return null;
  }

  public static JMeterTreeNode getNodeOf(TestElement element) {
    return getTreeModel().getNodeOf(element);
  }

  public static String saveTestPlanSnapshot() {
    String snapshotFilename = FileManagementUtils.getSnapshotFileName();
    try {
      HashTree snapshotTestPlan = getNormalizedTestPlan();
      SaveService.saveTree(snapshotTestPlan, Files.newOutputStream(Paths.get(snapshotFilename)));
      LOG.info("Test Plan's Snapshot saved to {}", snapshotFilename);
      return snapshotFilename;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return "";
  }

  public static String saveTestPlan(HashTree testPlan, String filename) {
    try {
      SaveService.saveTree(testPlan, Files.newOutputStream(Paths.get(filename)));
      LOG.info("Test Plan saved to {}", filename);
      return filename;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return "";
  }

  public static HashTree getNormalizedTestPlan() {
    return getNormalizedTestPlan(getTreeModel());
  }

  /**
   * Returns a normalized test plan tree, which allows to properly save it into a file.
   * Note: this method also removes all the disabled elements from the tree.
   *
   * @param model the JMeter tree model to be normalized.
   * @return the normalized test plan tree with the proper structure.
   */
  public static HashTree getNormalizedTestPlan(JMeterTreeModel model) {
    HashTree testPlan = model.getTestPlan();
    JMeter.convertSubTree(testPlan);
    return testPlan;
  }

  /**
   * Returns a JMeterTreeModel instance from a HashTree.
   *
   * @param testPlan the HashTree object to be converted.
   * @return a fully functional JMeterTreeModel instance.
   * @throws IllegalUserActionException when some elements of the JMeterTreeNode are not an
   *                                    AbstractConfigGui and no instance of TestPlan subTree.
   */
  public static JMeterTreeModel convertToTreeModel(HashTree testPlan)
      throws IllegalUserActionException {
    JMeterTreeModel model = new JMeterTreeModel();
    model.addSubTree(testPlan, null);
    return model;
  }

  private boolean isIgnoredParameter(String key) {
    return configuration.getIgnoredParameters().contains(key.toLowerCase().trim());
  }

  private boolean isIgnoredHeader(String key) {
    return configuration.getIgnoredHeaders().stream().anyMatch(key::equalsIgnoreCase);
  }

  static boolean isJson(String value) {
    try {
      new JSONObject(value);
    } catch (JSONException ex) {
      try {
        new JSONArray(value);
      } catch (JSONException exception) {
        return false;
      }
    }
    return true;
  }

  public static boolean isJsonObject(String value) {
    try {
      new JSONObject(value);
    } catch (JSONException ex) {
      return false;
    }
    return true;
  }

  public static boolean isJsonArray(String value) {
    try {
      new JSONArray(value);
    } catch (JSONException ex) {
      return false;
    }
    return true;
  }

  public static List<Pair<String, Object>> extractDataParametersFromJson(String body) {
    List<Pair<String, Object>> parameters = new ArrayList<>();
    boolean logDebug = false;
    try {
      Object o = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(body);
      if (o instanceof net.minidev.json.JSONObject) {
        net.minidev.json.JSONObject jsonObject = (net.minidev.json.JSONObject) o;
        Iterator<String> keys = jsonObject.keySet().iterator();
        while (keys.hasNext()) {
          String key = keys.next();
          Object objValue = jsonObject.get(key);
          if (objValue != null) {
            Object value;
            if (objValue instanceof String || objValue instanceof net.minidev.json.JSONObject) {
              value = objValue.toString();
            } else {
              value = objValue;
            }
            Pair<String, Object> parameter = Pair.of(key, value);
            parameters.add(parameter);
          }
        }
      } else if (o instanceof net.minidev.json.JSONArray) {
        net.minidev.json.JSONArray jsonArray = (net.minidev.json.JSONArray) o;
        int lenght = jsonArray.size();
        for (int i = 0; i < lenght; i++) {
          Object sub = jsonArray.get(i);
          if (sub instanceof net.minidev.json.JSONObject) {
            net.minidev.json.JSONObject jsonObject = (net.minidev.json.JSONObject) sub;
            Set<String> keys = jsonObject.keySet();
            keys.forEach(key -> {
              Object keyObj = jsonObject.get(key);
              Object value;
              if (keyObj instanceof String || keyObj instanceof net.minidev.json.JSONObject) {
                value = keyObj.toString();
              } else {
                value = keyObj;
              }
              Pair<String, Object> parameter = Pair.of(key, value);
              parameters.add(parameter);
            });
          } else {
            logDebug = true;
          }
        }
      }
    } catch (JSONException | ParseException e) {
      e.printStackTrace();
      logDebug = true;
    }
    if (logDebug) {
      LOG.debug("JSON Parsing error: {}", body);
    }
    return parameters;
  }

  //TODO: We need to improve this method to extract the parameters from a JSON
  protected void extractParametersFromJson(JSONObject json, Map<String,
      List<Appearances>> parameterMap, TestElement sampler, int level) {
    Iterator<String> keys = json.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      Object value = json.get(key);
      if (value instanceof Boolean
          && configuration.shouldIgnoreBooleanValues()) {
        continue;
      }

      if (value instanceof String) {
        String valueString = (String) value;
        if (canBeFiltered(key.toLowerCase(), valueString)) {
          continue;
        }

        if (isJsonObject(valueString)) {
          extractParametersFromJson(new JSONObject(valueString), parameterMap, sampler,
              level + 1);
          continue;
        }

        if (isJsonArray(valueString)) {
          extractParametersFromJsonArray(new JSONArray(valueString), parameterMap, sampler,
              level + 1, key);
          continue;
        }

        addToMap(parameterMap, key, valueString, sampler, "JSON");
        continue;
      }

      if (value instanceof JSONObject) {
        extractParametersFromJson(json.getJSONObject(key), parameterMap, sampler,
            level + 1);
      } else if (value instanceof JSONArray) {
        extractParametersFromJsonArray(json.getJSONArray(key), parameterMap, sampler,
            level + 1, key);
      } else {
        String stringValue = json.getString(key);
        if (isJson(stringValue)) {
          // If the value is a JSON, we need to parse it recursively
          JSONObject jsonObject;
          try {
            jsonObject = new JSONObject(stringValue);
          } catch (JSONException e) {
            LOG.error("Error parsing JSON: " + stringValue, e);
            continue;
          }

          extractParametersFromJson(jsonObject, parameterMap, sampler, level + 1);
        } else {
          addToMap(parameterMap, key, stringValue, sampler, "JSON");
        }
      }
    }
  }

  private void extractParametersFromJsonArray(JSONArray array, Map<String,
      List<Appearances>> parameterMap, TestElement sampler,
                                              int level, String key) {
    for (int i = 0; i < array.length(); i++) {
      Object item = array.get(i);
      if (item instanceof JSONObject) {
        extractParametersFromJson((JSONObject) item, parameterMap, sampler, level + 1);
      } else if (item instanceof JSONArray) {
        extractParametersFromJsonArray((JSONArray) item, parameterMap, sampler,
            level + 1, key);
      } else if (isJson(item.toString())) {
        extractParametersFromJson(new JSONObject(item.toString()), parameterMap, sampler,
            level + 1);
      } else if (item instanceof Boolean) {
        if (configuration.shouldIgnoreBooleanValues()) {
          continue;
        }
        addToMap(parameterMap, key, item.toString(), sampler, "JSON Array");
      } else if (item instanceof String) {
        String value = (String) item;
        if (value.isEmpty()) {
          continue;
        }

        if (configuration.shouldIgnoreBooleanValues()
            && (Boolean.TRUE.toString().equalsIgnoreCase(value)
            || Boolean.FALSE.toString().equalsIgnoreCase(value))) {
          continue;
        }
        addToMap(parameterMap, key, item.toString(), sampler, "JSON Array");
      }
    }
  }

  /**
   * Obtains the HTTPArgument from a JMeterProperty element.
   * Note: this method requires that the JMeterProperty is an instance of HTTPArgument.
   *
   * @param property the JMeterProperty element to be converted.
   * @return the HTTPArgument element.
   */
  public static HTTPArgument getHttpArgument(JMeterProperty property) {
    Object value = property.getObjectValue();
    try {
      return (HTTPArgument) value;
    } catch (ClassCastException e) {
      LOG.warn("{} cannot be cast to HTTPArgument", value.getClass().getName());
      return new HTTPArgument((Argument) value);
    }
  }

  public static List<Pair<String, String>> getHttpArguments(HTTPSamplerBase sampler) {
    List<Pair<String, String>> arguments = new ArrayList<>();
    for (JMeterProperty property : sampler.getArguments()) {
      HTTPArgument argument = getHttpArgument(property);
      String key = argument.getName();
      String value = argument.getValue();
      arguments.add(Pair.of(key, value));
    }
    return arguments;
  }

  /**
   * Returns the CollectionProperty associated to the HTTPSamplerProxy contained in a
   * JMeterTreeNode.
   * Note: this method requires that the JMeterTreeNode is a HTTPSamplerBase instance.
   *
   * @param requestElement the JMeterTreeNode element from which the collection will be
   *                       obtained.
   * @return the CollectionProperty associated to the HTTPSamplerProxy.
   */
  public static CollectionProperty getHttpArguments(JMeterTreeNode requestElement) {
    HTTPSamplerBase sampler = (HTTPSamplerBase) requestElement.getTestElement();
    return sampler.getArguments().getArguments();
  }

  protected void addToMap(Map<String, List<Appearances>> parametersMap, String key,
                          String value, TestElement sampler, String source) {
    // if the value length is smaller than the minimum length, we don't add it to the map
    if (value.length() < configuration.getMinLength()) {
      System.out.println("Is ignorable parameter '" + key + "' with value '" + value +
          "'. Reason: value length (" + value.length() + ") is smaller than the minimum length (" +
          configuration.getMinLength() + "). Source:" + source);
      return;
    }

    // Avoid comparing case-sensitive keys
    String cleanedKey = key.trim();
    // Add if we don't have the parameter yet
    List<Appearances> appearancesList = parametersMap.get(cleanedKey);
    Appearances appearance = new Appearances(value, cleanedKey, sampler);
    appearance.setSource(source);
    if (appearancesList == null || appearancesList.isEmpty()) {
      appearancesList = new ArrayList<>();
    }

    for (Appearances appearances : appearancesList) {
      if (appearances.getValue().equals(value)) {
        // We need to be careful here: if the value appears more than once,
        // we need to add it, so we can
        // generate multivalued extractors
        Optional<TestElement> isRepeated = appearances.getList().stream()
            .filter(app -> app.getName().equals(sampler.getName()))
            .findFirst();

        if (isRepeated.isPresent()) {
          LOG.debug(
              "Value detected is already added, excluded: " + value + " key:" + key + " source:" +
                  source);
          return;
        }
      }
    }

    appearancesList.add(appearance);
    parametersMap.put(cleanedKey, appearancesList);
    LOG.debug("Value detected:" + value + " key:" + key + " source:" + source);

  }

  /**
   * Get the charset for the specified string. Default to UTF_8
   *
   * @param contentEncoding String name
   * @return Charset or UTF_8 charset
   */
  protected static Charset getCharset(String contentEncoding) {
    if (StringUtils.isBlank(contentEncoding)) {
      return StandardCharsets.UTF_8;
    }

    try {
      return Charset.forName(contentEncoding);
    } catch (IllegalArgumentException e) {
      return StandardCharsets.UTF_8;
    }
  }

  public static void addNode(JMeterTreeNode parent, JMeterTreeNode node) {
    try {
      JMeterTreeNode newNode = getTreeModel().addComponent(node.getTestElement(), parent);
      for (int i = 0; i < node.getChildCount(); i++) {
        addNode(newNode, (JMeterTreeNode) node.getChildAt(i));
      }
    } catch (IllegalUserActionException iuae) {
      LOG.error("Illegal user action while adding a tree node.", iuae); // $NON-NLS-1$
      JMeterUtils.reportErrorToUser(iuae.getMessage());
    }
  }

  private static JMeterTreeModel getTreeModel() {
    return GuiPackage.getInstance().getTreeModel();
  }

  /**
   * Adds a new PostProcessor to the JMeter's tree node.
   *
   * @param destNode      the node to add the PostProcessor to.
   * @param postProcessor the PostProcessor to add.
   * @param model         the JMeter's tree model.
   */
  public static void addPostProcessorToNode(JMeterTreeNode destNode, PostProcessor postProcessor,
                                            JMeterTreeModel model) {
    JMeterTreeNode postProcessorNode = new JMeterTreeNode();
    postProcessorNode.setUserObject(postProcessor);

    if (isNonGui()) {
      model.getNodeOf(destNode.getTestElement()).add(postProcessorNode);
      return;
    }

    JMeterTreeNode node = GuiPackage.getInstance().getNodeOf(destNode.getTestElement());
    JMeterElementUtils.addNode(node, postProcessorNode);
  }

  public static boolean isNonGui() {
    return GuiPackage.getInstance() == null;
  }

  public static boolean isExtractorRepeated(JMeterTreeNode node, String name) {
    for (int i = 0; i < node.getChildCount(); i++) {
      JMeterTreeNode childNode = (JMeterTreeNode) node.getChildAt(i);
      if (childNode.getTestElement() instanceof RegexExtractor) {
        RegexExtractor extractor = (RegexExtractor) childNode.getTestElement();
        if (extractor.getRefName().equals(name)) {
          return true;
        }
      }
    }
    return false;
  }

  public static void refreshJMeter() {
    GuiPackage.getInstance().getMainFrame().repaint();
  }

  public static HashTree getTestPlan(String path) {
    HashTree hashTree = new HashTree();
    try {
      File file = new File(path);
      if (file.exists()) {
        hashTree = SaveService.loadTree(file);
      }
    } catch (Exception e) {
      System.out.println("Error loading the JMX file " + path + " " + e.getMessage());
      LOG.error("Error loading the JMX file {}", path, e);
      e.printStackTrace();
      return null;
    }
    return hashTree;
  }

  public static void setupResultCollectors(TestElement proxy) {
    FileManagementUtils.makeRecordingFolder();
    FileManagementUtils.makeReplayResultsFolder();
    FileManagementUtils.makeHistoryFolder();
    JMeterTreeNode recorderNode = getTreeModel().getNodeOf(proxy);
    if (recorderNode == null) {
      LOG.warn("Couldn't find the node of the proxy");
      return;
    }

    Enumeration<?> children = recorderNode.children();
    while (children.hasMoreElements()) {
      JMeterTreeNode subNode = (JMeterTreeNode) children.nextElement();
      if (subNode.isEnabled()) {
        TestElement testElement = subNode.getTestElement();
        if (testElement instanceof ResultCollector) {
          ResultCollector resultCollector = (ResultCollector) testElement;
          if (resultCollector.getFilename().isEmpty()) {
            resultCollector.setFilename(FileManagementUtils.getRecordingResultFileName());
          }
          LOG.info("Recording's result from {} located at {}", resultCollector.getName(),
              resultCollector.getFilename());
        }
      }
    }
  }

  public static boolean areEquivalent(JMeterTreeNode request, SampleResult result) {
    if (request.getTestElement() instanceof HTTPSamplerBase && result instanceof HTTPSampleResult) {
      return (request.getName().equals(result.getSampleLabel())
          || getComparableString(request.getTestElement(), true).equals(
          getComparableString(result, true)));
    }
    return false;
  }

  public static boolean areEquivalentRequests(JMeterTreeNode request, TestElement requestUsed) {
    return request.getName().equals(requestUsed.getName())
        || getComparableString(request.getTestElement(), true)
        .equals(getComparableString(requestUsed, true));
  }

  public static boolean areEquivalentRequests(TestElement request, TestElement requestUsed) {
    return getComparableString(request, true)
        .equals(getComparableString(requestUsed, true));
  }

  private static String getComparableString(Object jmeterObject, boolean includeArguments) {
    if (jmeterObject instanceof HTTPSamplerBase) {
      HTTPSamplerBase sampler = (HTTPSamplerBase) jmeterObject;
      return sampler.getMethod() + " - " + sampler.getPath()
          + (includeArguments ? " - " + sampler.getArguments().getArgumentsAsMap().keySet() : "");
    }

    if (jmeterObject instanceof HTTPSampleResult) {
      HTTPSampleResult sampleResult = (HTTPSampleResult) jmeterObject;
      return sampleResult.getHTTPMethod() + " - " + sampleResult.getURL().getPath();
    }

    return "";
  }

  /**
   * Gets the label of the provided TestElements, separated by comma.
   *
   * @param elements The TestElements to get the label from.
   * @return The label of the provided TestElements, separated by comma.
   */
  public static String getTestElementsLabels(List<TestElement> elements) {
    return elements.stream()
        .map(TestElement::getName)
        .collect(Collectors.joining(", "));
  }

  /**
   * Gets the label of the provided SampleResult, separated by comma.
   *
   * @param sampleResults The SampleResults to get the label from.
   * @return The label of the provided SampleResults, separated by comma.
   */
  public static String getSampleResultLabels(List<SampleResult> sampleResults) {
    return sampleResults.stream()
        .map(SampleResult::getSampleLabel)
        .collect(Collectors.joining(", "));
  }

  public static String decode(String value) {
    try {
      return URLDecoder.decode(value, "UTF-8");
    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
      return "";
    }
  }

  public ReplayReport getReplayErrors(String originalRecordingFilepath,
                                      String originalTraceFilepath, CorrelationHistory history) {
    ReplayReport report = new ReplayReport();
    CustomResultCollector collector = replayTestPlan(originalRecordingFilepath);
    report.setCollector(collector);
    if (!collector.hasErrors()) {
      report.setSuccessful(true);
      history.addSuccessfulReplay(originalRecordingFilepath, collector.getFilename(), false);
      return report;
    }

    List<SampleResult> originalResults = new ResultFileParser(configuration)
        .loadFromFile(new File(originalTraceFilepath), false);

    List<SampleResult> originalErrors = originalResults.stream()
        .filter(result -> !result.isSuccessful())
        .collect(Collectors.toList());

    List<SampleResult> replayErrors = collector.getErrors();
    List<SampleResult> newErrors = replayErrors.stream()
        .filter(replayError -> originalErrors.stream()
            .noneMatch(originalError -> originalError.getSampleLabel()
                .equals(replayError.getSampleLabel())))
        .collect(Collectors.toList());

    if (newErrors.isEmpty()) {
      report.setSuccessful(true);
      history.addSuccessfulReplay(originalRecordingFilepath, collector.getFilename(), true);
    } else {
      report.setSuccessful(false);
      report.setReplayNewErrors(newErrors);
      history.addFailedReplay(originalRecordingFilepath, collector.getFilename());
    }

    return report;
  }

  /**
   * Replay the test plan from the given filepath.
   *
   * @param filepath The filepath of the test plan to replay.
   *                 The test plan must be a JMeter test plan.
   * @return The result collector that was used to collect the
   * results of the replay.
   */
  @VisibleForTesting
  public CustomResultCollector replayTestPlan(String filepath) {
    HashTree testPlan = JMeterElementUtils.getTestPlan(filepath);
    CustomResultCollector collector = buildResultCollector();
    HashTree preparedTestPlan = addCollector(collector, testPlan);
    Thread replayingTests = new Thread(() -> runTestPlan(preparedTestPlan));
    replayingTests.start();
    try {
      replayingTests.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return collector;
  }

  public static JDialog makeWaitingFrame(String message) {
    JDialog runDialog = new JDialog();
    runDialog.setTitle("Generating suggestions");
    runDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    runDialog.setResizable(false);
    // Add a progress bar
    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    runDialog.add(progressBar, BorderLayout.SOUTH);
    JLabel label = new JLabel("Performing analysis while executing",
        SwingConstants.CENTER);
    label.setBorder(new EmptyBorder(25, 50, 25, 50));
    runDialog.add(label);
    runDialog.pack();

    // runDialog.setSize(new Dimension(400, 200));
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    // calculate the new location of the window
    int x = (dim.width - runDialog.getSize().width) / 2;
    int y = (dim.height - runDialog.getSize().height) / 2;
    runDialog.setLocation(x, y);

    return runDialog;
  }

  private static void runTestPlan(CustomResultCollector collector) {
    HashTree currentTestPlan = addCollector(collector);
    NonGuiEngine engine = new NonGuiEngine();
    engine.configure(currentTestPlan);
    engine.runTest();
  }

  private static void runTestPlan(HashTree testPlan) {
    NonGuiEngine engine = new NonGuiEngine();
    engine.configure(testPlan);
    engine.runTest();
  }

  private static HashTree addCollector(CustomResultCollector collector) {
    HashTree currentTestPlan = getNormalizedTestPlan();
    TreeCloner cloner = cloneTree(currentTestPlan);
    ListedHashTree clonedTree = cloner.getClonedTree();
    clonedTree.add(clonedTree.getArray()[0], collector);
    return clonedTree;
  }

  private static HashTree addCollector(CustomResultCollector collector, HashTree testPlan) {
    TreeCloner cloner = cloneTree(testPlan);
    ListedHashTree clonedTree = cloner.getClonedTree();
    clonedTree.add(clonedTree.getArray()[0], collector);
    return clonedTree;
  }

  /*
   * Migrated from org.apache.jmeter.gui.action.Start.cloneTree, since it is not public.
   * We need to clone the tree to avoid modifying the original tree with the collector.
   */
  private static TreeCloner cloneTree(HashTree testTree) {
    TreeCloner cloner = new TreeCloner(false);
    testTree.traverse(cloner);
    return cloner;
  }

  public boolean isParameterized(String value) {
    //We consider that a value is parametrized if it contains a ${,
    // followed by a variable name with a #, followed by }
    boolean hasParamBreaks = value.contains("${") && value.contains("}");
    if (!hasParamBreaks) {
      return false;
    }

    int variableStart = value.indexOf("${");
    int variableEnd = value.indexOf("}");
    if (variableStart < variableEnd) {
      String variableName = value.substring(variableStart + 2, variableEnd);
      // We are only considering the variables that are ours (i.e. the ones that contain a #)
      return variableName.contains("#");
    }

    return false;
  }

  /**
   * This class allow to have more control over the running of the test plan.
   */
  private static class NonGuiEngine extends StandardJMeterEngine {
    @Override
    public void runTest() {
      Thread replayThread = new Thread(this, "StandardJMeterEngine");
      replayThread.start();
      try {
        replayThread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private static CustomResultCollector buildResultCollector() {
    CustomResultCollector collector = new CustomResultCollector();
    collector.setFilename(FileManagementUtils.getReplayResultFileName());
    return collector;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public boolean canBeFiltered(Supplier<Object> elementSupplier) {
    Object o = elementSupplier.get();
    if (o instanceof HTTPSamplerProxy) {
      HTTPSamplerProxy sampler = (HTTPSamplerProxy) o;
      return sampler.getPropertyAsString("HTTPSampler.domain") == null
          || ignoredDomain(sampler.getDomain())
          || ignoredFile(sampler.getPath());
    } else if (o instanceof HTTPSampleResult) {
      HTTPSampleResult result = (HTTPSampleResult) o;
      return ignoredDomain(result.getURL())
          || ignoredFile(result.getContentType());
    } else if (o instanceof HeaderManager) {
      // Question: Do we have to filter headers?
      // What kind of headers are we talking about?
      return false;
    } else if (o instanceof SampleResult) {
      SampleResult result = (SampleResult) o;
      return ignoredDomain(result.getURL())
          || ignoredFile(result.getContentType());
    }
    return false;
  }

  protected boolean canBeFiltered(String key, String value) {
    if (value == null) {
      return true;
    }

    boolean emptyKey = key.isEmpty();
    if (emptyKey && value.isEmpty()) {
      return true;
    }

    if (!emptyKey && isIgnoredParameter(key)) {
      LOG.trace(" Is ignorable parameter '" + key + "' with value '" + value + "'");
      return true;
    }

    if (!emptyKey && isIgnoredHeader(key)) {
      LOG.trace(" Is ignorable header '" + key + "' with value '" + value + "'");
      return true;
    }

    return configuration.shouldIgnoreBooleanValues()
        && ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value));
  }

  private boolean ignoredDomain(URL samplerDomain) {
    if (samplerDomain == null) {
      return true;
    }

    String domainString = samplerDomain.getHost();
    for (String domain : configuration.getIgnoredDomains()) {
      if (domainString.contains(domain)) {
        System.out.println(" Ignoring domain '" + domainString + "'");
        return true;
      }
    }
    return false;
  }

  private boolean ignoredDomain(String samplerDomain) {
    if (samplerDomain == null) {
      return true;
    }

    for (String domain : configuration.getIgnoredDomains()) {
      if (samplerDomain.contains(domain)) {
        return true;
      }
    }
    return false;
  }

  private boolean ignoredFile(String resourcePath) {
    for (String file : configuration.getIgnoredFiles()) {
      if (resourcePath.contains(file)) {
        return true;
      }
    }
    return false;
  }

  protected boolean shouldFilter() {
    // If there is any ignored file, domain or parameter, we should filter
    return !configuration.getIgnoredFiles().isEmpty()
        || !configuration.getIgnoredDomains().isEmpty()
        || !configuration.getIgnoredParameters().isEmpty()
        || configuration.shouldIgnoreBooleanValues();
  }

  public static List<HTTPSamplerProxy> getCurrentSamplerList() {
    List<HTTPSamplerProxy> samplerProxies = new ArrayList<>();
    for (JMeterTreeNode samplerProxyNode : getCurrentSamplerNodes()) {
      samplerProxies.add((HTTPSamplerProxy) samplerProxyNode.getTestElement());
    }
    return samplerProxies;
  }

  public static List<JMeterTreeNode> getCurrentSamplerNodes() {
    return GuiPackage.getInstance().getTreeModel().getNodesOfType(HTTPSamplerProxy.class);
  }

  public static List<SampleResult> getCurrentSampleResults(String path) {
    List<SampleResult> sampleResults = new ResultFileParser(new Configuration())
        .loadFromFile(new File(path), true);
    List<SampleResult> filteredResults = new ArrayList<>();
    List<HTTPSamplerProxy> desiredSamplers = JMeterElementUtils.getCurrentSamplerList();
    for (SampleResult sampleResult : sampleResults) {
      for (HTTPSamplerProxy samplerProxy : desiredSamplers) {
        if (sampleResult.getSampleLabel().equals(samplerProxy.getName())) {
          filteredResults.add(sampleResult);
        }
      }
    }

    return filteredResults;
  }

  public static String getRecordingResultFileName() {

    JMeterTreeModel model = GuiPackage.getInstance().getTreeModel();
    Object root = model.getRoot();
    if (!(root instanceof TreeNode)) {
      System.out.println("Root is not a TreeNode");
      return "";
    }

    CorrelationProxyControl proxyControl = getProxyControl(model);

    JMeterTreeNode recorderNode = getTreeModel().getNodeOf(proxyControl);
    if (recorderNode == null) {
      LOG.warn("Couldn't find the node of the proxy");
      return "";
    }

    Enumeration<?> children = recorderNode.children();
    while (children.hasMoreElements()) {
      JMeterTreeNode subNode = (JMeterTreeNode) children.nextElement();
      if (subNode.isEnabled()) {
        TestElement testElement = subNode.getTestElement();
        if (testElement instanceof ResultCollector) {
          ResultCollector resultCollector = (ResultCollector) testElement;
          if (!resultCollector.getFilename().isEmpty()) {
            return resultCollector.getFilename();
          }
          LOG.info("Recording's result from {} located at {}", resultCollector.getName(),
              resultCollector.getFilename());
        }
      }
    }
    return "";
  }

  private static CorrelationProxyControl getProxyControl(JMeterTreeModel model) {
    List<JMeterTreeNode> proxyNodes = getCorrelationProxyControllers(model);
    if (proxyNodes.isEmpty()) {
      LOG.error("No Correlation Proxy Controller found in the Test Plan.");
      return null;
    }

    Optional<JMeterTreeNode> enabledProxy = proxyNodes.stream()
        .filter(JMeterTreeNode::isEnabled).findFirst();
    if (enabledProxy.isPresent()) {
      return (CorrelationProxyControl) enabledProxy.get().getTestElement();
    }
    return (CorrelationProxyControl) proxyNodes.get(0).getTestElement();
  }

  private static List<JMeterTreeNode> getCorrelationProxyControllers(JMeterTreeModel treeModel) {
    return treeModel.getNodesOfType(CorrelationProxyControl.class);
  }
}
