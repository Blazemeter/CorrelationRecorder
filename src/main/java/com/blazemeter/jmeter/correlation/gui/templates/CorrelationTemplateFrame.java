package com.blazemeter.jmeter.correlation.gui.templates;

import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion.Builder;
import com.blazemeter.jmeter.correlation.gui.common.CollapsiblePanel;
import com.blazemeter.jmeter.correlation.gui.common.PlaceHolderTextField;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils.ButtonBuilder;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import org.apache.jmeter.gui.util.HeaderAsPropertyRenderer;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.GuiUtils;
import org.apache.jorphan.gui.ObjectTableModel;
import org.apache.jorphan.reflect.Functor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationTemplateFrame extends JDialog implements ActionListener {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationTemplateFrame.class);

  private static final int FIELD_WIDTH = 350;
  private static final int LABEL_WIDTH = 70;
  private static final int FIELD_HEIGHT = 30;
  private static final Dimension FIELD_MINIMUM_DIMENSION = new Dimension(FIELD_WIDTH,
      FIELD_HEIGHT * 3);
  private static final Dimension LABEL_MINIMUM_DIMENSION = new Dimension(LABEL_WIDTH, FIELD_HEIGHT);
  private static final String ADD = "add";
  private static final String DELETE = "delete";
  private static final String CLEAR = "clear";

  private static final Border ERROR_BORDER = BorderFactory.createLineBorder(Color.RED, 1);
  private static final Border DEFAULT_BORDER = new JTextField().getBorder();

  private JButton saveButton;

  private PlaceHolderTextField templateIdField;
  private PlaceHolderTextField templateVersionField;
  private PlaceHolderTextField templateAuthorField;
  private PlaceHolderTextField templateUrlField;

  private JTextArea templateDescriptionField;
  private JTextArea templateChangesField;

  private JLabel idValidation;
  private JLabel versionValidation;
  private JLabel authorValidation;
  private JLabel urlValidation;
  private JLabel descriptionValidation;
  private JLabel changesValidation;

  private BufferedImage templateSnapshot;
  private ObjectTableModel dependencyModel;
  private JTable dependenciesTable;

  private CollapsiblePanel dependenciesCollapsiblePanel;

  public CorrelationTemplateFrame(CorrelationTemplatesRegistryHandler templatesRegistry,
      BufferedImage snapshot, Consumer<TemplateVersion> updateLastTemplateSupplier,
      JPanel parent) {
    super((JFrame) SwingUtilities.getWindowAncestor(parent), true);
    setTitle("Correlation Template");
    setName("correlationTemplateFrame");
    buildMainPanel(templatesRegistry);
    templateSnapshot = snapshot;
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    saveButton.addActionListener(
        createSaveTemplateActionListener(templatesRegistry, updateLastTemplateSupplier));
    pack();
  }

  private void buildMainPanel(CorrelationTemplatesRegistryHandler templatesRegistry) {
    saveButton = SwingUtils.createComponent("saveTemplateButton", new JButton("Save"));
    saveButton.setEnabled(false);
    saveButton.setToolTipText("Fill the field before saving");

    templateIdField = makePlaceHolderTextField("correlationTemplateIdField",
        "Unique identifier");
    templateVersionField = makePlaceHolderTextField("correlationTemplateVersionField",
        "1.0, 2.0-Beta, etc.");
    templateAuthorField = makePlaceHolderTextField("correlationTemplateAuthorField",
        "Owner, user, GitHub user, etc.");
    templateUrlField = makePlaceHolderTextField("correlationTemplateUrlField",
        "Url repository/example@email.com");

    templateDescriptionField = new JTextArea(3, 10);
    templateDescriptionField.setName("correlationTemplateDescriptionTextArea");
    templateDescriptionField.setBorder(BorderFactory.createEmptyBorder());

    templateChangesField = new JTextArea(3, 10);
    templateChangesField.setName("correlationTemplateChangesField");
    templateChangesField.setBorder(BorderFactory.createEmptyBorder());

    addFieldsValidations(templatesRegistry);

    JLabel idLabel = makeLabel("ID *:");
    JLabel descriptionLabel = makeLabel("Description *:");
    JLabel authorLabel = makeLabel("Author *:");
    JLabel urlLabel = makeLabel("URL/Email:");
    JLabel changesLabel = makeLabel("Changes *: ");
    JLabel versionLabel = makeLabel("Version *: ");
    JLabel descriptionInfo = makeLabel("This field allows HTML tags.");
    descriptionInfo.setFont(descriptionInfo.getFont().deriveFont(Font.ITALIC));
    JLabel asteriskInfo = makeLabel("All fields marked with * are required");
    asteriskInfo.setFont(asteriskInfo.getFont().deriveFont(Font.ITALIC));

    JScrollPane descriptionScrollPane = new JScrollPane(templateDescriptionField);
    descriptionScrollPane.setBorder(DEFAULT_BORDER);
    descriptionScrollPane.setBackground(templateDescriptionField.getBackground());

    JScrollPane changesScrollPane = new JScrollPane(templateChangesField);
    changesScrollPane.setBorder(DEFAULT_BORDER);
    changesScrollPane.setBackground(templateChangesField.getBackground());

    dependenciesCollapsiblePanel = buildDependenciesPanel();

    JPanel mainPanel = SwingUtils.createComponent("correlationTemplatePanel", new JPanel());
    GroupLayout layout = new GroupLayout(mainPanel);
    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);
    mainPanel.setLayout(layout);
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    layout.setHorizontalGroup(layout.createParallelGroup()
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup()
                .addComponent(asteriskInfo)))
        .addGroup(layout.createSequentialGroup()
            .addComponent(idLabel)
            .addGroup(layout.createParallelGroup()
                .addComponent(templateIdField)
                .addComponent(idValidation)))
        .addGroup(layout.createSequentialGroup()
            .addComponent(versionLabel)
            .addGroup(layout.createParallelGroup()
                .addComponent(templateVersionField)
                .addComponent(versionValidation)))
        .addGroup(layout.createSequentialGroup()
            .addComponent(authorLabel)
            .addGroup(layout.createParallelGroup()
                .addComponent(templateAuthorField)
                .addComponent(authorValidation)))
        .addGroup(layout.createSequentialGroup()
            .addComponent(urlLabel)
            .addGroup(layout.createParallelGroup()
                .addComponent(templateUrlField)
                .addComponent(urlValidation)))
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup()
                .addComponent(descriptionLabel)
                .addComponent(descriptionScrollPane)
                .addComponent(descriptionInfo)
                .addComponent(descriptionScrollPane)
                .addComponent(descriptionValidation)))
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup()
                .addComponent(changesLabel)
                .addComponent(changesScrollPane)
                .addComponent(changesValidation)))
        .addGroup(layout.createSequentialGroup()
            .addComponent(dependenciesCollapsiblePanel))
        .addComponent(saveButton, Alignment.CENTER)
    );

    //JTextField have max height to keep JMeter's consistency forms
    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup()
            .addComponent(asteriskInfo))
        .addGroup(layout.createParallelGroup()
            .addComponent(idLabel)
            .addGroup(layout.createSequentialGroup()
                .addComponent(templateIdField, FIELD_HEIGHT, FIELD_HEIGHT, FIELD_HEIGHT)
                .addComponent(idValidation)))
        .addGroup(layout.createParallelGroup()
            .addComponent(versionLabel)
            .addGroup(layout.createSequentialGroup()
                .addComponent(templateVersionField, FIELD_HEIGHT, FIELD_HEIGHT, FIELD_HEIGHT)
                .addComponent(versionValidation)))
        .addGroup(layout.createParallelGroup()
            .addComponent(authorLabel)
            .addGroup(layout.createSequentialGroup()
                .addComponent(templateAuthorField, FIELD_HEIGHT, FIELD_HEIGHT, FIELD_HEIGHT)
                .addComponent(authorValidation)))
        .addGroup(layout.createParallelGroup()
            .addComponent(urlLabel)
            .addGroup(layout.createSequentialGroup()
                 .addComponent(templateUrlField, FIELD_HEIGHT, FIELD_HEIGHT, FIELD_HEIGHT)
                 .addComponent(urlValidation)))
        .addGroup(layout.createParallelGroup()
            .addGroup(layout.createSequentialGroup()
                .addComponent(descriptionLabel)
                .addComponent(descriptionInfo)
                .addComponent(descriptionScrollPane)
                .addComponent(descriptionValidation)))
        .addGroup(layout.createParallelGroup()
            .addGroup(layout.createSequentialGroup()
                .addComponent(changesLabel)
                .addComponent(changesScrollPane)
                .addComponent(changesValidation)))
        .addGroup(layout.createParallelGroup()
            .addComponent(dependenciesCollapsiblePanel))
        .addGap(FIELD_HEIGHT, FIELD_HEIGHT, FIELD_HEIGHT)
        .addComponent(true, saveButton)
    );
    add(mainPanel);
  }

  private PlaceHolderTextField makePlaceHolderTextField(String name, String placeHolderText) {
    PlaceHolderTextField field = SwingUtils.createComponent(name, new PlaceHolderTextField(),
        FIELD_MINIMUM_DIMENSION);
    field.setPlaceHolder(placeHolderText);
    return field;
  }

  private void addFieldsValidations(
      CorrelationTemplatesRegistryHandler templatesRegistry) {
    //Value cant be empty
    Validation isEmpty = new Validation(text -> text.trim().isEmpty(), "This field can't be empty");

    //Value can't have special characters
    Validation hasInvalidCharacters = new Validation(text ->
        Pattern.compile("[^a-zA-Z0-9\\-_.]").matcher(text).find(), "Use only alphanumeric values "
        + "and dashes (- and _).");

    //Only will be tested if the previous validations pass. Version has to be unique
    Validation isRepeatedVersion = new Validation(version ->
        templatesRegistry.isLocalTemplateVersionSaved(getTemplateID(), version),
        "This %s is already in use");

    idValidation = makeValidationLabel("idValidation");
    versionValidation = makeValidationLabel("versionValidation");
    authorValidation = makeValidationLabel("authorValidation");
    urlValidation = makeValidationLabel("urlValidation");
    descriptionValidation = makeValidationLabel("descriptionValidation");
    changesValidation = makeValidationLabel("changesValidation");

    addFieldValidations("Id", templateIdField, idValidation, isEmpty, hasInvalidCharacters);
    addFieldValidations("Version", templateVersionField, versionValidation, isEmpty,
        hasInvalidCharacters, isRepeatedVersion);
    addFieldValidations("Author", templateAuthorField, authorValidation, isEmpty);
    addFieldValidations("Description", templateDescriptionField, descriptionValidation, isEmpty);
    addFieldValidations("Changes", templateChangesField, changesValidation, isEmpty);
  }

  private JLabel makeValidationLabel(String labelName) {
    JLabel label = makeLabel("");
    label.setName(labelName);
    label.setVisible(false);
    label.setForeground(Color.RED);
    return label;
  }

  public void addFieldValidations(String id, JTextComponent field, JLabel errorDisplay,
      Validation... validations) {
    field.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        Optional<Validation> failingValidation = Arrays.stream(validations)
            .filter(v -> v.failsValidation(field.getText()))
            .findFirst();
        boolean failsValidation = failingValidation.isPresent();
        updateComponentValidationStyle(field, errorDisplay, failsValidation);
        errorDisplay.setText(failsValidation ? failingValidation.get().formatErrorMessage(id) : "");
        validateForm();
        pack();
        super.focusLost(e);
      }
    });
  }

  private void updateComponentValidationStyle(JTextComponent field, JLabel errorDisplay,
      boolean hasFailedValidation) {
    //JTextArea are contained in JScroll's JView point, need to get 2 parent levels
    (field instanceof JTextField ? field : (JScrollPane) field.getParent().getParent())
        .setBorder(hasFailedValidation ? ERROR_BORDER : DEFAULT_BORDER);
    errorDisplay.setVisible(hasFailedValidation);
  }

  private void validateForm() {
    boolean formIsInvalid =
        idValidation.isVisible() || versionValidation.isVisible() || descriptionValidation
            .isVisible() || changesValidation.isVisible() || authorValidation.isVisible();
    saveButton.setEnabled(!formIsInvalid);
    saveButton
        .setToolTipText(formIsInvalid ? "Please correct the issues in red before proceed" : "");
  }

  private JLabel makeLabel(String text) {
    return SwingUtils.createComponent("", new JLabel(text), LABEL_MINIMUM_DIMENSION);
  }

  private CollapsiblePanel buildDependenciesPanel() {
    JPanel dependenciesBody = prepareDependenciesPanel();
    dependenciesBody.setPreferredSize(new Dimension(300, 200));
    /*
     * Since some L&F won't allow to cast LineBorder over the JTextField border to
     * obtain the color, we use the default border as it is (won't remove the top).
     */
    dependenciesBody.setBorder(DEFAULT_BORDER);
    CollapsiblePanel ret = new CollapsiblePanel.Builder()
        .withTitle("Dependencies")
        .withCollapsingListener(this::onCollapseResize)
        .withContent(dependenciesBody)
        .withCollapsed(true)
        .build();
    ret.getHeaderPanel().setPreferredSize(new Dimension(300, 30));
    return ret;
  }

  private void onCollapseResize() {
    Dimension actualSize = getSize();
    int contentHeight = 230;

    int finalHeight = dependenciesCollapsiblePanel.isCollapsed()
        ? actualSize.height - contentHeight : actualSize.height + contentHeight;

    Dimension finalSize = new Dimension(actualSize.width, finalHeight);
    setPreferredSize(finalSize);
    setSize(finalSize);
  }

  private JPanel prepareDependenciesPanel() {
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
        .addComponent(dependencyOverwriteLabel, Alignment.CENTER)
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
    dependenciesTable.setPreferredSize(new Dimension(300, 200));
    dependenciesTable.setName("templateDependenciesScroll");
    dependenciesTable.getColumnModel().getColumn(0).setPreferredWidth(50);
    dependenciesTable.getColumnModel().getColumn(1).setPreferredWidth(50);
    dependenciesTable.getColumnModel().getColumn(2).setPreferredWidth(200);
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
    pane.setPreferredSize(new Dimension(350, 150));
    return pane;
  }

  private JPanel buildDependencyButtonPanel() {
    ButtonBuilder base = new SwingUtils.ButtonBuilder()
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

  public void showFrame() {
    setVisible(true);
    requestFocus();
  }

  private String getTemplateID() {
    return templateIdField.getText();
  }

  private String getVersion() {
    return templateVersionField.getText();
  }

  private String getAuthor() {
    return templateAuthorField.getText();
  }

  private String getUrl() {
    return templateUrlField.getText();
  }

  private void close() {
    this.dispose();
  }

  private ActionListener createSaveTemplateActionListener(
      CorrelationTemplatesRegistryHandler templatesRegistry,
      Consumer<TemplateVersion> updateLastTemplateConsumer) {
    return e -> {
      //Left for the cases where the ID was changed after the version
      if (isVersionRepeatedLocally(templatesRegistry)) {
        JOptionPane.showMessageDialog(CorrelationTemplateFrame.this,
            "There is already a version " + getVersion() + " installed. Please change it before "
                + "continue",
            "Save template error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      boolean hasRepeatedDependencies = false;
      boolean hasFailingURLs = false;
      List<CorrelationTemplateDependency> dependenciesList = new ArrayList<>();

      if (dependencyModel.getRowCount() > 0) {
        Iterator<CorrelationTemplateDependency> iterator =
            (Iterator<CorrelationTemplateDependency>) dependencyModel
                .iterator();

        boolean allDependenciesComplete = true;
        int dependencyIndex = 0;
        while (iterator.hasNext()) {
          dependencyIndex++;
          CorrelationTemplateDependency dependency = iterator.next();
          if (hasRowEmptyValues(dependency, dependencyIndex)) {
            allDependenciesComplete = false;
            continue;
          }

          List<CorrelationTemplateDependency> repeatedDependencies = dependenciesList.stream()
              .filter(d -> d.getName().trim().toLowerCase()
                  .equals(dependency.getName().trim().toLowerCase()))
              .collect(Collectors.toList());

          if (!repeatedDependencies.isEmpty()) {
            hasRepeatedDependencies = true;
            dependenciesList.removeAll(repeatedDependencies);
          }

          if (!templatesRegistry.isValidDependencyURL(dependency.getUrl(), dependency.getName(),
              dependency.getVersion())) {
            hasFailingURLs = true;
            continue;
          }

          dependenciesList.add(dependency);
        }

        if (!allDependenciesComplete) {
          JOptionPane.showMessageDialog(CorrelationTemplateFrame.this,
              "There are incomplete dependencies. Fill or delete them before continue.");
          return;
        }
      }

      if (hasFailingURLs) {
        JOptionPane.showMessageDialog(CorrelationTemplateFrame.this,
            "There are some issues with some dependency's URLs, please fix then before continue"
                + ".\nCheck the logs for more information.");
        return;
      }

      if (hasRepeatedDependencies && JOptionPane.showConfirmDialog(CorrelationTemplateFrame.this,
          "There are dependencies that are repeated. Want to overwrite them?",
          "Saving Correlation Template",
          JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
        return;
      }

      Builder savingTemplateBuilder = new Builder()
          .withId(getTemplateID())
          .withVersion(getVersion())
          .withAuthor(getAuthor())
          .withUrl(getUrl())
          .withDescription(templateDescriptionField.getText())
          .withSnapshot(templateSnapshot)
          .withRepositoryId(LocalConfiguration.LOCAL_REPOSITORY_NAME)
          .withChanges(templateChangesField.getText())
          .withDependencies(dependenciesList);

      try {
        templatesRegistry.onSaveTemplate(savingTemplateBuilder);
        updateLastTemplateConsumer.accept(savingTemplateBuilder.build());
      } catch (IOException | ConfigurationException ex) {
        JOptionPane
            .showMessageDialog(CorrelationTemplateFrame.this,
                "Error while trying to save template", "Save template error",
                JOptionPane.ERROR_MESSAGE);
        LOG.error("Error while trying to convert rules into json file {}", getTemplateID(), ex);
      }
      close();
      clear();
    };
  }

  public void clear() {
    clearField(templateIdField);
    clearField(templateVersionField);
    clearField(templateAuthorField);
    clearField(templateUrlField);
    clearField(templateDescriptionField);
    clearField(templateChangesField);
    saveButton.setEnabled(false);
    clearTable();
    clearStyles();
  }

  private void clearField(JTextComponent field) {
    field.setText("");
    (field instanceof JTextArea ? ((JScrollPane) field.getParent().getParent()) : field)
        .setBorder(DEFAULT_BORDER);
  }

  private void clearTable() {
    dependencyModel.clearData();
  }

  private void clearStyles() {
    updateComponentValidationStyle(templateIdField, idValidation, false);
    updateComponentValidationStyle(templateVersionField, versionValidation, false);
    updateComponentValidationStyle(templateAuthorField, authorValidation, false);
    updateComponentValidationStyle(templateUrlField, urlValidation, false);
    updateComponentValidationStyle(templateDescriptionField, descriptionValidation, false);
    updateComponentValidationStyle(templateChangesField, changesValidation, false);
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

  private boolean isVersionRepeatedLocally(CorrelationTemplatesRegistryHandler repositoryHandler) {
    return repositoryHandler.isLocalTemplateVersionSaved(getTemplateID(), getVersion());
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

  public void setLoadedTemplates(Set<TemplateVersion> loadedTemplates) {
    clear();
    //If we have more than 1 loaded template, we don't set this fields
    if (loadedTemplates.size() == 1) {
      TemplateVersion loadedTemplate = loadedTemplates.iterator().next();
      templateIdField.setText(loadedTemplate.getId());
      templateAuthorField.setText(loadedTemplate.getAuthor());
      templateUrlField.setText(loadedTemplate.getUrl());
      templateVersionField.setText(loadedTemplate.getVersion());
      templateDescriptionField.setText(loadedTemplate.getDescription());
    }
    loadedTemplates.stream()
        .filter(t -> t.getDependencies() != null && !t.getDependencies().isEmpty())
        .forEach(t -> setDependencies(t.getDependencies()));
  }
}
