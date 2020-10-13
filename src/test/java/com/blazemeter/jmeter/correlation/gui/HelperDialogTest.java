package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.TestUtils;
import java.io.IOException;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;
import org.custommonkey.xmlunit.HTMLDocumentBuilder;
import org.custommonkey.xmlunit.TolerantSaxDocumentBuilder;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

public class HelperDialogTest {

  private HelperDialog helperDialog;

  @Before
  public void setup() {
    helperDialog = new HelperDialog(new JPanel());
  }
  @Test
  public void shouldDisplayDefaultValueWhenCreated() throws IOException {
    assertTemplateInfo("/DefaultExtractorHelper.html");
  }

  private void assertTemplateInfo(String templateInfoFile) throws IOException {
    CompareMatcher
        .isIdenticalTo(buildTestDocument(TestUtils.getFileContent(templateInfoFile, getClass())))
        .throwComparisonFailure()
        .matches(buildTestDocument(helperDialog.getDisplayedText()));
  }

  // we need to use this for comparison to avoid xml malformed (img without closing tag) nature of html
  private Document buildTestDocument(String html) {
    try {
      TolerantSaxDocumentBuilder tolerantSaxDocumentBuilder = new TolerantSaxDocumentBuilder(
          XMLUnit.newTestParser());
      HTMLDocumentBuilder htmlDocumentBuilder = new HTMLDocumentBuilder(tolerantSaxDocumentBuilder);
      return htmlDocumentBuilder.parse(html);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void shouldDisplayUpdatedDescriptionWhenUpdateDescription() throws IOException {
    helperDialog.updateDialogContent(TestUtils.getFileContent("/RegexCorrelationExtractorDescription.html", getClass()));
    assertTemplateInfo("/RegexCorrelationExtractorFullDisplay.html");
  }
}