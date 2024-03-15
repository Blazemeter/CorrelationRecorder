package com.blazemeter.jmeter.correlation.gui.automatic;

import com.blazemeter.jmeter.commons.SwingUtils;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory.Step;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.gui.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This class will allow the user see the history and select a step to roll back to.
 * It will display a list of the avaliable snapshots in the history file
 * and allow the user to select one.
 * After, based on that slection the new tree will be loaded.
 */

public class CorrelationHistoryFrame extends JDialog implements ActionListener {
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationSuggestionsPanel.class);
  private static final String DELETE = "delete";
  private static final String RESTORE = "restore";
  private static final String ZIP = "zip";
  protected CorrelationHistory history;
  protected JDialog runDialog;

  @VisibleForTesting
  protected JTable table;
  private JButton deleteButton;
  private JButton restoreButton;
  private JButton zipButton;

  public CorrelationHistoryFrame(CorrelationHistory history) {
    super();
    this.history = history;
    init();
    setModal(true);
  }

  public void init() {
    add(makeCorrelationHistoryPanel());
    setTitle("History Manager");
  }

  private JPanel makeCorrelationHistoryPanel() {
    SwingUtils.ButtonBuilder builder = new SwingUtils.ButtonBuilder()
        .isEnabled(true)
        .hasText(true)
        .withActionListener(this);

    table = new JTable();
    table.setName("historyTable");
    table.setModel(new HistoryTableModel());
    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    table.setRowHeight(30);
    table.setShowGrid(false);
    table.setShowHorizontalLines(true);
    table.setShowVerticalLines(true);
    table.setGridColor(table.getGridColor().darker());
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(false);
    table.setCellSelectionEnabled(false);
    table.setFillsViewportHeight(true);
    table.setRowSelectionAllowed(true);
    table.setAutoCreateRowSorter(false);
    table.addMouseListener(new TextBoxPopupDialog.TextBoxDoubleClick(table));

    ListSelectionModel selectionModel = table.getSelectionModel();

    TableColumn selectionColumn = table.getColumnModel().getColumn(0);
    selectionColumn.setMaxWidth(30);
    selectionColumn.setMaxWidth(50);

    TableColumn timestampColumn = table.getColumnModel().getColumn(1);
    timestampColumn.setMinWidth(150);

    TableColumn descColumn = table.getColumnModel().getColumn(2);
    descColumn.setMinWidth(400);

    JTableHeader header = table.getTableHeader();
    header.setFont(header.getFont().deriveFont(14f));

    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setViewportView(table);
    scrollPane.setPreferredSize(new Dimension(750, 350));
    scrollPane.setBorder(new EtchedBorder());

    deleteButton = builder.withAction(DELETE)
        .withName("deleteSteps")
        .withText("Delete")
        .withToolTip("Delete the selected iteration.")
        .build();

    restoreButton = builder.withAction(RESTORE)
        .withName("restoreStep")
        .withText("Restore")
        .withToolTip("Restore the selected iteration")
        .build();

    zipButton = builder.withAction(ZIP)
        .withName("zipSave")
        .withText("Export All")
        .withToolTip("Export history to a zip file.")
        .build();

    JPanel buttonsPanel = new JPanel();
    buttonsPanel.add(deleteButton);
    buttonsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
    buttonsPanel.add(restoreButton);
    buttonsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
    buttonsPanel.add(zipButton);

    JPanel displayTablePanel = new JPanel();
    displayTablePanel.setLayout(new GridBagLayout());
    displayTablePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0.9;

    displayTablePanel.add(scrollPane, constraints);

    JPanel displayHistoryPanel = new JPanel();
    BorderLayout layout = new BorderLayout(15, 15);
    displayHistoryPanel.setLayout(layout);
    displayHistoryPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    displayHistoryPanel.add(scrollPane, BorderLayout.CENTER);
    displayHistoryPanel.add(buttonsPanel, BorderLayout.PAGE_END);
    return displayHistoryPanel;
  }

  public void loadSteps(List<Step> steps) {
    HistoryTableModel model = (HistoryTableModel) table.getModel();
    model.loadSteps(steps);
  }

  protected void displayWaitingScreen(String message) {
    runDialog = JMeterElementUtils.makeWaitingFrame(message);
    runDialog.pack();
    runDialog.repaint();
    runDialog.setAlwaysOnTop(true);
    runDialog.setVisible(true);
    runDialog.toFront();
  }

  public void disposeWaitingDialog() {
    runDialog.dispose();
    runDialog.setVisible(false);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    HistoryTableModel tableModel = (HistoryTableModel) this.table.getModel();
    String action = e.getActionCommand();
    switch (action) {
      case DELETE:
        int confirmDelete = JOptionPane.showConfirmDialog(this,
            "You are about to delete one or more history iterations.\n" +
                "Do you want to continue? \n",
            "Delete confirmation", JOptionPane.YES_NO_OPTION);
        if (confirmDelete == JOptionPane.YES_OPTION) {
          this.deleteSteps(tableModel.getSelectedSteps());
          return;
        }
        return;
      case RESTORE:
        if (tableModel.getSelectedSteps().isEmpty()) {
          JOptionPane.showMessageDialog(this,
                  "Please select one iteration to restore");
        } else if (tableModel.getSelectedSteps().size() > 1) {
          JOptionPane.showMessageDialog(this,
                  "You can't restore more than one iteration at a time.");
        } else {
          try {
            this.displayWaitingScreen("Restoring iteration.");
            this.restoreStep(tableModel.getSelectedSteps().get(0));
            HistoryFrameSwingWorker worker = new HistoryFrameSwingWorker(this);
            worker.execute();
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          } catch (IllegalUserActionException ex) {
            throw new RuntimeException(ex);
          }
        }
        return;
      case ZIP:
        this.zipHistory();
        return;
      default:
        LOG.warn("Action {} not supported", action);
    }
  }

  public void deleteSteps(List<CorrelationHistory.Step> steps) {
    history.deleteSteps(steps);
    loadSteps(history.getSteps());
  }

  public void restoreStep(CorrelationHistory.Step step) throws IOException,
      IllegalUserActionException {
    history.addRestoredStep(step);
    HashTree hashTree = SaveService.loadTree(
        new File(URLDecoder.decode(step.getTestPlanFilepath(), "UTF-8")));
    GuiPackage guiPackage = GuiPackage.getInstance();
    guiPackage.clearTestPlan();
    guiPackage.addSubTree(hashTree);
    loadSteps(history.getSteps());
  }

  public void zipHistory() {
    String zipFilePath = this.history.zipHistory();
    if (!zipFilePath.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "History zipped at: \n" +
              zipFilePath);
    }
  }

  public static class HistoryTableModel extends DefaultTableModel {
    private final List<String> columns = Arrays.asList("", "Timestamp", "Description");
    private final List<HistoryItem> stepList = new ArrayList<>();
    private final Map<Template, List<CorrelationSuggestion>> suggestionsMap =
        new HashMap<>();

    @Override
    public String getColumnName(int column) {
      return columns.get(column);
    }

    @Override
    public int getRowCount() {
      if (stepList == null) {
        return 0;
      }
      return stepList.size();
    }

    @Override
    public int getColumnCount() {
      return this.columns.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      if (columnIndex == 0) {
        return Boolean.class;
      }
      return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      HistoryItem item = stepList.get(rowIndex);
      Step step = item.getStep();
      switch (columnIndex) {
        case 0:
          return item.isSelected();
        case 1:
          if (step.getTimestamp() != null) {
            try {
              Instant date = Instant.parse(step.getTimestamp());
              DateTimeFormatter outFormatter =
                  DateTimeFormatter.ofPattern("yyyy-MM-dd' - 'HH:mm:ss")
                      .withZone(ZoneId.systemDefault());
              return outFormatter.format(date);
            } catch (Exception ex) {
              LOG.warn("Error parsing timestamp for history iteration", ex);
            }
          }
          return "Not available";
        case 2:
          return step.getStepMessage();
        default:
          return "N/A";
      }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (!stepList.isEmpty() && columnIndex == 0) {
        stepList.get(rowIndex).setSelected((boolean) aValue);
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 0;
    }

    public List<Step> getSelectedSteps() {
      return stepList.stream()
          .filter(HistoryItem::isSelected)
          .map(HistoryItem::getStep)
          .collect(Collectors.toList());
    }

    public void loadSteps(List<Step> steps) {
      stepList.clear();
      steps.forEach(step -> stepList.add(new HistoryItem(step)));
      fireTableDataChanged();
    }
  }

  public void showFrame() {
    pack();
    ComponentUtil.centerComponentInWindow(this);
    setVisible(true);
    requestFocus();
  }

  private static class HistoryItem {

    private boolean selected = false;
    private final Step step;

    HistoryItem(Step step) {
      this.step = step;
    }

    public void setSelected(boolean selected) {
      this.selected = selected;
    }

    public boolean isSelected() {
      return selected;
    }

    public Step getStep() {
      return step;
    }
  }

  public static class HistoryFrameSwingWorker extends SwingWorker {
    private final CorrelationHistoryFrame frame;

    public HistoryFrameSwingWorker(CorrelationHistoryFrame frame) {
      this.frame = frame;
    }

    @Override
    protected String doInBackground() throws Exception {
      HistoryTableModel tableModel = (HistoryTableModel) frame.table.getModel();
      frame.restoreStep(tableModel.getSelectedSteps().get(0));
      return null;
    }

    @Override
    protected void done() {
      frame.disposeWaitingDialog();
      JOptionPane.showMessageDialog(frame,
              "Iteration restored.");
      frame.setVisible(false);
    }
  }

}
