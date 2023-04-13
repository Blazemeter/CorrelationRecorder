package com.blazemeter.jmeter.correlation.gui.analysis;

import com.blazemeter.jmeter.commons.SwingUtils;
import com.blazemeter.jmeter.correlation.core.analysis.Analysis;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.gui.automatic.WizardStepPanel;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
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
  private JTable correlationRulesTable;
  private JEditorPane informationPane;
  private JTextField traceFilePath;
  private BiConsumer<List<TemplateVersion>, String> startNonCorrelatedAnalysis;
  private Supplier<List<TemplateVersion>> templateVersionSupplier;
  private Map<String, List<TemplateVersion>> correlationTemplates = new HashMap<>();

  public CorrelationTemplatesSelectionPanel(Supplier<List<TemplateVersion>> versionsSupplier) {
    super();
    this.templateVersionSupplier = versionsSupplier;
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
    splitPane.setResizeWeight(0.4);
    splitPane.setName("splitPane");
    splitPane.setDividerLocation(0.4);
    splitPane.setBorder(BorderFactory.createEmptyBorder());
    splitPane.setDividerLocation(300);

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
    correlationRulesTable = new JTable();
    correlationRulesTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
    correlationRulesTable.setModel(new DefaultTableModel(
        new Object[][] {},
        new String[] {"Select", "Name", "Version"}) {

      Class[] types = new Class[] {
          java.lang.Boolean.class, java.lang.String.class, java.lang.String.class};

      boolean[] canEdit = new boolean[] {true, false, true};

      public Class getColumnClass(int columnIndex) {
        return types[columnIndex];
      }

      public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit[columnIndex];
      }
    });
    correlationRulesTable.setFillsViewportHeight(true);
    correlationRulesTable.getSelectionModel()
        .addListSelectionListener(displaySelectedTemplateInformation());
    TableColumn versionColumn = correlationRulesTable.getColumnModel().getColumn(2);
    versionColumn.setCellEditor(new VersionCellEditor());
    JScrollPane correlationRulesTableScrollPane = new JScrollPane(correlationRulesTable);

    GroupLayout layout = new GroupLayout(correlationRulesSelectionPanel);
    correlationRulesSelectionPanel.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(correlationRulesTableScrollPane, GroupLayout.DEFAULT_SIZE, 300,
                        Short.MAX_VALUE))
                .addContainerGap()));

    layout.setVerticalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(correlationRulesTableScrollPane, GroupLayout.DEFAULT_SIZE, 250,
                    Short.MAX_VALUE)
                .addContainerGap())
    );

    return correlationRulesSelectionPanel;
  }

  private class VersionCellEditor extends DefaultCellEditor {

    private JComboBox<String> comboBox;
    private String selectedVersion;
    private String selectedTemplate;

    private VersionCellEditor() {
      super(new JComboBox<>());
      comboBox = (JComboBox<String>) getComponent();
      comboBox.addActionListener(e -> {
        selectedVersion = (String) comboBox.getSelectedItem();
        correlationTemplates.get(selectedTemplate).stream()
            .filter(templateVersion -> templateVersion.getVersion().equals(selectedVersion))
            .findFirst()
            .ifPresent(CorrelationTemplatesSelectionPanel.this::onTemplateVersionFocus);
      });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                 int row, int column) {
      selectedTemplate = (String) table.getValueAt(row, 1);
      comboBox.removeAllItems();
      correlationTemplates.get(selectedTemplate)
          .forEach(templateVersion -> comboBox.addItem(templateVersion.getVersion()));
      return comboBox;
    }

    @Override
    public Object getCellEditorValue() {
      return selectedVersion;
    }
  }

  private ListSelectionListener displaySelectedTemplateInformation() {
    return e -> {
      TemplateVersion selectedVersion = getSelectedTemplateVersion();
      if (selectedVersion == null) {
        return;
      }

      onTemplateVersionFocus(selectedVersion);
    };
  }

  private TemplateVersion getSelectedTemplateVersion() {
    int selectedRow = correlationRulesTable.getSelectedRow();
    if (selectedRow == -1) {
      return null;
    }

    return getSelectedVersionAt(selectedRow);
  }

  private TemplateVersion getSelectedVersionAt(int row) {
    List<TemplateVersion> templateVersions = correlationTemplates
        .get(correlationRulesTable.getValueAt(row, 1));
    if (templateVersions == null) {
      return null;
    }

    String version = (String) correlationRulesTable.getValueAt(row, 2);
    TemplateVersion selectedVersion = templateVersions.get(templateVersions.size() - 1);
    for (TemplateVersion templateVersion : templateVersions) {
      if (version.equals(templateVersion.getVersion())) {
        selectedVersion = templateVersion;
        break;
      }
    }

    return selectedVersion;
  }

  private void onTemplateVersionFocus(TemplateVersion focusedVersion) {
    if (focusedVersion == null) {
      LOG.warn("No TemplateVersion selected");
      return;
    }

    if (informationPane == null) {
      return;
    }

    informationPane.setText(new StringBuilder()
        .append("<html><body><h1>").append(focusedVersion.getId()).append("(")
        .append(focusedVersion.getRepositoryId()).append(")").append("</h1>")
        .append("<p>Correlation Template Name: <b>")
        .append(focusedVersion.getDescription()).append("</b></p>")
        .append("<p>Correlation Template Version: <b>")
        .append(focusedVersion.getVersion()).append("</b></p>").toString());
  }

  private JPanel buildCorrelationRulesInformationPanel() {
    JPanel correlationRulesInformationPanel = new JPanel();
    correlationRulesInformationPanel.setBorder(BorderFactory.createEmptyBorder());
    correlationRulesInformationPanel.setName("templateInformationPanel");
    informationPane = new JEditorPane();
    informationPane.setEditable(false);
    informationPane.setContentType("text/html");
    informationPane.setText(
        "<html><body><h1>Correlation Template Information</h1><p>"
            + "Correlation Template Name: <b>Correlation Template 1</b></p><p>"
            + "Correlation Template Version: <b>1.0</b></p><p>Correlation Template Description: "
            + "<b>Correlation Template 1 Description</b></p></body></html>");
    informationPane.setPreferredSize(new Dimension(300, 250));
    //informationPane.setBorder(BorderFactory.createEtchedBorder());
    // Add an inside border to the information pane

    informationPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

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
    SwingUtils.ButtonBuilder builder = new SwingUtils.ButtonBuilder()
        .withActionListener(this);

    JPanel traceFilePanel = new JPanel(new FlowLayout());
    traceFilePanel.setBorder(BorderFactory.createTitledBorder("Select the .jtl file to use"));
    traceFilePath = new JTextField();
    traceFilePanel.add(traceFilePath);
    traceFilePanel.add(
        builder.withAction(BROWSE).withText("Browse").withName("templateBrowseButton").build());
    JButton cancelButton =
        builder.withAction(CANCEL).withText("Cancel").withName("templateCancelButton").build();
    JButton continueButton =
        builder.withAction(CONTINUE).withText("Continue").withName("templateContinueButton")
            .build();

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
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(
            new FileNameExtensionFilter("JTL Files", "jtl"));
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();
          traceFilePath.setText(file.getAbsolutePath());
        }
        break;
      case RELOAD:
        this.reloadCorrelationTemplates();
        break;
      default:
        break;
    }
  }

  private void validateAndContinue() {
    if (getSelectedTemplateVersions().isEmpty()) {
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
    this.startNonCorrelatedAnalysis.accept(getSelectedTemplateVersions(), traceFilePath.getText());
  }

  public void setStartNonCorrelatedAnalysis(BiConsumer<List<TemplateVersion>,
      String> nonCorrelatedAnalysis) {
    this.startNonCorrelatedAnalysis = nonCorrelatedAnalysis;
  }

  public void reloadCorrelationTemplates() {
    List<TemplateVersion> templateVersions = templateVersionSupplier.get();
    correlationTemplates.clear();
    for (TemplateVersion templateVersion : templateVersions) {
      String templateId = templateVersion.getId() + " (" + templateVersion.getRepositoryId() + ")";

      List<TemplateVersion> versions = correlationTemplates
          .computeIfAbsent(templateId, k -> new ArrayList<>());
      versions.add(templateVersion);
    }

    DefaultTableModel model = (DefaultTableModel) correlationRulesTable.getModel();
    model.setRowCount(0);

    correlationTemplates.forEach((id, versionList) -> {
      String versionString = versionList.stream()
          .map(TemplateVersion::getVersion)
          .collect(Collectors.joining(", "));
      model.addRow(new Object[] {true, id, versionString});
    });
  }

  public void setRecordingTrace() {
    String fileName = JMeterElementUtils.getRecordingResultFileName();
    traceFilePath.setText(fileName != null && !fileName.isEmpty()
        ? fileName : "Enter the path of the .jtl file to use");
  }

  @VisibleForTesting
  public void setRecordingTrace(String fileName) {
    traceFilePath.setText(fileName);
  }

  public void setTemplateVersionSupplier(Supplier<List<TemplateVersion>> templateVersionSupplier) {
    this.templateVersionSupplier = templateVersionSupplier;
  }

  public List<TemplateVersion> getSelectedTemplateVersions() {
    List<TemplateVersion> selectedTemplateVersions = new ArrayList<>();
    for (int i = 0; i < correlationRulesTable.getRowCount(); i++) {
      if (!isTemplateRowSelected(i)) {
        continue;
      }

      TemplateVersion selectedVersion = getSelectedVersionAt(i);
      if (selectedVersion == null) {
        LOG.warn("No version selected for template {}", getTemplateNameAt(i));
        continue;
      }
      selectedTemplateVersions.add(selectedVersion);
    }
    return selectedTemplateVersions;
  }

  private boolean isTemplateRowSelected(int row) {
    return (boolean) correlationRulesTable.getValueAt(row, 0);
  }

  private String getTemplateNameAt(int row) {
    return (String) correlationRulesTable.getValueAt(row, 1);
  }

  public String getTraceFilePath() {
    return traceFilePath.getText();
  }

  public void runNonCorrelatedAnalysis(List<TemplateVersion> templatesToAnalyse,
                                       String recordingTrace) {
    analysis.run(templatesToAnalyse, recordingTrace, false);
  }

  public void runCorrelatedAnalysis(List<TemplateVersion> templatesApply,
                                    String recordingTrace) {
    analysis.run(templatesApply, recordingTrace, true);
  }
}
