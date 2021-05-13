package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.common.CollapsiblePanel;
import com.blazemeter.jmeter.correlation.gui.common.NonStringValuedTableGui;
import com.blazemeter.jmeter.correlation.gui.common.RulePartType;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils.ButtonBuilder;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class GroupPanel extends JPanel implements ActionListener {

  private static final String DELETE = "delete";
  private static final String ADD = "add";
  private static final String UP = "up";
  private static final String DOWN = "down";
  private static final int MAIN_CONTAINER_WIDTH = 800;
  private static final Dimension MAIN_CONTAINER_DEFAULT_DIMENSION = new Dimension(
      MAIN_CONTAINER_WIDTH, 150);

  private final CollapsiblePanel container;
  private final Runnable groupsSizesUpdate;
  private final Runnable modelUpdate;
  private JButton upButton;
  private JButton downButton;
  private JButton deleteButton;
  private RulesTableGui table;
  private boolean groupEnabled;

  public GroupPanel(String title, boolean isGroupEnabled, Runnable onCollapseUpdate,
      Runnable modelUpdate) {
    setLayout(new GridLayout());
    setName("groupPanel-" + System.currentTimeMillis());
    this.groupsSizesUpdate = onCollapseUpdate;
    this.modelUpdate = modelUpdate;
    initializeRulesTable();
    this.groupEnabled = isGroupEnabled;
    this.container = buildCollapsiblePanel(title, isGroupEnabled, new JScrollPane(table));
    add(container);
    checkButtonsStatus();
  }

  public GroupPanel(RulesGroup container, Runnable onCollapseUpdate, Runnable modelUpdate) {
    this(container.getId(), container.isEnable(), onCollapseUpdate, modelUpdate);
    container.getRules().forEach(r -> addRule(buildRuleConfiguration(r)));
    table.clearSelection();
    table.scrollRectToVisible(new Rectangle(0, 0));
  }

  public RulesGroup getRulesGroup() {
    return new RulesGroup.Builder()
        .withId(container.getId())
        .withRules(table.getRules().stream()
            .map(RuleTableRow::getCorrelationRule)
            .collect(Collectors.toList()))
        .isEnabled(this.groupEnabled)
        .build();
  }

  public int getRequiredWidth() {
    return table.getWidth();
  }

  public int getRequiredHeight() {
    //The header's height is the same as 1 Row
    return RulesContainer.ROW_PREFERRED_HEIGHT + getContentHeight();
  }

  private int getContentHeight() {
    if (isCollapsed()) {
      return 0;
    }

    //RulesTableGUI's header has the same height as the default row
    int requiredHeight = RulesContainer.ROW_PREFERRED_HEIGHT;
    for (RuleTableRow rule : table.getRules()) {
      //This considers the cases where the advanced sections are collapsed/displayed
      requiredHeight += rule.getNeededHeight();
    }
    return requiredHeight;
  }

  private CollapsiblePanel buildCollapsiblePanel(String title, boolean isEnabled,
      JComponent contentPanel) {
    return new CollapsiblePanel.Builder()
        .withTitle(title)
        .withNamePrefix(getName())
        .withEditableTitle()
        .withEnabled(isEnabled)
        .withEnablingListener(this::updateEnabled)
        .withButtons(makeRulesButtons())
        .withCollapsingListener(groupsSizesUpdate)
        .withContent(contentPanel)
        .build();
  }

  private void updateEnabled(boolean enable) {
    this.groupEnabled = enable;
    container.setEnabled(enable);
    table.setGroupEnabled(enable);
  }

  private List<JButton> makeRulesButtons() {
    ButtonBuilder base = new SwingUtils.ButtonBuilder()
        .withActionListener(this);
    JButton add = base.withName("add").withAction(ADD).withIcon("add.png").build();
    upButton = base.withName("up").withAction(UP).withIcon("up-arrow.png").build();
    downButton = base.withName("down").withAction(DOWN).withIcon("down-arrow.png").build();
    deleteButton = base.withName("delete").withAction(DELETE).withIcon("remove.png").build();
    return Arrays.asList(add, upButton, downButton, deleteButton);
  }

  //Using get instead of is to avoid the JPanels "isEnabled"
  public boolean getEnabled() {
    return this.groupEnabled;
  }

  private RuleTableRow buildRuleConfiguration(CorrelationRule rule) {
    RuleTableRow ruleTableRow = new RuleTableRow(table.getRowCount() + 1,
        this::triggerTableResize, this::displayExtensionManager,
        CorrelationComponentsRegistry.getInstance());
    ruleTableRow.setVariableName(rule.getReferenceName());
    ruleTableRow.setEnabled(rule.isEnabled());
    CorrelationExtractor<?> extractor = rule.getCorrelationExtractor();
    CorrelationRulePartPanel extractorPanel = ruleTableRow.getExtractorConfigurationPanel();
    extractorPanel.setEnabled(rule.isEnabled());
    if (extractor != null) {
      extractorPanel.setValuesFromRulePart(extractor);
    } else {
      extractorPanel.setValuesFromRulePart(CorrelationComponentsRegistry.NONE_EXTRACTOR);
    }
    CorrelationReplacement<?> replacement = rule.getCorrelationReplacement();
    CorrelationRulePartPanel replacementPanel = ruleTableRow.getReplacementConfigurationPanel();
    replacementPanel.setEnabled(rule.isEnabled());
    if (replacement != null) {
      replacementPanel.setValuesFromRulePart(replacement);
    } else {
      replacementPanel.setValuesFromRulePart(CorrelationComponentsRegistry.NONE_REPLACEMENT);
    }
    return ruleTableRow;
  }

  private void initializeRulesTable() {
    table = SwingUtils.createComponent(getName() + "-rulesTableGui-" + System.currentTimeMillis(),
        new RulesTableGui(this::checkButtonsStatus), MAIN_CONTAINER_DEFAULT_DIMENSION);
    table.getSelectionModel().addListSelectionListener(e -> checkButtonsStatus());
    JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrollPane.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        table.doTableResize();
      }
    });
  }

  private void checkButtonsStatus() {
    deleteButton.setEnabled(table.getValues().size() > 0);
    int[] selectedRows = table.getSelectedRows();
    upButton.setEnabled(selectedRows.length > 0 && selectedRows[0] > 0);
    downButton.setEnabled(selectedRows.length > 0
        && selectedRows[selectedRows.length - 1] < table.getValues().size() - 1);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();
    switch (action) {
      case DELETE:
        deleteRule();
        break;
      case UP:
        moveUp();
        break;
      case DOWN:
        moveDown();
        break;
      case ADD:
        addRule();
        break;
      default:
        throw new UnsupportedOperationException();
    }
    modelUpdate.run();
    groupsSizesUpdate.run();
  }

  private void deleteRule() {
    table.delete();
  }

  private void moveUp() {
    table.moveUp();
  }

  private void moveDown() {
    table.moveDown();
  }

  private void addRule() {
    RuleTableRow rule = new RuleTableRow(table.getRowCount() + 1,
        this::triggerTableResize, this::displayExtensionManager,
        CorrelationComponentsRegistry.getInstance());
    //Setting default extractor and replacement when a rule is created
    rule.getExtractorConfigurationPanel().setSelectedItem(new RegexCorrelationExtractor<>());
    rule.getReplacementConfigurationPanel().setSelectedItem(new RegexCorrelationReplacement<>());
    addRule(rule);
  }

  private void addRule(RuleTableRow rule) {
    rule.setGroupEnabled(groupEnabled);
    table.addRow(rule);
  }

  public void displayExtensionManager(CorrelationRulePartTestElement<?> lastSelected) {
    RulePartType selectedType = RulePartType.fromComponent(lastSelected);
    Set<Class<? extends CorrelationRulePartTestElement>> usedExtensions = new HashSet<>();

    getRulesConfiguration().forEach(r -> {
      Class<? extends CorrelationRulePartTestElement> selectedItemClass = (
          selectedType == RulePartType.EXTRACTOR ? r
              .getExtractorConfigurationPanel()
              : r.getReplacementConfigurationPanel()).getSelectedItem().getClass();
      if (CorrelationComponentsRegistry.getInstance().isCustomExtension(selectedItemClass)) {
        usedExtensions.add(selectedItemClass);
      }
    });
    CustomExtensionsDialog extensionDialog = new CustomExtensionsDialog(this::updateComboOptions,
        this);
    extensionDialog.buildExtensions(usedExtensions, selectedType);
    extensionDialog.setVisible(true);
  }

  public List<RuleTableRow> getRulesConfiguration() {
    return table.getRules();
  }

  public void updateComboOptions() {
    getRulesConfiguration().forEach(r -> {
      r.getExtractorConfigurationPanel().updateComboOptions(
          CorrelationComponentsRegistry.getInstance().buildActiveExtractorRulePart());
      r.getReplacementConfigurationPanel().updateComboOptions(
          CorrelationComponentsRegistry.getInstance().buildActiveReplacementRulePart());
    });
    groupsSizesUpdate.run();
  }

  public void updateGroupDimensions(Dimension dimension) {
    JPanel header = container.getHeaderPanel();
    Dimension headerDimension = new Dimension(dimension.width, header.getHeight());
    header.setMinimumSize(headerDimension);
    header.setPreferredSize(headerDimension);

    table.setMinimumSize(dimension);
    table.setPreferredSize(dimension);
  }

  //This method is called when the advanced is shown and when a rule's combo changes
  public void triggerTableResize() {
    table.doTableResize();
    groupsSizesUpdate.run();
  }

  public boolean isCollapsed() {
    return container.isCollapsed();
  }

  @VisibleForTesting
  public RulesTableGui getTable() {
    return table;
  }

  public void setHeaderBorder(boolean isActive) {
    Color borderColor = NonStringValuedTableGui.SELECTED_BACKGROUND.apply(isActive);
    container.getHeaderPanel().setBorder(
        BorderFactory.createMatteBorder(0, 0, 2, 0, borderColor));
    container.setBorder(
        BorderFactory.createMatteBorder(2, 2, 2, 2, borderColor));
  }
}
