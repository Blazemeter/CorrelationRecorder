package com.blazemeter.jmeter.correlation.core.automatic;

import static com.blazemeter.jmeter.correlation.core.automatic.extraction.method.XmlBodyExtractor.getXmlPath;

import com.blazemeter.jmeter.correlation.TestUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.extractor.XPathExtractor;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.Before;
import org.junit.Test;
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
}