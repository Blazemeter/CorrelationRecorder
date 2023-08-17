package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.gui.common.NonStringValuedTableGui;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.apache.jorphan.gui.GuiUtils;

public class GroupsTableGui extends NonStringValuedTableGui<GroupPanel> {

  protected static final String PROTOCOL_HEADER = "Correlation Rules Groups";

  public GroupsTableGui(Runnable buttonsValidation) {
    super(buttonsValidation, new String[] {PROTOCOL_HEADER});
    configureColumn(PROTOCOL_HEADER, new GroupPanelRenderer(), new GroupPanelEditor());
    getSelectionModel().addListSelectionListener(event -> this.updateActiveGroup());
  }

  @Override
  public Object getValueAt(int row, int col) {
    return list.get(row);
  }

  @Override
  public Class<?> getColumnsClass(int col) {
    return GroupPanel.class;
  }

  public List<GroupPanel> getGroups() {
    return list;
  }

  public void clear() {
    if (!list.isEmpty()) {
      GuiUtils.stopTableEditing(GroupsTableGui.this);
    }
    clearSelection();
    list.clear();
  }

  public void updateActiveGroup() {
    int selectedIndex = getSelectedRow();
    List<GroupPanel> groups = getGroups();
    for (int i = 0; i < groups.size(); i++) {
      groups.get(i).setHeaderBorder(selectedIndex == i);
    }
  }

  private static class GroupPanelRenderer implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      Component component;
      if (value instanceof JTextField) {
        component = (JTextField) value;
        component.setPreferredSize(
            new Dimension(table.getColumnModel().getColumn(column).getWidth(), Integer.MAX_VALUE));
      } else {
        component = (GroupPanel) value;
        component.setPreferredSize(
            new Dimension(table.getColumnModel().getColumn(column).getWidth(), Integer.MAX_VALUE));
        component.setBackground(SELECTED_BACKGROUND.apply(isSelected));
        component.setForeground(SELECTED_FOREGROUND.apply(isSelected));
      }
      component.repaint();
      return component;
    }
  }

  private static class GroupPanelEditor extends AbstractCellEditor implements TableCellEditor {

    private GroupPanel groupPanel;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                 int row, int column) {
      groupPanel = (GroupPanel) value;
      groupPanel.setBackground(SELECTED_BACKGROUND.apply(true));
      groupPanel.setForeground(SELECTED_FOREGROUND.apply(true));
      groupPanel.repaint();
      return groupPanel;
    }

    @Override
    public Object getCellEditorValue() {
      return groupPanel;
    }
  }
}
