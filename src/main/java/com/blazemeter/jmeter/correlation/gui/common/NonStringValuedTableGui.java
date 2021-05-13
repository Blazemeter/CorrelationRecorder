package com.blazemeter.jmeter.correlation.gui.common;

import com.blazemeter.jmeter.correlation.gui.RulesContainer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.apache.jorphan.gui.GuiUtils;

public abstract class NonStringValuedTableGui<T> extends JTable {

  public static final Function<Boolean, Color> SELECTED_BACKGROUND = isSelected -> isSelected
      ? UIManager.getColor("Table.selectionBackground") : UIManager.getColor("Table.background");
  protected static final Function<Boolean, Color> SELECTED_FOREGROUND = isSelected -> isSelected
      ? UIManager.getColor("Table.selectionForeground") : UIManager.getColor("Table.foreground");
  protected final ArrayList<T> list = new ArrayList<>();
  protected final NonStringValuedModel model = new NonStringValuedModel();
  protected final Runnable buttonsValidation;
  private final String[] columnsNames;

  public NonStringValuedTableGui(Runnable buttonsValidation, String[] columnsNames) {
    this.buttonsValidation = buttonsValidation;
    this.columnsNames = columnsNames;
    getSelectionModel().addListSelectionListener(event -> buttonsValidation.run());
    setModel(model);
    setRowHeight(RulesContainer.ROW_PREFERRED_HEIGHT);
    getTableHeader().setReorderingAllowed(false);
  }

  protected void addTableModelListener(Runnable method) {
    model.addTableModelListener(e -> method.run());
  }

  public List<T> getValues() {
    return list;
  }

  public void addRow(T value) {
    if (!list.isEmpty()) {
      GuiUtils.stopTableEditing(this);
    }
    model.addRow(value);
    selectLast();
    validateButtons();
  }

  private void validateButtons() {
    if (buttonsValidation != null) {
      buttonsValidation.run();
    }
  }

  private void selectLast() {
    if (list.size() > 0) {
      int last = list.size() - 1;
      setRowSelectionInterval(last, last);
    }
  }

  public void delete() {
    GuiUtils.cancelEditing(this);
    int[] rowsSelected = getSelectedRows();
    int anchorSelection = getSelectionModel().getAnchorSelectionIndex();
    clearSelection();

    if (rowsSelected.length > 0) {
      for (int i = rowsSelected.length - 1; i >= 0; i--) {
        removeRow(rowsSelected[i]);
      }
    }

    if (getRowCount() > 0) {
      if (anchorSelection >= getRowCount()) {
        anchorSelection = getRowCount() - 1;
      }
      setRowSelectionInterval(anchorSelection, anchorSelection);
    }

    validateButtons();
  }

  public void removeRow(int row) {
    model.removeRow(row);
    selectLast();
  }

  public void moveUp() {
    // get the selected rows before stopping editing
    // or the selected rows will be unselected
    int[] selectedRows = getSelectedRows();
    GuiUtils.stopTableEditing(this);

    if (selectedRows.length > 0 && selectedRows[0] > 0) {
      clearSelection();
      for (int selectedIndex : selectedRows) {
        switchRows(selectedIndex, selectedIndex - 1);
      }

      for (int rowSelected : selectedRows) {
        addRowSelectionInterval(rowSelected - 1, rowSelected - 1);
      }
    }
    validateButtons();
  }

  public void moveDown() {
    int[] rowsSelected = getSelectedRows();
    GuiUtils.stopTableEditing(this);

    if (rowsSelected.length > 0 && rowsSelected[rowsSelected.length - 1] < getRowCount() - 1) {
      clearSelection();
      for (int i = rowsSelected.length - 1; i >= 0; i--) {
        int rowSelected = rowsSelected[i];
        switchRows(rowSelected, rowSelected + 1);
      }
      for (int rowSelected : rowsSelected) {
        addRowSelectionInterval(rowSelected + 1, rowSelected + 1);
      }
    }
    validateButtons();
  }

  private void switchRows(int source, int dest) {
    model.switchRows(source, dest);
  }

  public void clear() {
    if (!list.isEmpty()) {
      GuiUtils.stopTableEditing(NonStringValuedTableGui.this);
    }
    clearSelection();
    list.clear();
  }

  public void configureColumn(String columnName, TableCellRenderer renderer,
      TableCellEditor editor) {
    TableColumn groupColumn = getColumn(columnName);
    if (groupColumn == null) {
      return;
    }

    if (renderer != null) {
      groupColumn.setCellRenderer(renderer);
    }

    if (editor != null) {
      groupColumn.setCellEditor(editor);
    }
  }

  @Override
  public void setValueAt(Object cellValue, int row, int col) {
    list.set(row, (T) cellValue);
  }

  public abstract Object getValueAt(int row, int col);

  public abstract Class<?> getColumnsClass(int col);

  private class NonStringValuedModel extends DefaultTableModel implements Iterable<T> {

    private void addRow(T value) {
      if (!list.isEmpty()) {
        GuiUtils.stopTableEditing(NonStringValuedTableGui.this);
      }
      list.add(value);
      int rowToSelect = this.getRowCount() - 1;
      NonStringValuedTableGui.this.setRowSelectionInterval(rowToSelect, rowToSelect);
      NonStringValuedTableGui.this
          .scrollRectToVisible(NonStringValuedTableGui.this.getCellRect(rowToSelect, 0, true));
      fireTableDataChanged();
    }

    @Override
    public void removeRow(int row) {
      list.remove(row);
    }

    private void switchRows(int source, int dest) {
      Collections.swap(list, source, dest);
    }

    @Override
    public Object getValueAt(int row, int col) {
      return NonStringValuedTableGui.this.getValueAt(row, col);
    }

    @Override
    public void setValueAt(Object cellValue, int row, int col) {
      NonStringValuedTableGui.this.setValueAt(cellValue, row, col);
    }

    @Override
    public Class<?> getColumnClass(int col) {
      return NonStringValuedTableGui.this.getColumnsClass(col);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return true;
    }

    @Override
    public Iterator<T> iterator() {
      return list.iterator();
    }

    @Override
    public int getRowCount() {
      return list == null ? 0 : list.size();
    }

    public int getColumnCount() {
      return columnsNames.length;
    }

    public String getColumnName(int col) {
      return columnsNames[col];
    }
  }
}
