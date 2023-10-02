package com.blazemeter.jmeter.correlation.gui.templates;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class TemplateDependenciasTableIT {

  private static final String TEMPLATE_DESCRIPTION = "TestDescription";
  private static final String TEMPLATE_VERSION = "1.0.0";
  private static final String TEMPLATE_ID = "TestID";
  private FrameFixture frame;
  private TemplateDependenciesTable templateSaveFrame;
  private Set<Template> lastLoadedTemplates;
  @Mock
  private CorrelationTemplatesRegistryHandler templatesRegistry;
  @Mock
  private CorrelationTemplateDependency firstDependency;
  @Mock
  private CorrelationTemplateDependency secondDependency;
  @Mock
  private CorrelationTemplateDependency thirdDependency;
  @Mock
  private Template registeredTemplate;
  @Mock
  private Template lastLoadedTemplate;

  @Test
  public void shouldAddDependencyWhenAddPressed() {
    openAdvancedSectionPanel();
    findDependencyAddButton().click();
    assertThat(templateSaveFrame.getDependencies()).isNotEmpty();
  }

  private void openAdvancedSectionPanel() {
    frame.button("-collapsiblePanel-header-collapseButton").click();
  }

  private JButtonFixture findDependencyAddButton() {
    return frame.button("addButton");
  }

  @Test
  public void shouldClearRulesWhenClearPressed() {
    openAdvancedSectionPanel();
    findDependencyAddButton().click();
    frame.button("clearButton").click();
    assertThat(templateSaveFrame.getDependencies()).isEmpty();
  }

  @Test
  public void shouldSetDependenciesWhenSetDependencies() {
    List<CorrelationTemplateDependency> dependencies = buildExpectedDependencies();
    templateSaveFrame.setDependencies(dependencies);
    assertThat(templateSaveFrame.getDependencies().size()).isEqualTo(
        dependencies.size());
  }

  private List<CorrelationTemplateDependency> buildExpectedDependencies() {
    return Arrays.asList(firstDependency, secondDependency);
  }



  private void prepareRegisteredTemplate() {
    when(registeredTemplate.getId()).thenReturn(TEMPLATE_ID);
    when(registeredTemplate.getVersion()).thenReturn(TEMPLATE_VERSION);
    when(registeredTemplate.getDescription()).thenReturn(TEMPLATE_DESCRIPTION);
    when(registeredTemplate.getDependencies())
        .thenReturn(Arrays.asList(firstDependency, secondDependency));
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
}
