package com.blazemeter.jmeter.correlation.gui.analysis;

import com.blazemeter.jmeter.commons.SwingUtils;
import com.blazemeter.jmeter.correlation.core.analysis.Analysis;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesConfiguration;
import com.blazemeter.jmeter.correlation.core.templates.Repository;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.repository.Properties;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import com.blazemeter.jmeter.correlation.gui.automatic.CorrelationWizard;
import com.blazemeter.jmeter.correlation.gui.automatic.WizardStepPanel;
import com.blazemeter.jmeter.correlation.gui.common.TemplateVersionUtils;
import com.blazemeter.jmeter.correlation.gui.templates.UpdateRepositoriesWorker;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationTemplatesSelectionPanel extends WizardStepPanel implements ActionListener {

  private static final long serialVersionUID = 240L;
  private static final Logger LOG
      = LoggerFactory.getLogger(CorrelationTemplatesSelectionPanel.class);
  private static final String BROWSE = "browse";
  private static final String CONTINUE = "continue";
  private static final String CANCEL = "cancel";
  private static final String RELOAD = "reload";
  private final Analysis analysis = new Analysis();
  private TemplatesSelectionTable selectionTable;
  private JEditorPane informationPane;
  private JTextField traceFilePath;
  private BiConsumer<List<Template>, String> startNonCorrelatedAnalysis;
  private JButton continueButton;

  public CorrelationTemplatesSelectionPanel(CorrelationWizard wizard) {
    super(wizard);
    initPanel();
  }

  private void initPanel() {
    JPanel infoAndReloadPanel = buildInfoAndReloadPanel();
    JPanel buildCorrelationTemplatesSelectionPanel = buildSelectionPanel();
    JPanel buttonsPanel = buildButtonsPanel();

    setLayout(new BorderLayout());
    add(infoAndReloadPanel, BorderLayout.NORTH);
    add(buildCorrelationTemplatesSelectionPanel, BorderLayout.CENTER);
    add(buttonsPanel, BorderLayout.SOUTH);
  }

  private JPanel buildSelectionPanel() {
    JPanel correlationRulesSelectionPanel = buildCorrelationRulesSelectionPanel();
    JPanel correlationRulesInformationPanel = buildCorrelationRulesInformationPanel();
    JSplitPane splitPane = new JSplitPane();
    splitPane.setLeftComponent(correlationRulesSelectionPanel);
    splitPane.setRightComponent(correlationRulesInformationPanel);
    splitPane.setResizeWeight(0);
    splitPane.setName("splitPane");
    splitPane.setBorder(BorderFactory.createEmptyBorder());
    splitPane.setDividerLocation(330);

    JPanel selectionPanel = new JPanel();
    selectionPanel.setLayout(new BorderLayout());
    selectionPanel.add(splitPane, BorderLayout.CENTER);
    return selectionPanel;
  }

  private JPanel buildInfoAndReloadPanel() {
    JPanel infoAndReloadPanel = new JPanel();
    infoAndReloadPanel.setName("infoAndReloadPanel");

    JLabel informationLabel = new JLabel();
    informationLabel.setText(
        "Select which Correlation Template and the version that you want to apply:");

    JButton reloadTemplates = new SwingUtils.ButtonBuilder()
        .withActionListener(this)
        .withAction(RELOAD)
        .withName("templateReloadButton")
        .build();
    reloadTemplates.setText("Reload Templates");
    reloadTemplates.setToolTipText("Reload the available Correlation Templates");

    GroupLayout layout = new GroupLayout(infoAndReloadPanel);
    infoAndReloadPanel.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(informationLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                    GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(reloadTemplates)
                .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(informationLabel)
                    .addComponent(reloadTemplates))
                .addContainerGap())
    );
    return infoAndReloadPanel;
  }

  private JPanel buildCorrelationRulesSelectionPanel() {
    JPanel correlationRulesSelectionPanel = new JPanel();
    correlationRulesSelectionPanel.setName("templateSelectionPanel");
    selectionTable = new TemplatesSelectionTable();
    selectionTable.addRowSelectionListener(displaySelectedTemplateInformation());
    selectionTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
    selectionTable.setFillsViewportHeight(true);

    JScrollPane correlationRulesTableScrollPane = new JScrollPane(selectionTable);

    GroupLayout layout = new GroupLayout(correlationRulesSelectionPanel);
    correlationRulesSelectionPanel.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(correlationRulesTableScrollPane, GroupLayout.DEFAULT_SIZE, 400,
                        Short.MAX_VALUE))
                .addContainerGap()));

    layout.setVerticalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(correlationRulesTableScrollPane, GroupLayout.DEFAULT_SIZE, 200,
                    Short.MAX_VALUE)
                .addContainerGap())
    );

    return correlationRulesSelectionPanel;
  }

  private ListSelectionListener displaySelectedTemplateInformation() {
    return e -> onTemplateVersionFocus(selectionTable.getSelectedTemplateVersion());
  }

  private void onTemplateVersionFocus(Template focusedVersion) {
    if (focusedVersion == null || informationPane == null) {
      return;
    }

    TemplateSelectionTableModel model = (TemplateSelectionTableModel) selectionTable.getModel();
    boolean canUse = model.canUseTemplate(focusedVersion);

    informationPane.setText(TemplateVersionUtils
        .getInformationAsHTLM(focusedVersion, false, canUse,
        model.getRepositoryDisplayName(focusedVersion.getRepositoryId())));
    informationPane.setCaretPosition(0); // Scroll to the top
  }

  public void loadPanel() {
    if (this.wizard.getRepositoriesConfiguration().getCorrelationRepositories().isEmpty()) {
      reloadCorrelationTemplates();
    } else {
      this.loadCorrelationTemplates();
    }
  }

  public void loadCorrelationTemplates() {
    Map<String, Repository> repositoryMap = this.wizard.getRepositoriesSupplier().get();
    selectionTable.setRepositories(repositoryMap);
    selectionTable.selectFirstRow();
  }

  private JPanel buildCorrelationRulesInformationPanel() {
    JPanel correlationRulesInformationPanel = new JPanel();
    correlationRulesInformationPanel.setBorder(BorderFactory.createEmptyBorder());
    correlationRulesInformationPanel.setName("templateInformationPanel");
    informationPane = new JEditorPane();
    informationPane.setEditable(false);
    informationPane.setContentType("text/html");
    informationPane.setText(
        "<html><p>Select a Template on the list to see it's information</p></html>");
    informationPane.setPreferredSize(new Dimension(300, 250));
    informationPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    // Don't allow to automatic scroll the area
    informationPane.setCaretPosition(0);
    ((DefaultCaret) informationPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

    JScrollPane scrollPane = new JScrollPane(informationPane);

    GroupLayout layout = new GroupLayout(correlationRulesInformationPanel);
    correlationRulesInformationPanel.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                .addContainerGap())
    );

    layout.setVerticalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                .addContainerGap())
    );

    return correlationRulesInformationPanel;
  }

  private JPanel buildButtonsPanel() {
    JPanel traceFilePanel = new JPanel();
    BoxLayout boxLayout = new BoxLayout(traceFilePanel, BoxLayout.X_AXIS);
    traceFilePanel.setLayout(boxLayout);

    traceFilePanel.setBorder(BorderFactory.createTitledBorder("Select the .jtl file to use"));
    traceFilePath = new JTextField();

    SwingUtils.ButtonBuilder builder = new SwingUtils.ButtonBuilder().withActionListener(this);
    traceFilePanel.add(traceFilePath);
    traceFilePanel.add(
        builder.withAction(BROWSE).withText("Browse").withName("templateBrowseButton").build());
    JButton cancelButton =
        builder.withAction(CANCEL).withText("Cancel").withName("templateCancelButton").build();
    continueButton =
        builder.withAction(CONTINUE).withText("Continue").withName("templateContinueButton")
            .build();
    enableContinue(false);

    JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.add(cancelButton);
    buttonPanel.add(continueButton);

    JPanel returnPanel = new JPanel(new BorderLayout());
    returnPanel.add(traceFilePanel, BorderLayout.NORTH);
    returnPanel.add(buttonPanel, BorderLayout.SOUTH);
    return returnPanel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();
    switch (command) {
      case CANCEL:
        toggleWizardVisibility();
        break;
      case CONTINUE:
        validateAndContinue();
        break;
      case BROWSE:
        browseForJtl();
        break;
      case RELOAD:
        this.reloadCorrelationTemplates();
        break;
      default:
        break;
    }
  }

  private void reloadCorrelationTemplates() {
    UpdateRepositoriesWorker worker = new UpdateRepositoriesWorker() {
      @Override
      protected Boolean doInBackground() {
        return wizard.getRepositoriesConfiguration().getLocalConfig().refreshRepositories("",
            this::setProgress, this::publish);
      }
    };

    worker.addPropertyChangeListener(evt -> {
      if (evt.getPropertyName().equals("state") && evt.getNewValue()
          .equals(SwingWorker.StateValue.DONE)) {
        loadCorrelationTemplates();
      }
    });

    worker.execute();
  }

  private void validateAndContinue() {
    if (selectionTable.notSelectedTemplates()) {
      JOptionPane.showMessageDialog(this, "Please select a template version to continue",
          "No Template Selected", JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (traceFilePath.getText().isEmpty()) {
      JOptionPane.showMessageDialog(this, "Please select a trace file to continue",
          "No Trace File Selected", JOptionPane.ERROR_MESSAGE);
      return;
    }

    onContinue();
  }

  private void onContinue() {
    Map<String, List<TemplateVersion>> repositoryGrouped
        = selectionTable.getSelectedTemplateWithRepositoryMap();

    List<Template> canUseTemplates = new ArrayList<>();
    List<Template> cannotUseTemplates = new ArrayList<>();
    for (Map.Entry<String, List<TemplateVersion>> entry : repositoryGrouped.entrySet()) {
      String repositoryName = entry.getKey();
      List<TemplateVersion> templates = entry.getValue();
      CorrelationTemplatesRepositoriesConfiguration config
          = this.wizard.getRepositoriesConfiguration();
      RepositoryManager repManager = config.getRepositoryManager(repositoryName);

      Map<Template, TemplateProperties> templatesAndProperties
          = repManager.getTemplatesAndProperties(templates);

      if (templatesAndProperties == null || templatesAndProperties.isEmpty()) {

        templatesAndProperties = config
            .getCorrelationTemplatesAndPropertiesByRepositoryName(repositoryName, true);
      }

      for (Map.Entry<Template, TemplateProperties> templateEntry
          : templatesAndProperties.entrySet()) {
        TemplateProperties value = templateEntry.getValue();
        Properties properties = new Properties();
        properties.putAll(value);

        if (properties.canUse()) {
          canUseTemplates.add(templateEntry.getKey());
        } else {
          cannotUseTemplates.add(templateEntry.getKey());
        }
      }
    }

    if (!cannotUseTemplates.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "You don't have permission to use the following templates:\n"
              + cannotUseTemplates.stream()
              .map(RepositoryUtils::getTemplateInfo)
              .collect(Collectors.joining("\n")),
          "Cannot use templates", JOptionPane.ERROR_MESSAGE);
    }

    this.startNonCorrelatedAnalysis.accept(canUseTemplates, traceFilePath.getText());
  }

  private void browseForJtl() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setFileFilter(new FileNameExtensionFilter("JTL Files", "jtl"));
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      traceFilePath.setText(file.getAbsolutePath());
      enableContinue(true);
    }
  }

  private void enableContinue(boolean enable) {
    continueButton.setEnabled(enable);
    continueButton.setToolTipText(enable ? "" : "Analysis is enabled if a recording exists."
        + "\n"
        + "Make a recording or select a .jtl of a recording to be analyzed.");
  }

  public void setStartNonCorrelatedAnalysis(BiConsumer<List<Template>,
      String> nonCorrelatedAnalysis) {
    this.startNonCorrelatedAnalysis = nonCorrelatedAnalysis;
  }

  public void setRecordingTrace() {
    String fileName = JMeterElementUtils.getRecordingResultFileName();
    boolean fileExist = fileName != null && !fileName.isEmpty();
    traceFilePath.setText(fileExist
        ? fileName : "Enter the path of the .jtl file to use");
    enableContinue(fileExist);
  }

  @VisibleForTesting
  public void setRecordingTrace(String fileName) {
    traceFilePath.setText(fileName);
    enableContinue(false);
  }

  public String getTraceFilePath() {
    return traceFilePath.getText();
  }

  //Reminder: this is the place where the "Analysis by Template" is called.
  public void runNonCorrelatedAnalysis(List<Template> templatesToAnalyse,
                                       String recordingTrace) {
    analysis.run(templatesToAnalyse, recordingTrace, false);
  }

  //Reminder: this is the "Apply Suggestions" for Analysis.
  public void runCorrelatedAnalysis(List<Template> templatesApply,
                                    String recordingTrace) {
    analysis.run(templatesApply, recordingTrace, true);
  }
}
