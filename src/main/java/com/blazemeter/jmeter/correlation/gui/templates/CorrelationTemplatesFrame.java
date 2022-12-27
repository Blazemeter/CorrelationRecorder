package com.blazemeter.jmeter.correlation.gui.templates;

import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.INSTALL;
import static com.blazemeter.jmeter.correlation.core.templates.LocalConfiguration.UNINSTALL;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;

import com.blazemeter.jmeter.correlation.core.templates.ConfigurationException;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.gui.common.StringUtils;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.common.ThemedIcon;
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
import java.util.concurrent.ExecutionException;
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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
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

public class CorrelationTemplatesFrame extends JDialog implements ActionListener {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationTemplatesFrame.class);
  private static final Dimension FIELDS_DIMENSION = new Dimension(100, 30);
  private static final Dimension TEMPLATE_LIST_DIMENSION = new Dimension(200, 300);

  private static final String CONFIG = "config";
  private static final String LOAD = "load";
  private static final String REPLACE = "replace";
  private static final String IMAGE_CACHE_PROPERTY_NAME = "imageCache";
  private static final String VERSIONS_COMBO_NAME = "templateVersions";
  private static final String INSTALL_TEMPLATE_WARNING_TITLE = "Installing template";

  private static final String TEMPLATE_ID_LABEL = "templateIdLabel";
  private static final String REFRESH = "refresh";
  private static final String CONFIRM_REFRESH = "confirmRefresh";
  private final CorrelationTemplatesRegistryHandler templatesRegistryHandler;
  private final CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistryHandler;
  private final Consumer<TemplateVersion> lastTemplateHandler;
  private JTextField templatesSearchField;
  private DefaultListModel<Template> installedModel;
  private DefaultListModel<Template> availableModel;
  private JList<Template> installedTemplates;
  private JList<Template> availableTemplates;
  private JTabbedPane templatesTabbedPane;
  private JComboBox<TemplateVersion> version;
  private JLabel templateID;
  private JLabel versionLabel;
  private JTextPane templateInfoPane;
  private JButton installTemplateButton;
  private JButton loadTemplate;
  private JButton confirmRefreshButton;
  private JButton refreshButton;
  private CorrelationTemplatesRepositoryConfigFrame configFrame;
  private SwingWorker<Boolean, Integer> refreshTask;
  private JProgressBar progressBar;

  public CorrelationTemplatesFrame(CorrelationTemplatesRegistryHandler templatesRegistryHandler,
      CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistryHandler,
      Consumer<TemplateVersion> lastTemplateHandler, JPanel parent) {
    super((JFrame) SwingUtilities.getWindowAncestor(parent), true);
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

    prepareRefreshComponents();

    templatesTabbedPane = new JTabbedPane();
    templatesTabbedPane.setName("templatesTabbedPane");
    templatesTabbedPane.addTab("Installed", installedTemplatesScroll);
    templatesTabbedPane.addTab("Available", availableTemplatesScroll);
    templatesTabbedPane.addChangeListener(e -> {
      if (templatesTabbedPane.getSelectedIndex() == 0
          && installedTemplates.getSelectedIndex() != -1) {
        displaySelectedItem(installedTemplates.getSelectedValue());
      } else if (templatesTabbedPane.getSelectedIndex() == 1
          && availableTemplates.getSelectedIndex() != -1) {
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
                    .addComponent(versionTemplateDisplayPanel, 400, 400, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(confirmRefreshButton)
                        .addGap(75, 75, Short.MAX_VALUE)
                        .addComponent(progressBar, 200, 200, Short.MAX_VALUE))))
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
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createParallelGroup()
                                .addComponent(confirmRefreshButton)
                                .addComponent(progressBar, Alignment.CENTER))))))
    );

    return main;
  }

  private void prepareRefreshComponents() {
    refreshButton = buildButton("refreshButton", "", REFRESH);
    refreshButton.setToolTipText("Refresh repositories and templates");
    refreshButton.setIcon(ThemedIcon.fromResourceName("refresh-button.png"));
    progressBar = SwingUtils.createComponent("loadingProgressBar", new JProgressBar(0, 100));
    confirmRefreshButton = buildButton("confirmRefreshButton", "Refresh",
        CONFIRM_REFRESH);
    setRefreshComponentVisible(false);
  }

  private JList<Template> prepareTemplatesList(String listName) {
    JList<Template> template = SwingUtils.createComponent(listName, new JList<>());
    template.setPreferredSize(new Dimension(100, 500));

    template.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
        Component renderer = super
            .getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (renderer instanceof JLabel && value instanceof Template) {
          Template template = (Template) value;
          JLabel templateLabel = (JLabel) renderer;
          templateLabel.setText(StringUtils.capitalize(template.getId()));
        }
        return renderer;
      }
    });

    template.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        displaySelectedItem(template.getSelectedValue());
      }
    });

    return template;
  }

  private void displaySelectedItem(Template template) {
    if (template != null) {
      DefaultComboBoxModel<TemplateVersion> versionsModel = new DefaultComboBoxModel<>();
      List<TemplateVersion> versions = template.getVersions();
      versions.forEach(versionsModel::addElement);
      version.setVisible(true);
      version.setModel(versionsModel);
      versionLabel.setVisible(true);

      Optional<TemplateVersion> installedTemplate = versions.stream()
          .filter(TemplateVersion::isInstalled)
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
        if (value instanceof TemplateVersion) {
          setText(((TemplateVersion) value).getVersion());
        }
        return this;
      }
    });

    version.addActionListener(e -> {
      if (e.getSource() instanceof JComboBox) {
        TemplateVersion selectedTemplate = version.getItemAt(version.getSelectedIndex());
        if (selectedTemplate != null) {
          updateTemplateDisplay(selectedTemplate);
        }
      }
    });
  }

  private void updateTemplateDisplay(TemplateVersion selectedTemplate) {
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
    List<TemplateVersion> rawTemplates = new ArrayList<>();
    repositoriesRegistryHandler.getCorrelationRepositories().forEach(repository -> rawTemplates
        .addAll(repositoriesRegistryHandler
            .getCorrelationTemplatesByRepositoryName(repository.getName())));
    DefaultListModel<Template> unifiedTemplates = unifyTemplates(rawTemplates);

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

  private DefaultListModel<Template> unifyTemplates(
      List<TemplateVersion> relatedTemplates) {
    Map<String, Template> groupedTemplates = new HashMap<>();
    relatedTemplates.forEach(template -> {
      String templateKey = template.getId() + " (" + template.getRepositoryId() + ")";
      if (!groupedTemplates.containsKey(templateKey)) {
        groupedTemplates.put(templateKey, new Template(templateKey, template.isInstalled()));
      }
      if (template.isInstalled()) {
        groupedTemplates.get(templateKey).setHasInstalled(template.isInstalled());
      }
      groupedTemplates.get(templateKey).addTemplate(template);
    });

    DefaultListModel<Template> groupedTemplatesModel = new DefaultListModel<>();
    for (String templateName : groupedTemplates.keySet()) {
      groupedTemplatesModel.addElement(groupedTemplates.get(templateName));
    }

    return groupedTemplatesModel;
  }

  private void cacheImage(TemplateVersion template) {
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

  private JScrollPane prepareTemplatesScrollList(JList<Template> template) {
    JScrollPane scroll = new JScrollPane();
    scroll.setViewportView(template);
    scroll.createVerticalScrollBar();
    scroll.createHorizontalScrollBar();
    return scroll;
  }

  private void addFieldContent(StringBuilder content, String fieldText, String fieldHeader) {
    if (fieldText != null && !fieldText.isEmpty()) {
      content.append(makeHeader(fieldHeader))
          .append(makeParagraph(fieldText));
    }
  }

  private String getDescriptionHTML(TemplateVersion template) {
    StringBuilder content = new StringBuilder();

    content.append(makeHeader("Repository"))
        .append(makeParagraph(StringUtils.capitalize(template.getRepositoryId())));

    if (template.isInstalled()) {
      content.append(makeHeader("This version is installed"));
    }

    addFieldContent(content, template.getDescription(),
        "Description");

    addFieldContent(content, template.getAuthor(), "Author");

    addFieldContent(content, template.getUrl(), "Url/Email");

    addFieldContent(content, template.getChanges(), "Changes");

    if (template.getDependencies() != null && !template.getDependencies().isEmpty()) {
      content.append(makeHeader("Dependencies"))
          .append("<pre> [")
          .append(template.getDependencies().stream()
              .map(d -> d.getName() + ">=" + d.getVersion()).collect(Collectors.joining(",")))
          .append("]</pre>");
    }

    if (template.getSnapshot() != null) {
      content.append(makeHeader("Screenshot"))
          .append("<p> <img src='file:")
          .append(template.getSnapshotPath())
          .append("'/></p>");
    }

    return content.toString();
  }

  private String makeHeader(String text) {
    return makeParagraph(makeBold(text));
  }

  private String makeParagraph(String text) {
    return "<p>" + text + "</p>";
  }

  private String makeBold(String text) {
    return "<b>" + text + "</b>";
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

    templateInfoPane = SwingUtils.createComponent("displayInfoPanel", new JTextPane());
    templateInfoPane.setEditable(false);
    templateInfoPane.setContentType("text/html");
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

  private Predicate<Template> containsText(String text) {
    return item -> item.getId().toLowerCase().contains(text.toLowerCase());
  }

  private void loadTemplate() {
    TemplateVersion selectedTemplate = version.getItemAt(version.getSelectedIndex());
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

  private void filterTemplates(Predicate<Template> filterCondition) {
    filterList(filterCondition, installedTemplates, installedModel);
    filterList(filterCondition, availableTemplates, availableModel);
  }

  private void filterList(Predicate<Template> filterCondition,
      JList<Template> templatesList,
      DefaultListModel<Template> model) {
    DefaultListModel<Template> filteredTemplates = new DefaultListModel<>();
    for (int i = 0; i < model.getSize(); i++) {
      Template template = model.get(i);

      if (filterCondition.test(template)) {
        filteredTemplates.addElement(model.get(i));
      }
    }
    updateJListModel(templatesList, filteredTemplates);
  }

  private void updateJListModel(JList<Template> templatesList,
      DefaultListModel<Template> filteredTemplates) {
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
      case CONFIRM_REFRESH:
        setRefreshComponentVisible(false);
        updateTemplatesList();
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
    refreshTask = refreshTask == null ? buildRefreshRepositoriesWorker() : refreshTask;
    refreshTask.addPropertyChangeListener(evt -> {
      if (evt.getPropertyName().equals("progress")) {
        progressBar.setValue(refreshTask.getProgress());
      } else if (evt.getPropertyName().equals("state") && evt.getNewValue()
          .equals(StateValue.DONE)) {
        progressBar.setValue(100);
        confirmRefreshButton.setEnabled(true);
        try {
          if (refreshTask.get()) {
            setRefreshComponentVisible(false);
          } else {
            confirmRefreshButton.setEnabled(true);
          }
          refreshTask = null;
        } catch (InterruptedException | ExecutionException e) {
          LOG.error("There was an error trying verify if Templates are up to date", e);
        }
      }
    });
    progressBar.setValue(0);
    setRefreshComponentVisible(true);
    confirmRefreshButton.setEnabled(false);
    refreshTask.execute();
  }

  private void setRefreshComponentVisible(boolean isVisible) {
    progressBar.setVisible(isVisible);
    confirmRefreshButton.setVisible(isVisible);
  }

  private SwingWorker<Boolean, Integer> buildRefreshRepositoriesWorker() {
    return new SwingWorker<Boolean, Integer>() {
      @Override
      protected Boolean doInBackground() {
        return repositoriesRegistryHandler
            .refreshRepositories(repositoriesRegistryHandler.getConfigurationRoute(),
                this::setProgress);
      }
    };
  }

  private void configureRepository() {
    if (configFrame == null) {
      initializeConfigFrame();
    }
    configFrame.showFrame();
  }

  private void initializeConfigFrame() {
    configFrame = new CorrelationTemplatesRepositoryConfigFrame(repositoriesRegistryHandler, this);
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
    TemplateVersion selectedTemplate = version.getItemAt(version.getSelectedIndex());
    if (selectedTemplate == null) {
      displayMessageDialog("There must be a Template selected", INSTALL_TEMPLATE_WARNING_TITLE,
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    boolean anotherVersionInstalled = isAnotherVersionInstalled();
    if (anotherVersionInstalled && !acceptsOverwritingInstalledVersion()) {
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
    updateInstalledTemplates(anotherVersionInstalled ? REPLACE : INSTALL);
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
    TemplateVersion selectedTemplate = version.getItemAt(version.getSelectedIndex());
    ComboBoxModel<TemplateVersion> versionModel = version.getModel();
    for (int i = 0; i < versionModel.getSize(); i++) {
      TemplateVersion template = versionModel.getElementAt(i);
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
    if (triggerAction.equals(REPLACE)) {
      int installedTemplateIndex = installedTemplates.getSelectedIndex();
      int installedVersionIndex = version.getSelectedIndex();

      Template template = installedModel.getElementAt(installedTemplateIndex);
      template.getVersions().forEach(v -> v.setInstalled(false));
      template.getVersions().get(installedVersionIndex).setInstalled(true);
      installedTemplates.setSelectedIndex(installedTemplateIndex);
      return;
    } else if (triggerAction.equals(UNINSTALL)) {
      int selectedTemplateIndex = installedTemplates.getSelectedIndex();
      version.getItemAt(version.getSelectedIndex()).setInstalled(false);
      Template template = installedModel.getElementAt(selectedTemplateIndex);
      template.setHasInstalled(false);
      availableModel.addElement(installedModel.remove(selectedTemplateIndex));
      //The 1st tab its for installed and the 2nd its for available templates
      templatesTabbedPane.setSelectedIndex(1);
    } else {
      int selectedIndex = version.getSelectedIndex();

      ComboBoxModel<TemplateVersion> model = version.getModel();
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
        Template template = availableModel.getElementAt(selectedIndex);
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
    TemplateVersion selectedTemplate = version.getItemAt(version.getSelectedIndex());

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
  public void setConfigFrame(CorrelationTemplatesRepositoryConfigFrame configFrame) {
    this.configFrame = configFrame;
  }

}
