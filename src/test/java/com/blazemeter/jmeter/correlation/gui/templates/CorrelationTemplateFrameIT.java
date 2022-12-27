package com.blazemeter.jmeter.correlation.gui.templates;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion.Builder;
import java.awt.Container;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JPanel;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JLabelFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class CorrelationTemplateFrameIT {

    private static final String TEMPLATE_DESCRIPTION = "TestDescription";
    private static final String TEMPLATE_CHANGES = "TestChanges";
    private static final String TEMPLATE_VERSION = "1.0.0";
    private static final String TEMPLATE_ID = "TestID";
    private static final String TEMPLATE_AUTHOR = "TestAuthor";
    private static final String TEMPLATE_URL = "TestUrl";

    private FrameFixture frame;
    @Mock
    private CorrelationTemplatesRegistryHandler templatesRegistry;
    @Mock
    private TemplateVersion registeredTemplate;
    @Mock
    private TemplateVersion lastLoadedTemplate;
    @Mock
    private CorrelationTemplateDependency firstDependency;
    @Mock
    private CorrelationTemplateDependency secondDependency;
    @Mock
    private CorrelationTemplateDependency thirdDependency;
    @Mock
    private Consumer<TemplateVersion> lastTemplateHandler;
    private CorrelationTemplateFrame correlationTemplateFrame;
    private Set<TemplateVersion> lastLoadedTemplates;

    @Before
    public void setup() {
        correlationTemplateFrame = new CorrelationTemplateFrame(templatesRegistry, null,
            lastTemplateHandler, new JPanel());
        lastLoadedTemplates = new HashSet<>(Collections.singletonList(registeredTemplate));
        Container contentPane = correlationTemplateFrame.getContentPane();
        frame = showInFrame(contentPane);
    }

    @After
    public void tearDown() {
        frame.cleanUp();
    }

    @Test
    public void shouldNotifyListenerWhenSaveButton() throws IOException, ConfigurationException {
        fillFields();
        clickSaveButton();
        verify(templatesRegistry).onSaveTemplate(buildTemplate());
    }

    private void fillFields() {
        findTemplateIdField().setText(TEMPLATE_ID);
        findTemplateVersionField().setText(TEMPLATE_VERSION);
        findTemplateAuthorField().setText(TEMPLATE_AUTHOR);
        findTemplateUrlField().setText(TEMPLATE_URL);
        findTemplateDescriptionField().setText(TEMPLATE_DESCRIPTION);
        findTemplateChangesField().setText(TEMPLATE_CHANGES);
    }

    private JTextComponentFixture findTemplateIdField() {
        return frame.textBox("correlationTemplateIdField");
    }

    private JTextComponentFixture findTemplateVersionField() {
        return frame.textBox("correlationTemplateVersionField");
    }

    private JTextComponentFixture findTemplateAuthorField() {
        return frame.textBox("correlationTemplateAuthorField");
    }

    private JTextComponentFixture findTemplateUrlField() {
        return frame.textBox("correlationTemplateUrlField");
    }

    private JTextComponentFixture findTemplateDescriptionField() {
        return frame.textBox("correlationTemplateDescriptionTextArea");
    }

    private JTextComponentFixture findTemplateChangesField() {
        return frame.textBox("correlationTemplateChangesField");
    }

    private void clickSaveButton() {
        frame.button("saveTemplateButton").click();
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
        doThrow(new IOException()).when(templatesRegistry).onSaveTemplate(buildTemplate());
        fillFields();
        clickSaveButton();
        requireMessage("Error while trying to save template");
    }

    private void requireMessage(String message) {
        frame.optionPane().requireMessage(message);
    }

    @Test
    public void shouldShowTemplateIDWhenSetLoadedTemplatesWithOneLoadedTemplate() {
        prepareRegisteredTemplate();
        correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
        assertThat(findTemplateIdField().text()).isEqualTo(TEMPLATE_ID);
    }

    private void prepareRegisteredTemplate() {
        when(registeredTemplate.getId()).thenReturn(TEMPLATE_ID);
        when(registeredTemplate.getVersion()).thenReturn(TEMPLATE_VERSION);
        when(registeredTemplate.getDescription()).thenReturn(TEMPLATE_DESCRIPTION);
        when(registeredTemplate.getDependencies())
            .thenReturn(Arrays.asList(firstDependency, secondDependency));
    }


    @Test
    public void shouldSetupVersionFieldWhenSetLoadTemplatesWithOneLoadedTemplate() {
        prepareRegisteredTemplate();
        correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
        assertThat(findTemplateVersionField().text()).isEqualTo(TEMPLATE_VERSION);
    }

    @Test
    public void shouldSetupDescriptionFieldWhenSetLoadTemplatesWithOneLoadedTemplate() {
        prepareRegisteredTemplate();
        correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
        assertThat(findTemplateDescriptionField().text()).isEqualTo(TEMPLATE_DESCRIPTION);
    }

    @Test
    public void shouldShowTemplateIDEmptyWhenSetLoadedTemplatesWithMultipleLoadedTemplate() {
        prepareRegisteredTemplate();
        lastLoadedTemplates.add(lastLoadedTemplate);
        correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
        assertThat(findTemplateIdField().text()).isEmpty();
    }

    @Test
    public void shouldShowTemplateDescriptionEmptyWhenSetLoadedTemplatesWithMultipleLoadedTemplate() {
        prepareRegisteredTemplate();
        lastLoadedTemplates.add(lastLoadedTemplate);
        correlationTemplateFrame.setLoadedTemplates(lastLoadedTemplates);
        assertThat(findTemplateDescriptionField().text()).isEmpty();
    }

    @Test
    public void shouldCleanAllFieldsAfterSuccessfulSaveWhenSaveButton() {
        fillFields();
        clickSaveButton();
        findTemplateIdField().requireEmpty();
    }

    @Test
    public void shouldShowAdvanceSectionWhenExpandPanel() {
        openAdvancedSectionPanel();
        findDependencyAddButton().requireEnabled();
    }

    private void openAdvancedSectionPanel() {
        frame.button("-collapsiblePanel-header-collapseButton").click();
    }

    private JButtonFixture findDependencyAddButton() {
        return frame.button("addButton");
    }

    @Test
    public void shouldAddDependencyWhenAddPressed() {
        openAdvancedSectionPanel();
        findDependencyAddButton().click();
        assertThat(correlationTemplateFrame.getDependencies()).isNotEmpty();
    }

    @Test
    public void shouldClearRulesWhenClearPressed() {
        openAdvancedSectionPanel();
        findDependencyAddButton().click();
        frame.button("clearButton").click();
        assertThat(correlationTemplateFrame.getDependencies()).isEmpty();
    }

    @Test
    public void shouldSetDependenciesWhenSetDependencies() {
        List<CorrelationTemplateDependency> dependencies = buildExpectedDependencies();
        correlationTemplateFrame.setDependencies(dependencies);
        assertThat(correlationTemplateFrame.getDependencies().size()).isEqualTo(
            dependencies.size());
    }

    private List<CorrelationTemplateDependency> buildExpectedDependencies() {
        return Arrays.asList(firstDependency, secondDependency);
    }

    @Test
    public void shouldDisplayWarningWhenSaveWithRepeatedDependencies() {
        prepareDependencies();
        correlationTemplateFrame.setDependencies(buildExpectedDependencies());
        fillFields();
        clickSaveButton();
        requireMessage("There are dependencies that are repeated. Want to overwrite them?");
    }

    private void prepareDependencies() {
        prepareDependencyAndUrlValidity(firstDependency, 1, true);
        prepareDependencyAndUrlValidity(secondDependency, 1, true);
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

    @Test
    public void shouldDisplayWarningMessageWhenSaveWithIncompleteDependencies() {
        prepareDependencies();
        when(firstDependency.getName()).thenReturn("");
        correlationTemplateFrame.setDependencies(buildExpectedDependencies());
        fillFields();
        clickSaveButton();
        requireMessage("There are incomplete dependencies. Fill or delete them before continue.");
    }

    @Test
    public void shouldDisplayWarningMessageWhenSaveWithInvalidUrlDependency() {
        prepareDependencyAndUrlValidity(firstDependency, 1, true);
        prepareDependencyAndUrlValidity(secondDependency, 2, false);
        correlationTemplateFrame.setDependencies(buildExpectedDependencies());
        fillFields();
        clickSaveButton();
        requireMessage(
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
        assertThat(correlationTemplateFrame.getDependencies().size()).isEqualTo(3);
    }

    @Test
    public void shouldDisableSaveButtonWhenFormHasErrors() {
        findTemplateIdField().setText(TEMPLATE_ID + " /");
        findTemplateVersionField().setText(TEMPLATE_VERSION + "@");
        findTemplateDescriptionField().setText("");
        findTemplateChangesField().setText(TEMPLATE_CHANGES);
        assertThat(findSaveButton().isEnabled()).isFalse();
    }

    @Test
    public void shouldDisableSaveButtonWhenFormIsLoaded() {
        assertThat(findSaveButton().target().isEnabled()).isFalse();
    }

    private JButtonFixture findSaveButton() {
        return frame.button("saveTemplateButton");
    }

    @Test
    public void shouldDisplayIdErrorMessageWhenIdFieldLeftEmpty() {
        findTemplateIdField().setText("");
        findTemplateVersionField().setText("");
        assertThat(findIdValidationLabel().text()).isEqualTo(buildEmptyFieldMessage());
    }

    private String buildEmptyFieldMessage() {
        return "This field can't be empty";
    }


    private JLabelFixture findIdValidationLabel() {
        return frame.label("idValidation");
    }

    @Test
    public void shouldDisplayIdErrorMessageWhenIdFieldWithSpaces() {
        findTemplateIdField().setText(TEMPLATE_ID + " " + TEMPLATE_ID);
        findTemplateVersionField().setText("");
        assertThat(findIdValidationLabel().text()).isEqualTo(buildInvalidCharacterMessage());
    }

    private String buildInvalidCharacterMessage() {
        return "Use only alphanumeric values and dashes (- and _).";
    }


    @Test
    public void shouldDisplayIdErrorMessageWhenIdFieldWithSpecialCharacters() {
        findTemplateIdField().setText("ID@test/");
        findTemplateVersionField().setText("");
        assertThat(findIdValidationLabel().text()).isEqualTo(buildInvalidCharacterMessage());
    }

    @Test
    public void shouldDisplayVersionErrorMessageWhenVersionFieldWithSpecialCharacters() {
        findTemplateVersionField().setText("Version@test/");
        findTemplateIdField().setText("");
        assertThat(findVersionValidationLabel().text()).isEqualTo(buildInvalidCharacterMessage());
    }

    private JLabelFixture findVersionValidationLabel() {
        return frame.label("versionValidation");
    }

    @Test(expected = org.assertj.swing.exception.ComponentLookupException.class)
    public void shouldNotDisplayIdErrorMessageWhenIdFieldLoseFocusWithText() {
        findTemplateIdField().setText(TEMPLATE_ID);
        findTemplateVersionField().setText("");
        findIdValidationLabel();
    }

    @Test
    public void shouldDisplayVersionErrorMessageWhenVersionFieldLeftEmpty() {
        findTemplateVersionField().setText("");
        findTemplateIdField().setText(TEMPLATE_ID);
        assertThat(findVersionValidationLabel().text()).isEqualTo(buildEmptyFieldMessage());
    }

    @Test
    public void shouldDisplayVersionErrorMessageWhenVersionIsRepeated() {
        when(templatesRegistry.isLocalTemplateVersionSaved(TEMPLATE_ID, TEMPLATE_VERSION))
            .thenReturn(true);

        findTemplateIdField().setText(TEMPLATE_ID);
        findTemplateVersionField().setText(TEMPLATE_VERSION);
        findTemplateDescriptionField().setText(TEMPLATE_DESCRIPTION);
        assertThat(findVersionValidationLabel().text()).isEqualTo("This Version is already in use");
    }

    @Test
    public void shouldEnableSaveButtonWhenFillFieldWithValidTexts() {
        fillFields();
        findSaveButton().focus();
        assertThat(findSaveButton().isEnabled()).isTrue();
    }
}
