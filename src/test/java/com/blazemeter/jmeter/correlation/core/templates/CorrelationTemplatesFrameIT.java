package com.blazemeter.jmeter.correlation.core.templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.templates.gui.CorrelationTemplatesFrame;
import com.blazemeter.jmeter.correlation.core.templates.gui.CorrelationTemplatesRepositoryConfigFrame;
import com.blazemeter.jmeter.correlation.gui.StringUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JLabelFixture;
import org.assertj.swing.fixture.JListFixture;
import org.assertj.swing.fixture.JProgressBarFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.timing.Timeout;
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
public class CorrelationTemplatesFrameIT {

  private static final String LOCAL_TEMPLATE_ID = "LocalTemplate (local)";
  private static final String EXTERNAL_TEMPLATE_ID = "ExternalTemplate (external)";
  private static final int HALF_PROGRESS = 50;

  private FrameFixture frame;

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

    CorrelationTemplatesFrame correlationTemplatesFrame = new CorrelationTemplatesFrame(
        templatesRegistryHandler, repositoriesRegistryHandler, lastTemplateHandler, new JPanel());
    correlationTemplatesFrame.setConfigFrame(configFrame);
    frame = showInFrame(correlationTemplatesFrame.getContentPane());
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
    assertThat(findInstalledTemplateList().contents())
        .isEqualTo(new String[]{LOCAL_TEMPLATE_ID});
  }

  private JListFixture findInstalledTemplateList() {
    return frame.list("installedTemplatesList");
  }

  @Test
  public void shouldDisplayTemplateIDWhenTemplateSelected() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    findTemplateIdLabel().requireText(firstVersionFirstTemplateLocalRepository.getId());
  }

  private JLabelFixture findTemplateIdLabel() {
    return frame.label("templateIdLabel");
  }

  private void selectInstalledTemplate(String templateDisplayName) {
    findInstalledTemplateList().clickItem(templateDisplayName);
  }

  @Test
  public void shouldEnableLoadButtonWhenTemplateInstalled() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    findLoadButton().requireEnabled();
  }

  private JButtonFixture findLoadButton() {
    return frame.button("loadButton");
  }

  @Test
  public void shouldDisplayTemplateDescriptionWhenTemplateSelected() throws IOException {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    assertTemplateInfo("/firstTemplateDescription.html");
  }

  private void assertTemplateInfo(String templateInfoFile) throws IOException {
    CompareMatcher
        .isIdenticalTo(buildTestDocument(
            TestUtils.getFileContent(templateInfoFile, getClass())))
        .throwComparisonFailure()
        .matches(buildTestDocument(findDisplayInfoPane().text()));
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

  private JTextComponentFixture findDisplayInfoPane() {
    return frame.textBox("displayInfoPanel");
  }

  @Test
  public void shouldSetInstalledVersionWhenSelectTemplateWithInstalledVersion() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    findVersionsCombo().requireSelection("2.0");
  }

  private JComboBoxFixture findVersionsCombo() {
    return frame.comboBox("templateVersions");
  }

  @Test
  public void shouldSetUninstallLabelWhenSelectTemplateWithInstalledVersion() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    findInstallTemplateButton().requireText("Uninstall");
  }

  private JButtonFixture findInstallTemplateButton() {
    return frame.button("installTemplate");
  }

  @Test
  public void shouldEnableInstallButtonWhenTemplateNotInstalled() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    selectFirstVersion();
    findInstallTemplateButton().requireEnabled();
  }

  private void selectFirstVersion() {
    findVersionsCombo().selectItem("1.0");
  }

  @Test
  public void shouldDisableLoadButtonWhenTemplateNotInstalled() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    selectFirstVersion();
    findLoadButton().requireDisabled();
  }

  @Test
  public void shouldLoadWarningWhenInstallingVersionWithAnotherVersionInstalled() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    selectFirstVersion();
    findInstallTemplateButton().click();
    frame.optionPane().requireMessage(
        "There is another version of this Template installed. Want to overwrite it?");
  }

  @Test
  public void shouldShowErrorWhenOnLoadWithJsonException() throws IOException {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    doThrow(new IOException()).when(templatesRegistryHandler)
        .onLoadTemplate("local", "LocalTemplate", "2.0");
    findLoadButton().click();
    frame.optionPane().requireMessage("Error while trying to load LocalTemplate");
  }

  @Test
  public void shouldFilterTemplatesWithContainingText() {
    searchForSpecificText(localRepository.getName());
    assertThat(findInstalledTemplateList().contents()).isEqualTo(new String[]{LOCAL_TEMPLATE_ID});
  }

  private void searchForSpecificText(String str) {
    frame.textBox("searchField").setText(str);
  }

  @Test
  public void shouldDisplayTemplateInfoWithImage()
      throws IOException {
    when(secondVersionFirstTemplateLocalRepository.getSnapshot())
        .thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    assertTemplateInfo("/displayTemplateWithImage.html");
  }

  @Test
  public void shouldDisplayOnlyUninstalledTemplatesWhenClickAvailableTab() {
    changeToAvailableTemplates();
    assertThat(findAvailableTemplatesList().contents())
        .isEqualTo(new String[]{EXTERNAL_TEMPLATE_ID});
  }

  private void changeToAvailableTemplates() {
    frame.tabbedPane("templatesTabbedPane").selectTab(1);
  }

  private JListFixture findAvailableTemplatesList() {
    return frame.list("availableTemplatesList");
  }

  @Test
  public void shouldMoveAvailableTemplateToInstallListWhenInstall() {
    changeToAvailableTemplates();
    selectAvailableTemplate(EXTERNAL_TEMPLATE_ID);
    findInstallTemplateButton().click();
    frame.optionPane()
        .requireMessage("The template was successfully installed")
        .okButton()
        .click();
    selectInstalledTemplate(EXTERNAL_TEMPLATE_ID);
    findTemplateIdLabel().requireText(firstVersionFirstTemplateExternalRepository.getId());
  }

  private void selectAvailableTemplate(String templateDisplayName) {
    findAvailableTemplatesList().clickItem(templateDisplayName);
  }

  @Test
  public void shouldMoveInstalledTemplateToAvailableListWhenUninstall() {
    selectInstalledTemplate(LOCAL_TEMPLATE_ID);
    findInstallTemplateButton().click();
    frame.optionPane()
        .requireMessage("The template was successfully uninstalled")
        .okButton()
        .click();
    selectAvailableTemplate(LOCAL_TEMPLATE_ID);
    findTemplateIdLabel().requireText(firstVersionFirstTemplateLocalRepository.getId());
  }

  @Test
  public void shouldShowProgressBarWhenPressRefresh() {
    CountDownLatch refreshProgressLock = setupRefreshTaskWithUpdates();
    try {
      clickRefreshRepositories();
      findProgressBar().requireVisible();
    } finally {
      refreshProgressLock.countDown();
    }
  }

  private CountDownLatch setupRefreshTaskWithUpdates() {
    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(a -> {
      Consumer<Integer> consumer = a.getArgument(1);
      consumer.accept(HALF_PROGRESS);
      latch.await();
      consumer.accept(100);
      return false;
    }).when(repositoriesRegistryHandler).refreshRepositories(any(), any());
    return latch;
  }

  private void clickRefreshRepositories() {
    frame.button("refreshButton").click();
  }

  private JProgressBarFixture findProgressBar() {
    return frame.progressBar("loadingProgressBar");
  }

  @Test
  public void shouldIncrementProgressBarWhenRefreshRepositories() {
    CountDownLatch refreshProgressLock = setupRefreshTaskWithUpdates();
    try {
      clickRefreshRepositories();
      findProgressBar().waitUntilValueIs(HALF_PROGRESS);
    } finally {
      refreshProgressLock.countDown();
    }
  }

  @Test
  public void shouldEnableConfirmRefreshButtonWhenRefreshWithChanges() {
    refreshRepositories();
    findConfirmRefreshButton().requireEnabled(Timeout.timeout(10000));
  }

  private void refreshRepositories() {
    CountDownLatch refreshProgressLock = setupRefreshTaskWithUpdates();
    try {
      clickRefreshRepositories();
    } finally {
      refreshProgressLock.countDown();
    }
  }

  private JButtonFixture findConfirmRefreshButton() {
    return frame.button("confirmRefreshButton");
  }


}
