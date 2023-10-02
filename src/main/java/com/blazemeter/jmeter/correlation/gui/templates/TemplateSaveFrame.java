package com.blazemeter.jmeter.correlation.gui.templates;

import static com.blazemeter.jmeter.correlation.core.templates.RepositoryGeneralConst.LOCAL_REPOSITORY_NAME;

import com.blazemeter.jmeter.commons.SwingUtils;
import com.blazemeter.jmeter.correlation.core.DescriptionContent;
import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateDependency;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepository;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.Template.Builder;
import com.blazemeter.jmeter.correlation.gui.common.CollapsiblePanel;
import com.blazemeter.jmeter.correlation.gui.common.HelperDialog;
import com.blazemeter.jmeter.correlation.gui.common.PlaceHolderTextField;
import com.blazemeter.jmeter.correlation.gui.common.ThemedIconLabel;
import com.blazemeter.jmeter.correlation.gui.templates.validations.Condition;
import com.blazemeter.jmeter.correlation.gui.templates.validations.ValidationManager;
import com.blazemeter.jmeter.correlation.gui.templates.validations.type.NotEmptyCondition;
import com.blazemeter.jmeter.correlation.gui.templates.validations.type.UniqueVersionCondition;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import org.apache.jorphan.gui.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateSaveFrame extends JDialog implements ActionListener {

  private static final Logger LOG = LoggerFactory.getLogger(TemplateSaveFrame.class);
  private static final int FIELD_WIDTH = 350;
  private static final int LABEL_WIDTH = 70;
  private static final int FIELD_HEIGHT = 30;
  private static final Dimension FIELD_MINIMUM_DIMENSION = new Dimension(FIELD_WIDTH,
      FIELD_HEIGHT);
  private static final Dimension LABEL_MINIMUM_DIMENSION = new Dimension(LABEL_WIDTH, FIELD_HEIGHT);
  private static final Border DEFAULT_BORDER = new JTextField().getBorder();
  private static final String SAVE = "SAVE";
  private static final String VALIDATE = "VALIDATE";
  private static final String CANCEL = "CANCEL";
  private final JLabel helper = new ThemedIconLabel("help.png");
  private JButton saveButton;
  private JButton cancelButton;
  private JComboBox<CorrelationTemplatesRepository> repositoriesComboBox;
  private PlaceHolderComboBox protocolsComboBox;
  private JComboBox<String> existingVersionsComboBox;
  private PlaceHolderTextField newVersionTextField;
  private PlaceHolderTextField authorTextField;
  private PlaceHolderTextField urlTextField;
  private JTextArea descriptionTextArea;
  private JTextArea changesTextArea;
  private TemplateDependenciesTable dependenciesTable;
  private JLabel protocolValidation;
  private JLabel versionValidation;
  private JLabel authorValidation;
  private JLabel urlValidation;
  private JLabel descriptionValidation;
  private JLabel changesValidation;
  private BufferedImage templateSnapshot;
  private CollapsiblePanel dependenciesCollapsiblePanel;
  private final HashMap<String, CorrelationTemplatesRepository> repositoriesMap = new HashMap<>();
  private CorrelationTemplatesRegistryHandler templatesRegistry;
  private CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistry;
  private Consumer<Template> updateLastTemplateSupplier;
  private HelperDialog helperDialog;

  private Map<String, String> repositoryIdToName = new HashMap<>();

  private JTabbedPane tabbedPane = new JTabbedPane();

  private ValidationManager manager = new ValidationManager();
  private Template loadedTemplate;

  public TemplateSaveFrame(CorrelationTemplatesRegistryHandler templatesRegistry,
                           CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistry,
                           BufferedImage snapshot, Consumer<Template> updateLastTemplateSupplier,
                           JPanel parent) {
    super((JFrame) SwingUtilities.getWindowAncestor(parent), false);
    setTitle("Correlation Template");
    setName("correlationTemplateFrame");
    this.templateSnapshot = snapshot;
    this.templatesRegistry = templatesRegistry;
    this.repositoriesRegistry = repositoriesRegistry;
    this.updateLastTemplateSupplier = updateLastTemplateSupplier;

    buildMainPanel();
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    dependenciesTable.setTemplatesRegistry(templatesRegistry);
    saveButton.setEnabled(false);
    getRootPane().registerKeyboardAction(e -> closeDialog(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        closeDialog();
        super.windowClosing(e);
      }
    });

    TemplateSaveFrame currJDialog = this;
    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        ComponentUtil.centerComponentInWindow(currJDialog);
      }
    });

    pack();
  }

  private void closeDialog() {
    manager.resetAll();
    tabbedPane.setSelectedIndex(0);
    dispose();
  }

  private void buildMainPanel() {
    initElements();
    addValidations();

    tabbedPane.setName("tabbedPane");
    tabbedPane.addTab("Version Information", createCommonEditableFieldsPanel());
    tabbedPane.addTab("Template Information", createTemplateDescriptionPanel());

    JPanel buttonPanel = new JPanel();
    buttonPanel.setName("buttonPanel");
    GroupLayout buttonLayout = new GroupLayout(buttonPanel);
    buttonPanel.setLayout(buttonLayout);

    buttonLayout.setHorizontalGroup(buttonLayout.createSequentialGroup()
        .addComponent(saveButton)
        .addComponent(cancelButton));

    buttonLayout.setVerticalGroup(buttonLayout.createSequentialGroup()
        .addGroup(buttonLayout.createParallelGroup(Alignment.CENTER)
            .addComponent(saveButton)
            .addComponent(cancelButton)));

    JLabel information = new JLabel("Saving your current version of the Template");

    JPanel base = SwingUtils.createComponent("basePanel", new JPanel());
    GroupLayout layout = SwingUtils.createGroupLayout(base);

    layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addComponent(information)
            .addComponent(helper))
        .addGroup(layout.createSequentialGroup()
            .addComponent(tabbedPane))
        .addGroup(layout.createSequentialGroup()
            .addComponent(buttonPanel)));

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(Alignment.CENTER)
            .addComponent(information)
            .addComponent(helper))
        .addGap(10)
        .addComponent(tabbedPane)
        .addComponent(buttonPanel));

    add(base);
  }

  private JPanel createCommonEditableFieldsPanel() {
    JLabel repositoryLabel = makeLabel("Repository *:");
    JLabel templateLabel = makeLabel("Template *:");
    JLabel versionLabel = makeLabel("Version:");
    JLabel proposedLabel = makeLabel("New Version *:");
    JLabel changesLabel = makeLabel("Changes *:");

    JPanel fieldsPanel = new JPanel();
    fieldsPanel.setName("fieldsPanel");

    GroupLayout layout = SwingUtils.createGroupLayout(fieldsPanel);

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup()
            .addComponent(repositoryLabel)
            .addComponent(versionLabel))
        .addGroup(layout.createParallelGroup()
            .addComponent(repositoriesComboBox)
            .addComponent(existingVersionsComboBox))
        .addGroup(layout.createParallelGroup()
            .addComponent(templateLabel)
            .addComponent(proposedLabel))
        .addGroup(layout.createParallelGroup()
            .addComponent(protocolsComboBox)
            .addComponent(protocolValidation)
            .addComponent(newVersionTextField)
            .addComponent(versionValidation)));

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(repositoryLabel)
            .addComponent(repositoriesComboBox)
            .addComponent(templateLabel)
            .addComponent(protocolsComboBox, 0, FIELD_HEIGHT, FIELD_HEIGHT))
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(protocolValidation))
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(versionLabel)
            .addComponent(existingVersionsComboBox)
            .addComponent(proposedLabel)
            .addComponent(newVersionTextField))
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(versionValidation)));

    JScrollPane changesScrollPane = createScrollPane(changesTextArea);

    JPanel changesPanel = SwingUtils.createComponent("changesPanel", new JPanel());
    GroupLayout changesLayout = SwingUtils.createGroupLayout(changesPanel);

    changesLayout.setHorizontalGroup(changesLayout.createParallelGroup()
        .addComponent(changesLabel)
        .addComponent(changesScrollPane)
        .addComponent(changesValidation));

    changesLayout.setVerticalGroup(changesLayout.createSequentialGroup()
        .addComponent(changesLabel)
        .addComponent(changesScrollPane, FIELD_HEIGHT * 2, FIELD_HEIGHT * 3, FIELD_HEIGHT * 4)
        .addComponent(changesValidation));

    JLabel informationLabel = new JLabel("Select the repository, template, and template version");

    JPanel panel = SwingUtils.createComponent("currentVersionPanel", new JPanel());
    GroupLayout groupLayout = SwingUtils.createGroupLayout(panel);

    groupLayout.setHorizontalGroup(groupLayout.createParallelGroup()
        .addComponent(informationLabel)
        .addComponent(fieldsPanel)
        .addComponent(changesPanel));

    groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
        .addComponent(informationLabel)
        .addComponent(fieldsPanel)
        .addComponent(changesPanel));

    return panel;
  }

  private void initElements() {
    protocolValidation = makeValidationLabel("idValidation");
    versionValidation = makeValidationLabel("versionValidation");
    authorValidation = makeValidationLabel("authorValidation");
    urlValidation = makeValidationLabel("urlValidation");
    descriptionValidation = makeValidationLabel("descriptionValidation");
    changesValidation = makeValidationLabel("changesValidation");

    repositoriesComboBox = createComboBox("repositoriesComboBox");
    repositoriesComboBox.setRenderer(new RepositoryCellRender());

    protocolsComboBox = new PlaceHolderComboBox();
    protocolsComboBox.setPreferredSize(new Dimension(300, FIELD_HEIGHT));

    existingVersionsComboBox = createComboBox("templateVersionComboBox");

    changesTextArea = createTextArea("templateChangesTextArea");
    descriptionTextArea = createTextArea("correlationTemplateDescriptionTextArea");

    authorTextField = makePlaceHolderTextField("correlationTemplateAuthorField",
        "Owner, user, GitHub user, etc.");
    urlTextField = makePlaceHolderTextField("correlationTemplateUrlField",
        "Url repository/example@email.com");
    newVersionTextField = makePlaceHolderTextField("templateSuggestedVersionField",
        "1, 1.2, 2.0.1, etc.");

    repositoriesComboBox.addActionListener(e -> {
      CorrelationTemplatesRepository selectedRepo =
          (CorrelationTemplatesRepository) repositoriesComboBox.getSelectedItem();
      if (selectedRepo != null) {
        protocolsComboBox.setTemplates(new ArrayList<>(selectedRepo.getTemplates().values()));
      } else {
        protocolsComboBox.resetToDefault();
      }
      refreshExistingVersions();
    });

    protocolsComboBox.addActionListener(e -> refreshExistingVersions());
    protocolsComboBox.addEditorLostFocusListener(e -> refreshExistingVersions());
    existingVersionsComboBox.addActionListener(e -> updateProposedVersion());

    SwingUtils.ButtonBuilder builder = new SwingUtils.ButtonBuilder()
        .withActionListener(this)
        .isEnabled(true);

    saveButton = builder
        .withAction(SAVE)
        .withName("saveTemplateButton")
        .withText("Save")
        .withToolTip("Save the current template")
        .build();

    cancelButton = builder
        .withAction(CANCEL)
        .withName("cancelTemplateButton")
        .withText("Cancel")
        .withToolTip("Return to the main window")
        .build();

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
        helperDialog = new HelperDialog(TemplateSaveFrame.this);
        helperDialog.setName("helperDialog");
        helperDialog.setTitle("Repository Manager Information");
        helperDialog.updateDialogContent(descriptionFile);
        helperDialog.setVisible(true);
        helperDialog.updateDialogContent(descriptionFile);
      }
    });
  }

  private void loadFormData() {
    loadRepositoriesNames(repositoriesRegistry.getCorrelationRepositories());
    clearNonSavedRepository();

    repositoriesComboBox.removeAllItems();
    repositoriesMap.forEach((key, value) -> repositoriesComboBox.addItem(value));

    if (loadedTemplate == null) {
      selectLocalRepository();
    } else {
      loadDataFromTemplate();
    }
  }

  private void loadDataFromTemplate() {
    repositoriesComboBox.setSelectedItem(repositoriesMap.get(loadedTemplate.getRepositoryId()));
    setProtocol(loadedTemplate.getId());
    existingVersionsComboBox.setSelectedItem(loadedTemplate.getVersion());
    descriptionTextArea.setText(loadedTemplate.getDescription());
    authorTextField.setText(loadedTemplate.getAuthor());
    urlTextField.setText(loadedTemplate.getUrl());
    setDependencies(loadedTemplate.getDependencies());
  }

  private void refreshExistingVersions() {
    existingVersionsComboBox.removeAllItems();
    protocolsComboBox.getSelectedTemplateVersions().forEach(existingVersionsComboBox::addItem);
    existingVersionsComboBox.setSelectedIndex(0);
    refreshProposedNewVersion((String) existingVersionsComboBox.getSelectedItem());
  }

  private void loadRepositoriesNames(List<CorrelationTemplatesRepository> repositories) {
    repositoriesMap.clear();
    repositories.forEach(repository -> repositoriesMap.put(repository.getName(), repository));
  }

  private void clearNonSavedRepository() {
    // If the repository with String "central" exists in repositoriesMap, remove it
    if (repositoriesMap.containsKey("central")) {
      repositoriesMap.remove("central");
    }

    if (repositoriesMap.containsKey("bzm-central")) {
      repositoriesMap.remove("bzm-central");
    }
  }

  private static JTextArea createTextArea(String name) {
    JTextArea textArea = new JTextArea(5, 10);
    textArea.setName(name);
    textArea.setBorder(BorderFactory.createEmptyBorder());
    return textArea;
  }

  private static JScrollPane createScrollPane(JTextArea textArea) {
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setBorder(DEFAULT_BORDER);
    scrollPane.setBackground(textArea.getBackground());
    return scrollPane;
  }

  private static boolean isManuallyEditing(Object selectedProtocol) {
    return selectedProtocol == null || !(selectedProtocol instanceof CorrelationTemplateVersions);
  }

  private void refreshProposedNewVersion(String initialVersion) {
    String nextVersion = VersionUtils.getNextProposedPatch(initialVersion);
    newVersionTextField.setText(nextVersion);
  }

  private JComboBox createComboBox(String name) {
    return SwingUtils.createComponent(name, new JComboBox<>());
  }

  private void updateProposedVersion() {
    String baseVersion = (String) existingVersionsComboBox.getSelectedItem();
    if (baseVersion == null) {
      return;
    }

    refreshProposedNewVersion(baseVersion);
  }

  private JPanel createTemplateDescriptionPanel() {
    JLabel templateDescriptionLabel = new JLabel("Template's description (this field allows "
        + "HTML tags)");

    JScrollPane descriptionScrollPane = createScrollPane(descriptionTextArea);

    JPanel templateDescriptionPanel = SwingUtils.createComponent("templateDescriptionPanel",
        new JPanel());
    GroupLayout groupLayout = SwingUtils.createGroupLayout(templateDescriptionPanel);

    groupLayout.setHorizontalGroup(groupLayout.createParallelGroup()
        .addComponent(templateDescriptionLabel)
        .addComponent(descriptionScrollPane)
        .addComponent(descriptionValidation));

    groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
        .addComponent(templateDescriptionLabel)
        .addComponent(descriptionScrollPane, FIELD_HEIGHT, FIELD_HEIGHT * 2, FIELD_HEIGHT * 3)
        .addComponent(descriptionValidation));

    JPanel authorDetailsPanel = SwingUtils.createComponent("authorDetailsPanel", new JPanel());
    GroupLayout layout = SwingUtils.createGroupLayout(authorDetailsPanel);

    JLabel authorLabel = new JLabel("Author's Name:");
    JLabel authorEmailLabel = new JLabel("Author's Email:");

    // Add the elements so each label appears at the left of the TextField and the Validation
    // labels appear under the TextField
    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup()
            .addComponent(authorLabel)
            .addComponent(authorEmailLabel))
        .addGroup(layout.createParallelGroup()
            .addComponent(authorTextField)
            .addComponent(authorValidation)
            .addComponent(urlTextField)
            .addComponent(urlValidation)));

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(authorLabel)
            .addComponent(authorTextField))
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(authorValidation))
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(authorEmailLabel)
            .addComponent(urlTextField))
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(urlValidation)));

    dependenciesCollapsiblePanel = buildDependenciesPanel();

    JPanel templateInformationPanel = SwingUtils.createComponent("templateInformationPanel",
        new JPanel());
    GroupLayout templateInformationLayout = SwingUtils.createGroupLayout(templateInformationPanel);

    templateInformationLayout.setHorizontalGroup(
        templateInformationLayout.createParallelGroup()
            .addComponent(templateDescriptionPanel)
            .addComponent(authorDetailsPanel)
            .addComponent(dependenciesCollapsiblePanel));

    templateInformationLayout.setVerticalGroup(
        templateInformationLayout.createSequentialGroup()
            .addComponent(templateDescriptionPanel)
            .addComponent(authorDetailsPanel)
            .addComponent(dependenciesCollapsiblePanel));

    return templateInformationPanel;
  }

  private PlaceHolderTextField makePlaceHolderTextField(String name, String placeHolderText) {
    PlaceHolderTextField field = SwingUtils.createComponent(name, new PlaceHolderTextField(),
        new Dimension(150, 30));
    field.setMaximumSize(FIELD_MINIMUM_DIMENSION);
    field.setPlaceHolder(placeHolderText);
    return field;
  }

  private JTextField getProtocolsContentField() {
    return (JTextField) protocolsComboBox.getEditor().getEditorComponent();
  }

  private JLabel makeValidationLabel(String labelName) {
    JLabel label = makeLabel("");
    label.setName(labelName);
    label.setVisible(true);
    label.setForeground(Color.RED);
    return label;
  }

  private void addValidations() {
    UniqueVersionCondition uniqueVersionCondition = new UniqueVersionCondition();
    uniqueVersionCondition.setVersionsSupplier(
        () -> protocolsComboBox.getSelectedTemplateVersions());

    NotEmptyCondition emptyCondition = new NotEmptyCondition();

    List<Condition> nonEmptyCondition = Collections.singletonList(emptyCondition);
    List<Condition> versionConditions = Arrays.asList(emptyCondition, uniqueVersionCondition);

    JTextField protocolTextField = getProtocolsContentField();
    manager.register(protocolTextField, protocolValidation, nonEmptyCondition);
    manager.register(newVersionTextField, versionValidation, versionConditions);
    manager.register(changesTextArea, changesValidation, nonEmptyCondition);
    manager.register(descriptionTextArea, descriptionValidation, nonEmptyCondition);
    manager.register(authorTextField, authorValidation, nonEmptyCondition);
    manager.register(urlTextField, urlValidation, nonEmptyCondition);

    manager.setSaveButton(saveButton);
    manager.setAfterValidation(this::refreshWindow);
  }

  public void refreshWindow() {
    repaint();
    pack();
  }

  private void updateComponentValidationStyle(JTextComponent field, JLabel errorDisplay,
                                              boolean hasFailedValidation) {
    Color defaultBackgroundColor = UIManager.getColor("TextField.background");
    Color background = hasFailedValidation ? Color.PINK : defaultBackgroundColor;
    if (field instanceof JTextField) {
      field.setBackground(background);
    } else if (field instanceof JTextArea) {
      field.setBackground(background);
      Container viewPortContainer = field.getParent();
      viewPortContainer.setBackground(background);
      Container scrollContainer = viewPortContainer.getParent();
      if (scrollContainer != null) {
        scrollContainer.setBackground(background);
      }
    } else if (field instanceof PlaceHolderTextField) {
      field.setBackground(background);
    }
    errorDisplay.setVisible(hasFailedValidation);
  }

  private void validateForm() {
    boolean formIsInvalid = Stream.of(protocolValidation, versionValidation, authorValidation,
            urlValidation, descriptionValidation, changesValidation)
        .anyMatch(validation -> validation.isVisible());
    saveButton.setEnabled(!formIsInvalid);
    saveButton.setToolTipText(formIsInvalid ? "Please correct the issues in red before proceed"
        : "");
  }

  private JLabel makeLabel(String text) {
    return SwingUtils.createComponent("", new JLabel(text), LABEL_MINIMUM_DIMENSION);
  }

  private CollapsiblePanel buildDependenciesPanel() {
    dependenciesTable = new TemplateDependenciesTable();
    JPanel dependenciesBody = dependenciesTable.prepareDependenciesPanel();
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

  public void showFrame() {
    clear();
    loadFormData();
    pack();
    ComponentUtil.centerComponentInWindow(this);
    setVisible(true);
    requestFocus();
  }

  private String getTemplateID() {
    return " REMOVE THIS";
  }

  private String getVersion() {
    return newVersionTextField.getText();
  }

  private String getAuthor() {
    return authorTextField.getText();
  }

  private String getUrl() {
    return urlTextField.getText();
  }

  public void clear() {
    clearStyles();
    clearField(changesTextArea);
    clearField(descriptionTextArea);
    clearField(authorTextField);
    clearField(urlTextField);
    dependenciesTable.clear();
    saveButton.setEnabled(false);
  }

  public void selectLocalRepository() {
    repositoriesComboBox.setSelectedItem(repositoriesMap.get(LOCAL_REPOSITORY_NAME));
  }

  private void clearField(JTextComponent field) {
    field.setText("");
    (field instanceof JTextArea ? ((JScrollPane) field.getParent().getParent()) : field)
        .setBorder(DEFAULT_BORDER);
  }

  private void clearStyles() {
    resetRepositoryDependantFields();
    updateComponentValidationStyle(authorTextField, authorValidation, false);
    updateComponentValidationStyle(urlTextField, urlValidation, false);
    updateComponentValidationStyle(descriptionTextArea, descriptionValidation, false);
    updateComponentValidationStyle(changesTextArea, changesValidation, false);
  }

  private void resetRepositoryDependantFields() {
    updateComponentValidationStyle(getProtocolsContentField(), protocolValidation, false);
    updateComponentValidationStyle(newVersionTextField, versionValidation, false);
  }

  private boolean isVersionRepeatedLocally(CorrelationTemplatesRegistryHandler repositoryHandler) {
    return repositoryHandler.isLocalTemplateVersionSaved(getTemplateID(), getVersion());
  }

  public void setTemplateSnapshot(BufferedImage snapshot) {
    this.templateSnapshot = snapshot;
  }

  public void setDependencies(List<CorrelationTemplateDependency> dependencies) {
    dependenciesTable.setDependencies(dependencies);
  }

  public void setLoadedTemplates(Template loadedTemplate) {
    this.loadedTemplate = loadedTemplate;
  }

  public void setProtocol(String protocol) {
    boolean protocolFound = false;
    for (int i = 0; i < protocolsComboBox.getItemCount(); i++) {
      CorrelationTemplateVersions template = protocolsComboBox.getItemAt(i);
      if (template.getName().equals(protocol)) {
        protocolsComboBox.setSelectedIndex(i);
        protocolFound = true;
        break;
      }
    }
    if (!protocolFound) {
      protocolsComboBox.setSelectedItem(protocol);
    }
  }

  @VisibleForTesting
  public TemplateDependenciesTable getDependenciesTable() {
    return dependenciesTable;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();
    switch (command) {
      case SAVE:
        if (isValidForm()) {
          saveTemplate();
        }
        break;
      case CANCEL:
        closeDialog();
        break;
      case VALIDATE:
        validateForm();
        break;
      default:
        LOG.warn("Unknown action command: {}", command);
    }
  }

  private boolean isValidForm() {
    dependenciesTable.validateDependencies();
    if (dependenciesTable.isHasFailingURLs()) {
      JOptionPane.showMessageDialog(TemplateSaveFrame.this,
          "There are some issues with some dependency's URLs, please fix then before continue"
              + ".\nCheck the logs for more information.");
      return false;
    }

    if (dependenciesTable.isHasRepeatedDependencies()) {
      if (JOptionPane.showConfirmDialog(TemplateSaveFrame.this,
          "There are dependencies that are repeated. Want to overwrite them?",
          "Saving Correlation Template",
          JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) == JOptionPane.NO_OPTION) {
        return false;
      }
    }

    return true;
  }

  private void saveTemplate() {
    Builder savingTemplateBuilder = new Builder()
        .withId(getProtocolName())
        .withRepositoryId(getRepositoryName())
        .withVersion(getNewVersion())
        .withChanges(getChanges())

        .withDescription(getDescription())
        .withAuthor(getAuthorName())
        .withUrl(getAuthorEmail())
        .withDependencies(getDependencies())
        .withSnapshot(templateSnapshot);

    boolean wasSuccessful = true;
    String responseMessage = "Template saved successfully.";
    try {
      templatesRegistry.onSaveTemplate(savingTemplateBuilder);
    } catch (IOException | ConfigurationException ex) {
      responseMessage = "Error while trying to save template: " + ex.getMessage();
      wasSuccessful = false;
      LOG.error("Error while saving your template. Message: {}.", ex.getMessage());
      return;
    }

    JOptionPane.showMessageDialog(TemplateSaveFrame.this,
        responseMessage, "Save template",
        JOptionPane.INFORMATION_MESSAGE);

    if (wasSuccessful) {
      updateLastTemplateSupplier.accept(savingTemplateBuilder.build());
      closeDialog();
    }
  }

  public static class RepositoryCellRender extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {

      Component c =
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      CorrelationTemplatesRepository repository = (CorrelationTemplatesRepository) value;
      if (repository != null) {
        setText(repository.getDisplayName());
      } else {
        setText("None");
      }
      return c;
    }
  }

  private String getRepositoryName() {
    return ((CorrelationTemplatesRepository) repositoriesComboBox.getSelectedItem()).getName();
  }

  private String getProtocolName() {
    return protocolsComboBox.getSelectedTemplate().getName();
  }

  private String getNewVersion() {
    return newVersionTextField.getText();
  }

  private String getChanges() {
    return changesTextArea.getText();
  }

  private String getAuthorName() {
    return authorTextField.getText();
  }

  private String getAuthorEmail() {
    return urlTextField.getText();
  }

  private String getDescription() {
    return descriptionTextArea.getText();
  }

  private List<CorrelationTemplateDependency> getDependencies() {
    return dependenciesTable.getDependencies();
  }

  private String getCurrentVersion() {
    return (String) existingVersionsComboBox.getSelectedItem();
  }
}
