package com.blazemeter.jmeter.correlation.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.apache.jorphan.gui.GuiUtils;

public class RulesTable extends JTable {

  protected static final String EXTRACTOR_HEADER = "Correlation Extractor";
  protected static final String REPLACEMENT_HEADER = "Correlation Replacement";
  protected static final String VARIABLE_HEADER = "Reference Variable";

  private final ArrayList<RuleConfiguration> rules = new ArrayList<>();
  private final RulesTableModel model = new RulesTableModel();

  RulesTable() {
    setModel(model);
    model.addTableModelListener(e -> doTableResize());
    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    setRowHeight(RulesContainer.ROW_PREFERRED_HEIGHT);
    TableColumn variableColumn = getColumn(VARIABLE_HEADER);
    variableColumn.setCellRenderer(new CustomCellRenderer());
    variableColumn.setCellEditor(new VariableEditor());

    TableColumn extractorColumn = getColumn(EXTRACTOR_HEADER);
    extractorColumn.setCellRenderer(new CustomCellRenderer());
    extractorColumn.setCellEditor(new SelectorEditor());

    TableColumn replacementColumn = getColumn(REPLACEMENT_HEADER);
    replacementColumn.setCellRenderer(new CustomCellRenderer());
    replacementColumn.setCellEditor(new SelectorEditor());
    doTableResize();
    getTableHeader().setReorderingAllowed(false);
  }

  public void addRow(RuleConfiguration rule) {
    model.addRow(rule);
  }

  public void removeRow(int row) {
    model.removeRow(row);
  }

  public void switchRows(int source, int dest) {
    model.switchRows(source, dest);
  }

  public void clear() {
    GuiUtils.stopTableEditing(RulesTable.this);
    clearSelection();
    rules.clear();
  }

  public List<RuleConfiguration> getRules() {
    return rules;
  }

  private Object getValueFromRulesAt(int row, int col) {
    switch (col) {
      case 0:
        return rules.get(row).isEnable();
      case 1:
        return rules.get(row).getReferenceVariableField();
      case 2:
        return rules.get(row).getExtractorConfigurationPanel();
      default:
        return rules.get(row).getReplacementConfigurationPanel();
    }
  }

  private void doTableRepaint() {
    validate();
    repaint();
  }

  public void doTableResize() {
    int isEnableWidth = 30;
    int refVariableWidth = 110;
    int fixedSizes = isEnableWidth + refVariableWidth;
    int columnsCount = model.getColumnCount() - 2;
    int defaultWidth =
        getParent() == null ?
            (RulesContainer.MAIN_CONTAINER_WIDTH - fixedSizes) / columnsCount
            : (getParent().getWidth() - fixedSizes) / columnsCount;

    int maxExtractorWidth = rules.stream()
        .mapToInt(r -> r.getExtractorConfigurationPanel().getSumComponentsWidth())
        .max()
        .orElse(0);

    int maxReplacementWidth = rules.stream()
        .mapToInt(r -> r.getReplacementConfigurationPanel().getSumComponentsWidth())
        .max()
        .orElse(0);

    maxExtractorWidth = Math.max(maxExtractorWidth, defaultWidth);
    maxReplacementWidth = Math.max(maxReplacementWidth, defaultWidth);

    Dimension calculatedSize = new Dimension(
        maxExtractorWidth + maxReplacementWidth + fixedSizes,
        rules.size() * RulesContainer.ROW_PREFERRED_HEIGHT);
    setSize(calculatedSize);
    setPreferredSize(calculatedSize);
    doColumnsResize(isEnableWidth, refVariableWidth, maxExtractorWidth, maxReplacementWidth);
    doTableRepaint();
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
      Component component;
      if (value instanceof JTextField) {
        component = (JTextField) value;
        component.setPreferredSize(
            new Dimension(table.getColumnModel().getColumn(column).getWidth(), Integer.MAX_VALUE));
      } else {
        component = (ConfigurationPanel) value;
        component
            .setPreferredSize(new Dimension(table.getColumnModel().getColumn(column).getWidth(),
                Integer.MAX_VALUE));
        component
            .setBackground((isSelected) ? table.getSelectionBackground()
                : table.getBackground());
        component
            .setForeground((isSelected) ? table.getSelectionForeground()
                : table.getForeground());
      }
      component.repaint();
      return component;
    }
  }

  private static class VariableEditor extends AbstractCellEditor implements TableCellEditor {

    private JTextField referenceVariable;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
        int row, int column) {
      referenceVariable = (JTextField) value;
      return referenceVariable;
    }

    @Override
    public Object getCellEditorValue() {
      return referenceVariable;
    }
  }

  private static class SelectorEditor extends AbstractCellEditor implements TableCellEditor {

    private ConfigurationPanel selectorPanel;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
        int row, int column) {
      selectorPanel = (ConfigurationPanel) value;
      selectorPanel.setBackground(table.getSelectionBackground());
      selectorPanel.setForeground(table.getSelectionForeground());
      selectorPanel.repaint();
      return selectorPanel;
    }

    @Override
    public Object getCellEditorValue() {
      return selectorPanel;
    }
  }

  private class RulesTableModel extends DefaultTableModel implements Iterable<RuleConfiguration> {

    private final String[] columnNames = {"", VARIABLE_HEADER, EXTRACTOR_HEADER,
        REPLACEMENT_HEADER};

    private void addRow(RuleConfiguration value) {
      GuiUtils.stopTableEditing(RulesTable.this);
      rules.add(value);
      int rowToSelect = this.getRowCount() - 1;
      RulesTable.this.setRowSelectionInterval(rowToSelect, rowToSelect);
      RulesTable.this.scrollRectToVisible(RulesTable.this.getCellRect(rowToSelect, 0, true));
      doTableResize();
    }

    @Override
    public void removeRow(int row) {
      rules.remove(row);
      fireTableRowsDeleted(row, row);
      doTableResize();
    }

    private void switchRows(int source, int dest) {
      Collections.swap(rules, source, dest);
    }

    @Override
    public Object getValueAt(int row, int col) {
      return getValueFromRulesAt(row, col);
    }

    @Override
    public void setValueAt(Object cellValue, int row, int col) {
      if (col == 0) {
        rules.get(row).setEnable((Boolean) cellValue);
        doTableRepaint();
      } else if (col == 1) {
        rules.get(row).setVariableName(
            (cellValue instanceof JTextField ? ((JTextField) cellValue).getText()
                : (String) cellValue));
      } else if (col == 2) {
        rules.get(row).setExtractorConfigurationPanel((ConfigurationPanel) cellValue);
      } else {
        rules.get(row).setReplacementConfigurationPanel((ConfigurationPanel) cellValue);
      }
    }

    @Override
    public Class<?> getColumnClass(int col) {
      if (col == 0) {
        return Boolean.class;
      } else if (col == 1) {
        return JTextField.class;
      } else {
        return ConfigurationPanel.class;
      }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return true;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<RuleConfiguration> iterator() {
      return rules.iterator();
    }

    @Override
    public int getRowCount() {
      return rules == null ? 0 : rules.size();
    }

    public int getColumnCount() {
      return columnNames.length;
    }

    public String getColumnName(int col) {
      return columnNames[col];
    }
  }

}
