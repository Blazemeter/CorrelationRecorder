package com.blazemeter.jmeter.correlation.gui.templates;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.Template.Builder;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JLabelFixture;
import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class TemplateSaveFrameIT extends BaseTest {
  private final List<Template> lastLoadedTemplates = new ArrayList<Template>();
  private FrameFixture frame;
  @Mock
  private Template registeredTemplate;
  @Mock
  private Consumer<Template> lastTemplateHandler;
  @Mock
  private BufferedImage snapshot;
  private TemplateSaveFrame templateSaveFrame;

  @Before
  public void setup() {
    baseSetup();
    prepareRegisteredTemplate();
    templateSaveFrame = new TemplateSaveFrame(templatesRegistry, repositoriesRegistry,
        snapshot, lastTemplateHandler, new JPanel());
    templateSaveFrame.updateLastLoadedTemplate(registeredTemplate);
    Container contentPane = templateSaveFrame.getContentPane();
    frame = showInFrame(contentPane);
  }

  @After
  public void tearDown() {
    if (frame != null) {
      frame.cleanUp();
    }
  }

  @Test
  public void shouldLoadLastTemplateWhenSetLoadedTemplates() {
    templateSaveFrame.updateLastLoadedTemplate(lastLoadedTemplate);
    templateSaveFrame.showFrame(true);
    selectTabByIndex(0);
    assertThat(getSelectedRepository().getName()).isEqualTo(lastLoadedTemplate.getRepositoryId());
    assertThat(findProtocolsComboBox().selectedItem()).isEqualTo(lastLoadedTemplate.getId());

    selectTabByIndex(1);
    assertThat(getTemplateDescription()).isEqualTo(lastLoadedTemplate.getDescription());
    assertThat(getTemplateAuthor()).isEqualTo(lastLoadedTemplate.getAuthor());
    assertThat(getTemplateUrl()).isEqualTo(lastLoadedTemplate.getUrl());
  }

  private CorrelationTemplatesRepository getSelectedRepository() {
    return ((CorrelationTemplatesRepository) findRepositoriesComboBox().target().getSelectedItem());
  }

  @Test
  public void shouldNotifyListenerWhenSaveButton() throws IOException, ConfigurationException {
    templateSaveFrame.updateLastLoadedTemplate(lastLoadedTemplate);
    templateSaveFrame.showFrame(true);
    fillFields();
    clickSaveButton();
    verify(templatesRegistry).onSaveTemplate(any(Builder.class));
  }

  private void fillFields() {
    findRepositoriesComboBox().selectItem(FIRST_REPOSITORY_NAME);
    findProtocolsComboBox().selectItem(0);
    findTemplateVersionField().setText(TEMPLATE_VERSION + ".1");
    findTemplateChangesField().setText(TEMPLATE_CHANGES);
    selectTabByIndex(1);
    findTemplateAuthorField().setText(TEMPLATE_AUTHOR);
    findTemplateUrlField().setText(TEMPLATE_URL);
    findTemplateDescriptionField().setText(TEMPLATE_DESCRIPTION);
    // Forced the focus to lose to trigger the validation
    selectTabByIndex(0);
  }

  private JTabbedPaneFixture selectTabByIndex(int index) {
    return frame.tabbedPane("tabbedPane").selectTab(index);
  }

  private JComboBoxFixture findRepositoriesComboBox() {
    return frame.comboBox("repositoriesComboBox");
  }

  private JComboBoxFixture findProtocolsComboBox() {
    return frame.comboBox("protocolsComboBox");
  }

  private String getSelectedProtocol() {
    return (String) findProtocolsComboBox().target().getSelectedItem();
  }

  private JComboBoxFixture getExistingVersionsCombo() {
    return frame.comboBox("templateVersionComboBox");
  }

  private JTextComponentFixture findTemplateVersionField() {
    return frame.textBox("templateSuggestedVersionField");
  }

  private String getProposedVersion() {
    return findTemplateVersionField().text();
  }

  private JTextComponentFixture findTemplateAuthorField() {
    return frame.textBox("correlationTemplateAuthorField");
  }

  private String getTemplateAuthor() {
    return findTemplateAuthorField().text();
  }

  private JTextComponentFixture findTemplateUrlField() {
    return frame.textBox("correlationTemplateUrlField");
  }

  private String getTemplateUrl() {
    return findTemplateUrlField().text();
  }

  private JTextComponentFixture findTemplateDescriptionField() {
    return frame.textBox("correlationTemplateDescriptionTextArea");
  }

  private String getTemplateDescription() {

    return findTemplateDescriptionField().text();
  }

  private JTextComponentFixture findTemplateChangesField() {
    return frame.textBox("templateChangesTextArea");
  }

  private void clickSaveButton() {
    frame.button("saveTemplateButtonButton").click();
  }

  private Builder buildTemplate() {
    return new Builder()
        .withId(TEMPLATE_ID)
        .withVersion(TEMPLATE_VERSION)
        .withAuthor(TEMPLATE_AUTHOR)
        .withUrl(TEMPLATE_URL)
        .withChanges(TEMPLATE_CHANGES)
        .withDescription(TEMPLATE_DESCRIPTION)
        .withRepositoryId("local")
        .withDependencies(new ArrayList<>());
  }

  @Test
  public void shouldShowErrorToUserWhenExceptionOnSaveTemplate()
      throws IOException, ConfigurationException {
    templateSaveFrame.showFrame(true);
    doThrow(new IOException()).when(templatesRegistry).onSaveTemplate(any(Builder.class));
    fillFields();
    clickSaveButton();
    requireMessage("Error while trying to save template: null");
  }

  private void requireMessage(String message) {
    frame.optionPane().requireMessage(message);
  }

  @Test
  public void shouldShowTemplateIDWhenSetLoadedTemplatesWithOneLoadedTemplate() {
    templateSaveFrame.updateLastLoadedTemplate(lastLoadedTemplate);
    templateSaveFrame.showFrame(true);
    assertThat(findProtocolsComboBox().selectedItem()).isEqualTo(TEMPLATE_ID);
  }

  private Template getLastLoadedTemplate() {
    return lastLoadedTemplates.get(lastLoadedTemplates.size() - 1);
  }

  @Test
  public void shouldSetupVersionFieldWhenSetLoadTemplatesWithOneLoadedTemplate() {
    templateSaveFrame.updateLastLoadedTemplate(lastLoadedTemplate);
    templateSaveFrame.showFrame(true);
    assertThat(findTemplateVersionField().text()).isEqualTo(TEMPLATE_SUGGESTED_VERSION);
  }

  @Test
  public void shouldSetupDescriptionFieldWhenSetLoadTemplatesWithOneLoadedTemplate() {
    templateSaveFrame.updateLastLoadedTemplate(lastLoadedTemplate);
    templateSaveFrame.showFrame(true);
    selectTabByIndex(1);
    assertThat(getTemplateDescription()).isEqualTo(TEMPLATE_DESCRIPTION);
  }

  @Test
  public void shouldShowTemplateDescriptionEmptyWhenSetLoadedTemplatesWithMultipleLoadedTemplate() {
    lastLoadedTemplates.add(lastLoadedTemplate);
    templateSaveFrame.updateLastLoadedTemplate(getLastLoadedTemplate());
    selectTabByIndex(1);
    assertThat(findTemplateDescriptionField().text()).isEmpty();
  }

  @Test
  public void shouldShowAdvanceSectionWhenExpandPanel() {
    openAdvancedSectionPanel();
    findDependencyAddButton().requireEnabled();
  }

  private void openAdvancedSectionPanel() {
    selectTabByIndex(1);
    frame.button("-collapsiblePanel-header-collapseButton").click();
  }

  private JButtonFixture findDependencyAddButton() {
    return frame.button("addButton");
  }

  private List<CorrelationTemplateDependency> buildExpectedDependencies() {
    return Arrays.asList(firstDependency, secondDependency);
  }

  private void prepareDependencies() {
    prepareDependencyAndUrlValidity(firstDependency, 1, true);
    prepareDependencyAndUrlValidity(secondDependency, 1, true);
  }

  private void prepareRegisteredTemplate() {
    when(registeredTemplate.getId()).thenReturn(TEMPLATE_ID);
    when(registeredTemplate.getVersion()).thenReturn(TEMPLATE_VERSION);
    when(registeredTemplate.getDescription()).thenReturn(TEMPLATE_DESCRIPTION);
    when(registeredTemplate.getDependencies())
        .thenReturn(Arrays.asList(firstDependency, secondDependency));
  }

  private void prepareDependencyAndUrlValidity(CorrelationTemplateDependency dependency,
                                               int number,
                                               boolean urlValidity) {
    String dependencyName = "Dependency" + number;
    String dependencyVersion = number + ".0";
    String dependencyURL = "ulr" + number;

    when(dependency.getName()).thenReturn(dependencyName);
    when(dependency.getVersion()).thenReturn(dependencyVersion);
    when(dependency.getUrl()).thenReturn(dependencyURL);
    when(templatesRegistry
        .isValidDependencyURL(dependencyURL, dependencyName, dependencyVersion))
        .thenReturn(urlValidity);
  }

  private JButtonFixture findSaveButton() {
    return frame.button("saveTemplateButtonButton");
  }

  private String buildEmptyFieldMessage() {
    return "Field cannot be empty.";
  }

  private JLabelFixture findTemplateNameValidationLabel() {
    return frame.label("protocolValidation");
  }

  private JLabelFixture findVersionValidationLabel() {
    return frame.label("versionValidation");
  }

  @Test(expected = org.assertj.swing.exception.ComponentLookupException.class)
  public void shouldNotDisplayIdErrorMessageWhenIdFieldLoseFocusWithText() {
    templateSaveFrame.updateLastLoadedTemplate(lastLoadedTemplate);
    templateSaveFrame.showFrame(true);
    selectTabByIndex(0);
    selectTemplate(findProtocolsComboBox().valueAt(0));
    findTemplateVersionField().focus();
    findTemplateNameValidationLabel();
  }

  @Test
  public void shouldDisplayVersionErrorMessageWhenVersionFieldLeftEmpty() {
    templateSaveFrame.updateLastLoadedTemplate(lastLoadedTemplate);
    templateSaveFrame.showFrame(true);
    findTemplateVersionField().setText("");
    selectTemplate(findProtocolsComboBox().valueAt(0));
    frame.focus();
    assertThat(findVersionValidationLabel().text()).isEqualTo(buildEmptyFieldMessage());
  }

  @Test
  public void shouldDisplayVersionErrorMessageWhenVersionIsRepeated() {
    templateSaveFrame.updateLastLoadedTemplate(lastLoadedTemplate);
    templateSaveFrame.showFrame(true);
    selectTabByIndex(0);
    findTemplateVersionField().setText("");
    findTemplateChangesField().focus();
    assertThat(isVersionValidationLabelVisible()).isTrue();
  }

  private boolean isVersionValidationLabelVisible() {
    return findVersionValidationLabel().target().isVisible();
  }

  private void selectTemplate(String templateName) {
    findProtocolsComboBox().selectItem(templateName);
  }

  @Test
  public void shouldEnableSaveButtonWhenFillFieldWithValidTexts() {
    templateSaveFrame.showFrame(true);
    fillFields();
    findSaveButton().focus();
    assertThat(findSaveButton().isEnabled()).isTrue();
  }

  @Test
  public void shouldLoadMultipleDependenciesWhenLoadWithMultipleLoadedTemplates() {
    when(lastLoadedTemplate.getDependencies())
        .thenReturn(Collections.singletonList(thirdDependency));
    lastLoadedTemplates.add(lastLoadedTemplate);
    templateSaveFrame.updateLastLoadedTemplate(getLastLoadedTemplate());
    assertThat(templateSaveFrame.getDependenciesTable().getDependencies().size())
        .isEqualTo(lastLoadedTemplate.getDependencies().size());
  }
}
