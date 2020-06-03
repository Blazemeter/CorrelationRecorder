package com.blazemeter.jmeter.correlation.core.templates;

import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.INSTALL;
import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.UNINSTALL;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;

import com.blazemeter.jmeter.correlation.gui.StringUtils;
import com.blazemeter.jmeter.correlation.gui.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.ThemedIcon;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationTemplatesFrame extends JFrame implements ActionListener {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationTemplatesFrame.class);
  private static final Dimension FIELDS_DIMENSION = new Dimension(100, 30);
  private static final Dimension TEMPLATE_LIST_DIMENSION = new Dimension(200, 300);

  private static final String CONFIG = "config";
  private static final String LOAD = "load";

  private static final String IMAGE_CACHE_PROPERTY_NAME = "imageCache";
  private static final String VERSIONS_COMBO_NAME = "templateVersions";
  private static final String INSTALL_TEMPLATE_WARNING_TITLE = "Installing template";

  private static final String TEMPLATE_ID_LABEL = "templateIdLabel";
  private static final String REFRESH = "refresh";

  private JTextField templatesSearchField;
  private final CorrelationTemplatesRegistryHandler templatesRegistryHandler;
  private final CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistryHandler;

  private DefaultListModel<TemplateItem> installedModel;
  private DefaultListModel<TemplateItem> availableModel;
  private JList<TemplateItem> installedTemplates;
  private JList<TemplateItem> availableTemplates;
  private JTabbedPane templatesTabbedPane;

  private JComboBox<CorrelationTemplate> version;
  private JLabel templateID;
  private JLabel versionLabel;
  private JTextPane templateInfoPane;
  private JButton installTemplateButton;
  private JButton loadTemplate;
  private CorrelationTemplatesRepositoryConfigFrame configFrame;
  private final Consumer<CorrelationTemplate> lastTemplateHandler;
  private SwingWorker<Object, Object> refreshTask;

  public CorrelationTemplatesFrame(CorrelationTemplatesRegistryHandler templatesRegistryHandler,
      CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistryHandler,
      Consumer<CorrelationTemplate> lastTemplateHandler) {
    this.templatesRegistryHandler = templatesRegistryHandler;
    this.repositoriesRegistryHandler = repositoriesRegistryHandler;
    this.lastTemplateHandler = lastTemplateHandler;

    setTitle("Templates Manager");
    setName("correlationTemplateFrame");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(0, 5));

    add(prepareInfoPanel());
    pack();
  }

  private JPanel prepareInfoPanel() {
    prepareSearchField();
    prepareVersionCombo();
    installedTemplates = prepareTemplatesList("installedTemplatesList");
    availableTemplates = prepareTemplatesList("availableTemplatesList");
    updateTemplatesList();

    JScrollPane installedTemplatesScroll = prepareTemplatesScrollList(installedTemplates);
    JScrollPane availableTemplatesScroll = prepareTemplatesScrollList(availableTemplates);

    JPanel versionTemplateDisplayPanel = prepareTemplateInfoPanel();
    JButton configButton = buildButton("configButton", "Config", CONFIG);
    JButton refreshButton = buildButton("refreshButton", "", REFRESH);
    refreshButton.setToolTipText("Refresh repositories and templates");
    refreshButton.setIcon(ThemedIcon.fromResourceName("refresh-button.png"));

    templatesTabbedPane = new JTabbedPane();
    templatesTabbedPane.setName("templatesTabbedPane");
    templatesTabbedPane.addTab("Installed", installedTemplatesScroll);
    templatesTabbedPane.addTab("Available", availableTemplatesScroll);
    templatesTabbedPane.addChangeListener(e -> {
      if (templatesTabbedPane.getSelectedIndex() == 0
          && installedTemplates.getSelectedValue() != null) {
        displaySelectedItem(installedTemplates.getSelectedValue());
      } else if (templatesTabbedPane.getSelectedIndex() == 1
          && availableTemplates.getSelectedValue() != null) {
        displaySelectedItem(availableTemplates.getSelectedValue());
      }
    });

    JPanel main = new JPanel();
    main.setMinimumSize(new Dimension(600, 500));
    GroupLayout layout = new GroupLayout(main);
    main.setLayout(layout);
    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);

    layout.setHorizontalGroup(
        layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(templatesSearchField)
                        .addComponent(refreshButton)
                        .addComponent(configButton))
                    .addComponent(templatesTabbedPane, TEMPLATE_LIST_DIMENSION.width,
                        TEMPLATE_LIST_DIMENSION.width, TEMPLATE_LIST_DIMENSION.width)
                )
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(versionTemplateDisplayPanel, 400, 400, Short.MAX_VALUE)))
    );
    layout.setVerticalGroup(
        layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(TRAILING)
                        .addGroup(LEADING, layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(LEADING)
                                .addComponent(templatesSearchField)
                                .addComponent(refreshButton)
                                .addComponent(configButton))
                            .addComponent(templatesTabbedPane))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(versionTemplateDisplayPanel, 400,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
    );

    return main;
  }

  private JList<TemplateItem> prepareTemplatesList(String listName) {
    JList<TemplateItem> template = SwingUtils.createComponent(listName, new JList<>());
    template.setPreferredSize(new Dimension(100, 500));

    template.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
        Component renderer = super
            .getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (renderer instanceof JLabel && value instanceof TemplateItem) {
          TemplateItem template = (TemplateItem) value;
          JLabel templateLabel = (JLabel) renderer;
          templateLabel.setText(StringUtils.capitalize(template.getId()));
        }
        return renderer;
      }
    });

    template.addListSelectionListener(e -> {
      if (e.getSource() instanceof JList) {
        JList<TemplateItem> sourceTemplate = (JList<TemplateItem>) e.getSource();
        TemplateItem selectedGroupTemplate = sourceTemplate.getSelectedValue();
        displaySelectedItem(selectedGroupTemplate);
      }
    });

    return template;
  }

  private void displaySelectedItem(TemplateItem templateItem) {
    if (templateItem != null) {
      DefaultComboBoxModel<CorrelationTemplate> versionsModel = new DefaultComboBoxModel<>();
      List<CorrelationTemplate> versions = templateItem.getVersions();
      versions.forEach(versionsModel::addElement);
      version.setVisible(true);
      version.setModel(versionsModel);
      versionLabel.setVisible(true);

      Optional<CorrelationTemplate> installedTemplate = versions.stream()
          .filter(CorrelationTemplate::isInstalled)
          .findFirst();

      if (installedTemplate.isPresent()) {
        version.setSelectedItem(installedTemplate.get());
      } else {
        version.setSelectedIndex(versions.size() - 1);
      }
    }
  }

  private void prepareVersionCombo() {
    versionLabel = SwingUtils.createComponent("versionLabel", new JLabel("Versions: "));
    versionLabel.setVisible(false);
    Dimension versionDimension = new Dimension(70, 30);
    version = new JComboBox<>();
    version.setName(VERSIONS_COMBO_NAME);
    version.setVisible(false);
    version.setMaximumSize(versionDimension);
    version.setPreferredSize(versionDimension);
    version.setMinimumSize(versionDimension);

    version.setRenderer(new BasicComboBoxRenderer() {
      public Component getListCellRendererComponent(JList list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof CorrelationTemplate) {
          setText(((CorrelationTemplate) value).getVersion());
        }
        return this;
      }
    });

    version.addActionListener(e -> {
      if (e.getSource() instanceof JComboBox) {
        CorrelationTemplate selectedTemplate = version.getItemAt(version.getSelectedIndex());
        if (selectedTemplate != null) {
          updateTemplateDisplay(selectedTemplate);
        }
      }
    });
  }

  private void updateTemplateDisplay(CorrelationTemplate selectedTemplate) {
    templateID.setText(StringUtils.capitalize(selectedTemplate.getId()));
    templateID.repaint();

    templateInfoPane.validate();
    templateInfoPane.setText(getDescriptionHTML(selectedTemplate));
    templateInfoPane.setCaretPosition(0);
    templateInfoPane.repaint();
    cacheImage(selectedTemplate);
    toggleVersionButtons(selectedTemplate.isInstalled());
  }

  private void toggleVersionButtons(boolean installed) {
    loadTemplate.setVisible(true);
    loadTemplate.setEnabled(installed);
    loadTemplate
        .setToolTipText(!installed ? "You need to install this version before loading it" : "");
    installTemplateButton.setVisible(true);
    installTemplateButton
        .setText(installed ? StringUtils.capitalize(UNINSTALL) : StringUtils.capitalize(INSTALL));
    installTemplateButton.setActionCommand(installed ? UNINSTALL : INSTALL);
  }

  private void updateTemplatesList() {
    List<CorrelationTemplate> rawTemplates = new ArrayList<>();
    repositoriesRegistryHandler.getCorrelationRepositories().forEach(repository -> rawTemplates
        .addAll(repositoriesRegistryHandler
            .getCorrelationTemplatesByRepositoryName(repository.getName())));
    DefaultListModel<TemplateItem> unifiedTemplates = unifyTemplates(rawTemplates);

    installedModel = new DefaultListModel<>();
    availableModel = new DefaultListModel<>();

    for (int i = 0; i < unifiedTemplates.getSize(); i++) {
      if (unifiedTemplates.get(i).hasInstalledVersion()) {
        installedModel.addElement(unifiedTemplates.get(i));
      } else {
        availableModel.addElement(unifiedTemplates.get(i));
      }
    }

    installedTemplates.setModel(installedModel);
    availableTemplates.setModel(availableModel);
  }

  private DefaultListModel<TemplateItem> unifyTemplates(
      List<CorrelationTemplate> relatedTemplates) {
    Map<String, TemplateItem> groupedTemplates = new HashMap<>();
    relatedTemplates.forEach(template -> {
      String templateKey = template.getId() + " (" + template.getRepositoryId() + ")";
      if (!groupedTemplates.containsKey(templateKey)) {
        groupedTemplates.put(templateKey, new TemplateItem(templateKey, template.isInstalled()));
      }
      if (template.isInstalled()) {
        groupedTemplates.get(templateKey).setHasInstalled(template.isInstalled());
      }
      groupedTemplates.get(templateKey).addTemplate(template);
    });

    DefaultListModel<TemplateItem> groupedTemplatesModel = new DefaultListModel<>();
    for (String templateName : groupedTemplates.keySet()) {
      groupedTemplatesModel.addElement(groupedTemplates.get(templateName));
    }

    return groupedTemplatesModel;
  }

  private void cacheImage(CorrelationTemplate template) {
    if (template.getSnapshot() != null) {
      Dictionary cache = (Dictionary) templateInfoPane.getDocument()
          .getProperty(IMAGE_CACHE_PROPERTY_NAME);
      if (cache == null) {
        cache = new Hashtable<>();
        templateInfoPane.getDocument().putProperty(IMAGE_CACHE_PROPERTY_NAME, cache);
      }

      BufferedImage image = template.getSnapshot();
      if (image != null) {
        cache.put(template.getSnapshotPath(), image);
      }
    }
  }

  private JButton buildButton(String name, String text, String action) {
    JButton button = SwingUtils.createComponent(name, new JButton(text));
    button.setActionCommand(action);
    button.addActionListener(this);
    button.setMaximumSize(FIELDS_DIMENSION);

    return button;
  }

  private JScrollPane prepareTemplatesScrollList(JList<TemplateItem> template) {
    JScrollPane scroll = new JScrollPane();
    scroll.setViewportView(template);
    scroll.createVerticalScrollBar();
    scroll.createHorizontalScrollBar();
    return scroll;
  }

  private String getDescriptionHTML(CorrelationTemplate template) {
    String txt = "";

    txt +=
        "<p> <b> Repository </b>: " + StringUtils.capitalize(template.getRepositoryId()) + ". </p>";

    if (template.isInstalled()) {
      txt += "<p> <b>This version is installed.</b> </p>";
    }

    if (!template.getDescription().isEmpty()) {
      txt += "<p> <b> Description: </b> </p> <p>" + template.getDescription() + "</p>";
    }

    if (template.getDependencies() != null && !template.getDependencies().isEmpty()) {
      txt += "<p> <b> Dependencies: </b> </p>";
      txt += "<pre> [" + template.getDependencies().stream()
          .map(d -> d.getName() + ">=" + d.getVersion()).collect(Collectors.joining(","))
          + "]</pre>";
    }

    if (template.getSnapshot() != null) {
      txt += "<p> <b> Screenshot: </b> </p>";
      txt += "<p> <img src='file:" + template.getSnapshotPath() + "'/></p>";
    }

    return txt;
  }

  private JPanel prepareTemplateInfoPanel() {
    int headerPreferredHeight = FIELDS_DIMENSION.height;
    Dimension templateIdDimension = new Dimension(250, headerPreferredHeight);
    templateID = SwingUtils.createComponent(TEMPLATE_ID_LABEL, new JLabel(""));
    templateID.setMaximumSize(templateIdDimension);
    templateID.setPreferredSize(templateIdDimension);
    templateID.setMinimumSize(templateIdDimension);
    templateID
        .setFont(new Font(templateID.getFont().getFontName(), Font.BOLD, headerPreferredHeight));

    int templateInfoPreferredWidth = 390;
    Dimension headerPreferredSize = new Dimension(templateInfoPreferredWidth,
        headerPreferredHeight);
    JPanel versionsPanel = prepareGluedPanelWithTwoComponents(versionLabel, version);

    JPanel header = prepareGluedPanelWithTwoComponents(templateID, versionsPanel);
    header.setMinimumSize(headerPreferredSize);
    header.setPreferredSize(headerPreferredSize);

    templateInfoPane = new JTextPane();
    templateInfoPane.setEditable(false);
    templateInfoPane.setContentType("text/html");
    templateInfoPane.setName("displayInfoPanel");
    templateInfoPane.setPreferredSize(new Dimension(templateInfoPreferredWidth, 470));
    templateInfoPane.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && Desktop.isDesktopSupported()) {
        try {
          Desktop.getDesktop().browse(e.getURL().toURI());
        } catch (IOException | URISyntaxException ex) {
          LOG.error("There was an issue trying to open the url {}", e.getURL(), ex);
        }
      }
    });

    JPanel buttonsPanel = prepareButtonsPanel(headerPreferredSize);
    JPanel versionTemplateDisplayPanel = new JPanel();
    versionTemplateDisplayPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    GroupLayout layout = new GroupLayout(versionTemplateDisplayPanel);
    versionTemplateDisplayPanel.setLayout(layout);

    layout.setHorizontalGroup(layout.createParallelGroup(LEADING)
        .addComponent(header)
        .addComponent(templateInfoPane)
        .addComponent(buttonsPanel)
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addComponent(header)
        .addComponent(templateInfoPane)
        .addComponent(buttonsPanel)
    );

    return versionTemplateDisplayPanel;
  }

  private JPanel prepareButtonsPanel(Dimension templateHeaderDimension) {
    installTemplateButton = buildButton("installTemplate", "Install", INSTALL);
    installTemplateButton.setMaximumSize(FIELDS_DIMENSION);
    loadTemplate = buildButton("loadButton", "Load", LOAD);

    JPanel buttonsPanel = prepareGluedPanelWithTwoComponents(installTemplateButton, loadTemplate);
    buttonsPanel.setMinimumSize(templateHeaderDimension);
    buttonsPanel.setPreferredSize(templateHeaderDimension);
    return buttonsPanel;
  }

  private JPanel prepareGluedPanelWithTwoComponents(JComponent leftComponent,
      JComponent rightComponent) {
    JPanel gluedPanel = new JPanel();
    gluedPanel.setLayout(new BoxLayout(gluedPanel, BoxLayout.LINE_AXIS));

    gluedPanel.add(leftComponent);
    gluedPanel.add(Box.createHorizontalGlue());
    gluedPanel.add(rightComponent);

    return gluedPanel;
  }

  private void prepareSearchField() {
    templatesSearchField = SwingUtils.createComponent("searchField", new JTextField());
    templatesSearchField.setMinimumSize(FIELDS_DIMENSION);
    templatesSearchField.setPreferredSize(FIELDS_DIMENSION);
    templatesSearchField.setMaximumSize(FIELDS_DIMENSION);
    templatesSearchField.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        filterTemplates(containsText(templatesSearchField.getText()));
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        filterTemplates(containsText(templatesSearchField.getText()));
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        filterTemplates(containsText(templatesSearchField.getText()));
      }
    });
  }

  private Predicate<TemplateItem> containsText(String text) {
    return item -> item.getId().toLowerCase().contains(text.toLowerCase());
  }

  private void loadTemplate() {
    CorrelationTemplate selectedTemplate = version.getItemAt(version.getSelectedIndex());
    if (selectedTemplate != null) {
      try {
        templatesRegistryHandler
            .onLoadTemplate(selectedTemplate.getRepositoryId(), selectedTemplate.getId(),
                selectedTemplate.getVersion());
        lastTemplateHandler.accept(selectedTemplate);
        this.dispose();
      } catch (IOException e) {
        JOptionPane
            .showMessageDialog(null, "Error while trying to load " + selectedTemplate.getId(),
                "Load error", JOptionPane.ERROR_MESSAGE);
        LOG.error("Error while trying to load json file {}", selectedTemplate.getId(), e);
      }
    } else {
      JOptionPane
          .showMessageDialog(getContentPane(), "A template must be selected", "Load warning",
              JOptionPane.WARNING_MESSAGE);
    }
  }

  private void filterTemplates(Predicate<TemplateItem> filterCondition) {
    filterList(filterCondition, installedTemplates, installedModel);
    filterList(filterCondition, availableTemplates, availableModel);
  }

  private void filterList(Predicate<TemplateItem> filterCondition,
      JList<TemplateItem> templatesList,
      DefaultListModel<TemplateItem> model) {
    DefaultListModel<TemplateItem> filteredTemplates = new DefaultListModel<>();
    for (int i = 0; i < model.getSize(); i++) {
      TemplateItem template = model.get(i);

      if (filterCondition.test(template)) {
        filteredTemplates.addElement(model.get(i));
      }
    }
    updateJListModel(templatesList, filteredTemplates);
  }

  private void updateJListModel(JList<TemplateItem> templatesList,
      DefaultListModel<TemplateItem> filteredTemplates) {
    templatesList.setModel(filteredTemplates);
    templatesList.repaint();
  }

  public void showFrame() {
    /*
     * For the cases the user modifies the templates
     * after closing this frame, and open it again.
     * */
    if (installedTemplates != null && availableTemplates != null) {
      updateTemplatesList();
      clearTemplateInfo();
    }
    setVisible(true);
    refreshRepositoriesAndTemplates();
  }

  private void clearTemplateInfo() {
    templateInfoPane.setText("");
    version.removeAllItems();
    version.setVisible(false);
    versionLabel.setVisible(false);
    templateID.setText("");
    installTemplateButton.setVisible(false);
    loadTemplate.setVisible(false);
  }

  @VisibleForTesting
  public String getDisplayText() {
    return templateInfoPane.getText();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();
    switch (action) {
      case CONFIG:
        configureRepository();
        break;
      case LOAD:
        loadTemplate();
        break;
      case INSTALL:
        installTemplate();
        break;
      case UNINSTALL:
        uninstallTemplates();
        break;
      case REFRESH:
        refreshRepositoriesAndTemplates();
        break;
      default:
        LOG.warn("Unsupported action {}", action);
        JMeterUtils.reportErrorToUser("Unsupported action " + action, "Managing Templates");
    }
  }

  private void refreshRepositoriesAndTemplates() {
    if (configFrame == null) {
      initializeConfigFrame();
    }
    JDialog dialog = new JDialog(this, "Loading");
    JProgressBar progressBar = new JProgressBar(0, 100);
    setupDialogAProgressBar(dialog, progressBar);
    // verification done in order to satisfy test coverage
    refreshTask = refreshTask == null ? buildRefreshRepositoriesWorker() : refreshTask;
    refreshTask.addPropertyChangeListener(evt -> {
      if (evt.getPropertyName().equals("progress")) {
        progressBar.setValue(refreshTask.getProgress());
      } else if (evt.getPropertyName().equals("state") && evt.getNewValue()
          .equals(StateValue.DONE)) {
        updateTemplatesList();
        progressBar.setValue(100);
        dialog.dispose();
        refreshTask = null;
      }
    });
    dialog.setVisible(true);
    refreshTask.execute();
  }

  public void setupDialogAProgressBar(JDialog dialog, JProgressBar progressBar) {
    dialog.setName("loadingDialog");
    dialog.setSize(250, 100);
    progressBar.setName("loadingProgressBar");
    progressBar.setValue(0);
    progressBar.setStringPainted(true);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel("Updating templates and repositories"), BorderLayout.NORTH);
    panel.add(progressBar, BorderLayout.CENTER);
    dialog.add(panel);
    dialog.setLocationRelativeTo(this);
    dialog.setAlwaysOnTop(true);
  }

  private SwingWorker<Object, Object> buildRefreshRepositoriesWorker() {
    return new SwingWorker<Object, Object>() {
      @Override
      protected Object doInBackground() {
        repositoriesRegistryHandler
            .refreshRepositories(repositoriesRegistryHandler.getConfigurationRoute(),
                this::setProgress);
        return null;
      }
    };
  }

  private void configureRepository() {
    if (configFrame == null) {
      initializeConfigFrame();
    }
    configFrame.setAlwaysOnTop(true);
    configFrame.showFrame();
  }

  private void initializeConfigFrame() {
    configFrame = new CorrelationTemplatesRepositoryConfigFrame(
        repositoriesRegistryHandler);
    configFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        updateTemplatesList();
      }

      @Override
      public void windowLostFocus(WindowEvent e) {
        updateTemplatesList();
      }
    });
  }

  private void installTemplate() {
    if (version.getSelectedItem() == null) {
      displayMessageDialog("There must be a Version selected", INSTALL_TEMPLATE_WARNING_TITLE,
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    CorrelationTemplate selectedTemplate = version.getItemAt(version.getSelectedIndex());
    if (selectedTemplate == null) {
      displayMessageDialog("There must be a Template selected", INSTALL_TEMPLATE_WARNING_TITLE,
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    if (isAnotherVersionInstalled() && !acceptsOverwritingInstalledVersion()) {
      return;
    }

    List<File> conflictingInstalledDependencies = repositoriesRegistryHandler
        .getConflictingInstalledDependencies(selectedTemplate.getDependencies());

    if (!conflictingInstalledDependencies.isEmpty() && !acceptsDeletingInstalledDependencies(
        conflictingInstalledDependencies)) {
      return;
    }

    try {
      repositoriesRegistryHandler.deleteConflicts(conflictingInstalledDependencies);
      repositoriesRegistryHandler.downloadDependencies(selectedTemplate.getDependencies());
      repositoriesRegistryHandler
          .installTemplate(selectedTemplate.getRepositoryId(), selectedTemplate.getId(),
              selectedTemplate.getVersion());
    } catch (ConfigurationException | IOException e) {
      JMeterUtils.reportErrorToUser(
          "There was an error trying to install the repository " + selectedTemplate.getId()
              + ", please check the logs for more info.");
      return;
    }
    updateInstalledTemplates(INSTALL);
    updateTemplateDisplay(selectedTemplate);

    if (selectedTemplate.getDependencies().isEmpty()) {
      displayMessageDialog("The template was successfully installed",
          INSTALL_TEMPLATE_WARNING_TITLE,
          JOptionPane.INFORMATION_MESSAGE);
    } else if (acceptsResetAfterInstalling()) {
      repositoriesRegistryHandler.resetJMeter();
    }
  }

  private void displayMessageDialog(String message, String title, int messageDialogType) {
    JOptionPane.showMessageDialog(getContentPane(), message, title, messageDialogType);
  }

  private boolean isAnotherVersionInstalled() {
    CorrelationTemplate selectedTemplate = version.getItemAt(version.getSelectedIndex());
    ComboBoxModel<CorrelationTemplate> versionModel = version.getModel();
    for (int i = 0; i < versionModel.getSize(); i++) {
      CorrelationTemplate template = versionModel.getElementAt(i);
      if (!template.getVersion().equals(selectedTemplate.getVersion()) && template.isInstalled()) {
        return true;
      }
    }
    return false;
  }

  private boolean acceptsOverwritingInstalledVersion() {
    return JOptionPane.showConfirmDialog(getContentPane(),
        "There is another version of this Template installed. Want to overwrite it?",
        "Install Template's version ", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }

  private boolean acceptsDeletingInstalledDependencies(
      List<File> conflictingInstalledDependencies) {
    return JOptionPane.showConfirmDialog(getContentPane(),
        "The following dependencies potentially interferes with the dependencies that we will "
            + "download: "
            + conflictingInstalledDependencies.stream().map(
            File::getName).collect(Collectors.joining(","))
            + ". If you continue, those will be deleted. Do you want to continue?",
        "Install Template's version ", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }

  private boolean acceptsResetAfterInstalling() {
    return JOptionPane.showConfirmDialog(getContentPane(),
        "The template was successfully installed. Since some dependencies were installed, JMeter "
            + "needs to restart. Want to do it now?",
        "Install Template's version ", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }

  private void updateInstalledTemplates(String triggerAction) {
    if (triggerAction.equals(UNINSTALL)) {
      int selectedTemplateIndex = installedTemplates.getSelectedIndex();
      version.getItemAt(version.getSelectedIndex()).setInstalled(false);
      TemplateItem template = installedModel.getElementAt(selectedTemplateIndex);
      template.setHasInstalled(false);
      availableModel.addElement(installedModel.remove(selectedTemplateIndex));
      //The 1st tab its for installed and the 2nd its for available templates
      templatesTabbedPane.setSelectedIndex(1);
    } else {
      int selectedIndex = version.getSelectedIndex();

      ComboBoxModel<CorrelationTemplate> model = version.getModel();
      for (int i = 0; i < model.getSize(); i++) {
        model.getElementAt(i).setInstalled(false);
      }
      model.getElementAt(selectedIndex).setInstalled(true);
      version.setModel(model);
      version.setSelectedItem(selectedIndex);

      selectedIndex = availableTemplates.getSelectedIndex();
      //This update only applies for Templates that didn't had an installed version
      if (selectedIndex != -1 && !availableModel.getElementAt(selectedIndex)
          .hasInstalledVersion()) {
        TemplateItem template = availableModel.getElementAt(selectedIndex);
        template.setHasInstalled(true);
        installedModel.addElement(availableModel.remove(selectedIndex));
        templatesTabbedPane.setSelectedIndex(0);
      }
    }
    updateJListModel(installedTemplates, installedModel);
    installedTemplates.setSelectedIndex(installedModel.size() - 1);

    updateJListModel(availableTemplates, availableModel);
    availableTemplates.setSelectedIndex(availableModel.size() - 1);
  }

  private void uninstallTemplates() {
    CorrelationTemplate selectedTemplate = version.getItemAt(version.getSelectedIndex());

    if (selectedTemplate == null) {
      displayMessageDialog("No template selected", INSTALL_TEMPLATE_WARNING_TITLE,
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    try {
      repositoriesRegistryHandler
          .uninstallTemplate(selectedTemplate.getRepositoryId(), selectedTemplate.getId(),
              selectedTemplate.getVersion());

    } catch (ConfigurationException e) {
      JMeterUtils.reportErrorToUser(
          "There was an error trying to uninstall the repository " + selectedTemplate.getId()
              + ". Please check the logs.", "Installing template");
      return;
    }

    updateInstalledTemplates(UNINSTALL);
    updateTemplateDisplay(selectedTemplate);

    displayMessageDialog("The template was successfully uninstalled",
        INSTALL_TEMPLATE_WARNING_TITLE, JOptionPane.INFORMATION_MESSAGE);
  }

  @VisibleForTesting
  public String getDisplayedText() {
    return templateInfoPane.getText();
  }

  @VisibleForTesting
  public void setConfigFrame(CorrelationTemplatesRepositoryConfigFrame configFrame) {
    this.configFrame = configFrame;
  }

  @VisibleForTesting
  public void setRefreshTask(SwingWorker<Object, Object> refreshTask) {
    this.refreshTask = refreshTask;
  }
}
