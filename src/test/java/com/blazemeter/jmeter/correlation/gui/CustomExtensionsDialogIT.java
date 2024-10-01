package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.Mockito.doReturn;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.custom.extension.CustomCorrelationExtractor;
import com.blazemeter.jmeter.correlation.custom.extension.CustomCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.CustomExtensionsDialog.ExtensionItem;
import com.blazemeter.jmeter.correlation.gui.common.RulePartType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.xml.parsers.ParserConfigurationException;
import org.assertj.core.util.CheckReturnValue;
import org.assertj.swing.fixture.FrameFixture;
import org.custommonkey.xmlunit.HTMLDocumentBuilder;
import org.custommonkey.xmlunit.TolerantSaxDocumentBuilder;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

@RunWith(SwingTestRunner.class)
public class CustomExtensionsDialogIT {

  @Mock
  private Runnable updateComboOptions;
  @Mock
  private Function<Class<?>, List<String>> classFinderFunction;
  private CustomExtensionsDialog customExtensionsDialog;
  private CorrelationComponentsRegistry extensionsRegistry;

  private FrameFixture frame;

  @Before
  public void setUp() {
    extensionsRegistry = CorrelationComponentsRegistry.getNewInstance();
    extensionsRegistry.reset();
    List<String> availableExtensions = Arrays
        .asList(CustomCorrelationExtractor.class.getCanonicalName(),
            CustomCorrelationReplacement.class.getCanonicalName());
    doReturn(availableExtensions).when(classFinderFunction)
        .apply(CorrelationRulePartTestElement.class);
    extensionsRegistry.setClassFinderFunction(classFinderFunction);
    customExtensionsDialog = new CustomExtensionsDialog(updateComboOptions,
        new JPanel());
    frame = showInFrame(customExtensionsDialog.getContentPane());
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldDisplayCustomExtractorExtensionsWhenNoUsedExtensionsAndNoLoadedExtensions() {
    customExtensionsDialog.buildExtensions(new HashSet<>(), RulePartType.EXTRACTOR);
    assertThat(buildCustomExtractors()).isEqualTo(getAvailableExtensions());
  }

  private List<ExtensionItem> buildCustomExtractors() {
    return buildExpectedExtensionItemsList(
        Collections.singletonList(CustomCorrelationExtractor.class), false);
  }

  private List<ExtensionItem> getAvailableExtensions() {
    return getExtensionItems("availableExtensionList");
  }

  private List<ExtensionItem> getExtensionItems(String name) {
    ListModel<ExtensionItem> model = ((JList<ExtensionItem>) frame.list(name).target()).getModel();
    List<ExtensionItem> extensionItems = new ArrayList<>();

    for (int i = 0; i < model.getSize(); i++) {
      extensionItems.add(model.getElementAt(i));
    }
    return extensionItems;
  }

  @Test
  public void shouldDisplayCustomReplacementExtensionsWhenNoUsedExtensionsAndNoLoadedExtensions() {
    customExtensionsDialog.buildExtensions(new HashSet<>(), RulePartType.REPLACEMENT);
    assertThat(buildCustomReplacements(false)).isEqualTo(getAvailableExtensions());
  }

  private List<ExtensionItem> buildCustomReplacements(boolean active) {
    return buildExpectedExtensionItemsList(
        Collections.singletonList(CustomCorrelationReplacement.class),
        active);
  }

  @Test
  public void shouldDisplayOnlyNotLoadedReplacementsWhenNoUsedExtensionsAndLoadedReplacements() {
    customExtensionsDialog.buildExtensions(new HashSet<>(), RulePartType.REPLACEMENT);
    assertThat(buildExpectedExtensionItemsList(
        Collections.singletonList(CustomCorrelationReplacement.class), false))
        .isEqualTo(getAvailableExtensions());
  }

  @Test
  public void shouldNotDisplayExtensionsWhenNoUsedExtensionsAndAllAvailableExtensionsAreLoaded() {
    loadCustomExtensions(Arrays
        .asList(CustomCorrelationReplacement.class, CustomCorrelationExtractor.class));
    customExtensionsDialog.buildExtensions(new HashSet<>(), RulePartType.REPLACEMENT);
    assertThat(new ArrayList<>()).isEqualTo(getAvailableExtensions());
  }

  @Test
  public void shouldDisplayActiveExtensionsWhenLoadedExtensionsAreUsed() {
    Set<Class<? extends CorrelationRulePartTestElement>> usedExtensions = Stream
        .of(CustomCorrelationExtractor.class, CustomCorrelationReplacement.class)
        .collect(Collectors.toCollection(HashSet::new));

    loadCustomExtensions(Arrays
        .asList(CustomCorrelationExtractor.class, CustomCorrelationReplacement.class));
    customExtensionsDialog.buildExtensions(usedExtensions, RulePartType.REPLACEMENT);
    assertListsEquals(
        buildExpectedExtensionItemsList(Arrays.asList(CustomCorrelationExtractor.class,
                CustomCorrelationReplacement.class),
            true), getActiveExtensions());
  }

  private List<ExtensionItem> getActiveExtensions() {
    return getExtensionItems("activeExtensionList");
  }

  private void loadCustomExtensions(
      List<Class<? extends CorrelationRulePartTestElement<?>>> extensionsClasses) {
    for (Class<? extends CorrelationRulePartTestElement<?>> clazz : extensionsClasses) {
      extensionsRegistry.addCustomExtension(
          extensionsRegistry.buildRulePartFromClassName(clazz.getCanonicalName()));
    }
  }

  private List<ExtensionItem> buildExpectedExtensionItemsList(List<Class<?
      extends CorrelationRulePartTestElement<?>>> extensionsClasses, boolean... active) {
    List<ExtensionItem> extensionItems = new ArrayList<>();
    int position = 0;
    for (Class<? extends CorrelationRulePartTestElement<?>> clazz : extensionsClasses) {
      extensionItems.add(new ExtensionItem(
          extensionsRegistry.buildRulePartFromClassName(clazz.getCanonicalName()),
          active[position]));
      if (active.length > 1) {
        position++;
      }
    }
    return extensionItems;
  }

  @Test
  public void shouldDisplayUsedExtensionsAsActiveWhenSomeLoadedExtensionsAreUsed() {
    Set<Class<? extends CorrelationRulePartTestElement>> usedExtensions = Stream
        .of(CustomCorrelationReplacement.class)
        .collect(Collectors.toCollection(HashSet::new));

    loadCustomExtensions(Arrays
        .asList(CustomCorrelationExtractor.class, CustomCorrelationReplacement.class));
    customExtensionsDialog.buildExtensions(usedExtensions, RulePartType.REPLACEMENT);
    assertListsEquals(
        buildExpectedExtensionItemsList(Arrays.asList(CustomCorrelationExtractor.class,
                CustomCorrelationReplacement.class),
            true, true), getActiveExtensions());
  }

  //Created to avoid assertions failing because elements not being in the same order
  @CheckReturnValue
  private boolean assertListsEquals(List<ExtensionItem> expected, List<ExtensionItem> actual) {
    if (expected.size() != actual.size()) {
      return false;
    }

    for (int i = 0; i < expected.size(); i++) {
      ExtensionItem item = actual.get(i);
      if (!expected.contains(item)) {
        return false;
      }
    }

    return true;
  }

  @Test
  public void shouldAddAvailableExtensionWhenClickAddAndExtensionNotLoaded() {
    customExtensionsDialog.buildExtensions(new HashSet<>(), RulePartType.REPLACEMENT);
    List<ExtensionItem> originalActive = getActiveExtensions();
    selectAvailableExtensionByIndex(0);
    pressAddExtension();
    pressAddExtension();
    assertThat(originalActive).isNotEqualTo(getActiveExtensions());
  }

  private void selectAvailableExtensionByIndex(int index) {
    frame.list("availableExtensionList").selectItem(index);
  }

  private void pressAddExtension() {
    frame.button("addExtensionButton").target().doClick();
  }

  @Test
  public void shouldRemoveActiveExtensionWhenClickRemoveAndExtensionNotUsed() {
    customExtensionsDialog.buildExtensions(new HashSet<>(), RulePartType.REPLACEMENT);
    List<ExtensionItem> originalActive = getActiveExtensions();
    selectAvailableExtensionByIndex(0);
    pressAddExtension();
    assertThat(originalActive).isNotEqualTo(getActiveExtensions());
  }

  @Test
  public void shouldDisplayDescriptionWhenExtensionSelected() throws IOException {
    customExtensionsDialog.buildExtensions(new HashSet<>(), RulePartType.REPLACEMENT);
    List<ExtensionItem> originalActive = getActiveExtensions();
    selectAvailableExtensionByIndex(0);
    assertDescription("/correlation-descriptions/CustomCorrelationReplacement.html");
  }


  private void assertDescription(String templateInfoFile) throws IOException {
    CompareMatcher
        .isSimilarTo(buildTestDocument(
            TestUtils.getFileContent(templateInfoFile, getClass())))
        .throwComparisonFailure()
        .matches(buildTestDocument(frame.textBox("displayInfoPanel").text()));
  }

  // we need to use this for comparison to avoid xml malformed (img without closing tag) nature
  // of html
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
}
