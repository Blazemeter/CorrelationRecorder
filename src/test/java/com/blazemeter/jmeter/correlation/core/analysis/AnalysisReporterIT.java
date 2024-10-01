package com.blazemeter.jmeter.correlation.core.analysis;

import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.BaseCorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationEngine;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.automatic.ResultFileParser;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.gui.automatic.CorrelationSuggestionsPanel;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.collections.HashTree;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class AnalysisReporterIT {

  @Mock
  private CorrelationComponentsRegistry registry;
  private CorrelationEngine engine = new CorrelationEngine();
  private FrameFixture frame;
  private CorrelationSuggestionsPanel panel;
  private Analysis analysis;

  @Mock
  private ActionListener listener;

  @Before
  public void setup() throws IOException {
    JMeterTestUtils.setupUpdatedJMeter();
    engine = new CorrelationEngine();
    panel = new CorrelationSuggestionsPanel(null);
    frame = showInFrame(panel);
    when(registry.getContext(BaseCorrelationContext.class))
        .thenReturn(new BaseCorrelationContext());
    AnalysisReporter.startCollecting();
    AnalysisReporter.disableCorrelation();
  }

  @After
  public void tearDown() {
    frame.cleanUp();
    frame = null;
    if (AnalysisReporter.isCollecting()) {
      AnalysisReporter.stopCollecting();
    }
    AnalysisReporter.enableCorrelation();
  }

  /**
   * This test is using the logic tested in the following test.
   * Now we can safely test the Analysis to Suggestions logic.
   */
  @Test
  public void shouldGenerateMatchingSuggestions()
          throws IllegalUserActionException, InterruptedException, UnsupportedEncodingException {
    //Prepare the engine with the desired rules
    engine.setCorrelationRules(createWordpressRules(), registry);

    List<SampleResult> sampleResults = loadSampleResults();
    List<JMeterTreeNode> samplerNodes = getSamplerNodes();
    List<HTTPSamplerProxy> samplers = getSamplerProxies(samplerNodes);

    for (int i = 0; i < sampleResults.size(); i++) {
      List<TestElement> children = new ArrayList<>();
      JMeterTreeNode node = samplerNodes.get(i);
      for (int j = 0; j < node.getChildCount(); j++) {
        children.add((TestElement) ((JMeterTreeNode) node.getChildAt(j)).getUserObject());
      }
      engine.process(samplers.get(i), children, sampleResults.get(i), "");
    }

    List<CorrelationSuggestion> suggestions = AnalysisReporter.generateCorrelationSuggestions();
  }

  /**
   * This test is used a way to manually generate the recording.
   * We could replace the Wiremock server with this one as long as
   * we have a stable way to obtain the JMX (Requests) and the JTL (Responses).
   * Also, remember that, in this test, we are using both with matching
   * indexes (the first request and the first response are matched, and so on).
   */
  @Test
  public void shouldApplyRulesOverFiles() throws UnsupportedEncodingException {
    //Prepare the engine with the desired rules
    engine.setCorrelationRules(createWordpressRules(), registry);

    List<SampleResult> sampleResults = loadSampleResults();
    List<JMeterTreeNode> samplerNodes = getSamplerNodes();
    List<HTTPSamplerProxy> samplers = getSamplerProxies(samplerNodes);

    for (int i = 0; i < sampleResults.size(); i++) {
      List<TestElement> children = new ArrayList<>();
      JMeterTreeNode node = samplerNodes.get(i);
      for (int j = 0; j < node.getChildCount(); j++) {
        children.add((TestElement) ((JMeterTreeNode) node.getChildAt(j)).getUserObject());
      }
      engine.process(samplers.get(i), children, sampleResults.get(i), "");
    }

    System.out.println(AnalysisReporter.getReporter().getReportAsString());
  }

  private List<SampleResult> loadSampleResults() throws UnsupportedEncodingException {
    return new ResultFileParser(new Configuration())
        .loadFromFile(new File(getFilePath("/recordings/recordingTrace/recordingWithNonces.jtl")),
            true);
  }

  private String getFilePath(String filename) throws UnsupportedEncodingException {
    return URLDecoder.decode(getClass().getResource(filename).getPath(), "UTF-8");
  }

  private JMeterTreeModel loadJmxFile() throws IllegalUserActionException, UnsupportedEncodingException {
    HashTree hashTree = JMeterElementUtils
        .getTestPlan(getFilePath("/recordings/testplans/recordingWithNonces.jmx"));

    return JMeterElementUtils.convertToTreeModel(hashTree);
  }

  private List<JMeterTreeNode> getSamplerNodes() {
    try {
      return loadJmxFile().getNodesOfType(HTTPSamplerProxy.class);
    } catch (IllegalUserActionException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
    }
      return new ArrayList<>();
  }

  private List<HTTPSamplerProxy> getSamplerProxies(List<JMeterTreeNode> nodesOfType) {
    List<HTTPSamplerProxy> samplers = new ArrayList<>();
    for (JMeterTreeNode node : nodesOfType) {
      HTTPSamplerProxy element = (HTTPSamplerProxy) node.getTestElement();
      element.setRunningVersion(true);
      samplers.add(element);
    }
    return samplers;
  }

  private List<RulesGroup> createWordpressRules() {
    CorrelationRule rule = new CorrelationRule();
    String referenceName = "wpnonce";
    rule.setReferenceName(referenceName);
    RegexCorrelationExtractor extractor = new RegexCorrelationExtractor("wpnonce=(.+)'");
    extractor.setVariableName(referenceName);
    rule.setCorrelationExtractor(extractor);
    RegexCorrelationReplacement replacement =
        new RegexCorrelationReplacement("wpnonce=(.+)");
    replacement.setVariableName(referenceName);
    rule.setCorrelationReplacement(replacement);
    return createGroupWithRules(Collections.singletonList(rule));
  }

  private List<RulesGroup> createGroupWithRules(List<CorrelationRule> rules) {
    RulesGroup.Builder builder = new RulesGroup.Builder()
        .withRules(rules)
        .isEnabled(true);
    return Collections.singletonList(builder.build());
  }
}
