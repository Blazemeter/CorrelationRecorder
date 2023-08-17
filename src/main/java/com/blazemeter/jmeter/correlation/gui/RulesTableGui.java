package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.commons.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.common.NonStringValuedTableGui;
import com.blazemeter.jmeter.correlation.gui.common.PlaceHolderTextField;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class RulesTableGui extends NonStringValuedTableGui<RuleTableRow> {

  protected static final String ENABLE_HEADER = "";
  protected static final String EXTRACTOR_HEADER = "Correlation Extractor";
  protected static final String REPLACEMENT_HEADER = "Correlation Replacement";
  protected static final String VARIABLE_HEADER = "Reference Variable";

  private boolean groupEnabled = true;

  public RulesTableGui(Runnable buttonsValidation) {
    super(buttonsValidation, new String[] {ENABLE_HEADER, VARIABLE_HEADER,
        EXTRACTOR_HEADER, REPLACEMENT_HEADER});
    addTableModelListener(this::doTableResize);
    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    CustomCellRenderer cellRenderer = new CustomCellRenderer();
    configureColumn(ENABLE_HEADER, new EnableRenderer(), new EnableEditor());
    configureColumn(VARIABLE_HEADER, buildVariableRenderer(), new VariableEditor());
    configureColumn(EXTRACTOR_HEADER, cellRenderer, new SelectorEditor());
    configureColumn(REPLACEMENT_HEADER, cellRenderer, new SelectorEditor());
    doTableResize();
  }

  private TableCellRenderer buildVariableRenderer() {

    return new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        PlaceHolderTextField field = (PlaceHolderTextField) value;
        boolean displayPlaceHolder = field.getText().isEmpty();
        setValue(displayPlaceHolder ? field.getPlaceHolder() : field.getText());

        //PlaceHolder text should look like disabled
        boolean displayEnabled = (groupEnabled && list.get(row).isEnabled()
            && !displayPlaceHolder);
        /* 
          Forces a "new font color" to avoid issues for JMeter 5.4+ where it doesn't update the 
          Field font color if it comes come `new JTextField().getDisabledTextColor()`
         */
        Color color = new Color(SwingUtils.getEnabledForegroundColor(displayEnabled).getRGB());
        setForeground(color);
        setBackground(table.getBackground());

        return this;
      }
    };
  }

  public void setGroupEnabled(boolean enabled) {
    this.groupEnabled = enabled;
    list.forEach(r -> r.setGroupEnabled(enabled));
    repaint();
  }

  @Override
  public Object getValueAt(int row, int col) {
    switch (col) {
      case 0:
        return list.get(row).isEnabled();
      case 1:
        return list.get(row).getReferenceVariableField();
      case 2:
        return list.get(row).getExtractorConfigurationPanel();
      default:
        return list.get(row).getReplacementConfigurationPanel();
    }
  }

  @Override
  public void setValueAt(Object cellValue, int row, int col) {
    if (col == 0) {
      list.get(row).setEnabled((Boolean) cellValue);
    } else if (col == 1) {
      list.get(row).setVariableName(
          (cellValue instanceof JTextField ? ((JTextField) cellValue).getText()
              : (String) cellValue));
    } else if (col == 2) {
      list.get(row).setExtractorConfigurationPanel((CorrelationRulePartPanel) cellValue);
    } else {
      list.get(row).setReplacementConfigurationPanel((CorrelationRulePartPanel) cellValue);
    }
  }

  @Override
  public Class<?> getColumnsClass(int col) {
    switch (col) {
      case 0:
        return Boolean.class;
      case 1:
        return String.class;
      default:
        return CorrelationRulePartPanel.class;
    }
  }

  public List<RuleTableRow> getRules() {
    return list;
  }

  public int getColumnDefaultWidth(int fixedWidth) {
    //Unless we add another dynamic column, Extractor and Replacement are the only dynamic ones
    int dynamicColumns = 2;
    return ((getParent() == null ? RulesContainer.MAIN_CONTAINER_WIDTH : getParent().getWidth())
        - fixedWidth) / dynamicColumns;
  }

  public void doTableResize() {
    int rowCount = 0;
    int totalHeight = RulesContainer.ROW_PREFERRED_HEIGHT;
    for (RuleTableRow rule : list) {
      int neededHeight = rule.getNeededHeight();
      setRowHeight(rowCount, neededHeight);
      totalHeight += neededHeight;
      rowCount++;
    }

    int isEnableWidth = 30;
    int refVariableWidth = 110;
    int fixedSizes = isEnableWidth + refVariableWidth;
    int defaultWidth = getColumnDefaultWidth(fixedSizes);

    int maxExtractorWidth = list.stream()
        .mapToInt(r -> r.getExtractorConfigurationPanel().calculateRequiredColumnWidth())
        .max()
        .orElse(defaultWidth);

    int maxReplacementWidth = list.stream()
        .mapToInt(r -> r.getReplacementConfigurationPanel().calculateRequiredColumnWidth())
        .max()
        .orElse(defaultWidth);

    Dimension calculatedSize = new Dimension(maxExtractorWidth + maxReplacementWidth + fixedSizes,
        totalHeight);

    setSize(calculatedSize);
    setPreferredSize(calculatedSize);
    doColumnsResize(isEnableWidth, refVariableWidth, maxExtractorWidth, maxReplacementWidth);
    revalidate();
    repaint();
  }

  private void doColumnsResize(int isEnableWidth, int varColumnWidth, int extractorColumnWidth,
                               int replacementColumnWidth) {
    setColumnWidth(0, isEnableWidth);
    setColumnWidth(1, varColumnWidth);
    setColumnWidth(2, extractorColumnWidth);
    setColumnWidth(3, replacementColumnWidth);
  }

  private void setColumnWidth(int column, int width) {
    TableColumn model = getColumnModel().getColumn(column);
    model.setWidth(width);
    getTableHeader().setResizingColumn(model);
  }

  private static class CustomCellRenderer implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {

      CorrelationRulePartPanel rulePartPanel = (CorrelationRulePartPanel) value;
      Component component = rulePartPanel;
      component.setPreferredSize(
          new Dimension(table.getColumnModel().getColumn(column).getWidth(), Integer.MAX_VALUE));
      Color background = (isSelected) ? table.getSelectionBackground() : table.getBackground();
      Color foreground = (isSelected) ? table.getSelectionForeground() : table.getForeground();
      component.setBackground(background);
      component.setForeground(foreground);
      rulePartPanel.getAdvancedPanel().setBackground(background);
      rulePartPanel.getAdvancedPanel().setForeground(foreground);
      return component;
    }
  }

  private static class EnableRenderer implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      JCheckBox enableCheckBox = new JCheckBox();
      enableCheckBox.setSelected((Boolean) value);
      return enableCheckBox;
    }
  }

  private static class VariableEditor extends AbstractCellEditor implements TableCellEditor {

    private PlaceHolderTextField referenceVariable;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                 int row, int column) {
      referenceVariable = (PlaceHolderTextField) value;
      return referenceVariable;
    }

    @Override
    public Object getCellEditorValue() {
      return referenceVariable;
    }
  }

  private static class SelectorEditor extends AbstractCellEditor implements TableCellEditor {

    private CorrelationRulePartPanel selectorPanel;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                 int row, int column) {
      selectorPanel = (CorrelationRulePartPanel) value;
      Color selectionBackground = SELECTED_BACKGROUND.apply(true);
      Color selectionForeground = SELECTED_FOREGROUND.apply(true);
      selectorPanel.setBackground(selectionBackground);
      selectorPanel.setForeground(selectionForeground);
      selectorPanel.getAdvancedPanel().setBackground(selectionBackground);
      selectorPanel.getAdvancedPanel().setForeground(selectionForeground);
      selectorPanel.repaint();
      return selectorPanel;
    }

    @Override
    public Object getCellEditorValue() {
      return selectorPanel;
    }
  }

  private static class EnableEditor extends AbstractCellEditor implements TableCellEditor {

    private final JCheckBox enableCheckBox = new JCheckBox();

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                 int row, int column) {
      enableCheckBox.addActionListener(e -> {
        //This code is to fire the table repaint when an action occurs in the checkbox, after the
        // event is fired it is needed to request focus in the checkbox because stopCellEditing
        // remove the focus
        stopCellEditing();
        enableCheckBox.requestFocus();
      });

      enableCheckBox.setSelected((Boolean) value);
      enableCheckBox.setBackground(SELECTED_BACKGROUND.apply(true));
      enableCheckBox.setForeground(SELECTED_FOREGROUND.apply(true));
      enableCheckBox.repaint();
      return enableCheckBox;
    }

    @Override
    public Object getCellEditorValue() {
      return enableCheckBox.isSelected();
    }
  }
}
