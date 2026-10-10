package com.blazemeter.jmeter.correlation.gui.automatic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * This class represents a JTableHeader that allows the user to select/deselect all the
 * rows of a table.
 */
public class SelectAllHeader extends JToggleButton implements TableCellRenderer {

  private static final String ALL = "✓ Select all";
  private static final String NONE = "✓ Select none";
  private static final String SOME = "✓ Some Selected";

  private static final Icon ALL_ICON = new Icon() {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      // a blank space: themes such as Windows paint the header with a skin, not with a color
    }

    @Override
    public int getIconWidth() {
      return 10;
    }

    @Override
    public int getIconHeight() {
      return 10;
    }
  };

  private static final Icon SOME_ICON = new Icon() {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Color textColor = c.getForeground();
      JPanel panel = new JPanel();
      Color backgroundColor = panel.getBackground();
      g.setColor(textColor);
      g.fillRect(x, y, getIconWidth(), getIconHeight());
      g.setColor(backgroundColor);
      g.fillRect(x + 2, y + 2, getIconWidth() - 4, getIconHeight() - 4);
    }

    @Override
    public int getIconWidth() {
      return 10;
    }

    @Override
    public int getIconHeight() {
      return 10;
    }
  };

  private static final Icon NONE_ICON = new Icon() {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(c.getForeground());
      // drawRect covers width + 1 pixels: keep the box inside the icon
      g.drawRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
    }

    @Override
    public int getIconWidth() {
      return 10;
    }

    @Override
    public int getIconHeight() {
      return 10;
    }
  };

  private final JTable table;
  private final TableModel tableModel;
  private final JTableHeader header;
  private final TableColumnModel tcm;
  private final int targetColumn;
  private int viewColumn;
  private boolean updating = false;

  public SelectAllHeader(JTable table, int targetColumn) {
    super(ALL, getCheckSelectedIcon());
    this.table = table;
    this.tableModel = table.getModel();
    if (tableModel.getColumnClass(targetColumn) != Boolean.class) {
      throw new IllegalArgumentException("Boolean column required.");
    }
    this.targetColumn = targetColumn;
    this.header = table.getTableHeader();
    this.tcm = table.getColumnModel();
    this.addItemListener(new RowCheckBoxUpdateListener());
    header.addMouseListener(new MouseHandler());
    tableModel.addTableModelListener(new ModelHandler());
  }

  /**
   * Paints the column with the look and feel's own header renderer, so it reads like the other
   * headers in every theme; this toggle only keeps the state, its text and its icon.
   */
  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
    Component renderer = header.getDefaultRenderer()
        .getTableCellRendererComponent(table, getText(), isSelected, hasFocus, row, column);
    if (renderer instanceof JLabel) {
      ((JLabel) renderer).setIcon(getIcon());
    }
    return renderer;
  }

  private class RowCheckBoxUpdateListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
      try {
        boolean state = e.getStateChange() == ItemEvent.SELECTED;
        setText((state) ? NONE : ALL);
        setIcon((state) ? NONE_ICON : ALL_ICON);
        header.repaint();
        int modelRowCount = table.getModel().getRowCount();
        int tableRowCount = table.getRowCount();
        // We are clearing the table
        if (modelRowCount != tableRowCount && modelRowCount == 0) {
          return;
        }
        for (int r = 0; r < tableRowCount; r++) {
          updating = true;
          table.setValueAt(state, r, viewColumn);
        }
      } catch (IndexOutOfBoundsException ex) {
        // This is thrown when the table is cleared
        return;
      } finally {
        updating = false;
      }
    }
  }

  private class MouseHandler extends MouseAdapter {

    @Override
    public void mouseClicked(MouseEvent e) {
      viewColumn = header.columnAtPoint(e.getPoint());
      int modelColumn = tcm.getColumn(header.columnAtPoint(e.getPoint())).getModelIndex();
      if (modelColumn == targetColumn) {
        doClick();
      }
    }
  }

  private class ModelHandler implements TableModelListener {

    @Override
    public void tableChanged(TableModelEvent e) {
      if (updating) {
        System.out.println("Updating rows. Stopping triggers.");
        return;
      }

      if (needsToggle()) {
        doClick();
        header.repaint();
      }
    }
  }

  // Return true if this toggle needs to match the model.
  private boolean needsToggle() {
    boolean allTrue = true;
    boolean allFalse = true;
    for (int row = 0; row < tableModel.getRowCount(); row++) {
      boolean isSelected = (Boolean) tableModel.getValueAt(row, targetColumn);
      allTrue &= isSelected;
      allFalse &= !isSelected;
    }

    if (!allFalse && !allTrue) {
      if (!getText().equals(SOME)) {
        setText(SOME);
        setIcon(SOME_ICON);
        header.repaint();
      }
      return false;
    }

    boolean toggle = allTrue && !isSelected() || allFalse && isSelected();
    if (!toggle && getText().equals(SOME)) {
      // back to all or none without a toggle: show this toggle's state again
      setText(isSelected() ? NONE : ALL);
      setIcon(isSelected() ? NONE_ICON : ALL_ICON);
      header.repaint();
    }
    return toggle;
  }

  // Return the icon used for a JCheckBox when it is selected.
  public static Icon getCheckSelectedIcon() {
    return ALL_ICON;
  }

  // Return the icon used for a JCheckBox when it is not selected.
  public Icon getDeselectedIcon() {
    return UIManager.getIcon("CheckBox.deselectedIcon");
  }
}
