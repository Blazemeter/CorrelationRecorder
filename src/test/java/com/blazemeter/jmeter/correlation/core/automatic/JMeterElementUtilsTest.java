package com.blazemeter.jmeter.correlation.core.automatic;

import static com.blazemeter.jmeter.correlation.JMeterTestUtils.setupUpdatedJMeter;
import static com.blazemeter.jmeter.correlation.TestUtils.findTestFile;
import static com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils.convertSubTree;
import static com.blazemeter.jmeter.correlation.core.automatic.extraction.method.XmlBodyExtractor.getXmlPath;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.TestUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.extractor.XPathExtractor;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeListener;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.config.gui.HttpDefaultsGui;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.testelement.WorkBench;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JMeterElementUtilsTest extends TestCase {

  private XPathExtractor extractor;
  //  private XPath2Extractor extractor2;
  private SampleResult result;
  private String data;
  private JMeterVariables vars;
  private JMeterContext jmctx;
  private static final String VAL_NAME = "value";
  private static final String VAL_NAME_NR = "value_matchNr";
  private static final String TEST_ELEMENT_CLASS = "org.apache.jmeter.testelement";
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setup() throws UnsupportedEncodingException {
    jmctx = JMeterContextService.getContext();
    extractor = new XPathExtractor();
    extractor.setThreadContext(jmctx);// This would be done by the run command
    extractor.setRefName(VAL_NAME);
    extractor.setDefaultValue("Default");
    result = new SampleResult();
    data =
        "<book><preface title='Intro'>zero</preface><page>one</page><page>two</page><empty></empty><a><b></b></a></book>";
    result.setResponseData(data.getBytes("UTF-8"));
    vars = new JMeterVariables();

    jmctx.setVariables(vars);
    jmctx.setPreviousResult(result);
  }

  @Test
  public void shouldGetXmlPath() throws IOException {
    String xmlDirtyResponse =
        TestUtils.getFileContent("/responses/xml/dirtyResponse.xml", getClass());
    String paramValue = "wqsJ9wyF8EqViOs9vo7zIg";
    String paramName = "queryId";
    String xmlPath = getXmlPath(xmlDirtyResponse, paramName, paramValue);
    String expectedXmlPath = "/m:page/m:templates/m:template/div/div/div/div/@data-mendix-props";
    assertEquals(expectedXmlPath, xmlPath);
  }

  @Test
  public void shouldGenerateXmlExtractorFromPath() {
    extractor.setXPathQuery("/book/preface");
    extractor.process();
    assertEquals("zero", vars.get(VAL_NAME));
    assertEquals("1", vars.get(VAL_NAME_NR));
    assertEquals("zero", vars.get(VAL_NAME + "_1"));
    assertNull(vars.get(VAL_NAME + "_2"));

    extractor.setXPathQuery("/book/page");
    extractor.process();
    assertEquals("one", vars.get(VAL_NAME));
    assertEquals("2", vars.get(VAL_NAME_NR));
    assertEquals("one", vars.get(VAL_NAME + "_1"));
    assertEquals("two", vars.get(VAL_NAME + "_2"));
    assertNull(vars.get(VAL_NAME + "_3"));

    // Test match 1
    extractor.setXPathQuery("/book/page");
    extractor.setMatchNumber(1);
    extractor.process();
    assertEquals("one", vars.get(VAL_NAME));
    assertEquals("1", vars.get(VAL_NAME_NR));
    assertEquals("one", vars.get(VAL_NAME + "_1"));
    assertNull(vars.get(VAL_NAME + "_2"));
    assertNull(vars.get(VAL_NAME + "_3"));

    // Test match Random
    extractor.setXPathQuery("/book/page");
    extractor.setMatchNumber(0);
    extractor.process();
    assertEquals("1", vars.get(VAL_NAME_NR));
    assertTrue(StringUtils.isNoneEmpty(vars.get(VAL_NAME)));
    assertTrue(StringUtils.isNoneEmpty(vars.get(VAL_NAME + "_1")));
    assertNull(vars.get(VAL_NAME + "_2"));
    assertNull(vars.get(VAL_NAME + "_3"));

    // Put back default value
    extractor.setMatchNumber(-1);

    extractor.setXPathQuery("/book/page[2]");
    extractor.process();
    assertEquals("two", vars.get(VAL_NAME));
    assertEquals("1", vars.get(VAL_NAME_NR));
    assertEquals("two", vars.get(VAL_NAME + "_1"));
    assertNull(vars.get(VAL_NAME + "_2"));
    assertNull(vars.get(VAL_NAME + "_3"));

    extractor.setXPathQuery("/book/index");
    extractor.process();
    assertEquals("Default", vars.get(VAL_NAME));
    assertEquals("0", vars.get(VAL_NAME_NR));
    assertNull(vars.get(VAL_NAME + "_1"));

    // Has child, but child is empty
    extractor.setXPathQuery("/book/a");
    extractor.process();
    assertEquals("Default", vars.get(VAL_NAME));
    assertEquals("1", vars.get(VAL_NAME_NR));
    assertNull(vars.get(VAL_NAME + "_1"));

    // Has no child
    extractor.setXPathQuery("/book/empty");
    extractor.process();
    assertEquals("Default", vars.get(VAL_NAME));
    assertEquals("1", vars.get(VAL_NAME_NR));
    assertNull(vars.get(VAL_NAME + "_1"));

    // No text
    extractor.setXPathQuery("//a");
    extractor.process();
    assertEquals("Default", vars.get(VAL_NAME));

    // No text all matches
    extractor.setXPathQuery("//a");
    extractor.process();
    extractor.setMatchNumber(-1);
    assertEquals("Default", vars.get(VAL_NAME));

    // No text match second
    extractor.setXPathQuery("//a");
    extractor.process();
    extractor.setMatchNumber(2);
    assertEquals("Default", vars.get(VAL_NAME));

    // No text match random
    extractor.setXPathQuery("//a");
    extractor.process();
    extractor.setMatchNumber(0);
    assertEquals("Default", vars.get(VAL_NAME));

    extractor.setMatchNumber(-1);
    // Test fragment
    extractor.setXPathQuery("/book/page[2]");
    extractor.setFragment(true);
    extractor.process();
    assertEquals("<page>two</page>", vars.get(VAL_NAME));
    // Now get its text
    extractor.setXPathQuery("/book/page[2]/text()");
    extractor.process();
    assertEquals("two", vars.get(VAL_NAME));

    // No text, but using fragment mode
    extractor.setXPathQuery("//a");
    extractor.process();
    assertEquals("<a><b/></a>", vars.get(VAL_NAME));
  }
  @Test
  public void convertSubTreeShouldChangeKeyClassToTestElement() throws IllegalUserActionException {

    // Create a new TestPlan
    TestPlan testPlan = new TestPlan("My Test Plan");

    // Create a ThreadGroup
    ThreadGroup threadGroup = new ThreadGroup();
    threadGroup.setName("My Thread Group");
    threadGroup.setNumThreads(1);
    threadGroup.setRampUp(1);
    threadGroup.setSamplerController(new LoopController());

    // Create an HTTPSampler
    HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
    httpSampler.setDomain("www.google.com");
    httpSampler.setPath("/");
    httpSampler.setMethod("GET");
    httpSampler.setName("HTTP Request");

    // Add the HTTPSampler to the ThreadGroup
    threadGroup.addTestElement(httpSampler);
    threadGroup.addIterationListener(new LoopController());

    // Add the ThreadGroup to the TestPlan
    testPlan.addTestElement(threadGroup);

    // Create a JMeterTreeModel and add the TestPlan to it
    JMeterTreeModel treeModel = new JMeterTreeModel();
    treeModel.addComponent(testPlan, (JMeterTreeNode) treeModel.getRoot());

    HashTree tree = treeModel.getTestPlan();

    convertSubTree(tree);

    checkTree(tree);

  }

  private void checkTree(HashTree tree) {
    for (Object o : new ArrayList<>(tree.list())) {
      assertTrue(o.getClass().getName().contains(TEST_ELEMENT_CLASS));
      checkTree(tree.getTree(o));
    }
  }
}