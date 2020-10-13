package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate;
import com.blazemeter.jmeter.correlation.core.templates.gui.CorrelationTemplateFrame;
import com.blazemeter.jmeter.correlation.core.templates.gui.CorrelationTemplatesFrame;
import com.blazemeter.jmeter.correlation.siebel.SiebelCounterCorrelationReplacement;
import java.awt.Color;
import java.awt.Component;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.driver.AbstractJTableCellWriter;
import org.assertj.swing.driver.JTableTextComponentEditorCellWriter;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.ComponentLookupException;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SwingTestRunner.class)
public class RulesContainerIT {

  private static final Logger LOG = LoggerFactory.getLogger(RulesContainerIT.class);
  private static final Color DISABLE_FONT_RULE_COLOR = new JTextField().getDisabledTextColor();

  @Mock
  private CorrelationProxyControl repositoryHandler;
  @Mock
  private Runnable modelUpdate;
  @Mock
  private CorrelationTemplateFrame templateFrame;
  @Mock
  private CorrelationTemplatesFrame loadFrame;

  private FrameFixture frame;
  private RulesContainer rulesContainer;
  private JTableFixture rulesTable;

  @Before
  public void setup() {
    rulesContainer = new RulesContainer(repositoryHandler, modelUpdate);
    rulesContainer.setTemplateFrame(templateFrame);
    rulesContainer.setLoadFrame(loadFrame);
    frame = showInFrame(rulesContainer);
    rulesTable = frame.table("rulesTable");
    rulesTable.replaceCellWriter(new MyCellWriter(frame.robot()));
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldEnableUpButtonWhenMoreThanOneRowAndNotFirstRowSelected() {
    addRule();
    addRule();
    clickRuleByIndex(1);
    findUpButton().requireEnabled();
  }

  private void addRule() {
    frame.button("addButton").click();
  }

  private void clickRuleByIndex(int ruleNumber) {
    rulesTable.selectRows(ruleNumber);
  }

  private JButtonFixture findUpButton() {
    return frame.button("upButton");
  }

  @Test
  public void shouldDisableDownButtonWhenSelectedRuleIsLast() {
    addRule();
    addRule();
    clickRuleByIndex(1);
    findDownButton().requireDisabled();
  }

  private JButtonFixture findDownButton() {
    return frame.button("downButton");
  }

  @Test
  public void shouldEnableDownButtonWhenSelectFirstRuleWithMultipleRules() {
    addRule();
    addRule();
    clickRuleByIndex(0);
    findDownButton().requireEnabled();
  }

  @Test
  public void shouldAddRulePanelWhenAddPressed() {
    addRule();
    assertThat(rulesContainer.getRules().size()).isEqualTo(1);
  }

  @Test
  public void shouldAddRuleConfigWhenAddPressed() {
    addRule();
    assertThat(rulesTable.contents()[0]).isNotEqualTo(new String[]{null, null, null});
  }

  @Test
  public void shouldMoveRuleUpWhenUpPressed() {
    addRule();
    addRule();
    RuleConfiguration secondRule = rulesContainer.getRules().get(1);
    clickRuleByIndex(1);
    findUpButton().click();
    assertThat(rulesContainer.getRules().get(0)).isSameAs(secondRule);
  }

  @Test
  public void shouldMoveRuleDownWhenDownPressed() {
    addRule();
    addRule();
    RuleConfiguration firstRule = rulesContainer.getRules().get(0);
    clickRuleByIndex(0);
    findDownButton().click();
    assertThat(rulesContainer.getRules().get(1)).isSameAs(firstRule);
  }

  @Test
  public void shouldDeleteRuleWhenDeletePressed() {
    addRule();
    addRule();
    clickRuleByIndex(1);
    frame.button("deleteButton").click();
    assertThat(rulesContainer.getRules().size()).isEqualTo(1);
  }

  @Test
  public void shouldUpButtonBeDisabledWhenMovedRuleReachTheTopOfTheList() {
    addRule();
    addRule();
    clickRuleByIndex(1);
    findUpButton().click();
    findUpButton().requireDisabled();
  }

  @Test
  public void shouldShowErrorMessageWhenClickSaveTemplateWithEmptyRule() {
    addRule();
    rulesContainer.getRules().get(0).setVariableName("");
    selectNonExtractorComboByRuleIndex(0);
    selectNonReplacementComboByRuleIndex(0);
    saveTemplate();
    frame.optionPane()
        .requireMessage("Rules are incomplete or empty, please fill them before continue saving.");
  }

  private void saveTemplate() {
    frame.button("exportButton").click();
  }

  @Test
  public void shouldNotShowAdvanceOptionsWhenPanelIsCollapsed() {
    assertNotShowAdvancedOptions();
  }

  private void assertNotShowAdvancedOptions() {
    try {
      findComponentsList();
      fail("Found advanced options which shouldn't be present");
    } catch (ComponentLookupException e) {
      LOG.debug("Expected exception was received", e);
    }
  }

  private JTextComponentFixture findComponentsList() {
    return frame.textBox("componentsList");
  }

  @Test
  public void shouldShowAdvanceOptionsWhenExpandPanel() {
    openAdvancedSectionPanel();
    frame.label("responseFilterLabel").requireEnabled();
  }

  private void openAdvancedSectionPanel() {
    frame.label("headerPanel-expandedIcon").click();
    frame.target().pack();
  }

  @Test
  public void shouldNotShowExtensionsAreaWhenCollapsePanelAfterExpand() {
    openAdvancedSectionPanel();
    closeAdvancedSectionPanel();
    assertNotShowAdvancedOptions();
  }

  private void closeAdvancedSectionPanel() {
    frame.label("headerPanel-collapsedIcon").click();
    frame.target().pack();
  }

  @Test
  public void shouldAllowSaveTemplateFrameWhenSaveTemplateWithRulesWithoutExtractors() {
    addRule();
    addRule();
    selectNonExtractorComboByRuleIndex(0);
    selectNonExtractorComboByRuleIndex(1);
    saveTemplate();
    verify(templateFrame).showFrame();
  }

  private void selectNonExtractorComboByRuleIndex(int ruleNumber) {
    /*
     ComboBox does not appear inside rules table as expected, probably due to custom handling of
     rendering, so we can't use AssertJSwing api, and do this instead
     */
    GuiActionRunner.execute(() -> rulesContainer.getRules()
        .get(ruleNumber)
        .getExtractorConfigurationPanel()
        .setSelectedItem(CorrelationComponentsRegistry.NONE_EXTRACTOR));
  }

  @Test
  public void shouldAllowSaveTemplateFrameWhenSaveTemplateWithRulesWithoutReplacement() {
    addRule();
    addRule();
    selectNonReplacementComboByRuleIndex(0);
    selectNonReplacementComboByRuleIndex(1);
    saveTemplate();
    verify(templateFrame).showFrame();
  }

  private void selectNonReplacementComboByRuleIndex(int ruleNumber) {
    /*
     ComboBox does not appear inside rules table as expected, probably due to custom handling of
     rendering, so we can't use AssertJSwing api, and do this instead
     */
    GuiActionRunner.execute(() -> rulesContainer.getRules()
        .get(ruleNumber)
        .getReplacementConfigurationPanel()
        .setSelectedItem(CorrelationComponentsRegistry.NONE_REPLACEMENT));
  }

  @Test
  public void shouldClearRulesWhenClickClearButton() {
    addRule();
    clickClear();
    assertThat(rulesContainer.getRules()).asList().isEmpty();
  }

  private void clickClear() {
    frame.button("clearButton").click();
  }

  @Test
  public void shouldClearFilterWhenClickClearButton() {
    frame.textBox("responseFilterField").setText("TestFilter");
    clickClear();
    assertThat(rulesContainer.getResponseFilter()).isEmpty();
  }

  @Test
  public void shouldClearLastLoadedTemplateWhenClickClearButton() {
    CorrelationTemplate template = Mockito.mock(CorrelationTemplate.class);
    rulesContainer.updateLoadedTemplate(template);
    clickClear();
    saveTemplate();
    verify(templateFrame, never()).setLoadedTemplates(any());
  }

  @Test
  public void shouldClearComponentsWhenClickClearButton() {
    openAdvancedSectionPanel();
    findComponentsList().setText("TestComponents");
    clickClear();
    assertThat(rulesContainer.getCorrelationComponents()).isEmpty();
  }

  @Test
  public void shouldRuleAppearDisableWhenCheckDisableRule() {
    addRule();
    pressDisableCheckAt(0);
    assertThat(rulesTable.cell(TableCell.row(0).column(1)).foreground().target())
        .isEqualTo(DISABLE_FONT_RULE_COLOR);
  }

  private void pressDisableCheckAt(int row) {
    rulesTable.click(TableCell.row(row).column(0), MouseButton.LEFT_BUTTON);
  }

  @Test
  public void shouldKeepRuleDisableWhenDisableRuleAndModifyExtractor() {
    addRule();
    pressDisableCheckAt(0);
    RegexCorrelationExtractor<?> regex = new RegexCorrelationExtractor<>();
    GuiActionRunner.execute(() -> rulesContainer.getRules().get(0).getExtractorConfigurationPanel()
        .setSelectedItem(regex));
    assertThat(((ConfigurationPanel) rulesTable.cell(TableCell.row(0).column(2)).editor())
        .getListComponents().get(0).getForeground())
        .isEqualTo(DISABLE_FONT_RULE_COLOR);
  }

  @Test
  public void shouldRuleAppearEnableWhenPressDisableAndThenPressEnable() {
    addRule();
    pressDisableCheckAt(0);
    pressDisableCheckAt(0);
    assertThat(rulesTable.cell(TableCell.row(0).column(1)).foreground().target())
        .isEqualTo(new JTextField().getForeground());
  }

  @Test
  public void shouldSelectRegexReplacementByDefaultWhenRuleAdded() {
    addRule();
    List<RuleConfiguration> rules = rulesContainer.getRules();
    assertThat(rules.get(rules.size() - 1).getReplacementConfigurationPanel()
        .getSelectedItem())
        .isInstanceOf(RegexCorrelationReplacement.class);
  }

  @Test
  public void shouldSelectRegexExtractorByDefaultWhenRuleAdded() {
    addRule();
    List<RuleConfiguration> rules = rulesContainer.getRules();
    assertThat(rules.get(rules.size() - 1).getExtractorConfigurationPanel()
        .getSelectedItem())
        .isInstanceOf(RegexCorrelationExtractor.class);
  }

  @Test
  public void shouldNotSetRulesWhenConfigureWithNullTestElement() {
    prepareRepositoryHandler("", "", null);
    rulesContainer.clean();
    rulesContainer.configure(repositoryHandler);
    assertThat(rulesContainer.getCorrelationRules())
        .isEqualTo(Collections.EMPTY_LIST);
  }

  private void prepareRepositoryHandler(String correlationComponents, String responseFilters,
      CorrelationRulesTestElement rules) {
    when(repositoryHandler.getCorrelationComponents()).thenReturn(correlationComponents);
    when(repositoryHandler.getResponseFilter()).thenReturn(responseFilters);
    when(repositoryHandler.getCorrelationRulesTestElement()).thenReturn(rules);
  }

  @Test
  public void shouldSetRulesWhenConfigure() {
    List<CorrelationRule> rules = Collections.singletonList(new CorrelationRule("refVar1",
        new RegexCorrelationExtractor<>(), new RegexCorrelationReplacement<>()));
    prepareRepositoryHandler("", "", buildCorrelationRulesTestElementFromRules(rules));
    rulesContainer.configure(repositoryHandler);
    assertThat(rulesContainer.getCorrelationRules())
        .isEqualTo(rules);
  }

  private CorrelationRulesTestElement buildCorrelationRulesTestElementFromRules(
      List<CorrelationRule> rules) {
    return new CorrelationRulesTestElement(rules.stream()
        .map(correlationRule -> correlationRule
            .buildTestElement(new CorrelationComponentsRegistry()))
        .collect(Collectors.toList()));
  }

  @Test
  public void shouldUpdateComponentsContainerWithNewCorrelationComponents() {
    String extensionComponentString = SiebelCounterCorrelationReplacement.class.getCanonicalName();
    prepareRepositoryHandler(extensionComponentString, "", null);
    rulesContainer.configure(repositoryHandler);
    assertThat(rulesContainer.getCorrelationComponents())
        .isEqualTo(extensionComponentString);
  }

  private static class MyCellWriter extends AbstractJTableCellWriter {

    private final JTableTextComponentEditorCellWriter textComponentWriter;

    private MyCellWriter(Robot robot) {
      super(robot);
      textComponentWriter = new JTableTextComponentEditorCellWriter(robot);
    }

    @Override
    public void startCellEditing(JTable table, int row, int column) {
      Component editor = editorForCell(table, row, column);
      if (editor instanceof JTextComponent) {
        textComponentWriter.startCellEditing(table, row, column);
      }
    }

    @Override
    public void enterValue(JTable table, int row, int column, String value) {
    }

  }
}
