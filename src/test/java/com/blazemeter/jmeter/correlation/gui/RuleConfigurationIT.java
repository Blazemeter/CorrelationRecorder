package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
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
public class RuleConfigurationIT {

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
  private CorrelationComponentsRegistry registry;
  private RuleConfiguration ruleConfiguration;
  private FrameFixture frame;

  @Before
  public void setup() {
    prepareRegistry();
    ruleConfiguration = new RuleConfiguration(1, update, registry);
    ruleConfiguration.paintHandlers();
    JPanel container = new JPanel();
    container.setName("container");
    container.add(ruleConfiguration.getReferenceVariableField());
    container.add(ruleConfiguration.getReplacementConfigurationPanel());
    container.add(ruleConfiguration.getExtractorConfigurationPanel());
    frame = showInFrame(container);
    frame.target().pack();
  }

  private void prepareRegistry() {
    when(registry.buildActiveExtractors()).thenReturn(Arrays.asList(extractor, noneExtractor));
    when(registry.buildActiveReplacements()).thenReturn(Arrays.asList(replacement, noneReplacement));
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
    assertThat(ruleConfiguration.getReplacementConfigurationPanel().getListComponents()).isEmpty();
  }

  private void selectReplacement(CorrelationRulePartTestElement<?> item) {
    selectRulePartComboItem("Replacement", item);
  }

  private void selectRulePartComboItem(String comboType, CorrelationRulePartTestElement<?> item) {
    frame.comboBox(ruleConfiguration.getName() + "-Correlation" + comboType + "-comboBox").target()
        .getModel().setSelectedItem(item);
    frame.target().pack();
  }

  @Test
  public void shouldClearExtractorComponentsWhenNoneSelected() {
    //We ensure there is an Extractor selected
    selectExtractor(extractor);
    selectExtractor(noneExtractor);
    assertThat(ruleConfiguration.getExtractorConfigurationPanel().getListComponents()).isEmpty();
  }

  private void selectExtractor(CorrelationRulePartTestElement<?> item) {
    selectRulePartComboItem("Extractor", item);
  }

  @Test
  public void shouldChangeReplacementComponentsWhenChangeReplacement() {
    selectReplacement(noneReplacement);
    ConfigurationPanel replacementConfig = ruleConfiguration.getReplacementConfigurationPanel();
    ArrayList<Component> originalComponents = new ArrayList<>(
        replacementConfig.getListComponents());
    selectReplacement(replacement);
    assertThat(replacementConfig.getListComponents()).isNotEqualTo(originalComponents);
  }

  @Test
  public void shouldChangeExtractorComponentsWhenChangeExtractor() {
    selectExtractor(noneExtractor);
    ConfigurationPanel panelHandler = ruleConfiguration.getExtractorConfigurationPanel();
    ArrayList<Component> originalComponents = new ArrayList<>(panelHandler.getListComponents());
    selectExtractor(extractor);
    assertThat(panelHandler.getListComponents()).isNotEqualTo(originalComponents);
  }

  @Test
  public void shouldContainRefVariableExampleWhenRuleCreated() {
    findReferenceVariableField().requireText("refVar1");
  }

  private JTextComponentFixture findReferenceVariableField() {
    return frame.textBox(ruleConfiguration.getName() + "-referenceVariable");
  }

  @Test
  public void shouldDisplayHelperWhenNoneNotSelected() {
    selectExtractor(noneExtractor);
    selectExtractor(extractor);
    assertThat(ruleConfiguration.getReplacementConfigurationPanel().getHelper().isVisible())
        .isTrue();
  }

  @RunWith(Parameterized.class)
  public static class UpdateFieldIT {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Runnable update;

    @Mock
    private CorrelationComponentsRegistry registry;
    private RuleConfiguration ruleConfiguration;
    private FrameFixture frame;
    @Parameter()
    public String referenceVariableName;
    @Parameter(1)
    public String startReplacementStringValue;
    @Parameter(2)
    public BiConsumer<FrameFixture, RuleConfiguration> triggerAction;
    @Parameter(3)
    public String expectedReplacementStringValue;

    @After
    public void tearDown() {
      frame.cleanUp();
    }

    @Before
    public void setup() {
      prepareRegistry();
      ruleConfiguration = new RuleConfiguration(1, update, registry);
      ruleConfiguration.paintHandlers();
      JPanel container = new JPanel();
      container.setName("container");
      container.add(ruleConfiguration.getReferenceVariableField());
      container.add(ruleConfiguration.getReplacementConfigurationPanel());
      container.add(ruleConfiguration.getExtractorConfigurationPanel());
      frame = showInFrame(container);
      frame.target().pack();
    }

    private void prepareRegistry() {
      when(registry.buildActiveExtractors()).thenReturn(Arrays.asList(extractor, noneExtractor));
      when(registry.buildActiveReplacements())
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
              (BiConsumer<FrameFixture, RuleConfiguration>) (frame, ruleConfig) -> findReferenceVariableField(
                  frame, ruleConfig).deleteText().enterText("id"), "${__javaScript(${id})}"},
          {"var", "${__javaScript(${var_1})}",
              buildVariableFieldConsumer("id"), "${__javaScript(${varid_1})}"}});
    }

    private static BiConsumer<FrameFixture, RuleConfiguration> buildVariableFieldConsumer(
        String input) {
      return (frame, ruleConfig) -> findReferenceVariableField(frame, ruleConfig)
          .click().enterText(input);
    }

    @Test
    public void shouldReplaceReplacementStringFieldWhenRefVarChanges() {
      selectExtractor(noneExtractor);
      selectReplacement(noneExtractor);
      selectExtractor(extractor);
      selectReplacement(replacement);
      findReferenceVariableField(frame, ruleConfiguration).setText(referenceVariableName);
      findReplacementStringField().setText(startReplacementStringValue);
      triggerAction.accept(frame, ruleConfiguration);
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
      frame.comboBox(ruleConfiguration.getName() + "-Correlation" + comboType + "-comboBox")
          .target()
          .getModel().setSelectedItem(item);
      frame.target().pack();
    }

    private static JTextComponentFixture findReferenceVariableField(FrameFixture frame,
        RuleConfiguration ruleConfiguration) {
      return frame.textBox(ruleConfiguration.getName() + "-referenceVariable");
    }

    private JTextComponentFixture findReplacementStringField() {
      return frame
          .textBox(ruleConfiguration.getName()
              + "-CorrelationReplacement-text-CorrelationRule.CorrelationReplacement"
              + ".replacementString");
    }
  }
}
