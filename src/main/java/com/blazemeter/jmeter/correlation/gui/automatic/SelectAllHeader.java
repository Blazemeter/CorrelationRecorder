package com.blazemeter.jmeter.correlation.gui.automatic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
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
      g.setColor(new Label().getBackground());
      g.fillRect(x, y, getIconWidth(), getIconHeight());
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
      Color textColor = new Label().getForeground();
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
      g.setColor(new Label().getForeground());
      g.drawRect(x, y, getIconWidth(), getIconHeight());
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
    this.applyTableHeaderStyle();
    this.addItemListener(new RowCheckBoxUpdateListener());
    header.addMouseListener(new MouseHandler());
    tableModel.addTableModelListener(new ModelHandler());
  }

  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
    return this;
  }

  private class RowCheckBoxUpdateListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
      try {
        boolean state = e.getStateChange() == ItemEvent.SELECTED;
        setText((state) ? NONE : ALL);
        setIcon((state) ? NONE_ICON : ALL_ICON);
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
        updating = false;
      } catch (ArrayIndexOutOfBoundsException ex) {
        // This is thrown when the table is cleared
        return;
      }
    }
  }

  @Override
  public void updateUI() {
    super.updateUI();
    applyTableHeaderStyle();
  }

  /**
   * Makes the toggle button look like a table header.
   */
  private void applyTableHeaderStyle() {
    this.setFont(UIManager.getFont("TableHeader.font"));
    this.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
    this.setBackground(UIManager.getColor("TableHeader.background"));
    this.setForeground(UIManager.getColor("TableHeader.foreground"));
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
    }

    return allTrue && !isSelected() || allFalse && isSelected();
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
