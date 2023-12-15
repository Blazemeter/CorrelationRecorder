package com.blazemeter.jmeter.correlation.gui.analysis;

import com.blazemeter.jmeter.correlation.core.templates.Repository;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.gui.common.ThemedIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;

/**
 * Table that contains the list of Correlation Templates with their versions.
 * Added to simplify how we handle the events when selecting a row, a Correlation Template
 * and a Correlation Template Version.
 * It also contains the custom renderer and editor for the TemplateVersion column.
 */
public class TemplatesSelectionTable extends JTable {
  private static final long serialVersionUID = 1L;
  private final TemplateSelectionTableModel model;

  /**
   * Set-ups the basic configuration of the table.
   */
  public TemplatesSelectionTable() {
    this.model = new TemplateSelectionTableModel();
    this.setModel(model);
    this.setDefaultRenderer(Boolean.class, new SelectCheckboxCellRenderer());
    this.setDefaultRenderer(String.class, new TemplateNameCellRenderer());
    this.setDefaultRenderer(TemplateVersionsTableItem.class, new TemplateVersionCellRenderer());

    this.setDefaultEditor(TemplateVersionsTableItem.class, new TemplateVersionCellEditor());
    this.setRowHeight(30);

    TableColumnModel columnModel = getColumnModel();
    columnModel.getColumn(0).setPreferredWidth(60);
    columnModel.getColumn(1).setPreferredWidth(120);
    columnModel.getColumn(2).setPreferredWidth(100);
    columnModel.getColumn(3).setPreferredWidth(150);
  }

  public void addRowSelectionListener(ListSelectionListener selectionListener) {
    getSelectionModel().addListSelectionListener(selectionListener);
  }

  /**
   * Gets the Template (Version) for the selected row.
   *
   * @return Template (Version) for the selected row
   */
  public Template getSelectedTemplateVersion() {
    int selectedRow = getSelectedRow();
    if (selectedRow == -1) {
      return null;
    }

    return getSelectedVersionAt(selectedRow);
  }

  public boolean notSelectedTemplates() {
    return model.notSelectedTemplates();
  }

  private Template getSelectedVersionAt(int row) {
    return model.getSelectedTemplateAt(row);
  }

  /**
   * Selects the first row of the table.
   * Used for QoL when the table is loaded or updated.
   */
  public void selectFirstRow() {
    if (getRowCount() > 0) {
      setRowSelectionInterval(0, 0);
    }
  }

  /**
   * Set the list of repositories stored in the local configuration.
   *
   * @param repositoryMap Map of repositories with the key as the repository name and the value
   */
  public void setRepositories(Map<String, Repository> repositoryMap) {
    model.updateTableContentWithRepositories(repositoryMap);
    setModel(model);
    repaint();
  }

  public Map<String, List<TemplateVersion>> getSelectedTemplateWithRepositoryMap() {
    return model.getSelectedTemplateWithRepositoryMap();
  }

  private static class TemplateVersionCellEditor extends AbstractCellEditor
      implements TableCellEditor, ActionListener {
    private TemplateVersionsTableItem tableItem;

    TemplateVersionCellEditor() {
      this.tableItem = new TemplateVersionsTableItem();
    }

    @Override
    public TemplateVersionsTableItem getCellEditorValue() {
      return this.tableItem;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
      if (value instanceof TemplateVersionsTableItem) {
        this.tableItem = (TemplateVersionsTableItem) value;
      }

      JComboBox<String> versions = new JComboBox<>();

      for (String version : tableItem.getVersions()) {
        versions.addItem(version);
      }

      versions.setSelectedItem(tableItem.getSelectedVersion());
      versions.addActionListener(this);
      setDefaultColors(table, isSelected, versions);

      return versions;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JComboBox<String> combo = (JComboBox<String>) e.getSource();
      String selectedVersion = (String) combo.getSelectedItem();
      tableItem.setSelectedVersion(selectedVersion);
    }
  }

  private static void setDefaultColors(JTable table, boolean isSelected, JComponent target) {
    Color background = isSelected ? table.getSelectionBackground() : table.getBackground();
    Color foreground = isSelected ? table.getSelectionForeground() : table.getForeground();

    target.setForeground(foreground);
    target.setBackground(background);
  }

  private static class TemplateVersionCellRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row,
                                                   int column) {

      setDefaultColors(table, isSelected, this);
      if (value instanceof TemplateVersionsTableItem) {
        TemplateVersionsTableItem tableItem = (TemplateVersionsTableItem) value;
        setText(tableItem.getSelectedTemplateVersion().getVersion());
        setForegroundBasedOnPermissions(table, row, this);
      }

      return this;
    }
  }

  private class TemplateNameCellRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row,
                                                   int column) {

      setDefaultColors(table, isSelected, this);

      TemplateVersionsTableItem item = (TemplateVersionsTableItem) getModel().getValueAt(row, 3);
      setText(item.getName());
      if (column == 1) {
        setText(
            model.getRepositoryDisplayName(item.getSelectedTemplateVersion().getRepositoryId()));
      }

      setForegroundBasedOnPermissions(table, row, this);

      return this;
    }
  }

  private static void setForegroundBasedOnPermissions(JTable table, int row, JComponent target) {
    if (canUseTemplate(table, row)) {
      target.setForeground(UIManager.getColor("Label.foreground"));
    } else {
      target.setForeground(UIManager.getColor("Label.disabledForeground"));
    }
  }

  private static class SelectCheckboxCellRenderer extends DefaultTableCellRenderer {

    private SelectCheckboxCellRenderer() {
      super();
      setHorizontalAlignment(SwingConstants.CENTER);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row,
                                                   int column) {

      setDefaultColors(table, isSelected, this);

      if (!canUseTemplate(table, row)) {
        ImageIcon imageIcon = ThemedIcon.fromResourceName("lock.png");
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setIcon(imageIcon);
        label.setToolTipText("You don't have permission to select this template.");
        label.setOpaque(true);

        setDefaultColors(table, isSelected, label);
        Dimension minimumSize = new Dimension(28, 23);
        label.setMinimumSize(minimumSize);
        label.setSize(minimumSize);
        return label;
      }

      JCheckBox checkbox = new JCheckBox();
      checkbox.setHorizontalAlignment(SwingConstants.CENTER);
      setDefaultColors(table, isSelected, checkbox);

      if (hasFocus) {
        setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
      } else {
        setBorder(new EmptyBorder(1, 1, 1, 1));
      }

      checkbox.setSelected(Boolean.TRUE.equals(value));
      return checkbox;
    }
  }

  private static boolean canUseTemplate(JTable table, int row) {
    TemplateSelectionTableModel model = (TemplateSelectionTableModel) table.getModel();
    return model.canUseTemplate(row);
  }
}
