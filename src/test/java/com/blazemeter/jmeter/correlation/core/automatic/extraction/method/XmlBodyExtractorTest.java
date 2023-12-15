package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import static com.mongodb.util.MyAsserts.assertTrue;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.XmlCorrelationExtractor;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.jmeter.samplers.SampleResult;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

@RunWith(MockitoJUnitRunner.class)
public class XmlBodyExtractorTest {
  private static final String SEARCH_VALUE = "sampleValue";
  private static final String NAME = "attribute";
  private static final String EMPTY_STRING = "";
  private static final String SIMPLE_XML_RESPONSE = "<root><data>TestData</data></root>";
  private static final String OTHER_XML_CONTENT =
      "<root><element attribute='otherValue'>Content</element></root>";
  private static final String INVALID_XML_CONTENT =
      "<root><element attribute='sampleValue' Content</element></root>";
  private XmlBodyExtractor extractor;

  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();

  @Mock
  private SampleResult mockSampleResult;
  @Before
  public void setUp() throws Exception {
    extractor = new XmlBodyExtractor();
  }

  @Test
  public void shouldReturnXPathWhenGetXmlPathWithValidXmlAndValueFound() {
    String xmlContent = "<root>\n" +
        "  <element attribute='sampleValue 1'>Content 1</element>\n" +
        "  <element attribute='sampleValue 2'>Content 2</element>\n" +
        "</root>";

    String result = XmlBodyExtractor.getXmlPath(xmlContent, SEARCH_VALUE, NAME);

    assertEquals(result, "/root/element/@attribute");
  }

  @Test
  public void shouldReturnEmptyStringWhenGetXmlPathWithValidXmlAndValueNotFound() {
    String result = XmlBodyExtractor.getXmlPath(OTHER_XML_CONTENT, SEARCH_VALUE, NAME);
    assertEquals(result, EMPTY_STRING);
  }

  @Test
  public void shouldReturnEmptyStringWhenGetXmlPathWithInvalidXml() {
    String searchValue = "sampleValue";
    String name = "attribute";

    String result = XmlBodyExtractor.getXmlPath(INVALID_XML_CONTENT, searchValue, name);

    assertEquals(result, EMPTY_STRING);
  }

  @Test
  public void shouldReturnXPathWhenGetXmlPathWithXmlStartingWithBOM() {
    String xmlWithBOM = "ï»¿<root><element attribute='sampleValue'>Content</element></root>";
    String result = extractor.getXmlPath(xmlWithBOM, SEARCH_VALUE, NAME);
    assertEquals(result, "/root/element/@attribute");
  }

  @Test
  public void shouldReturnEmptyStringWhenGetXmlPathWithNullOrEmptyXml() {
    String resultForNull = extractor.getXmlPath(null, SEARCH_VALUE, NAME);
    assertEquals(resultForNull, EMPTY_STRING);
  }

  @Test
  public void shouldReturnEmptyStringWhenGetXmlPathWithEmptyXml() {
    String nullXmlContent = EMPTY_STRING;
    String resultForNull = extractor.getXmlPath(nullXmlContent, SEARCH_VALUE, NAME);
    assertEquals(EMPTY_STRING, resultForNull);
  }

  @Test
  public void shouldReturnExtractorWhenGetCorrelationExtractorsWithMultipleIndexes() {
    when(mockSampleResult.getResponseDataAsString()).thenReturn(SIMPLE_XML_RESPONSE);

    List<CorrelationExtractor<?>>
        result = extractor.getCorrelationExtractors(mockSampleResult, "TestData", "data");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof XmlCorrelationExtractor);
  }

  @Test
  public void shouldReturnExtractorWhenGetCorrelationExtractorsWithSingleIndex() {
    when(mockSampleResult.getResponseDataAsString()).thenReturn(SIMPLE_XML_RESPONSE);

    List<CorrelationExtractor<?>> result =
        extractor.getCorrelationExtractors(mockSampleResult, "TestData", "data");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof XmlCorrelationExtractor);
  }

  @Test
  public void shouldReturnExtractorWhenGetCorrelationExtractorsWithNoIndexes() {
    when(mockSampleResult.getResponseDataAsString()).thenReturn(SIMPLE_XML_RESPONSE);

    List<CorrelationExtractor<?>> xmlBodyExtractorList =
        extractor.getCorrelationExtractors(mockSampleResult, "TestData", "data");

    softly.assertThat(xmlBodyExtractorList).isNotNull();
    softly.assertThat(xmlBodyExtractorList.size()).isEqualTo(1);
    CorrelationExtractor<?> correlationExtractor = xmlBodyExtractorList.get(0);
    softly.assertThat(correlationExtractor instanceof XmlCorrelationExtractor).isTrue();
    softly.assertThat(correlationExtractor.getParams())
        .isEqualTo(Arrays.asList("/root/data/#text", "1", "BODY"));
  }

  @Test
  public void shouldReturnMultipleXmlBodyExtractorsWhenGetCorrelationExtractorsWithMultipleIndexes()
      throws IOException {
    String xmlContent = TestUtils.getFileContent("/responses/xml/cleanResponse2.xml", getClass());
    when(mockSampleResult.getResponseDataAsString()).thenReturn(xmlContent);

    String value = "495.Atlas_UI_Resources.Atlas_Default.image1";
    String name = "$widgetId";
    List<CorrelationExtractor<?>> listPostProcessors = extractor
        .getCorrelationExtractors(mockSampleResult, value, name);

    List<Integer> expectedValueResponseIndexes =
        ExtractorGenerator
            .getIndexes(value, mockSampleResult.getResponseDataAsString());

    softly.assertThat(listPostProcessors).isNotNull();
    softly.assertThat(listPostProcessors.size()).isEqualTo(2);
    for (int i = 0; i < listPostProcessors.size(); i++) {
      CorrelationExtractor<?> extractor = listPostProcessors.get(i);
      softly.assertThat(extractor.getVariableName())
          .isEqualTo(name + "_" + expectedValueResponseIndexes.get(i));
    }
  }

  @Test
  public void shouldReturnXPathForValueAsTextNode()
      throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
    String xmlContent = "<root>\n" +
        "    <element attribute='valueToFind'>valueToFind</element>\n" +
        "    <sub>\n" +
        "        <element>valueToFind</element>\n" +
        "        <complex>\n" +
        "            <data>randomData</data>\n" +
        "            <info attribute='valueToFind'>More Info</info>\n" +
        "            <valueToFind>A node with name as the value</valueToFind>\n" +
        "        </complex>\n" +
        "    </sub>\n" +
        "    <valueToFind attribute='valueToFind'>valueToFind</valueToFind>\n" +
        "    <element>NotTheValue</element>\n" +
        "</root>";
    String value = "valueToFind";
    List<String> xPathsForValue = extractor.getXPathsForValue(xmlContent, value);
    softly.assertThat(xPathsForValue).isNotNull();
    softly.assertThat(xPathsForValue.size()).isEqualTo(6);
    softly.assertThat(xPathsForValue.contains("/root[1]/element[1]"));
  }
}