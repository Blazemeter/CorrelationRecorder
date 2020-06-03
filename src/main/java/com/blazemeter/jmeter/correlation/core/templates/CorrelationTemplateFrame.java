package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate.Builder;
import com.blazemeter.jmeter.correlation.gui.StringUtils;
import com.blazemeter.jmeter.correlation.gui.SwingUtils;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import org.apache.jmeter.gui.util.HeaderAsPropertyRenderer;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.GuiUtils;
import org.apache.jorphan.gui.ObjectTableModel;
import org.apache.jorphan.reflect.Functor;
import org.jdesktop.swingx.JXTaskPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationTemplateFrame extends JFrame implements ActionListener {

  private static final String ADD_DEPENDENCY_BUTTON_NAME = "addDependencyRow";
  private static final String DELETE_DEPENDENCY_BUTTON_NAME = "deleteDependencyRow";
  private static final String CLEAR_TABLE_BUTTON_NAME = "clearDependencyTable";
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationTemplateFrame.class);
  private static final int FIELD_WIDTH = 200;
  private static final int LABEL_WIDTH = 100;
  private static final int FIELD_HEIGHT = 30;
  private static final int TEXT_AREA_HEIGHT = 120;
  private static final Dimension FIELD_MINIMUM_DIMENSION = new Dimension(FIELD_WIDTH, FIELD_HEIGHT);
  private static final Dimension LABEL_MINIMUM_DIMENSION = new Dimension(LABEL_WIDTH, FIELD_HEIGHT);
  private static final String ADD = "addRow";
  private static final String DELETE = "deleteRow";
  private static final String CLEAR = "clearRow";
  private BufferedImage templateSnapshot;
  private final JPanel mainPanel = SwingUtils
      .createComponent("correlationTemplatePanel", new JPanel());
  private final JButton saveButton = SwingUtils
      .createComponent("saveTemplateButton", new JButton());
  private final JTextField templateIdField = makeTextField("correlationTemplateIdField");
  private final JTextField templateVersionField = makeTextField("correlationTemplateVersionField");
  private final JTextArea templateDescriptionField = makeTextArea(
      "correlationTemplateDescriptionTextArea");
  private final JTextArea templateChangesField = makeTextArea("correlationTemplateChangesField");
  private ObjectTableModel dependencyModel;
  private JTable dependenciesTable;

  public CorrelationTemplateFrame(CorrelationTemplatesRegistryHandler repositoryHandler,
      BufferedImage snapshot, Consumer<CorrelationTemplate> updateLastTemplateSupplier) {
    setTitle("Correlation Template");
    setName("correlationTemplateFrame");
    setResizable(false);
    setLayout(new CardLayout(10, 10));
    buildMainPanel();
    add(mainPanel);
    templateSnapshot = snapshot;
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    saveButton.addMouseListener(
        buildTemplateMouseListener(repositoryHandler, updateLastTemplateSupplier));
    pack();
  }

  private JTextField makeTextField(String name) {
    return SwingUtils.createComponent(name, new JTextField(), FIELD_MINIMUM_DIMENSION);
  }

  private JTextArea makeTextArea(String name) {
    return SwingUtils.createComponent(name, new JTextArea());
  }

  private void buildMainPanel() {
    GroupLayout layout = new GroupLayout(mainPanel);
    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);
    mainPanel.setLayout(layout);
    saveButton.setText("Save");

    JLabel idLabel = makeLabel("idLabel", "ID:");
    JLabel descriptionLabel = makeLabel("descriptionLabel", "Description:");
    JLabel changesLabel = makeLabel("changesLabel", "Changes: ");
    JLabel versionLabel = makeLabel("versionLabel", "Version: ");
    JLabel descriptionInfo = makeLabel("descriptionInfoLabel", "This field allows HTML tags.");
    Font defaultLabelFont = descriptionInfo.getFont();
    descriptionInfo
        .setFont(new Font(defaultLabelFont.getFontName(), Font.ITALIC, defaultLabelFont.getSize()));

    Border defaultFieldBolder = templateIdField.getBorder();
    JScrollPane descriptionScrollPane = new JScrollPane(templateDescriptionField);
    descriptionScrollPane.setBorder(defaultFieldBolder);
    descriptionScrollPane.setBackground(templateDescriptionField.getBackground());

    JScrollPane changesScrollPane = new JScrollPane(templateChangesField);
    changesScrollPane.setBorder(defaultFieldBolder);
    changesScrollPane.setBackground(templateChangesField.getBackground());

    JXTaskPane dependenciesCollapsiblePanel = buildDependenciesPanel();

    dependenciesCollapsiblePanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        resizeFrame();
      }
    });

    layout.setHorizontalGroup(layout.createParallelGroup()
        .addGroup(layout.createSequentialGroup()
            .addComponent(idLabel)
            .addComponent(templateIdField)
        )
        .addGroup(layout.createSequentialGroup()
            .addComponent(versionLabel)
            .addComponent(templateVersionField)
        )
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup()
                .addComponent(descriptionLabel)
                .addComponent(descriptionScrollPane, GroupLayout.PREFERRED_SIZE, 300,
                    GroupLayout.PREFERRED_SIZE)
                .addComponent(descriptionInfo)
            )
        )
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup()
                .addComponent(changesLabel)
                .addComponent(changesScrollPane, GroupLayout.PREFERRED_SIZE, 300,
                    GroupLayout.PREFERRED_SIZE)
            )
        )
        .addGroup(layout.createSequentialGroup()
            .addComponent(dependenciesCollapsiblePanel)
        )
        .addComponent(saveButton, Alignment.CENTER)
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup()
            .addComponent(idLabel)
            .addComponent(templateIdField, GroupLayout.PREFERRED_SIZE, FIELD_HEIGHT,
                GroupLayout.PREFERRED_SIZE)
        )
        .addGroup(layout.createParallelGroup()
            .addComponent(versionLabel)
            .addComponent(templateVersionField, GroupLayout.PREFERRED_SIZE, FIELD_HEIGHT,
                GroupLayout.PREFERRED_SIZE)
        )
        .addGroup(layout.createParallelGroup()
            .addGroup(layout.createSequentialGroup()
                .addComponent(descriptionLabel)
                .addComponent(descriptionScrollPane, GroupLayout.PREFERRED_SIZE, TEXT_AREA_HEIGHT,
                    GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(descriptionInfo)
            )
        )
        .addGroup(layout.createParallelGroup()
            .addGroup(layout.createSequentialGroup()
                .addComponent(changesLabel)
                .addComponent(changesScrollPane, GroupLayout.PREFERRED_SIZE, TEXT_AREA_HEIGHT,
                    GroupLayout.PREFERRED_SIZE)
            )
        )
        .addGroup(layout.createParallelGroup()
            .addComponent(dependenciesCollapsiblePanel)
        )
        .addGap(GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
        .addComponent(true, saveButton)
    );
  }

  private void resizeFrame() {
    pack();
  }

  private JLabel makeLabel(String name, String text) {
    return SwingUtils.createComponent(name, new JLabel(text), LABEL_MINIMUM_DIMENSION);
  }

  private JXTaskPane buildDependenciesPanel() {
    JXTaskPane collapsiblePanel = SwingUtils.buildCollapsiblePane("dependenciesCollapsiblePanel");
    collapsiblePanel.add(prepareDependenciesPanel());
    collapsiblePanel.setTitle("Dependencies");
    return collapsiblePanel;
  }

  private JPanel prepareDependenciesPanel() {

    JPanel templateDependenciesButtonsPanel = buildDependencyButtonPanel();
    templateDependenciesButtonsPanel.setPreferredSize(new Dimension(300, 50));
    JLabel dependencyOverwriteLabel = new JLabel("Repeated dependencies will be overwritten.");
    JScrollPane templateDependenciesScroll = prepareDependenciesTable();
    templateDependenciesScroll.setMinimumSize(new Dimension(300, 150));
    templateDependenciesScroll.setPreferredSize(templateDependenciesScroll.getMinimumSize());

    JPanel dependenciesPanel = new JPanel();
    GroupLayout dependenciesLayout = new GroupLayout(dependenciesPanel);
    dependenciesPanel.setLayout(dependenciesLayout);

    dependenciesLayout.setHorizontalGroup(dependenciesLayout.createParallelGroup()
        .addComponent(templateDependenciesScroll)
        .addComponent(dependencyOverwriteLabel)
        .addComponent(templateDependenciesButtonsPanel)
    );

    dependenciesLayout.setVerticalGroup(dependenciesLayout.createSequentialGroup()
        .addComponent(templateDependenciesScroll)
        .addComponent(dependencyOverwriteLabel)
        .addComponent(templateDependenciesButtonsPanel)
    );

    return dependenciesPanel;
  }

  private JScrollPane prepareDependenciesTable() {
    dependencyModel = new ObjectTableModel(new String[]{"Name", "Version", "URL"},
        CorrelationTemplateDependency.class,
        new Functor[]{new Functor("getName"), new Functor("getVersion"), new Functor("getUrl")},
        new Functor[]{new Functor("setName"), new Functor("setVersion"), new Functor("setUrl")},
        new Class[]{String.class, String.class, String.class});

    dependenciesTable = new JTable(dependencyModel);
    dependenciesTable.setMinimumSize(new Dimension(300, 150));
    dependenciesTable.setPreferredSize(dependenciesTable.getMinimumSize());
    dependenciesTable.setName("templateDependenciesScroll");
    dependenciesTable.getColumnModel().getColumn(0).setMinWidth(50);
    dependenciesTable.getColumnModel().getColumn(1).setMinWidth(50);
    dependenciesTable.getColumnModel().getColumn(2).setMinWidth(200);
    dependenciesTable.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer());
    dependenciesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    dependenciesTable.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer() {
      @Override
      protected String getText(Object value, int row, int column) {
        return (value == null) ? "" : value.toString();
      }
    });
    JMeterUtils.applyHiDPI(dependenciesTable);

    JScrollPane pane = new JScrollPane(dependenciesTable);
    pane.setName("templateDependenciesScroll");
    return pane;
  }

  private JPanel buildDependencyButtonPanel() {
    JButton addDependencyRow = SwingUtils
        .buildJButton(ADD_DEPENDENCY_BUTTON_NAME, "Add", ADD, this);
    JButton deleteDependencyRow = SwingUtils
        .buildJButton(DELETE_DEPENDENCY_BUTTON_NAME, "Delete", DELETE, this);
    JButton clearDependencyTable = SwingUtils
        .buildJButton(CLEAR_TABLE_BUTTON_NAME, "Clear", CLEAR, this);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

    buttonPanel.add(addDependencyRow);
    buttonPanel.add(deleteDependencyRow);
    buttonPanel.add(clearDependencyTable);
    return buttonPanel;
  }

  public void showFrame() {
    setVisible(true);
    requestFocus();
  }

  private String getTemplateID() {
    return templateIdField.getText();
  }

  private String getTemplateDescription() {
    return templateDescriptionField.getText();
  }

  private String getVersion() {
    return templateVersionField.getText();
  }

  private String getChanges() {
    return templateChangesField.getText();
  }

  private void close() {
    this.dispose();
  }

  private MouseListener buildTemplateMouseListener(
      CorrelationTemplatesRegistryHandler repositoryHandler,
      Consumer<CorrelationTemplate> correlationTemplateSupplier) {
    return new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (isFormIncomplete()) {
          JOptionPane.showMessageDialog(CorrelationTemplateFrame.this, "All fields are required",
              "Save template error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        if (isRepeatedVersion(repositoryHandler)) {
          JOptionPane.showMessageDialog(CorrelationTemplateFrame.this,
              "That version is already in use. Try a new one.",
              "Save template error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        boolean hasRepeatedDependencies = false;
        boolean hasFailingURLs = false;
        List<CorrelationTemplateDependency> dependencyList = new ArrayList<>();

        if (dependencyModel.getRowCount() > 0) {
          Iterator<CorrelationTemplateDependency> iterator =
              (Iterator<CorrelationTemplateDependency>) dependencyModel
                  .iterator();

          boolean allDependenciesComplete = true;
          int dependencyIndex = 0;
          while (iterator.hasNext()) {
            dependencyIndex++;
            CorrelationTemplateDependency dependency = iterator.next();
            if (hasEmptyFields(dependency, dependencyIndex)) {
              allDependenciesComplete = false;
              continue;
            }

            List<CorrelationTemplateDependency> repeatedDependencies = dependencyList.stream()
                .filter(d -> d.getName().trim().toLowerCase()
                    .equals(dependency.getName().trim().toLowerCase()))
                .collect(Collectors.toList());

            if (!repeatedDependencies.isEmpty()) {
              hasRepeatedDependencies = true;
              dependencyList.removeAll(repeatedDependencies);
            }

            if (!repositoryHandler.isValidDependencyURL(dependency.getUrl(), dependency.getName(),
                dependency.getVersion())) {
              hasFailingURLs = true;
              continue;
            }

            dependencyList.add(dependency);
          }

          if (!allDependenciesComplete) {
            JOptionPane.showMessageDialog(null,
                "There are incomplete dependencies. Fill or delete them before continue.");
            return;
          }
        }

        if (hasFailingURLs) {
          JOptionPane.showMessageDialog(null,
              "There are some issues with some dependency's URLs, please fix then before continue"
                  + ".\nCheck the logs for more information.");
          return;
        }

        if (hasRepeatedDependencies && JOptionPane.showConfirmDialog(null,
            "There are dependencies that are repeated. Want to overwrite them?",
            "Saving Correlation Template",
            JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
          return;
        }

        try {
          Builder savingTemplateBuilder = new Builder()
              .withId(getTemplateID())
              .withVersion(getVersion())
              .withDescription(getTemplateDescription())
              .withSnapshot(templateSnapshot)
              .withRepositoryId(LocalConfiguration.LOCAL_REPOSITORY_NAME)
              .withChanges(getChanges())
              .withDependencies(dependencyList);
          repositoryHandler.onSaveTemplate(savingTemplateBuilder);
          correlationTemplateSupplier.accept(savingTemplateBuilder.build());
        } catch (IOException | ConfigurationException ex) {
          JOptionPane.showMessageDialog(null, "Error while trying to save template",
              "Save template error", JOptionPane.ERROR_MESSAGE);
          LOG.error("Error while trying to convert rules into json file {}", getTemplateID(), ex);
        }
        close();
        clear();
      }
    };
  }

  public void clear() {
    templateIdField.setText("");
    templateVersionField.setText("");
    templateDescriptionField.setText("");
    templateChangesField.setText("");
    clearTable();
  }

  private boolean isRepeatedVersion(CorrelationTemplatesRegistryHandler repositoryHandler) {
    return repositoryHandler.isLocalTemplateVersionSaved(getTemplateID(), getVersion());
  }

  private boolean hasEmptyFields(CorrelationTemplateDependency dependency, int dependencyIndex) {
    boolean allFieldComplete = true;
    if (StringUtils.isBlank(dependency.getName())) {
      LOG.error("The dependency in the row {}, has no name.", dependencyIndex);
      allFieldComplete = false;
    }
    if (StringUtils.isBlank(dependency.getUrl())) {
      LOG.error("The dependency in the row {}, has no url.", dependencyIndex);
      allFieldComplete = false;
    }
    if (StringUtils.isBlank(dependency.getVersion())) {
      LOG.error("The dependency in the row {}, has no version.", dependencyIndex);
      allFieldComplete = false;
    }
    return !allFieldComplete;
  }

  private boolean isFormIncomplete() {
    return isEmpty(templateIdField) || isEmpty(templateVersionField) || isEmpty(
        templateChangesField) || isEmpty(templateDescriptionField);
  }

  private boolean isEmpty(JTextComponent field) {
    return field.getText().trim().isEmpty();
  }


  public void setTemplateSnapshot(BufferedImage snapshot) {
    this.templateSnapshot = snapshot;
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

  private void clearTable() {
    dependencyModel.clearData();
  }

  private void deleteRow() {
    GuiUtils.cancelEditing(dependenciesTable);

    int[] rowsSelected = dependenciesTable.getSelectedRows();
    int anchorSelection = dependenciesTable.getSelectionModel().getAnchorSelectionIndex();
    dependenciesTable.clearSelection();
    if (rowsSelected.length > 0) {
      for (int i = rowsSelected.length - 1; i >= 0; i--) {
        dependencyModel.removeRow(rowsSelected[i]);
      }

      if (dependencyModel.getRowCount() > 0) {
        if (anchorSelection >= dependencyModel.getRowCount()) {
          anchorSelection = dependencyModel.getRowCount() - 1;
        }
        dependenciesTable.setRowSelectionInterval(anchorSelection, anchorSelection);
      }
    }
  }

  private void addRow() {
    GuiUtils.stopTableEditing(dependenciesTable);
    dependencyModel.addRow(new CorrelationTemplateDependency());

    int rowToSelect = dependenciesTable.getRowCount() - 1;
    dependenciesTable.setRowSelectionInterval(rowToSelect, rowToSelect);
    dependenciesTable.scrollRectToVisible(dependenciesTable.getCellRect(rowToSelect, 0, true));
  }

  @VisibleForTesting
  public List<CorrelationTemplateDependency> getDependencies() {
    return (List<CorrelationTemplateDependency>) dependencyModel.getObjectList();
  }

  public void setDependencies(List<CorrelationTemplateDependency> dependencies) {
    dependencies.forEach(d -> dependencyModel.addRow(d));
  }

  public void setLoadedTemplates(Set<CorrelationTemplate> loadedTemplates) {
    clear();
    //If we have more than 1 loaded template, we don't set this fields
    if (loadedTemplates.size() == 1) {
      CorrelationTemplate loadedTemplate = loadedTemplates.iterator().next();
      templateIdField.setText(loadedTemplate.getId());
      templateVersionField.setText(loadedTemplate.getVersion());
      templateDescriptionField.setText(loadedTemplate.getDescription());
    }
    loadedTemplates.stream()
        .filter(t -> t.getDependencies() != null && !t.getDependencies().isEmpty())
        .forEach(t -> setDependencies(t.getDependencies()));
  }
}
