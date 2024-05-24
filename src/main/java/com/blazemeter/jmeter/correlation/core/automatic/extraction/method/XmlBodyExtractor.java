package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.automatic.ExtractorGenerator;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.XmlCorrelationExtractor;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.jmeter.extractor.XPathExtractor;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlBodyExtractor extends Extractor {

  private static final Logger LOG = LoggerFactory.getLogger(XmlBodyExtractor.class);

  public static String getXmlPath(String xmlContent, String searchValue, String name) {
    if (xmlContent == null || xmlContent.isEmpty()) {
      return "";
    }

    String usedValue = JMeterElementUtils.removeBomIfExistFromContent(xmlContent);

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(usedValue));
      Document parse = builder.parse(is);
      Node rootNode = parse.getDocumentElement();

      String xpath = findPathByValue(rootNode, searchValue, "/" + rootNode.getNodeName(), name);
      if (xpath != null) {
        return xpath;
      }
    } catch (Exception e) {
      LOG.error("Error while trying to find '" + searchValue + "': ", e);
    }
    return "";
  }

  public static String findPathByValue(Node node, String value, String currentPath, String name) {
    // If it's an element node, check the attributes
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element element = (Element) node;
      NamedNodeMap attributes = element.getAttributes();
      for (int i = 0; i < attributes.getLength(); i++) {
        Attr attr = (Attr) attributes.item(i);
        if (attr.getValue().contains(value) && attr.getValue().contains(name)) {
          String finalPath = currentPath + "/@" + attr.getName();
          if (LOG.isDebugEnabled()) {
            System.out.println(" - Value found: " + value);
            System.out.println(" - Value name: " + name);
            System.out.println(" - At path: " + finalPath);
            System.out.println(" - In Attribute: " + attr.getName());
            System.out.println(" - Attribute value: " + attr.getValue());
          }
          return finalPath; // Return the path with attribute name
        } else if (attr.getValue().contains(value) && attr.getName().contains(name)) {
          String finalPath = currentPath + "/@" + attr.getName();
          if (LOG.isDebugEnabled()) {
            System.out.println(" - Value found: " + value);
            System.out.println(" - Value name: " + name);
            System.out.println(" - At path: " + finalPath);
            System.out.println(" - In Attribute: " + attr.getName());
            System.out.println(" - Attribute value: " + attr.getValue());
          }
          return finalPath; // Return the path with attribute name
        }
      }
    }

    // If it's a text node and matches the value
    if (node.getNodeType() == Node.TEXT_NODE && node.getNodeValue().trim().equals(value)) {
      return currentPath;  // Return the path till its parent (as this is a text node)
    }

    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      String result = findPathByValue(child, value, currentPath + "/" + child.getNodeName(), name);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  @Override
  public List<String> extractValue(SampleResult response, String value) {
    return null;
  }

  @Override
  public List<PostProcessor> getPostProcessors(SampleResult response, String value, String name) {
    String xmlPath = getXmlPath(response.getResponseDataAsString(), value, name);

    XPathExtractor extractor = new XPathExtractor();
    extractor.setRefName(name);
    extractor.setXPathQuery(xmlPath);
    extractor.setDefaultValue("NOT_FOUND");
    extractor.setMatchNumber(1);
    extractor.setScopeVariable("xml");

    return Arrays.asList(extractor);
  }

  @Override
  public List<CorrelationExtractor<?>> getCorrelationExtractors(SampleResult response, String value,
      String name) {
    List<Integer> indexes =
        ExtractorGenerator.getIndexes(value, response.getResponseDataAsString());
    List<CorrelationExtractor<?>> extractors = new ArrayList<>();

    if (LOG.isDebugEnabled() && indexes.size() > 1) {
      System.out.println(
          "[" + getClass().getSimpleName() + "]" + ": " + name + ") " + indexes.size() + " found.");
    }

    String xmlPath = getXmlPath(response.getResponseDataAsString(), value, name);

    for (Integer index : indexes) {
      XmlCorrelationExtractor<?> extractor = new XmlCorrelationExtractor<>();
      extractor.setRefName(
          name + "_" + index);
      extractor.setXPathQuery(xmlPath);

      extractors.add(extractor);
    }

    return extractors;
  }

  private String getXPathForNode(Node node) {
    if (node.getNodeType() == Node.DOCUMENT_NODE) {
      // Base case: If it's the root XML document node, return an empty string.
      return "";
    }

    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
      Attr attr = (Attr) node;
      // Construct the XPath for the attribute's owner element and then append the attribute to it.
      return getXPathForNode(attr.getOwnerElement()) + "/@" + attr.getName();
    }

    Node parent = node.getParentNode();
    if (parent == null) {
      // This means it's a root element node.
      return "/" + node.getNodeName();
    }

    int position = getSiblingPosition(node);
    return getXPathForNode(parent) + "/" + node.getNodeName() + "[" + position + "]";
  }

  public List<String> getXPathsForValue(String xmlContent, String value)
      throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
    List<String> xpaths = new ArrayList<>();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(xmlContent));
    Document xmlDocument = builder.parse(is);

    // Search for element nodes containing the value.
    NodeList matchingNodes = evaluateXPath(xmlDocument, "//*[text()='" + value + "']");
    for (int i = 0; i < matchingNodes.getLength(); i++) {
      Node node = matchingNodes.item(i);
      xpaths.add(getXPathForNode(node)); // Get the unique XPath for the node.
    }

    // Search for attributes containing the value.
    NodeList matchingAttributes = evaluateXPath(xmlDocument, "//@*[.='" + value + "']");
    for (int i = 0; i < matchingAttributes.getLength(); i++) {
      Node attr = matchingAttributes.item(i);
      xpaths.add(getXPathForNode(attr)); // Get the unique XPath for the attribute.
    }

    return xpaths;
  }

  private NodeList evaluateXPath(Document document, String expression) throws
      XPathExpressionException {
    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath xpath = xpathFactory.newXPath();
    XPathExpression expr = xpath.compile(expression);
    return (NodeList) expr.evaluate(document, XPathConstants.NODESET);
  }

  private int getSiblingPosition(Node node) {
    NodeList siblings = node.getParentNode().getChildNodes();
    int index = 0;
    for (int i = 0; i < siblings.getLength(); i++) {
      Node sibling = siblings.item(i);
      if (sibling.getNodeName() == node.getNodeName()) {
        index += 1;
        if (sibling == node) {
          return index; // XPath is 1-based index
        }
      }
    }
    return -1; // Shouldn't reach here normally
  }
}
