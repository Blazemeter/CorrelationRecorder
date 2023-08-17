package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.awt.Color;
import java.util.List;
import javax.swing.JTextField;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class GroupPanelIT {

  private static final Color DISABLE_FONT_RULE_COLOR = new JTextField().getDisabledTextColor();

  @Mock
  private Runnable onCollapseUpdate;
  @Mock
  private Runnable modelUpdate;

  private FrameFixture frame;
  private GroupPanel panel;
  private JTableFixture table;

  @Before
  public void setup() {
    panel = new GroupPanel("Group-1", true, onCollapseUpdate, modelUpdate);
    frame = showInFrame(panel);
    table = frame.table(panel.getTable().getName());
    table.replaceCellWriter(new CustomCellWriter(frame.robot()));
  }

  @After
  public void tearDown() {
    frame.cleanUp();
    frame = null;
  }

  @Test
  public void shouldDisplayRulesTableWhenAdded() {
    table.requireVisible();
  }

  @Test
  public void shouldAddBeEnabledWhenGroupAdded() {
    findAddButton().requireEnabled();
  }

  private JButtonFixture findAddButton() {
    return frame.button("addButton");
  }

  @Test
  public void shouldDeleteBeDisabledWhenGroupAdded() {
    findDeleteButton().requireDisabled();
  }

  private JButtonFixture findDeleteButton() {
    return frame.button("deleteButton");
  }

  @Test
  public void shouldUpButtonBeDisabledWhenGroupAddedWithNoRules() {
    findUpButton().requireDisabled();
  }

  private JButtonFixture findUpButton() {
    return frame.button("upButton");
  }

  @Test
  public void shouldDownButtonBeDisabledWhenGroupAddedWithNoRules() {
    findDownButton().requireDisabled();
  }

  private JButtonFixture findDownButton() {
    return frame.button("downButton");
  }

  @Test
  public void shouldAddRulesWhenAddButtonPressed() {
    addRule();
    assertThat(panel.getRulesConfiguration()).asList().isNotEmpty();
  }

  private void addRule() {
    findAddButton().click();
  }

  @Test
  public void shouldEnableDeleteWhenRuleAdded() {
    addRule();
    findDeleteButton().requireEnabled();
  }

  @Test
  public void shouldSelectRegexExtractorByDefaultWhenRuleAdded() {
    addRule();
    List<RuleTableRow> rules = panel.getRulesConfiguration();
    assertThat(rules.get(rules.size() - 1).getExtractorConfigurationPanel()
        .getSelectedItem())
        .isInstanceOf(RegexCorrelationExtractor.class);
  }


  @Test
  public void shouldSelectRegexReplacementByDefaultWhenRuleAdded() {
    addRule();
    List<RuleTableRow> rules = panel.getRulesConfiguration();
    assertThat(rules.get(rules.size() - 1).getReplacementConfigurationPanel()
        .getSelectedItem())
        .isInstanceOf(RegexCorrelationReplacement.class);
  }

  @Test
  public void shouldRuleAppearDisableWhenCheckDisableRule() {
    addRule();
    pressDisableCheckAt(0);
    assertThat(table.cell(TableCell.row(0).column(1)).foreground().target())
        .isEqualTo(DISABLE_FONT_RULE_COLOR);
  }

  @Test
  public void shouldRuleAppearEnableWhenPressDisableAndThenPressEnable() {
    addRule();
    pressDisableCheckAt(0);
    pressDisableCheckAt(0);
    assertThat(table.cell(TableCell.row(0).column(1)).foreground().target())
        .isEqualTo(new JTextField().getForeground());
  }

  @Test
  public void shouldKeepRuleDisableWhenDisableRuleAndModifyExtractor() {
    addRule();
    pressDisableCheckAt(0);
    RegexCorrelationExtractor<?> regex = new RegexCorrelationExtractor<>();
    GuiActionRunner
        .execute(() -> panel.getRulesConfiguration().get(0).getExtractorConfigurationPanel()
            .setSelectedItem(regex));
    assertThat(((CorrelationRulePartPanel) table.cell(TableCell.row(0).column(2)).editor())
        .getListComponents().get(0).getForeground())
        .isEqualTo(DISABLE_FONT_RULE_COLOR);
  }

  private void pressDisableCheckAt(int row) {
    table.click(TableCell.row(row).column(0), MouseButton.LEFT_BUTTON);
  }

  @Test
  public void shouldDisableGroupWhenUncheckDisableGroupCheckBox() {
    addRule();
    pressGroupDisableCheck();
    assertThat(panel.getRulesGroup().isEnable()).isFalse();
  }

  private void pressGroupDisableCheck() {
    frame.checkBox(panel.getName() + "-collapsiblePanel-header-disableCheck").click();
  }

  @Test
  public void shouldNotChangeRulesEnabledStateWhenCheckDisabledGroupCheckBox() {
    addRule();
    pressGroupDisableCheck();
    assertThat(1).isEqualTo((int) panel.getRulesGroup().getRules().stream()
        .filter(CorrelationRule::isEnabled)
        .count());
  }
}
