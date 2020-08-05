package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.TestUtils.findByName;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.assertj.swing.timing.Pause.pause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.gui.StringUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JProgressBarFixture;
import org.assertj.swing.timing.Condition;
import org.custommonkey.xmlunit.HTMLDocumentBuilder;
import org.custommonkey.xmlunit.TolerantSaxDocumentBuilder;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluator;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationTemplatesFrameIT {

  private static final String LOCAL_TEMPLATE_ID = "LocalTemplate (local)";
  private static final String EXTERNAL_TEMPLATE_ID = "ExternalTemplate (external)";

  private static final String INSTALLED_TEMPLATE_LIST = "installedTemplatesList";
  private static final String AVAILABLE_TEMPLATE_LIST = "availableTemplatesList";

  private static final String SEARCH_FIELD_NAME = "searchField";
  private static final String LOAD_BUTTON_NAME = "loadButton";
  private static final String EXPECTED_HTML_WITH_IMAGE_PATH = "/displayTemplateWithImage.txt";
  private static final String EXPECTED_HTML_WITHOUT_IMAGE_PATH = "/displayTemplateWithoutImage.txt";

  private static final long TIMEOUT_MILLIS = 3000;

  private static final String INSTALL_TEMPLATE_BUTTON_NAME = "installTemplate";
  private static final String TEMPLATE_VERSIONS_COMBOBOX = "templateVersions";
  private static final String FIRST_TEMPLATE_DESCRIPTION_FILE_PATH = "/firstTemplateDescription"
      + ".txt";
  private static final String VERSIONS_COMBO_NAME = "templateVersions";
  private static final String ID_LABEL_NAME = "templateIdLabel";
  private static final String SECOND_VERSION = "2.0";
  private static final String FIRST_VERSION = "1.0";

  private FrameFixture frame;
  private CorrelationTemplatesFrame correlationTemplatesFrame;
  private JTabbedPane templatesTab;
  private BufferedImage snapshot;

  @Mock
  private CorrelationTemplatesRegistryHandler templatesRegistryHandler;
  @Mock
  private CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistryHandler;
  @Mock
  private CorrelationTemplate firstVersionFirstTemplateLocalRepository;
  @Mock
  private CorrelationTemplate secondVersionFirstTemplateLocalRepository;
  @Mock
  private CorrelationTemplate firstVersionFirstTemplateExternalRepository;
  @Mock
  private CorrelationTemplatesRepository localRepository;
  @Mock
  private CorrelationTemplatesRepository externalRepository;
  @Mock
  private CorrelationTemplatesRepository alphaRepository;
  @Mock
  private Consumer<CorrelationTemplate> lastTemplateHandler;
  @Mock
  private CorrelationTemplatesRepositoryConfigFrame configFrame;

  @Before
  public void setup() {
    when(repositoriesRegistryHandler.getCorrelationRepositories())
        .thenReturn(Arrays.asList(alphaRepository, localRepository, externalRepository));

    setupRepository(localRepository, "local",
        Arrays.asList(firstVersionFirstTemplateLocalRepository,
            secondVersionFirstTemplateLocalRepository));
    setupRepository(alphaRepository, "external",
        Collections.singletonList(firstVersionFirstTemplateExternalRepository));

    correlationTemplatesFrame = new CorrelationTemplatesFrame(templatesRegistryHandler,
        repositoriesRegistryHandler, lastTemplateHandler);
    snapshot = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

    frame = showInFrame(correlationTemplatesFrame.getContentPane());
    templatesTab = (JTabbedPane) frame.robot().finder().findByName("templatesTabbedPane");
  }

  private void setupRepository(CorrelationTemplatesRepository repository, String name,
      List<CorrelationTemplate> templates) {
    when(repository.getName()).thenReturn(name);
    when(repositoriesRegistryHandler.getCorrelationTemplatesByRepositoryName(name))
        .thenReturn(templates);
    for (int i = 0; i < templates.size(); i++) {
      when(templates.get(i).getId()).thenReturn(StringUtils.capitalize(name) + "Template");
      when(templates.get(i).getDescription()).thenReturn("Description" + i);
      when(templates.get(i).getVersion()).thenReturn((i + 1) + ".0");
      when(templates.get(i).getSnapshotPath()).thenReturn("route/to/snapshot/" + (i + 1));
      when(templates.get(i).getRepositoryId()).thenReturn(name);
      when(templates.get(i).isInstalled()).thenReturn(i % 2 != 0);
    }
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldDisplayOnlyInstalledTemplatesWhenVisible() {
    buildPauseForCondition(
        () -> Arrays.equals(frame.list(INSTALLED_TEMPLATE_LIST).contents(),
            new String[]{LOCAL_TEMPLATE_ID}), "Display only templates with installed versions");
  }

  private void buildPauseForCondition(BooleanSupplier condition, String description) {
    pause(new Condition(description) {
      @Override
      public boolean test() {
        return condition.getAsBoolean();
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldDisplayTemplateIDWhenTemplateSelected() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    buildPauseForCondition(() -> frame.label(ID_LABEL_NAME).text()
            .equals(firstVersionFirstTemplateLocalRepository.getId()),
        "Display Template's ID when Local Template Selected");
  }

  private void selectInstalledTemplate(String templateDisplayName) {
    frame.list(INSTALLED_TEMPLATE_LIST).clickItem(templateDisplayName);
  }

  @Test
  public void shouldEnableLoadButtonWhenTemplateInstalled() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    buildPauseForCondition(() -> buttonIsEnabled(LOAD_BUTTON_NAME),
        "Load button is enabled when template is installed");
  }

  @Test
  public void shouldDisplayTemplateDescriptionWhenTemplateSelected() throws IOException {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);

    Diff myDiff = DiffBuilder
        .compare(TestUtils.getFileContent(FIRST_TEMPLATE_DESCRIPTION_FILE_PATH, getClass()))
        .withTest(correlationTemplatesFrame.getDisplayedText()).ignoreWhitespace()
        .checkForSimilar()
        .build();

    assertFalse(myDiff.toString(), myDiff.hasDifferences());
  }

  @Test
  public void shouldSetInstalledVersionWhenSelectTemplateWithInstalledVersion() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    buildPauseForCondition(
        () -> frame.comboBox(VERSIONS_COMBO_NAME).selectedItem().equals(SECOND_VERSION),
        "Load the Installed Version in the Version's ComboBox");
  }

  @Test
  public void shouldSetUninstallLabelWhenSelectTemplateWithInstalledVersion() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    buildPauseForCondition(() -> frame.button(INSTALL_TEMPLATE_BUTTON_NAME).text().equals(
        "Uninstall"), "Set Uninstall label when Selected version is installed");
  }

  @Test
  public void shouldEnableInstallButtonWhenTemplateNotInstalled() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    selectVersion(FIRST_VERSION);
    buildPauseForCondition(() -> buttonIsEnabled(INSTALL_TEMPLATE_BUTTON_NAME),
        "Install button is enabled when template is not installed");
  }

  private void selectVersion(String version) {
    frame.comboBox(VERSIONS_COMBO_NAME).selectItem(version);
  }

  @Test
  public void shouldDisableLoadButtonWhenTemplateNotInstalled() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    selectVersion(FIRST_VERSION);
    buildPauseForCondition(() -> !buttonIsEnabled(LOAD_BUTTON_NAME),
        "Disable load when selected Template is not installed");
  }

  @Test
  public void shouldSetTemplateIDOnLabelWhenTemplateSelected() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    assertEquals(firstVersionFirstTemplateLocalRepository.getVersion(),
        ((CorrelationTemplate) findByName(frame.robot(), TEMPLATE_VERSIONS_COMBOBOX,
            JComboBox.class).getModel()
            .getElementAt(0)).getVersion());
  }

  @Test
  public void shouldLoadWarningWhenInstallingVersionWithAnotherVersionInstalled() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    selectVersion(FIRST_VERSION);
    clickButton(INSTALL_TEMPLATE_BUTTON_NAME);

    JOptionPaneFinder.findOptionPane().using(frame.robot())
        .requireMessage(
            "There is another version of this Template installed. Want to overwrite it?")
        .noButton()
        .click();
  }

  private void clickButton(String buttonName) {
    frame.button(buttonName).click();
  }

  private boolean buttonIsEnabled(String buttonName) {
    return frame.button(buttonName).isEnabled();
  }

  private void setUpImage(CorrelationTemplate template) {
    when((template.getSnapshot())).thenReturn(snapshot);
  }

  private void clickLoadButton() {
    frame.button(LOAD_BUTTON_NAME).click();
  }

  @Test
  public void shouldShowErrorWhenOnLoadWithJsonException() throws IOException {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    doThrow(new IOException()).when(templatesRegistryHandler)
        .onLoadTemplate("local", "LocalTemplate", "2.0");
    clickLoadButton();
    frame.optionPane().requireMessage("Error while trying to load " + "LocalTemplate");
  }

  @Test
  public void shouldFilterTemplatesWithContainingText() {
    searchForSpecificText(localRepository.getName());
    buildPauseForCondition(() -> Arrays.equals(frame.list(INSTALLED_TEMPLATE_LIST).contents(),
        new String[]{LOCAL_TEMPLATE_ID}), "Filter templates by text");
  }

  private void searchForSpecificText(String str) {
    frame.textBox(SEARCH_FIELD_NAME).setText(str);
  }


  @Test
  public void shouldDisplayTemplateInfoWithoutImage() throws IOException {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);

    Diff myDiff = DiffBuilder
        .compare(TestUtils.getFileContent(EXPECTED_HTML_WITHOUT_IMAGE_PATH, getClass()))
        .withTest(correlationTemplatesFrame.getDisplayText()).ignoreWhitespace()
        .withDifferenceEvaluator(new IgnoreAttributeDifferenceEvaluator("src"))
        .checkForSimilar()
        .build();

    assertFalse(myDiff.toString(), myDiff.hasDifferences());
  }

  @Test
  public void shouldDisplayTemplateInfoWithImage()
      throws IOException, ParserConfigurationException, SAXException {
    setUpImage(secondVersionFirstTemplateLocalRepository);
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);

    Diff myDiff = DiffBuilder.compare(
        buildTestDocument(TestUtils.getFileContent(EXPECTED_HTML_WITH_IMAGE_PATH, getClass())))
        .withTest(buildTestDocument(correlationTemplatesFrame.getDisplayText())).ignoreWhitespace()
        .withDifferenceEvaluator(new IgnoreAttributeDifferenceEvaluator("src"))
        .checkForSimilar()
        .build();

    assertFalse(myDiff.toString(), myDiff.hasDifferences());
  }

  private Document buildTestDocument(String html)
      throws ParserConfigurationException, IOException, SAXException {
    TolerantSaxDocumentBuilder tolerantSaxDocumentBuilder = new TolerantSaxDocumentBuilder(
        XMLUnit.newTestParser());
    HTMLDocumentBuilder htmlDocumentBuilder = new HTMLDocumentBuilder(tolerantSaxDocumentBuilder);
    return htmlDocumentBuilder.parse(html);
  }

  @Test
  public void shouldDisplayOnlyUninstalledTemplatesWhenClickAvailableTab() {
    changeToAvailableTemplates();
    buildPauseForCondition(() -> Arrays.equals(frame.list(AVAILABLE_TEMPLATE_LIST).contents(),
        new String[]{EXTERNAL_TEMPLATE_ID}), "Display only uninstalled");
  }

  private void changeToAvailableTemplates() {
    templatesTab.setSelectedIndex(1);
  }

  @Test
  public void shouldMoveAvailableTemplateToInstallListWhenInstall() {
    changeToAvailableTemplates();
    selectAvailableTemplate(EXTERNAL_TEMPLATE_ID);
    clickButton(INSTALL_TEMPLATE_BUTTON_NAME);

    JOptionPaneFinder.findOptionPane().using(frame.robot())
        .requireMessage("The template was successfully installed")
        .okButton()
        .click();

    selectInstalledTemplate(EXTERNAL_TEMPLATE_ID);

    buildPauseForCondition(() -> frame.label(ID_LABEL_NAME).text()
            .equals(firstVersionFirstTemplateExternalRepository.getId()),
        "Display Template's ID when External Template Selected after installed");
  }

  private void selectAvailableTemplate(String templateDisplayName) {
    frame.list(AVAILABLE_TEMPLATE_LIST).clickItem(templateDisplayName);
  }

  @Test
  public void shouldMoveInstalledTemplateToAvailableListWhenUninstall() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    clickButton(INSTALL_TEMPLATE_BUTTON_NAME);

    JOptionPaneFinder.findOptionPane().using(frame.robot())
        .requireMessage("The template was successfully uninstalled")
        .okButton()
        .click();

    selectAvailableTemplate(LOCAL_TEMPLATE_ID);
    buildPauseForCondition(() -> frame.label(ID_LABEL_NAME).text()
            .equals(firstVersionFirstTemplateLocalRepository.getId()),
        "Display Template's ID when Local Template Selected after uninstalled");
  }

  //With this we do custom XMLs Tags comparison
  private static class IgnoreAttributeDifferenceEvaluator implements DifferenceEvaluator {

    private final String attributeName;

    private IgnoreAttributeDifferenceEvaluator(String attributeName) {
      this.attributeName = attributeName;
    }

    @Override
    public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
      if (outcome == ComparisonResult.EQUAL) {
        return outcome;
      }

      final Node controlNode = comparison.getControlDetails().getTarget();
      if (controlNode instanceof Attr) {
        Attr attr = (Attr) controlNode;
        if (attr.getName().equals(attributeName)) {
          return ComparisonResult.SIMILAR;
        }
      }
      return outcome;
    }
  }

  @Test
  public void shouldValidateLoadingDialogWhenRefreshButton() {
    setupForRefreshRepository(false);
    buildPauseForCondition(
        () -> frame.robot().finder().findByName("loadingDialog", true).isVisible(),
        "Waiting for loading dialog appears");

  }

  private void setupForRefreshRepository(boolean doCompleteFlow) {
    correlationTemplatesFrame.setConfigFrame(configFrame);
    correlationTemplatesFrame.setRefreshTask(buildRefreshTask(doCompleteFlow));
    clickButton("refreshButton");
  }

  private SwingWorker<Object, Object> buildRefreshTask(boolean doCompleteFlow) {
    return new SwingWorker<Object, Object>() {
      @Override
      protected Object doInBackground() throws Exception {
        Thread.sleep(1000);
        if (doCompleteFlow) {
          IntStream.range(1, 6)
              .forEach(i -> {
                try {
                  setProgress(i * 20);
                  Thread.sleep(250);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              });
        }
        return null;
      }
    };
  }

  @Test
  public void shouldIncrementProgressBarWhenRefreshRepositories() {
    setupForRefreshRepository(true);
    new JProgressBarFixture(frame.robot(),
        "loadingProgressBar")
        .waitUntilValueIs(80);

  }
}
