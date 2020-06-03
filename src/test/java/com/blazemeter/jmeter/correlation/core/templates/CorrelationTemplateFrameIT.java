package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.TestUtils.requireMessage;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.assertj.swing.timing.Pause.pause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate.Builder;
import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JPanelFixture;
import org.assertj.swing.timing.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationTemplateFrameIT {

  private static final String TEMPLATE_DESCRIPTION = "TestDescription";
  private static final String TEMPLATE_CHANGES = "TestChanges";
  private static final String TEMPLATE_VERSION = "1.0.0";
  private static final String TEMPLATE_ID = "TestID";
  private static final String TEMPLATE_TYPE = "local";
  private static final String TEMPLATE_ID_FIELD_NAME = "correlationTemplateIdField";
  private static final String TEMPLATE_CHANGES_TEXT_AREA = "correlationTemplateChangesField";
  private static final String TEMPLATE_VERSION_FIELD_NAME = "correlationTemplateVersionField";
  private static final String TEMPLATE_DESCRIPTION_TEXT_AREA_NAME = "correlationTemplateDescriptionTextArea";

  private static final String DEPENDENCY_ADD_BUTTON_NAME = "addDependencyRow";
  private static final String DEPENDENCY_CLEAR_BUTTON_NAME = "clearDependencyTable";
  private static final String DEPENDENCY_SAVE_BUTTON_NAME = "saveTemplateButton";
  private static final String COLLAPSIBLE_PANE_NAME = "dependenciesCollapsiblePanel";
  private static final long TIMEOUT_MILLIS = 10000;
  private FrameFixture frame;

  @Mock
  private CorrelationTemplatesRegistryHandler correlationTemplatesRegistry;
  @Mock
  private CorrelationTemplate registeredTemplate;
  @Mock
  private CorrelationTemplate lastLoadedTemplate;
  @Mock
  private CorrelationTemplateDependency firstDependency;
  @Mock
  private CorrelationTemplateDependency secondDependency;
  @Mock
  private CorrelationTemplateDependency thirdDependency;
  @Mock
  private Consumer<CorrelationTemplate> lastTemplateHandler;
  private CorrelationTemplateFrame correlationTemplateFrame;
  private JButtonFixture saveButton;
  private Set<CorrelationTemplate> lastLoadedTemplates;

  @Before
  public void setup() {
    correlationTemplateFrame = new CorrelationTemplateFrame(correlationTemplatesRegistry, null,
        lastTemplateHandler);
    lastLoadedTemplates = new HashSet<>(Collections.singletonList(registeredTemplate));
    frame = showInFrame(correlationTemplateFrame.getContentPane());
    saveButton = frame.button(DEPENDENCY_SAVE_BUTTON_NAME);
    frame.resizeTo(new Dimension(300, 900));
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldShowErrorMessageWhenEmptyFields() {
    frame.textBox(TEMPLATE_ID_FIELD_NAME).setText(TEMPLATE_ID);
    clickSaveButton();
    requireMessage(frame.robot(), "All fields are required");
  }

  private void clickSaveButton() {
    frame.button("saveTemplateButton").click();
  }

  @Test
  public void shouldNotifyListenerWhenSaveButton() throws IOException, ConfigurationException {
    fillFields();
    clickSaveButton();
    verify(correlationTemplatesRegistry).onSaveTemplate(getTemplateBuilderWithTestingValues());
  }

  private void fillFields() {
    frame.textBox(TEMPLATE_ID_FIELD_NAME).setText(TEMPLATE_ID);
    frame.textBox(TEMPLATE_VERSION_FIELD_NAME).setText(TEMPLATE_VERSION);
    frame.textBox(TEMPLATE_CHANGES_TEXT_AREA).setText(TEMPLATE_CHANGES);
    frame.textBox(TEMPLATE_DESCRIPTION_TEXT_AREA_NAME).setText(TEMPLATE_DESCRIPTION);
  }

  private Builder getTemplateBuilderWithTestingValues() {
    return new Builder()
        .withId(TEMPLATE_ID)
        .withVersion(TEMPLATE_VERSION)
        .withChanges(TEMPLATE_CHANGES)
        .withDescription(TEMPLATE_DESCRIPTION)
        .withRepositoryId(TEMPLATE_TYPE)
        .withDependencies(new ArrayList<>());
  }

  @Test
  public void shouldShowErrorToUserWhenExceptionOnSaveTemplate()
      throws IOException, ConfigurationException {
    doThrow(new IOException()).when(correlationTemplatesRegistry).onSaveTemplate(any());
    fillFields();
    clickSaveButton();
    requireMessage(frame.robot(), "Error while trying to save template");
  }

  @Test
  public void shouldShowErrorWhenRegisteredTemplateIDAndRepeatedVersion() {
    fillFields();
    prepareRegisteredTemplate();
    when(correlationTemplatesRegistry.isLocalTemplateVersionSaved(TEMPLATE_ID, TEMPLATE_VERSION))
        .thenReturn(true);
    clickSaveButton();
    requireMessage(frame.robot(), "That version is already in use. Try a new one.");
  }

  private void prepareRegisteredTemplate() {
    when(registeredTemplate.getId()).thenReturn(TEMPLATE_ID);
    when(registeredTemplate.getVersion()).thenReturn(TEMPLATE_VERSION);
    when(registeredTemplate.getDescription()).thenReturn(TEMPLATE_DESCRIPTION);
    when(registeredTemplate.getDependencies())
        .thenReturn(Arrays.asList(firstDependency, secondDependency));
  }

  @Test
  public void shouldShowTemplateIDWhenSetLoadedTemplatesWithOneLoadedTemplate() {
    prepareRegisteredTemplate();
    correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
    assertEquals(TEMPLATE_ID,
        frame.robot().finder()
            .findByName(TEMPLATE_ID_FIELD_NAME, JTextField.class)
            .getText());
  }

  @Test
  public void shouldSetupVersionFieldWhenSetLoadTemplatesWithOneLoadedTemplate() {
    prepareRegisteredTemplate();
    correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
    assertEquals(TEMPLATE_VERSION,
        frame.robot().finder()
            .findByName(TEMPLATE_VERSION_FIELD_NAME, JTextField.class)
            .getText());
  }

  @Test
  public void shouldSetupDescriptionFieldWhenSetLoadTemplatesWithOneLoadedTemplate() {
    prepareRegisteredTemplate();
    correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
    assertEquals(TEMPLATE_DESCRIPTION,
        frame.robot().finder()
            .findByName(TEMPLATE_DESCRIPTION_TEXT_AREA_NAME, JTextArea.class)
            .getText());
  }

  @Test
  public void shouldShowTemplateIDEmptyWhenSetLoadedTemplatesWithMultipleLoadedTemplate() {
    prepareRegisteredTemplate();
    lastLoadedTemplates.add(lastLoadedTemplate);
    correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
    assertEquals("",
        frame.robot().finder()
            .findByName(TEMPLATE_ID_FIELD_NAME, JTextField.class)
            .getText());
  }

  @Test
  public void shouldShowTemplateDescriptionEmptyWhenSetLoadedTemplatesWithMultipleLoadedTemplate() {
    prepareRegisteredTemplate();
    lastLoadedTemplates.add(lastLoadedTemplate);
    correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
    assertEquals("",
        frame.robot().finder()
            .findByName(TEMPLATE_DESCRIPTION_TEXT_AREA_NAME, JTextArea.class)
            .getText());
  }

  @Test
  public void shouldCleanAllFieldsAfterSuccessfulSaveWhenSaveButton() {
    fillFields();
    clickSaveButton();

    pause(new Condition("Should clean all") {
      @Override
      public boolean test() {
        return frame.textBox(TEMPLATE_ID_FIELD_NAME).text().isEmpty();
      }
    }, TIMEOUT_MILLIS);
  }

  @Test
  public void shouldAddRowsWhenAddPressed() {
    toggleAdvancedSectionPanel(frame.panel(COLLAPSIBLE_PANE_NAME));
    frame.button(DEPENDENCY_ADD_BUTTON_NAME).click();
    List<CorrelationTemplateDependency> rules = correlationTemplateFrame.getDependencies();
    assertFalse(rules.isEmpty());
  }

  @Test
  public void shouldClearRulesWhenClearPressed() {
    toggleAdvancedSectionPanel(frame.panel(COLLAPSIBLE_PANE_NAME));
    frame.button(DEPENDENCY_ADD_BUTTON_NAME).click();
    frame.button(DEPENDENCY_CLEAR_BUTTON_NAME).click();

    List<CorrelationTemplateDependency> rules = correlationTemplateFrame.getDependencies();
    assertTrue(rules.isEmpty());
  }

  @Test
  public void shouldSetDependenciesWhenSetDependencies() {
    List<CorrelationTemplateDependency> dependencies = buildExpectedDependencies();
    correlationTemplateFrame.setDependencies(dependencies);

    assertEquals(correlationTemplateFrame.getDependencies().size(), dependencies.size());
  }

  private List<CorrelationTemplateDependency> buildExpectedDependencies() {
    return Arrays.asList(firstDependency, secondDependency);
  }

  @Test
  public void shouldDisplayWarningWhenSaveWithRepeatedDependencies() {
    prepareDependencies();

    List<CorrelationTemplateDependency> dependencies = buildExpectedDependencies();
    correlationTemplateFrame.setDependencies(dependencies);
    fillFields();
    saveButton.click();
    JOptionPaneFinder.findOptionPane().using(frame.robot())
        .requireMessage("There are dependencies that are repeated. Want to overwrite them?");
  }

  private void prepareDependencies() {
    prepareDependencyAndUrlValidity(firstDependency, 1, true);
    prepareDependencyAndUrlValidity(secondDependency, 1, true);
  }

  private void prepareDependencyAndUrlValidity(CorrelationTemplateDependency dependency, int number,
      boolean urlValidity) {
    String dependencyName = "Dependency" + number;
    String dependencyVersion = number + ".0";
    String dependencyURL = "ulr" + number;

    when(dependency.getName()).thenReturn(dependencyName);
    when(dependency.getVersion()).thenReturn(dependencyVersion);
    when(dependency.getUrl()).thenReturn(dependencyURL);
    when(correlationTemplatesRegistry
        .isValidDependencyURL(dependencyURL, dependencyName, dependencyVersion))
        .thenReturn(urlValidity);
  }

  @Test
  public void shouldShowAdvanceSectionWhenExpandPanel() {
    toggleAdvancedSectionPanel(frame.panel(COLLAPSIBLE_PANE_NAME));
    assertThat(frame.button(DEPENDENCY_ADD_BUTTON_NAME).isEnabled()).isTrue();
  }

  private void toggleAdvancedSectionPanel(JPanelFixture panel) {
    frame.robot().click(panel.target(), new Point(0, 0));
  }

  @Test
  public void shouldDisplayWarningMessageWhenSaveWithIncompleteDependencies() {
    prepareDependencies();
    when(firstDependency.getName()).thenReturn("");
    correlationTemplateFrame.setDependencies(buildExpectedDependencies());
    fillFields();
    saveButton.click();
    JOptionPaneFinder.findOptionPane().using(frame.robot())
        .requireMessage("There are incomplete dependencies. Fill or delete them before continue.");
  }

  @Test
  public void shouldDisplayWarningMessageWhenSaveWithInvalidUrlDependency() {
    prepareDependencyAndUrlValidity(firstDependency, 1, true);
    prepareDependencyAndUrlValidity(secondDependency, 2, false);
    correlationTemplateFrame.setDependencies(buildExpectedDependencies());
    fillFields();
    saveButton.click();
    JOptionPaneFinder.findOptionPane().using(frame.robot())
        .requireMessage(
            "There are some issues with some dependency's URLs, please fix then before continue.\n"
                + "Check the logs for more information.");
  }

  @Test
  public void shouldLoadMultipleDependenciesWhenLoadWithMultipleLoadedTemplates() {
    prepareDependencies();
    prepareRegisteredTemplate();
    when(lastLoadedTemplate.getDependencies())
        .thenReturn(Collections.singletonList(thirdDependency));
    lastLoadedTemplates.add(lastLoadedTemplate);
    correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);

    List<CorrelationTemplateDependency> dependencies = Arrays
        .asList(firstDependency, secondDependency, thirdDependency);
    assertEquals(correlationTemplateFrame.getDependencies().size(), dependencies.size());
  }
}
