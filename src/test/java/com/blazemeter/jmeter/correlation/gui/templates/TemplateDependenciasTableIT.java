package com.blazemeter.jmeter.correlation.gui.templates;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class TemplateDependenciasTableIT extends BaseTest {

  private FrameFixture frame;
  private TemplateDependenciesTable templateSaveFrame;

  @Mock
  private CorrelationTemplateDependency firstDependency;
  @Mock
  private CorrelationTemplateDependency secondDependency;


  @Before
  public void setup() {
    templateSaveFrame = new TemplateDependenciesTable();
    frame = showInFrame(templateSaveFrame.prepareDependenciesPanel());
    frame.show();
  }

  @After
  public void tearDown() {
    if (frame != null) {
      frame.cleanUp();
    }
  }

  @Test
  public void shouldAddDependencyWhenAddPressed() {
    findDependencyAddButton().click();
    assertThat(templateSaveFrame.getDependencies()).isNotEmpty();
  }

  private JButtonFixture findDependencyAddButton() {
    return frame.button("addButton");
  }

  @Test
  public void shouldClearRulesWhenClearPressed() {
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

}
