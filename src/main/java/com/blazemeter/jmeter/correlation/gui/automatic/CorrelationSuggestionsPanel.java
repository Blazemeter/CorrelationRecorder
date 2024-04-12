package com.blazemeter.jmeter.correlation.gui.automatic;

import com.blazemeter.jmeter.commons.SwingUtils;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.automatic.JMeterElementUtils;
import com.blazemeter.jmeter.correlation.core.suggestions.SuggestionGenerator;
import com.blazemeter.jmeter.correlation.core.suggestions.context.AnalysisContext;
import com.blazemeter.jmeter.correlation.core.suggestions.context.ComparisonContext;
import com.blazemeter.jmeter.correlation.core.suggestions.method.AnalysisMethod;
import com.blazemeter.jmeter.correlation.core.suggestions.method.ComparisonMethod;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.repository.Properties;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryManager;
import com.blazemeter.jmeter.correlation.core.templates.repository.RepositoryUtils;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.apache.jmeter.gui.util.JMeterToolBar;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the panel that will be shown in the Correlation Tab. It contains the logic to show
 * the suggestions and the buttons to replay the recording and clear the suggestions.
 */
public class CorrelationSuggestionsPanel extends WizardStepPanel implements ActionListener {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationSuggestionsPanel.class);
  private static final String CORRELATE = "correlate";
  private static final String MANUAL_REPLAY = "replay";
  private static final String CLEAR_SUGGESTIONS = "clear";
  private static final String EXPORT_AS_RULES = "export";
  private final String iconSize = JMeterUtils.getPropDefault(
      JMeterToolBar.TOOLBAR_ICON_SIZE, JMeterToolBar.DEFAULT_TOOLBAR_ICON_SIZE);
  private final String toolbarPath = "toolbar/" + iconSize + "/";
  private JTabbedPane tabbedPane;
  private JTable table;
  private JList<String> reportList = new JList<>();
  private Runnable autoCorrelateMethod;
  private Runnable replaySelectionMethod;
  private final boolean isExtraDebuggingEnabled = JMeterUtils.getPropDefault(
      "correlation.debug.extra_debugging", false);

  public CorrelationSuggestionsPanel(CorrelationWizard wizard) {
    super(wizard);
    init();
    setupDefaultAutoCorrelateMethod();
  }

  private void init() {
    JPanel displaySuggestionsPanel = makeCorrelationPanel();
    JPanel displayReportPanel = makeApplyReportPanel();
    tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Suggestions", displaySuggestionsPanel);
    if (isExtraDebuggingEnabled) {
      tabbedPane.addTab("Apply Report", displayReportPanel);
    }

    GroupLayout layout = SwingUtils.createGroupLayout(this);
    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addComponent(tabbedPane));
    layout.setVerticalGroup(layout.createSequentialGroup()
        .addComponent(tabbedPane));
  }

  private JPanel makeCorrelationPanel() {
    SwingUtils.ButtonBuilder builder = new SwingUtils.ButtonBuilder()
        .isEnabled(true)
        .hasText(true)
        .withActionListener(this);

    JButton replayRecording = builder.withAction(MANUAL_REPLAY)
        .withName("replayRecording")
        .withText("Replay Recording")
        .withToolTip("Replay the recording to generate suggestions")
        .build();
    replayRecording.setIcon(JMeterUtils.getImage(toolbarPath + "arrow-right-3.png"));

    JButton clearSuggestions = builder.withAction(CLEAR_SUGGESTIONS)
        .withName("clearSuggestions")
        .withText("Clear Suggestions")
        .withToolTip("Clear the suggestions table")
        .build();
    clearSuggestions.setIcon(JMeterUtils.getImage(toolbarPath + "run-build-clean.png"));

    JPanel debugButtonsPanel = new JPanel();
    GroupLayout debugLayout = SwingUtils.createGroupLayout(debugButtonsPanel);
    debugLayout.setVerticalGroup(debugLayout.createSequentialGroup()
        .addGroup(debugLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(replayRecording)
            .addComponent(clearSuggestions)));

    debugLayout.setHorizontalGroup(debugLayout.createSequentialGroup()
        .addGroup(debugLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(debugLayout.createSequentialGroup()
                .addComponent(replayRecording)
                .addComponent(clearSuggestions))));

    JLabel infoLabel = new JLabel("Select which suggestions you want to apply and "
        + "click on Correlate");
    infoLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
    infoLabel.setVerticalAlignment(Label.CENTER);
    infoLabel.setVerticalTextPosition(Label.CENTER);
    Font font = infoLabel.getFont();
    infoLabel.setFont(font.deriveFont(font.getStyle() | java.awt.Font.BOLD));

    table = new JTable();
    table.setName("suggestionsTable");
    table.setModel(new SuggestionsTableModel());
    table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
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
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    table.addMouseListener(new TextBoxPopupDialog.TextBoxDoubleClick(table));

    int selectionColumn = 0;
    TableColumn tc = table.getColumnModel().getColumn(selectionColumn);
    tc.setHeaderRenderer(new SelectAllHeader(table, selectionColumn));

    JTableHeader header = table.getTableHeader();
    header.setFont(header.getFont().deriveFont(14f));

    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setViewportView(table);
    scrollPane.setPreferredSize(new Dimension(500, 100));
    scrollPane.setBorder(new EtchedBorder());

    JPanel buttonsPanel = new JPanel();
    buttonsPanel.add(builder.withAction(CORRELATE)
        .withName("correlate")
        .withText("Apply")
        .build());

    buttonsPanel.add(builder.withAction(EXPORT_AS_RULES)
        .withName("exportSuggestions")
        .withText("Save correlation rules")
        .withToolTip("Save the correlation rules associated to the suggestions")
        .build());

    JPanel displaySuggestionsPanel = new JPanel();
    BorderLayout layout = new BorderLayout(15, 15);
    displaySuggestionsPanel.setLayout(layout);
    displaySuggestionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    JPanel headerPanel = new JPanel(new BorderLayout(0, 0));
    headerPanel.add(debugButtonsPanel, BorderLayout.NORTH);
    headerPanel.add(infoLabel, BorderLayout.CENTER);
    displaySuggestionsPanel.add(headerPanel, BorderLayout.PAGE_START);
    displaySuggestionsPanel.add(scrollPane, BorderLayout.CENTER);
    displaySuggestionsPanel.add(buttonsPanel, BorderLayout.PAGE_END);

    return displaySuggestionsPanel;
  }

  private JPanel makeApplyReportPanel() {
    JLabel reportFirstLine = new JLabel("The following request were modified as part of the"
        + " automatic correlation process");
    JLabel reportSecondLine = new JLabel("It is recommended to review them before proceeding");
    reportList = new JList<>();
    reportList.setVisibleRowCount(10);
    reportList.setFixedCellWidth(500);
    reportList.setFixedCellHeight(15);
    reportList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    reportList.setLayoutOrientation(JList.VERTICAL);
    reportList.setVisible(true);

    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setViewportView(reportList);

    JPanel displayReportPanel = new JPanel();
    GroupLayout layout = SwingUtils.createGroupLayout(displayReportPanel);
    layout.setVerticalGroup(layout.createSequentialGroup()
        .addComponent(reportFirstLine)
        .addComponent(reportSecondLine)
        .addComponent(scrollPane));

    layout.setHorizontalGroup(layout.createParallelGroup()
        .addComponent(reportFirstLine)
        .addComponent(reportSecondLine)
        .addComponent(scrollPane));
    return displayReportPanel;
  }

  public void showColumn(int column, int width) {
    TableColumnModel tcm = table.getColumnModel();
    TableColumn tc = tcm.getColumn(column);
    tc.setMinWidth(0);
    tc.setMaxWidth(width);
    tc.setPreferredWidth(width);
  }

  @VisibleForTesting
  public void toggleSuggestionItem(int index) {
    SuggestionsTableModel model = (SuggestionsTableModel) table.getModel();
    SuggestionItem suggestionItem = model.suggestionList.get(index);
    suggestionItem.setSelected(!suggestionItem.isSelected());
  }

  public void clearSuggestions() {
    SuggestionsTableModel model = (SuggestionsTableModel) table.getModel();
    model.setRowCount(0);
    model.suggestionList.clear();
    model.fireTableDataChanged();
  }

  public void loadSuggestions(List<CorrelationSuggestion> suggestions) {
    SuggestionsTableModel model = (SuggestionsTableModel) table.getModel();
    model.loadSuggestions(suggestions);
  }

  public List<CorrelationSuggestion> exportSelectedSuggestions() {
    SuggestionsTableModel model = (SuggestionsTableModel) table.getModel();
    return model.getSelectedSuggestions();
  }

  @VisibleForTesting
  public int getSuggestionsCount() {
    SuggestionsTableModel model = (SuggestionsTableModel) table.getModel();
    return model.suggestionList.size();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();
    switch (action) {
      case CORRELATE:
        SwingUtilities.invokeLater(autoCorrelateMethod);
        return;
      case MANUAL_REPLAY:
        SwingUtilities.invokeLater(this::manualReplayAndGenerateSuggestions);
        return;
      case CLEAR_SUGGESTIONS:
        SwingUtilities.invokeLater(this::clearSuggestions);
        return;
      case EXPORT_AS_RULES:
        SwingUtilities.invokeLater(this::exportSuggestions);
        return;
      default:
        LOG.warn("Action {} not supported", action);
    }
  }

  private void manualReplayAndGenerateSuggestions() {
    toggleWizardVisibility();
    replayTestPlan();
  }

  private void setupDefaultAutoCorrelateMethod() {
    autoCorrelateMethod = this::applySuggestions;
  }

  // Reminder: This method is called when the Suggestions came from Comparison methods.
  public void applySuggestions() {
    List<CorrelationSuggestion> suggestions = exportSelectedSuggestions();
    if (suggestions.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "No suggestions selected. Please select at least one suggestion to "
              + "apply", "No suggestions selected",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    AnalysisContext context = new AnalysisContext();
    String recordingTraceFilePath = getRecordingTraceSupplier.get();
    context.setRecordingTraceFilePath(recordingTraceFilePath);
    context.setRecordingTestPlan(JMeterElementUtils.getNormalizedTestPlan());
    context.setRegistry(CorrelationComponentsRegistry.getInstance());

    logStepConsumer.accept("(Save) Before apply suggestions");
    SuggestionGenerator generator
        = SuggestionGenerator.getInstance(new AnalysisMethod(context));
    generator.applySuggestions(suggestions);
    logStepConsumer.accept("(Save) After apply suggestions");
    JMeterElementUtils.refreshJMeter();

    if (isExtraDebuggingEnabled) {
      displayAppliedResults();
    }
  }

  public void setAutoCorrelateMethod(Runnable autoCorrelateMethod) {
    this.autoCorrelateMethod = autoCorrelateMethod;
  }

  public void setReplaySelectionMethod(Runnable replaySelectionMethod) {
    this.replaySelectionMethod = replaySelectionMethod;
  }

  private void exportSuggestions() {
    List<CorrelationSuggestion> suggestions = exportSelectedSuggestions();
    if (suggestions.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "No suggestions selected. Please select at least one suggestion to "
              + "export", "No suggestions selected",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    Map<String, Set<TemplateVersion>> repositoryAndSuggestions = new HashMap<>();
    for (CorrelationSuggestion suggestion : suggestions) {
      Template source = suggestion.getSource();
      if (source != null) { // Source null is automatic, and not null is Template based
        String repositoryId = source.getRepositoryId();
        if (!repositoryAndSuggestions.containsKey(repositoryId)) {
          repositoryAndSuggestions.put(repositoryId, new HashSet<>());
        }
        repositoryAndSuggestions.get(repositoryId).add(source.toTemplateVersion());
      }
    }

    Set<Template> canExport = new HashSet<>();
    Set<Template> cannotExport = new HashSet<>();
    for (Map.Entry<String, Set<TemplateVersion>> entry : repositoryAndSuggestions.entrySet()) {
      String repositoryName = entry.getKey();
      List<TemplateVersion> templates = new ArrayList<>(entry.getValue());
      RepositoryManager repManager =
          this.wizard.getRepositoriesConfiguration().getRepositoryManager(repositoryName);
      Map<Template, TemplateProperties> templatesAndProperties =
          repManager.getTemplatesAndProperties(templates);

      if (templatesAndProperties == null || templatesAndProperties.isEmpty()) {
        // Get all the templates and properties for the local repository and filter the selected
        templatesAndProperties = this.wizard.getRepositoriesConfiguration()
            .getCorrelationTemplatesAndPropertiesByRepositoryName(repositoryName, true)
            .entrySet()
            .stream()
            .filter(templateEntry -> templates.stream().anyMatch(t ->
                templateEntry.getKey().getId().equals(t.getName()) &&
                    templateEntry.getKey().getVersion().equals(t.getVersion())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      }

      for (Map.Entry<Template, TemplateProperties> templateEntry
          : templatesAndProperties.entrySet()) {
        TemplateProperties value = templateEntry.getValue();
        Properties properties = new Properties();
        properties.putAll(value);
        if (properties.canExport()) {
          canExport.add(templateEntry.getKey());
        } else {
          cannotExport.add(templateEntry.getKey());
        }
      }
    }

    if (!cannotExport.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "The suggestions generated from the following sources\n can't be exported:\n"
              + String.join("\n", cannotExport.stream()
              .map(RepositoryUtils::getTemplateInfo)
              .collect(Collectors.joining("\n"))),
          "Non-exportable templates",
          JOptionPane.INFORMATION_MESSAGE);
    }

    Set<CorrelationRule> rules = new HashSet<>();
    for (CorrelationSuggestion suggestion : suggestions) {
      Template source = suggestion.getSource();
      // Automatic or Template based
      if (source == null || templateContains(canExport, source)) {
        rules.addAll(suggestion.toCorrelationRules());
      }
    }

    exportRulesConsumer.accept(new ArrayList<>(rules));
    JOptionPane.showMessageDialog(this,
        rules.isEmpty() ? "We didn't find any rules to export."
            : "Export successful.",
        "Exporting rules",
        JOptionPane.INFORMATION_MESSAGE);
  }

  private boolean templateContains(Set<Template> list, Template toMatch) {
    return list.stream()
        .anyMatch(toEvaluate -> toEvaluate.getId().equals(toMatch.getId()) &&
            toEvaluate.getVersion().equals(toMatch.getVersion()));
  }

  public void displayAppliedResults() {
    tabbedPane.setSelectedIndex(1);
  }

  public void displaySuggestionsTab() {
    tabbedPane.setSelectedIndex(0);
  }

  public void triggerSuggestionsGeneration(int totalErrors) {
    if (totalErrors == 0) {
      LOG.warn("No errors were found in the replay report, cannot generate suggestions");
      return;
    }

    SuggestionGenerator generator = SuggestionGenerator.getInstance(new ComparisonMethod());

    ComparisonContext context = new ComparisonContext();
    context.setRecordingTraceFilePath(getRecordingTraceSupplier.get());
    context.setReplayTraceFilePath(getReplayTraceSupplier.get());
    context.setConfiguration(new Configuration());

    List<CorrelationSuggestion> suggestions = generator.generateSuggestions(context);

    loadSuggestions(suggestions);
    toggleWizardVisibility();
    showColumn(3, 0);
  }

  public void loadSuggestionsMap(Map<Template, List<CorrelationSuggestion>> suggestions) {
    SuggestionsTableModel model = (SuggestionsTableModel) table.getModel();
    model.loadSuggestionsMap(suggestions);
  }

  public static class SuggestionsTableModel extends DefaultTableModel {

    //Extended table will also show "New Value" column
    private boolean extended = false;
    private final List<String> columns = Arrays.asList("Select", "Source", "Name",
        "Value", "Used on", "Obtained from");
    private final List<SuggestionItem> suggestionList = new ArrayList<>();
    private final Map<Template, List<CorrelationSuggestion>> suggestionsMap =
        new HashMap<>();

    @Override
    public String getColumnName(int column) {
      return columns.get(column);
    }

    @Override
    public int getRowCount() {
      if (suggestionList == null) {
        return 0;
      }
      return suggestionList.size();
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
      SuggestionItem item = suggestionList.get(rowIndex);
      CorrelationSuggestion suggestion = item.getSuggestion();
      switch (columnIndex) {
        case 0:
          return item.isSelected();
        case 1:
          return resolveSource(suggestion);
        case 2:
          return suggestion.getParamName();
        case 3:
          return suggestion.getOriginalValueString();
        case 4:
          return suggestion.getUsedOnString();
        case 5:
          return suggestion.getObtainedFromString();
        default:
          return "N/A";
      }
    }

    private String resolveSource(CorrelationSuggestion suggestion) {
      Template sourceTemplate = suggestion.getSource();
      if (sourceTemplate == null) {
        return "'Auto-generated'";
      }

      return "'" + sourceTemplate.getId() + "' (" + sourceTemplate.getRepositoryId() + ")";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (columnIndex == 0) {
        suggestionList.get(rowIndex).setSelected((boolean) aValue);
      } else {
        super.setValueAt(aValue, rowIndex, columnIndex);
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 0;
    }

    public List<CorrelationSuggestion> getSelectedSuggestions() {
      return suggestionList.stream()
          .filter(SuggestionItem::isSelected)
          .map(SuggestionItem::getSuggestion)
          .collect(Collectors.toList());
    }

    public void loadSuggestions(List<CorrelationSuggestion> suggestions) {
      suggestionList.clear();
      suggestions.forEach(suggestion -> suggestionList.add(new SuggestionItem(suggestion)));
      fireTableDataChanged();
    }

    public void loadSuggestionsMap(Map<Template, List<CorrelationSuggestion>> suggestions) {
      suggestionsMap.clear();
      suggestionsMap.putAll(suggestions);
      fireTableDataChanged();
    }

    public boolean toggleExpand() {
      this.extended = !extended;
      fireTableStructureChanged();
      return extended;
    }
  }

  private static class SuggestionItem {

    private boolean selected = true;
    private final CorrelationSuggestion suggestion;

    SuggestionItem(CorrelationSuggestion suggestion) {
      this.suggestion = suggestion;
    }

    public void setSelected(boolean selected) {
      this.selected = selected;
    }

    public boolean isSelected() {
      return selected;
    }

    public CorrelationSuggestion getSuggestion() {
      return suggestion;
    }
  }
}
