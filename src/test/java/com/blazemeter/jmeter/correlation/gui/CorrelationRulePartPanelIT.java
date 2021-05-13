package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.Mockito.verify;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class CorrelationRulePartPanelIT {

  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private final CorrelationRulePartTestElement<?> none =
      CorrelationComponentsRegistry.NONE_EXTRACTOR;
  private final CorrelationRulePartTestElement<?> replacement =
      new RegexCorrelationReplacement<>();
  private final CorrelationRulePartTestElement<?> extractor =
      new RegexCorrelationExtractor<>();
  private FrameFixture frame;
  private CorrelationRulePartPanel panel;

  @Mock
  private Runnable modelUpdate;

  @Mock
  private Runnable fieldUpdate;

  @Before
  public void setup() {
    panel = new CorrelationRulePartPanel.Builder()
        .withName("TestRulePart")
        .withTableUpdateRunner(modelUpdate)
        .withOptions(Arrays.asList(none, extractor, replacement))
        .build();

    panel.setFieldsListener(fieldUpdate);
    panel.addFields();
    frame = showInFrame(panel);
    frame.target().pack();
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldUpdateComponentWidthWhenOptionSelected() {
    int initialRequiredWidth = panel.calculateRequiredColumnWidth();
    selectExtractorInCombo();
    assertThat(panel.calculateRequiredColumnWidth()).isNotEqualTo(initialRequiredWidth);
  }

  private void selectExtractorInCombo() {
    frame.comboBox("TestRulePart-comboBox").selectItem(1);
    frame.target().pack();
  }

  @Test
  public void shouldReturnAdvancedAndDefaultComponentsWhenGetListComponents() {
    selectExtractorInCombo();
    assertComponents(
        buildExpectedComponents(extractor.getParamsDefinition()),
        panel.getListComponents());
  }

  /*
    Regular assertThat(Component).isEquals(Component) takes in consideration the size of the 
    components which is not relevant for the test case.
  */
  private void assertComponents(List<Component> expected, List<Component> actual) {
    softly.assertThat(expected.size()).isEqualTo(actual.size());

    for (int i = 0; i < expected.size(); i++) {
      Component expectedComp = expected.get(i);
      Component actualComp = actual.get(i);

      softly.assertThat(expectedComp.getName()).isEqualTo(actualComp.getName());
      softly.assertThat(actualComp.isVisible()).isEqualTo(expectedComp.isVisible());

      if (expectedComp instanceof JTextField) {
        softly.assertThat(((JTextField) actualComp).getText())
            .isEqualTo(((JTextField) expectedComp).getText());
      } else if (expectedComp instanceof JComboBox) {
        softly.assertThat(((JComboBox<?>) actualComp).getSelectedIndex())
            .isEqualTo(((JComboBox<?>) expectedComp).getSelectedIndex());
      } else if (expectedComp instanceof JCheckBox) {
        softly.assertThat(((JCheckBox) actualComp).isSelected())
            .isEqualTo(((JCheckBox) expectedComp).isSelected());
      }
    }
  }

  private List<Component> buildExpectedComponents(List<ParameterDefinition> parameterDefinitions) {
    return parameterDefinitions.stream()
        .map(param -> panel.buildField(param))
        .collect(Collectors.toList());
  }

  @Test
  public void shouldUpdateComponentListWhenSelectedElementsChange() {
    frame.comboBox("TestRulePart-comboBox").selectItem(0);
    List<Component> previousComponents = panel.getListComponents();
    selectExtractorInCombo();
    assertThat(previousComponents).isNotEqualTo(panel.getListComponents());
  }

  @Test
  public void shouldCallUpdateTableWhenAdvancedCheckBoxChecked() {
    selectExtractorInCombo();
    clickAdvanceSectionIcon();
    verify(fieldUpdate).run();
  }

  @Test
  public void shouldMakeAdvancedVisibleWhenAdvancedChecked() {
    selectExtractorInCombo();
    clickAdvanceSectionIcon();
    frame.panel("TestRulePart-advancedPanel").requireVisible();
  }

  private void clickAdvanceSectionIcon() {
    frame.label("TestRulePart-collapsedIcon").click();
    frame.target().pack();
  }

  @Test
  public void shouldNotSetValuesOnAdvancedComponentsWhenHidden() {
    List<String> expectedValues = Arrays.asList("param=\"(.+?)\"", "1", "2", "BODY", "true");
    selectExtractorInCombo();
    panel.updateComponentValues(expectedValues);
    assertThat(expectedValues).isEqualTo(panel.getComponentsValues());
  }

  @Test
  public void shouldIncreaseNeededHeightWhenDisplayAdvanced() {
    selectExtractorInCombo();
    int previousHeight = panel.getCellNeededHeight();
    clickAdvanceSectionIcon();
    int actualHeight = panel.getCellNeededHeight();
    assertThat(previousHeight).isLessThan(actualHeight);
  }
}
