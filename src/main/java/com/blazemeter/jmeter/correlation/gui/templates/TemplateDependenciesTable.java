package com.blazemeter.jmeter.correlation.gui.templates;

import com.blazemeter.jmeter.commons.SwingUtils;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import org.apache.jmeter.gui.util.HeaderAsPropertyRenderer;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.GuiUtils;
import org.apache.jorphan.gui.ObjectTableModel;
import org.apache.jorphan.reflect.Functor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateDependenciesTable extends JTable implements ActionListener {
  private static final Logger LOG = LoggerFactory.getLogger(TemplateSaveFrame.class);
  private static final String ADD = "add";
  private static final String DELETE = "delete";
  private static final String CLEAR = "clear";
  private ObjectTableModel dependencyModel;

  private CorrelationTemplatesRegistryHandler templatesRegistry;

  private boolean hasRepeatedDependencies = false;
  private boolean hasFailingURLs = false;
  private List<CorrelationTemplateDependency> dependenciesList = new ArrayList<>();

  public TemplateDependenciesTable() {
    super();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();
    switch (action) {
      case ADD:
        addRow();
        break;
      case DELETE:
        deleteRow();
        break;
      case CLEAR:
        clearTable();
        break;
      default:
        LOG.warn("Unsupported action {}", action);
        JMeterUtils
            .reportErrorToUser("Unsupported action " + action, "Managing Repositories & Templates");
    }
  }

  private void addRow() {
    GuiUtils.stopTableEditing(this);
    dependencyModel.addRow(new CorrelationTemplateDependency());
    int rowToSelect = this.getRowCount() - 1;
    this.setRowSelectionInterval(rowToSelect, rowToSelect);
    this.scrollRectToVisible(this.getCellRect(rowToSelect, 0, true));
  }

  private void deleteRow() {
    GuiUtils.cancelEditing(this);

    int[] rowsSelected = this.getSelectedRows();
    int anchorSelection = this.getSelectionModel().getAnchorSelectionIndex();
    this.clearSelection();
    if (rowsSelected.length > 0) {
      for (int i = rowsSelected.length - 1; i >= 0; i--) {
        dependencyModel.removeRow(rowsSelected[i]);
      }

      if (dependencyModel.getRowCount() > 0) {
        if (anchorSelection >= dependencyModel.getRowCount()) {
          anchorSelection = dependencyModel.getRowCount() - 1;
        }
        this.setRowSelectionInterval(anchorSelection, anchorSelection);
      }
    }
  }

  private void clearTable() {
    dependencyModel.clearData();
  }

  @VisibleForTesting
  public List<CorrelationTemplateDependency> getDependencies() {
    return (List<CorrelationTemplateDependency>) dependencyModel.getObjectList();
  }

  public Iterator<CorrelationTemplateDependency> getDependenciesIterator() {
    return (Iterator<CorrelationTemplateDependency>) dependencyModel.iterator();
  }

  public void clear() {
    dependencyModel.clearData();
  }

  public JPanel prepareDependenciesPanel() {
    JPanel templateDependenciesButtonsPanel = buildDependencyButtonPanel();
    JLabel dependencyOverwriteLabel = new JLabel("Repeated dependencies will be overwritten.");
    dependencyOverwriteLabel.setPreferredSize(new Dimension(300, 30));
    JScrollPane templateDependenciesScroll = prepareDependenciesTable();
    templateDependenciesScroll.setPreferredSize(new Dimension(300, 200));

    JPanel dependenciesPanel = new JPanel();
    GroupLayout dependenciesLayout = new GroupLayout(dependenciesPanel);
    dependenciesPanel.setLayout(dependenciesLayout);

    dependenciesLayout.setHorizontalGroup(dependenciesLayout.createParallelGroup()
        .addComponent(templateDependenciesScroll)
        .addComponent(dependencyOverwriteLabel, GroupLayout.Alignment.CENTER)
        .addComponent(templateDependenciesButtonsPanel)
    );

    dependenciesLayout.setVerticalGroup(dependenciesLayout.createSequentialGroup()
        .addComponent(templateDependenciesScroll)
        .addComponent(dependencyOverwriteLabel)
        .addComponent(templateDependenciesButtonsPanel)
    );

    return dependenciesPanel;
  }

  private JPanel buildDependencyButtonPanel() {
    SwingUtils.ButtonBuilder base = new SwingUtils.ButtonBuilder()
        .withActionListener(this);

    JButton deleteButton = base.withName("delete").withAction(DELETE).build();
    JButton clearButton = base.withName("clear").withAction(CLEAR).build();

    JPanel buttonPanel = new JPanel();
    buttonPanel.setPreferredSize(new Dimension(300, 50));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
    buttonPanel.add(base.withName("add").withAction(ADD).build());
    buttonPanel.add(deleteButton);
    buttonPanel.add(clearButton);
    return buttonPanel;
  }

  private JScrollPane prepareDependenciesTable() {
    dependencyModel = new ObjectTableModel(new String[] {"Name", "Version", "URL"},
        CorrelationTemplateDependency.class,
        new Functor[] {new Functor("getName"), new Functor("getVersion"), new Functor("getUrl")},
        new Functor[] {new Functor("setName"), new Functor("setVersion"), new Functor("setUrl")},
        new Class[] {String.class, String.class, String.class});

    setModel(dependencyModel);

    this.setPreferredSize(new Dimension(300, 200));
    this.setName("templateDependenciesScroll");
    this.getColumnModel().getColumn(0).setPreferredWidth(50);
    this.getColumnModel().getColumn(1).setPreferredWidth(50);
    this.getColumnModel().getColumn(2).setPreferredWidth(200);
    this.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer());
    this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    this.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer() {
      @Override
      protected String getText(Object value, int row, int column) {
        return (value == null) ? "" : value.toString();
      }
    });
    JMeterUtils.applyHiDPI(this);
    JScrollPane pane = new JScrollPane(this);
    pane.setName("templateDependenciesScroll");
    pane.setPreferredSize(new Dimension(350, 150));
    return pane;
  }

  public JPanel getJPanel() {
    SwingUtils.ButtonBuilder base = new SwingUtils.ButtonBuilder()
        .withActionListener(this);

    JButton deleteButton = base.withName("delete").withAction(DELETE).build();
    JButton clearButton = base.withName("clear").withAction(CLEAR).build();

    JPanel buttonPanel = new JPanel();
    buttonPanel.setPreferredSize(new Dimension(300, 50));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
    buttonPanel.add(base.withName("add").withAction(ADD).build());
    buttonPanel.add(deleteButton);
    buttonPanel.add(clearButton);
    return buttonPanel;
  }

  public boolean hasDependencies() {
    return dependencyModel.getRowCount() > 0;
  }

  public void setDependencies(List<CorrelationTemplateDependency> dependencies) {
    dependencyModel.clearData();
    dependencies.forEach(dependencyModel::addRow);
  }

  public void validateDependencies() {
    hasRepeatedDependencies = false;
    hasFailingURLs = false;
    processDependenciesInformation();
  }

  public void processDependenciesInformation() {
    boolean hasFailingURLs = false;
    boolean hasRepeatedDependencies = false;
    List<CorrelationTemplateDependency> rawDependencies = getDependencies();
    if (hasDependencies()) {
      Iterator<CorrelationTemplateDependency> iterator = getDependenciesIterator();

      boolean allDependenciesComplete = true;
      int dependencyIndex = 0;
      while (iterator.hasNext()) {
        dependencyIndex++;
        CorrelationTemplateDependency dependency = iterator.next();
        if (hasRowEmptyValues(dependency, dependencyIndex)) {
          allDependenciesComplete = false;
          continue;
        }

        List<CorrelationTemplateDependency> repeatedDependencies = rawDependencies.stream()
            .filter(d -> d.getName().trim().toLowerCase()
                .equals(dependency.getName().trim().toLowerCase()))
            .collect(Collectors.toList());

        if (!repeatedDependencies.isEmpty()) {
          hasRepeatedDependencies = true;
          rawDependencies.removeAll(repeatedDependencies);
        }

        if (!templatesRegistry.isValidDependencyURL(dependency.getUrl(), dependency.getName(),
            dependency.getVersion())) {
          hasFailingURLs = true;
          continue;
        }

        rawDependencies.add(dependency);
      }
    }
    dependenciesList = rawDependencies;
  }

  private boolean hasRowEmptyValues(CorrelationTemplateDependency dependency, int dependencyIndex) {
    boolean allFieldComplete = true;
    if (isBlank(dependency.getName())) {
      LOG.error("The dependency in the row {}, has no name.", dependencyIndex);
      allFieldComplete = false;
    }
    if (isBlank(dependency.getUrl())) {
      LOG.error("The dependency in the row {}, has no url.", dependencyIndex);
      allFieldComplete = false;
    }
    if (isBlank(dependency.getVersion())) {
      LOG.error("The dependency in the row {}, has no version.", dependencyIndex);
      allFieldComplete = false;
    }
    return !allFieldComplete;
  }

  public static boolean isBlank(String str) {
    return str == null || str.trim().isEmpty();
  }

  public boolean isHasRepeatedDependencies() {
    return hasRepeatedDependencies;
  }

  public void setHasRepeatedDependencies(boolean hasRepeatedDependencies) {
    this.hasRepeatedDependencies = hasRepeatedDependencies;
  }

  public boolean isHasFailingURLs() {
    return hasFailingURLs;
  }

  public void setHasFailingURLs(boolean hasFailingURLs) {
    this.hasFailingURLs = hasFailingURLs;
  }

  public List<CorrelationTemplateDependency> getDependenciesList() {
    return dependenciesList;
  }

  public void setDependenciesList(
      List<CorrelationTemplateDependency> dependenciesList) {
    this.dependenciesList = dependenciesList;
  }

  public CorrelationTemplatesRegistryHandler getTemplatesRegistry() {
    return templatesRegistry;
  }

  public void setTemplatesRegistry(
      CorrelationTemplatesRegistryHandler templatesRegistry) {
    this.templatesRegistry = templatesRegistry;
  }
}
