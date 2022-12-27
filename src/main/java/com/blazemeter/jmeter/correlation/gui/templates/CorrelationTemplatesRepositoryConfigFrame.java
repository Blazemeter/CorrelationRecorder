package com.blazemeter.jmeter.correlation.gui.templates;

import com.blazemeter.jmeter.correlation.core.DescriptionContent;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.gui.common.HelperDialog;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.common.ThemedIconLabel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.GuiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationTemplatesRepositoryConfigFrame extends JDialog implements ActionListener {

  private static final Logger LOG = LoggerFactory
      .getLogger(CorrelationTemplatesRepositoryConfigFrame.class);

  private static final String[] REPOSITORY_TABLE_HEADERS = {"Id", "URL"};
  private static final String ADD_ACTION = "add";
  private static final String DELETE_ACTION = "delete";
  private static final String SAVE_ACTION = "save";
  private final CorrelationTemplatesRepositoriesRegistryHandler repositoryHandler;
  private final JButton removeButton = SwingUtils
      .buildJButton("repositoryRemoveButton", "Remove", DELETE_ACTION, this);
  private final JButton saveButton = SwingUtils
      .buildJButton("repositorySaveButton", "Save", SAVE_ACTION, this);
  private final RepositoryInputTableModel repositoryTableModel = new RepositoryInputTableModel();
  private final JTable repositoryTable = SwingUtils
      .createComponent("repositoriesTable", new JTable(repositoryTableModel));
  private final JLabel helper = new ThemedIconLabel("help.png");
  private boolean isChanged = false;
  private HelperDialog helperDialog;

  public CorrelationTemplatesRepositoryConfigFrame(
      CorrelationTemplatesRepositoriesRegistryHandler repositoryHandler, Dialog owner) {
    super(owner, "Repositories Manager", true);
    setName("correlationTemplatesRepositoryConfigFrame");
    this.repositoryHandler = repositoryHandler;
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    layout.setAutoCreateContainerGaps(true);
    panel.setLayout(layout);
    Component repo = makeRepositoryTablePanel();
    Component button = makeButtonPanel();
    layout.setHorizontalGroup(layout.createParallelGroup()
        .addComponent(repo)
        .addComponent(button));

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addComponent(repo)
        .addComponent(button));

    add(panel);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        onCloseWindowsValidation();
      }
    });

    pack();
  }

  private JScrollPane makeRepositoryTablePanel() {
    repositoryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    int textFieldPreferredSize = new JTextField().getPreferredSize().height;
    repositoryTable.setRowHeight(textFieldPreferredSize);
    repositoryTable
        .setPreferredScrollableViewportSize(new Dimension(500, textFieldPreferredSize * 20));
    repositoryTable.setEnabled(true);
    JMeterUtils.applyHiDPI(repositoryTable);

    // making central repository immutable
    Class<?> centralRepository = repositoryTable.getColumnClass(0);
    repositoryTable.getDefaultEditor(centralRepository).addCellEditorListener(repositoryTableModel);
    repositoryTable.setDefaultRenderer(centralRepository,
        buildDefaultRender(repositoryTable.getDefaultRenderer(centralRepository)));
    repositoryTable.revalidate();
    updateRepositoriesTable();
    return new JScrollPane(repositoryTable);
  }

  private void onCloseWindowsValidation() {
    if (isChanged) {
      int action = saveChangesWarningMessage();
      switch (action) {
        case JOptionPane.YES_OPTION:
          saveChanges();
          if (!isChanged) {
            dispose();
          }
          break;
        case JOptionPane.NO_OPTION:
          dispose();
          break;
        case JOptionPane.CANCEL_OPTION:
          break;
        default:
          //Shouldn't reach this point since the Window only has Yes/No options
          throw new UnsupportedOperationException(
              "Unsupported action ({" + action + "}) triggered");
      }
    } else {
      dispose();
    }
  }

  private TableCellRenderer buildDefaultRender(TableCellRenderer defaultRenderer) {
    return new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
          boolean hasFocus, int row, int column) {
        if (row == 0 && (column == 0 || column == 1)) {
          setBackground(table.getBackground());
          setValue(value);
          setFocusable(false);
          setEnabled(false);
          return this;
        }
        return defaultRenderer
            .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }
    };
  }

  private void updateRepositoriesTable() {
    GuiUtils.cancelEditing(repositoryTable);
    List<CorrelationTemplatesRepository> listCorrelationRepositories = repositoryHandler
        .getCorrelationRepositories();
    repositoryTableModel.clear();
    listCorrelationRepositories.forEach(r -> {
      if (!r.getName().equals(LocalConfiguration.LOCAL_REPOSITORY_NAME)) {
        if (r.getName().equals(LocalConfiguration.CENTRAL_REPOSITORY_ID)) {
          repositoryTableModel.addRow(0,
              new RepositoryInput(r.getName(), repositoryHandler.getRepositoryURL(r.getName())));
        } else {
          repositoryTableModel.addRow(
              new RepositoryInput(r.getName(), repositoryHandler.getRepositoryURL(r.getName())));
        }
      }
    });
    repositoryTable.clearSelection();
    repositoryTable.repaint();
  }

  private JPanel makeButtonPanel() {
    JButton addButton = SwingUtils.buildJButton("repositoryAddButton", "Add", ADD_ACTION, this);
    updateEnabledButtons();
    String descriptionFile = DescriptionContent.getFromClass(this.getClass());
    helper.setName("helperIcon");
    helper.setToolTipText("Get more information");
    helper.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    Dimension helperDimension = new Dimension(35, 35);
    helper.setPreferredSize(helperDimension);
    helper.setSize(helperDimension);
    helper.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        helperDialog = new HelperDialog(CorrelationTemplatesRepositoryConfigFrame.this);
        helperDialog.setName("helperDialog");
        helperDialog.setTitle("Repository Manager Information");
        helperDialog.updateDialogContent(descriptionFile);
        helperDialog.setVisible(true);
        helperDialog.updateDialogContent(descriptionFile);
      }
    });

    JPanel buttonPanel = SwingUtils.createComponent("buttonPanel", new JPanel());
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
    buttonPanel.add(addButton);
    buttonPanel.add(removeButton);
    buttonPanel.add(saveButton);
    buttonPanel.add(helper);

    return buttonPanel;
  }

  private void updateEnabledButtons() {
    int rowCount = repositoryTableModel.getRowCount();
    removeButton.setEnabled(isEnabled() && rowCount != 0);
    saveButton.setEnabled(isEnabled() && isChanged);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();
    switch (action) {
      case DELETE_ACTION:
        deleteRepositoryInput();
        isChanged = true;
        break;
      case ADD_ACTION:
        addRepositoryInput();
        isChanged = true;
        break;
      case SAVE_ACTION:
        saveChanges();
        break;
      default:
        LOG.warn("Unsupported action {}", action);
        JMeterUtils
            .reportErrorToUser("Unsupported action " + action, "Managing Repositories & Templates");
    }
    updateEnabledButtons();
  }

  private void saveChanges() {
    GuiUtils.stopTableEditing(repositoryTable);
    List<String> errors = new ArrayList<>(checkFieldValid());

    if (!errors.isEmpty()) {
      String message =
          "It is not possible to save your changes because the following errors were found:\n\n"
              + String.join("\n", errors)
              + "\n\nFor more information, read the logs.\nDo you want to discard this changes?";

      if (discardChangesErrorMessage(message)) {
        updateRepositoriesTable();
        isChanged = false;
      }
    } else {
      errors.addAll(updateRepositories());
      if (!errors.isEmpty()) {
        String message =
            "A problem removing/adding the following repositories occurred:\n" + String
                .join("\n", errors) + "\nDo you want to discard this changes?";
        if (discardChangesErrorMessage(message)) {
          updateRepositoriesTable();
          isChanged = false;
        }
      } else {
        informativeMessage("All repositories has been saved correctly.");
        isChanged = false;
      }
    }
  }

  private List<String> checkFieldValid() {
    Pattern allowed = Pattern.compile("[^a-z0-9_\\- ]", Pattern.CASE_INSENSITIVE);
    List<String> errors = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    Set<String> urls = new HashSet<>();
    int index = 0;
    for (RepositoryInput repository : repositoryTableModel) {
      index++;

      if (repository.id.isEmpty()) {
        errors.add("- The ID for the repository at index " + index
            + " is missing. All fields are required.");
      } else if (!ids.add(repository.id)) {
        errors.add("- The repository id '" + repository.id + "' is repeated");
      } else if (allowed.matcher(repository.id).find()) {
        errors.add("- The repository id '" + repository.id
            + "' uses special characters. Supported special characters: '_' and '-'.");
      }

      if (repository.url.isEmpty()) {
        errors.add("- The URL for the repository at index " + index
            + " is missing. All fields are required.");
      } else if (!urls.add(repository.url)) {
        errors.add("- The URL '" + repository.id + "' is repeated.");
      } else {
        errors.addAll(repositoryHandler.checkURL(repository.id, repository.url));
      }
    }
    return errors;
  }

  private List<String> updateRepositories() {
    List<String> errors = new ArrayList<>();
    for (RepositoryInput r : repositoryTableModel) {
      Optional<CorrelationTemplatesRepository> repository =
          repositoryHandler.getCorrelationRepositories().stream()
              .filter(i -> i.getName().equals(r.id))
              .findFirst();

      try {
        if (repository.isPresent()
            && !repositoryHandler.getRepositoryURL(repository.get().getName()).equals(r.url)) {
          repositoryHandler.deleteRepository(r.id);
        }
        repositoryHandler.saveRepository(r.id, r.url);
      } catch (IOException e) {
        errors.add("There was an error trying to update the repository " + r.id
            + ". Check the logs for more info.");
        LOG.error("There was an error trying to update the repository {}.", r.id, e);
      }
    }

    for (CorrelationTemplatesRepository r : repositoryHandler.getCorrelationRepositories()) {
      try {
        if (!r.getName().equals(LocalConfiguration.LOCAL_REPOSITORY_NAME) && repositoryTableModel
            .stream().noneMatch(i -> i.id.equals(r.getName()))) {
          repositoryHandler.deleteRepository(r.getName());
        }
      } catch (IOException e) {
        errors.add("There was an error trying to update the repository " + r.getName()
            + ". Check the logs for more info.");
        LOG.error("There was an error trying to update the repository {}.", r.getName(), e);
      }
    }
    return errors;
  }

  private void addRepositoryInput() {
    GuiUtils.stopTableEditing(repositoryTable);
    repositoryTableModel.addRow(new RepositoryInput());
    int rowToSelect = repositoryTableModel.getRowCount() - 1;
    repositoryTable.setRowSelectionInterval(rowToSelect, rowToSelect);
    repositoryTable.scrollRectToVisible(repositoryTable.getCellRect(rowToSelect, 0, true));
  }

  private void deleteRepositoryInput() {
    GuiUtils.cancelEditing(repositoryTable);
    int[] rowsSelected = repositoryTable.getSelectedRows();
    int anchorSelection = repositoryTable.getSelectionModel().getAnchorSelectionIndex();
    repositoryTable.clearSelection();
    if (rowsSelected.length > 0) {
      Arrays.stream(rowsSelected).forEach(repositoryTableModel::deleteRow);
      if (repositoryTableModel.getRowCount() > 0) {
        if (anchorSelection >= repositoryTableModel.getRowCount()) {
          anchorSelection = repositoryTableModel.getRowCount() - 1;
        }
        repositoryTable.setRowSelectionInterval(anchorSelection, anchorSelection);
      }
    }
  }

  private boolean discardChangesErrorMessage(String message) {
    return JOptionPane
        .showConfirmDialog(getContentPane(), message, "Error saving repositories",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.OK_OPTION;
  }

  private int saveChangesWarningMessage() {
    return JOptionPane
        .showConfirmDialog(getContentPane(), "Do you want to save your changes?", "Save?",
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
  }

  private void informativeMessage(String message) {
    JOptionPane.showMessageDialog(getContentPane(), message);
  }

  public void showFrame() {
    isChanged = false;
    updateEnabledButtons();
    updateRepositoriesTable();
    setVisible(true);
  }

  public static class RepositoryInput {

    private String id;
    private String url;

    private RepositoryInput() {
      id = "";
      url = "";
    }

    RepositoryInput(String id, String url) {
      this.id = id;
      this.url = url;
    }

    public String getId() {
      return id;
    }

    public String getUrl() {
      return url;
    }
  }

  private class RepositoryInputTableModel extends DefaultTableModel implements
      Iterable<RepositoryInput>, TableModelListener, CellEditorListener {

    private final ArrayList<RepositoryInput> inputs;

    private RepositoryInputTableModel() {
      super(REPOSITORY_TABLE_HEADERS, 0);
      inputs = new ArrayList<>();
      this.addTableModelListener(this);
    }

    private void clear() {
      inputs.clear();
    }

    private void deleteRow(int row) {
      LOG.debug("Removing row: {}", row);
      this.inputs.remove(row);
      fireTableRowsDeleted(row, row);
    }

    private void addRow(RepositoryInput value) {
      LOG.debug("Adding row value: {}", value);
      inputs.add(value);
      int insertedRowIndex = inputs.size() - 1;
      super.fireTableRowsInserted(insertedRowIndex, insertedRowIndex);
    }

    private void addRow(int index, RepositoryInput value) {
      LOG.debug("Adding indexed row value: {}, at: {}", value, index);
      inputs.add(index, value);
      super.fireTableRowsInserted(index, index);
    }

    private Stream<RepositoryInput> stream() {
      return inputs.stream();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<RepositoryInput> iterator() {
      return this.inputs.iterator();
    }

    @Override
    public int getRowCount() {
      return this.inputs == null ? 0 : this.inputs.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
      if (col == 0) {
        return this.inputs.get(row).id;
      } else if (col == 1) {
        return this.inputs.get(row).url;
      } else {
        return "";
      }
    }

    @Override
    public void setValueAt(Object cellValue, int row, int col) {
      if (row < this.inputs.size() && (cellValue instanceof String)) {
        if (col == 0) {
          inputs.get(row).id = (String) cellValue;
        } else if (col == 1) {
          this.inputs.get(row).url = (String) cellValue;
        }
      }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
      if (e.getType() == TableModelEvent.UPDATE) {
        isChanged = true;
      }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return row != 0 && super.isCellEditable(row, column);
    }

    @Override
    public void editingStopped(ChangeEvent e) {
      isChanged = true;
      updateEnabledButtons();
    }

    @Override
    public void editingCanceled(ChangeEvent e) {

    }
  }
}
