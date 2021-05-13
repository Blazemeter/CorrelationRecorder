package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JPanel;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(SwingTestRunner.class)
public class RuleTableRowIT {

  public final static CorrelationRulePartTestElement<?> noneExtractor =
      CorrelationComponentsRegistry.NONE_EXTRACTOR;
  public final static CorrelationRulePartTestElement<?> noneReplacement =
      CorrelationComponentsRegistry.NONE_REPLACEMENT;
  public final static CorrelationRulePartTestElement<?> replacement =
      new RegexCorrelationReplacement<>();
  public final static CorrelationRulePartTestElement<?> extractor =
      new RegexCorrelationExtractor<>();

  @Mock
  private Runnable update;
  @Mock
  private Consumer<CorrelationRulePartTestElement<?>> displayExtensions;
  @Mock
  private CorrelationComponentsRegistry registry;
  
  private RuleTableRow rule;
  private FrameFixture frame;

  @Before
  public void setup() {
    prepareRegistry();
    rule = new RuleTableRow(1, update, displayExtensions, registry);
    rule.paintHandlers();
    JPanel container = new JPanel();
    container.setName("container");
    container.add(rule.getReferenceVariableField());
    container.add(rule.getReplacementConfigurationPanel());
    container.add(rule.getExtractorConfigurationPanel());
    frame = showInFrame(container);
    frame.target().pack();
  }

  private void prepareRegistry() {
    when(registry.buildActiveExtractorRulePart()).thenReturn(Arrays.asList(extractor, noneExtractor));
    when(registry.buildActiveReplacementRulePart()).thenReturn(Arrays.asList(replacement, noneReplacement));
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldClearReplacementComponentsWhenNoneSelected() {
    //We ensure there is a Replacement selected
    selectReplacement(replacement);
    selectReplacement(noneReplacement);
    assertThat(rule.getReplacementConfigurationPanel().getListComponents()).isEmpty();
  }

  private void selectReplacement(CorrelationRulePartTestElement<?> item) {
    selectRulePartComboItem("Replacement", item);
  }

  private void selectRulePartComboItem(String comboType, CorrelationRulePartTestElement<?> item) {
    frame.comboBox(rule.getName() + "-Correlation" + comboType + "-comboBox").target()
        .getModel().setSelectedItem(item);
    frame.target().pack();
  }

  @Test
  public void shouldClearExtractorComponentsWhenNoneSelected() {
    //We ensure there is an Extractor selected
    selectExtractor(extractor);
    selectExtractor(noneExtractor);
    assertThat(rule.getExtractorConfigurationPanel().getListComponents()).isEmpty();
  }

  private void selectExtractor(CorrelationRulePartTestElement<?> item) {
    selectRulePartComboItem("Extractor", item);
  }

  @Test
  public void shouldChangeReplacementComponentsWhenChangeReplacement() {
    selectReplacement(noneReplacement);
    CorrelationRulePartPanel replacementConfig = rule.getReplacementConfigurationPanel();
    ArrayList<Component> originalComponents = new ArrayList<>(
        replacementConfig.getListComponents());
    selectReplacement(replacement);
    assertThat(replacementConfig.getListComponents()).isNotEqualTo(originalComponents);
  }

  @Test
  public void shouldChangeExtractorComponentsWhenChangeExtractor() {
    selectExtractor(noneExtractor);
    CorrelationRulePartPanel panelHandler = rule.getExtractorConfigurationPanel();
    ArrayList<Component> originalComponents = new ArrayList<>(panelHandler.getListComponents());
    selectExtractor(extractor);
    assertThat(panelHandler.getListComponents()).isNotEqualTo(originalComponents);
  }

  @Test
  public void shouldContainRefVariableExampleWhenRuleCreated() {
    findReferenceVariableField().requireText("refVar1");
  }

  private JTextComponentFixture findReferenceVariableField() {
    return frame.textBox(rule.getName() + "-referenceVariable");
  }

  @Test
  public void shouldDisplayHelperWhenNoneNotSelected() {
    selectExtractor(noneExtractor);
    selectExtractor(extractor);
    assertThat(rule.getReplacementConfigurationPanel().getHelper().isVisible())
        .isTrue();
  }

  @RunWith(Parameterized.class)
  public static class UpdateFieldIT {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Runnable update;
    @Mock
    private Consumer<CorrelationRulePartTestElement<?>> displayExtensions;

    @Mock
    private CorrelationComponentsRegistry registry;
    private RuleTableRow ruleTableRow;
    private FrameFixture frame;
    @Parameter()
    public String referenceVariableName;
    @Parameter(1)
    public String startReplacementStringValue;
    @Parameter(2)
    public BiConsumer<FrameFixture, RuleTableRow> triggerAction;
    @Parameter(3)
    public String expectedReplacementStringValue;

    @After
    public void tearDown() {
      frame.cleanUp();
    }

    @Before
    public void setup() {
      prepareRegistry();
      ruleTableRow = new RuleTableRow(1, update, displayExtensions, registry);
      ruleTableRow.paintHandlers();
      JPanel container = new JPanel();
      container.setName("container");
      container.add(ruleTableRow.getReferenceVariableField());
      container.add(ruleTableRow.getReplacementConfigurationPanel());
      container.add(ruleTableRow.getExtractorConfigurationPanel());
      frame = showInFrame(container);
      frame.target().pack();
    }

    private void prepareRegistry() {
      when(registry.buildActiveExtractorRulePart()).thenReturn(Arrays.asList(extractor, noneExtractor));
      when(registry.buildActiveReplacementRulePart())
          .thenReturn(Arrays.asList(replacement, noneExtractor));
    }

    @Parameters
    public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][]{
          {"var", "${__javaScript(${var})}", buildVariableFieldConsumer("1"),
              "${__javaScript(${var1})}"},
          {"id", "${__groovy(vars.get(\"id\"))}", buildVariableFieldConsumer("2"),
              "${__groovy(vars.get(\"id2\"))}"},
          {"ite", "${__javaScript(${ite#2_1})}", buildVariableFieldConsumer("m"),
              "${__javaScript(${item#2_1})}"},
          {"refVar", "${__groovy(vars[\"refVar\"])}", buildVariableFieldConsumer("1"),
              "${__groovy(vars[\"refVar1\"])}"},
          {"var", "${__javaScript(${var})}",
              (BiConsumer<FrameFixture, RuleTableRow>) (frame, ruleConfig) -> findReferenceVariableField(
                  frame, ruleConfig).deleteText().enterText("id"), "${__javaScript(${id})}"},
          {"var", "${__javaScript(${var_1})}",
              buildVariableFieldConsumer("id"), "${__javaScript(${varid_1})}"}});
    }

    private static BiConsumer<FrameFixture, RuleTableRow> buildVariableFieldConsumer(
        String input) {
      return (frame, ruleConfig) -> findReferenceVariableField(frame, ruleConfig)
          .click().enterText(input);
    }

    @Test
    public void shouldReplaceReplacementStringFieldWhenRefVarChanges() {
      selectExtractor(noneExtractor);
      selectReplacement(noneExtractor);
      selectExtractor(extractor);
      toggleExtractorAdvancedSection();
      selectReplacement(replacement);
      toggleReplacementAdvancedSection();
      findReferenceVariableField(frame, ruleTableRow).setText(referenceVariableName);
      findReplacementStringField().setText(startReplacementStringValue);
      triggerAction.accept(frame, ruleTableRow);
      assertThat(findReplacementStringField().target().getText())
          .isEqualTo(expectedReplacementStringValue);
    }

    private void selectExtractor(CorrelationRulePartTestElement<?> item) {
      selectRulePartComboItem("Extractor", item);
    }

    private void selectReplacement(CorrelationRulePartTestElement<?> item) {
      selectRulePartComboItem("Replacement", item);
    }

    private void selectRulePartComboItem(String comboType, CorrelationRulePartTestElement<?> item) {
      frame.comboBox(ruleTableRow.getName() + "-Correlation" + comboType + "-comboBox").selectItem(item.getDisplayName());
      frame.target().pack();
    }

    private static JTextComponentFixture findReferenceVariableField(FrameFixture frame,
        RuleTableRow rule) {
      return frame.textBox(rule.getName() + "-referenceVariable");
    }

    private void toggleExtractorAdvancedSection() {
      toggleAdvancedSection(ruleTableRow.getExtractorConfigurationPanel());
    }

    private void toggleReplacementAdvancedSection() {
      toggleAdvancedSection(ruleTableRow.getReplacementConfigurationPanel());
    }

    //We cant click on a label so trigger the toggle method instead
    private void toggleAdvancedSection(CorrelationRulePartPanel panel) {
      panel.toggleAdvanced(update);
      frame.target().pack();
    }

    private JTextComponentFixture findReplacementStringField() {
      return frame
          .textBox(ruleTableRow.getName()
              + "-CorrelationReplacement-text-CorrelationRule.CorrelationReplacement"
              + ".replacementString");
    }
  }

}
