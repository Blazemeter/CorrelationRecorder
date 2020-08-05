package com.blazemeter.jmeter.correlation.core.templates;

import static org.junit.Assert.assertEquals;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.ResultField;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate.Builder;
import com.blazemeter.jmeter.correlation.gui.CorrelationRulesTestElement;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalCorrelationTemplatesRegistryTest {

  private static final String TEMPLATES_FOLDER = "correlation-templates";
  private static final String CORRELATION_RULE_SERIALIZATION_PATH = "Siebel-CRM-1.0-template.json";
  private static final String TEMPLATE_WITH_DEPENDENCIES_PATH = "Siebel-CRM-2.0-template.json";
  private static final String TEMPLATE_ID = "Siebel-CRM";
  private static final ResultField DEFAULT_TARGET = ResultField.BODY;
  private static final String DEFAULT_TEMPLATE_VERSION = "1.0";
  private static final String TEMPLATE_WITH_DEPENDENCIES_VERSION = "2.0";
  private static final String TEMPLATE_REPOSITORY_OWNER_ID = "local";

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  @Mock
  private CorrelationEngine correlationEngine;
  private CorrelationProxyControl proxyControl;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    LocalConfiguration localConfiguration = new LocalConfiguration(folder.getRoot().getPath());
    LocalCorrelationTemplatesRegistry repository = new LocalCorrelationTemplatesRegistry(
        localConfiguration);
    proxyControl = new CorrelationProxyControl(localConfiguration.getRootFolder());
    proxyControl.setCorrelationEngine(correlationEngine);
    proxyControl.setCorrelationRules(buildCorrelationRules());
    proxyControl.setCorrelationTemplatesRegistry(repository);
  }

  @Test
  public void shouldSerializeCorrelationTemplateIntoJsonWhenOnSave()
      throws IOException, ConfigurationException {
    proxyControl.onSaveTemplate(getBuilderCorrelationTemplate());

    String expected = getFileContent("/" + CORRELATION_RULE_SERIALIZATION_PATH);
    String actual = TestUtils
        .readFile(getGeneratedJSON(CORRELATION_RULE_SERIALIZATION_PATH).getPath(),
            Charset.defaultCharset());
    compareWithoutSpaces(expected, actual);
  }

  private void compareWithoutSpaces(String expected, String actual) {
    assertEquals(expected.replaceAll("[\\n\\t ]", ""), actual.replaceAll("[\\n\\t ]", ""));
  }

  public String getFileContent(String filePath) {
    try {
      return Resources.toString(getClass().getResource(filePath), Charset.defaultCharset());
    } catch (IOException e) {
      return "";
    }
  }

  private File getGeneratedJSON(String fileNamePath) {
    return new File(
        folder.getRoot().getAbsolutePath() + "/" + TEMPLATES_FOLDER + "/"
            + fileNamePath);
  }

  private Builder getBuilderCorrelationTemplate() {
    return new Builder()
        .withDescription("This is a description")
        .withRepositoryId(TEMPLATE_REPOSITORY_OWNER_ID)
        .withVersion(DEFAULT_TEMPLATE_VERSION)
        .withChanges("")
        .withId(TEMPLATE_ID);
  }

  private List<CorrelationRule> buildCorrelationRules() {
    List<CorrelationRule> rules = new ArrayList<>();
    rules.add(
        new CorrelationRule("TestRuleOne",
            new RegexCorrelationExtractor("SWEACn=([a-z])", "1", "2",
                ResultField.RESPONSE_HEADERS.name()),
            new RegexCorrelationReplacement("SWEACn=([A-Z])")));
    rules.add(
        new CorrelationRule("TestRuleTwo",
            new RegexCorrelationExtractor("SWEACn=(ˆ[\\.\\.])", "5", "6",
                DEFAULT_TARGET.name()),
            new RegexCorrelationReplacement("SWEACn=([(\\d)])")));
    return rules;
  }

  @Test
  public void shouldNotGenerateJSONFileWithDefaultValuesWhenOnSave()
      throws IOException, ConfigurationException {
    proxyControl.onSaveTemplate(getBuilderCorrelationTemplate());
    String json = TestUtils
        .readFile(getGeneratedJSON(CORRELATION_RULE_SERIALIZATION_PATH).getPath(),
            Charset.defaultCharset());
    String replacementNode = json
        .substring(json.indexOf("replacement"), json.indexOf("}", json.indexOf("replacement")));
    assertEquals(-1, replacementNode.indexOf(DEFAULT_TARGET.name()));
  }

  @Test
  public void shouldDeserializeRulesFromJsonWhenOnLoadTemplate()
      throws IOException, ConfigurationException {
    proxyControl.onSaveTemplate(getBuilderCorrelationTemplate());
    CorrelationRulesTestElement testElement = buildCorrelationRulesTestElement();
    proxyControl.onLoadTemplate(TEMPLATE_REPOSITORY_OWNER_ID, TEMPLATE_ID,
        DEFAULT_TEMPLATE_VERSION);
    assertEquals(testElement.getRules(), proxyControl.getCorrelationRulesTestElement().getRules());
  }

  private CorrelationRulesTestElement buildCorrelationRulesTestElement() {
    return new CorrelationRulesTestElement(buildCorrelationRules().stream()
        .map(CorrelationRule::buildTestElement)
        .collect(Collectors.toList()));
  }

  @Test
  public void shouldGenerateJSONWhenOnSaveWithDependencies()
      throws IOException, ConfigurationException {
    CorrelationTemplateDependency dependency = new CorrelationTemplateDependency("Dependency1",
        TEMPLATE_WITH_DEPENDENCIES_VERSION, "URL");
    proxyControl.onSaveTemplate(
        getBuilderCorrelationTemplate().withDependencies(Collections.singletonList(dependency)));

    String expected = TestUtils.getFileContent("/" + TEMPLATE_WITH_DEPENDENCIES_PATH, getClass());
    String actual = TestUtils
        .readFile(getGeneratedJSON(CORRELATION_RULE_SERIALIZATION_PATH).getPath(),
            Charset.defaultCharset());

    compareWithoutSpaces(expected, actual);
  }
}
