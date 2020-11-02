package com.blazemeter.jmeter.correlation.gui;

import static javax.swing.BorderFactory.createEmptyBorder;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.gui.CorrelationTemplateFrame;
import com.blazemeter.jmeter.correlation.core.templates.gui.CorrelationTemplatesFrame;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.GuiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesContainer extends JPanel implements ActionListener {

  protected static final int MAIN_CONTAINER_WIDTH = 800;
  private static final int FIELD_PREFERRED_HEIGHT = 25;
  protected static final Dimension FIELD_PREFERRED_SIZE = new Dimension(150,
      FIELD_PREFERRED_HEIGHT);
  protected static final int ROW_PREFERRED_HEIGHT = FIELD_PREFERRED_HEIGHT + 10;
  private static final Logger LOG = LoggerFactory.getLogger(RulesContainer.class);
  private static final String ADD = "add";
  private static final String DELETE = "delete";
  private static final String UP = "up";
  private static final String DOWN = "down";
  private static final String SAVE = "save";
  private static final String LOAD = "load";
  private static final String CLEAR = "clear";
  private static final String TEMPLATE_ACTIONS_BUTTON_SUFFIX_TEXT = " Template";
  private static final Dimension MAIN_CONTAINER_DEFAULT_DIMENSION = new Dimension(
      MAIN_CONTAINER_WIDTH, 300);

  private final CorrelationTemplatesRegistryHandler templatesRegistryHandler;
  private final CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistryHandler;
  private final CorrelationComponentsRegistry componentsRegistry;
  private final Runnable modelUpdater;
  private final Set<CorrelationTemplate> loadedTemplates;
  private JButton deleteButton;
  private JButton upButton;
  private JButton downButton;
  private JScrollPane scrollPane;
  private ResponseFilterField responseFilterField;
  private JLabel responseFilterNotificationLabel;
  private CorrelationTemplateFrame templateFrame;
  private CorrelationTemplatesFrame loadFrame;
  private ComponentContainer componentContainer;
  private boolean isSiebelTestPlan;
  private RulesTable rulesTable;

  public RulesContainer(CorrelationTemplatesRegistryHandler correlationTemplatesRegistryHandler,
      Runnable modelUpdater) {
    templatesRegistryHandler = correlationTemplatesRegistryHandler;
    repositoriesRegistryHandler =
        (CorrelationTemplatesRepositoriesRegistryHandler) correlationTemplatesRegistryHandler;
    //Used to update model from GUI
    this.modelUpdater = modelUpdater;
    componentsRegistry = new CorrelationComponentsRegistry();
    loadedTemplates = new HashSet<>();
    makeContainer();
  }

  @VisibleForTesting
  public void setLoadFrame(CorrelationTemplatesFrame loadFrame) {
    this.loadFrame = loadFrame;
  }

  @VisibleForTesting
  public void setTemplateFrame(CorrelationTemplateFrame templateFrame) {
    this.templateFrame = templateFrame;
  }

  private void makeContainer() {
    setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);

    prepareRulesTable();
    JPanel templateButtonPanel = makeTemplateButtonPanel();
    JPanel buttonPanel = makeRulesButtonsPanel();
    CollapsiblePanelsList componentPanel = makeAdvancedComponentPanel();

    JLabel responseFilterLabel = SwingUtils
        .createComponent("responseFilterLabel", new JLabel("Response Filters: "),
            FIELD_PREFERRED_SIZE);
    responseFilterLabel.setMinimumSize(FIELD_PREFERRED_SIZE);
    responseFilterLabel.setToolTipText("Response filter's regex");

    Consumer<String> notifyRemoved = (notificationText) -> {
      String informativeMessage =
          "<html> This field allows to filter the responses by their MIME type." +
              " You can add more than one separating them using commas. If left empty, no "
              + "filtering will be applied.<br>"
              + notificationText + "</html>";
      responseFilterNotificationLabel.setText(informativeMessage);
      modelUpdater.run();
    };

    responseFilterField = new ResponseFilterField("responseFilterField", FIELD_PREFERRED_SIZE,
        notifyRemoved);
    responseFilterNotificationLabel = SwingUtils
        .createComponent("responseFilterNotification", new JLabel());
    responseFilterNotificationLabel.setMinimumSize(FIELD_PREFERRED_SIZE);
    responseFilterNotificationLabel.setText(
        "This field allows to filter the responses by their MIME type. You can add more than one "
            + "separating them using commas.");

    JLabel responseFilterHelper = SwingUtils
        .createComponent("responseFilterHelper", new ThemedIconLabel("help.png"));
    responseFilterHelper.setMinimumSize(new Dimension(35, 35));
    responseFilterHelper.setToolTipText(
        "Filter the responses by the Content Type where this field contains. Regular Expressions "
            + "are supported.");

    layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
        .addComponent(templateButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            Short.MAX_VALUE)
        .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            Short.MAX_VALUE)
        .addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            Short.MAX_VALUE)
        .addGroup(layout.createSequentialGroup()
            .addGap(10)
            .addComponent(responseFilterLabel)
            .addComponent(responseFilterField)
            .addComponent(responseFilterHelper)
        )
        .addComponent(responseFilterNotificationLabel)
        .addComponent(componentPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            Short.MAX_VALUE));

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addComponent(templateButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            GroupLayout.PREFERRED_SIZE)
        .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            GroupLayout.PREFERRED_SIZE)
        .addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            GroupLayout.PREFERRED_SIZE)
        .addGap(GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
        .addGroup(layout.createParallelGroup()
            .addComponent(responseFilterLabel)
            /*
            Since the Collapsible panel forces the sizes of other components when it expands,
            this filter needs to have a "max" height fixed.
            */
            .addComponent(responseFilterField, GroupLayout.PREFERRED_SIZE,
                GroupLayout.PREFERRED_SIZE, FIELD_PREFERRED_SIZE.height)
            .addComponent(responseFilterHelper, GroupLayout.PREFERRED_SIZE,
                GroupLayout.PREFERRED_SIZE, FIELD_PREFERRED_SIZE.height)
        )
        .addComponent(responseFilterNotificationLabel, GroupLayout.PREFERRED_SIZE,
            GroupLayout.PREFERRED_SIZE, FIELD_PREFERRED_SIZE.height)
        .addGap(GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
        .addComponent(componentPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            GroupLayout.PREFERRED_SIZE));
  }

  private void prepareRulesTable() {
    rulesTable = SwingUtils
        .createComponent("rulesTable", new RulesTable(), MAIN_CONTAINER_DEFAULT_DIMENSION);
    rulesTable.getSelectionModel().addListSelectionListener(e -> checkButtonsStatus());
    scrollPane = new JScrollPane(null, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrollPane.setViewportView(rulesTable);
    scrollPane.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        rulesTable.doTableResize();
      }
    });
  }

  public void updateTable() {
    rulesTable.doTableResize();
  }

  private JPanel makeTemplateButtonPanel() {
    JButton exportButton = makeButton("export", SAVE);
    exportButton.setText(JMeterUtils.getResString("save") + TEMPLATE_ACTIONS_BUTTON_SUFFIX_TEXT);

    JButton loadButton = makeButton("load", LOAD);
    loadButton.setText(JMeterUtils.getResString("load") + TEMPLATE_ACTIONS_BUTTON_SUFFIX_TEXT);

    JButton clear = makeButton("clear", CLEAR);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    buttonPanel.setMinimumSize(new Dimension(100, 200));
    buttonPanel.add(loadButton);
    buttonPanel.add(exportButton);
    buttonPanel.add(clear);
    return buttonPanel;
  }

  private JPanel makeRulesButtonsPanel() {
    JButton add = makeButton("add", ADD);
    upButton = makeButton("up", UP);
    downButton = makeButton("down", DOWN);
    deleteButton = makeButton("delete", DELETE);
    checkButtonsStatus();

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(add);
    buttonPanel.add(deleteButton);
    buttonPanel.add(upButton);
    buttonPanel.add(downButton);
    buttonPanel.setMinimumSize(new Dimension(100, 200));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    return buttonPanel;
  }

  private JButton makeButton(String name, String action) {
    String parsedName = JMeterUtils.getResString(name);
    JButton button = SwingUtils.createComponent(name + "Button",
        new JButton(parsedName.contains("res_key") ? StringUtils.capitalize(name) : parsedName));
    button.setActionCommand(action);
    button.addActionListener(this);
    return button;
  }

  private void checkButtonsStatus() {
    int totalRules = rulesTable.getRules().size();
    deleteButton.setEnabled(totalRules > 0);
    int[] selectedRows = rulesTable.getSelectedRows();
    upButton.setEnabled(selectedRows.length > 0 && selectedRows[0] > 0);
    downButton.setEnabled(
        selectedRows.length > 0
            && selectedRows[selectedRows.length - 1] < rulesTable.getRules().size() - 1);
  }

  private CollapsiblePanelsList makeAdvancedComponentPanel() {
    componentContainer = new ComponentContainer(this::validateRulesConsistency);

    HeaderPanel extensionsHeader = new HeaderPanel("Custom Extensions");
    extensionsHeader.setPreferredSize(new Dimension(300, 30));

    JPanel extensionsBody = componentContainer;
    extensionsBody.setPreferredSize(new Dimension(300, 200));
    extensionsBody.setBorder(new JTextField().getBorder());

    CollapsiblePanel collapsiblePanel = new CollapsiblePanel(extensionsBody, extensionsHeader,
        true);
    CollapsiblePanelsList collapsiblePane = new CollapsiblePanelsList();
    collapsiblePane.addComponent(collapsiblePanel);
    return collapsiblePane;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();
    switch (action) {
      case CLEAR:
        clearContainer();
        break;
      case DELETE:
        deleteRule();
        break;
      case ADD:
        addRule();
        break;
      case UP:
        moveUp();
        break;
      case DOWN:
        moveDown();
        break;
      case SAVE:
        if (templateFrame == null) {
          templateFrame = new CorrelationTemplateFrame(templatesRegistryHandler,
              getSnapshotBufferedImage(), this::updateLoadedTemplate, this);
        }
        displaySaveTemplateFrame();
        break;
      case LOAD:
        if (loadFrame == null) {
          loadFrame = new CorrelationTemplatesFrame(templatesRegistryHandler,
              repositoriesRegistryHandler, this::updateLoadedTemplate, this);
        }
        loadFrame.showFrame();
        break;
      default:
        throw new UnsupportedOperationException(action);
    }
    checkButtonsStatus();
  }

  private void clearContainer() {
    clearLocalConfiguration();
    /*
     * If we don't force the update the of the Model
     * with the info in the RulesContainer,
     * the rules "stored" in the Model won't reset
     * and will be appended on LoadTemplate
     * */
    modelUpdater.run();
    rulesTable.doTableResize();
  }

  private void displaySaveTemplateFrame() {
    templateFrame.clear();
    if (!loadedTemplates.isEmpty()) {
      templateFrame.setLoadedTemplates(loadedTemplates);
    }
    templateFrame.setTemplateSnapshot(getSnapshotBufferedImage());
    templateFrame.showFrame();
  }

  private void deleteRule() {
    GuiUtils.cancelEditing(rulesTable);
    int[] rowsSelected = rulesTable.getSelectedRows();
    int anchorSelection = rulesTable.getSelectionModel().getAnchorSelectionIndex();
    rulesTable.clearSelection();

    if (rowsSelected.length > 0) {
      for (int i = rowsSelected.length - 1; i >= 0; i--) {
        rulesTable.removeRow(rowsSelected[i]);
      }
    }

    if (rulesTable.getRowCount() > 0) {
      if (anchorSelection >= rulesTable.getRowCount()) {
        anchorSelection = rulesTable.getRowCount() - 1;
      }
      rulesTable.setRowSelectionInterval(anchorSelection, anchorSelection);
    }
  }

  private void addRule() {
    RuleConfiguration rule = new RuleConfiguration(rulesTable.getRowCount() + 1, this::updateTable,
        componentsRegistry);
    //Setting default extractor and replacement when a rule is created
    rule.getExtractorConfigurationPanel().setSelectedItem(new RegexCorrelationExtractor<>());
    rule.getReplacementConfigurationPanel().setSelectedItem(new RegexCorrelationReplacement<>());
    addRule(rule);
  }

  private RuleConfiguration buildRuleConfiguration(CorrelationRule rule) {
    RuleConfiguration ruleConfiguration = new RuleConfiguration(rulesTable.getRowCount() + 1,
        this::updateTable, componentsRegistry);
    ruleConfiguration.setVariableName(rule.getReferenceName());
    ruleConfiguration.setEnable(rule.isEnabled());
    CorrelationExtractor<?> extractor = rule.getCorrelationExtractor();
    if (extractor != null) {
      ConfigurationPanel panel = ruleConfiguration.getExtractorConfigurationPanel();
      panel.setSelectedItem(extractor);
      panel.setParamValues(extractor.getParams());
      panel.setEnabled(rule.isEnabled());
    }

    CorrelationReplacement<?> replacement = rule.getCorrelationReplacement();
    if (replacement != null) {
      ConfigurationPanel panel = ruleConfiguration.getReplacementConfigurationPanel();
      panel.setSelectedItem(replacement);
      panel.setParamValues(replacement.getParams());
      panel.setEnabled(rule.isEnabled());
    }
    return ruleConfiguration;
  }
  
  private void addRule(RuleConfiguration rule) {
    if (!rulesTable.getRules().isEmpty()) {
      GuiUtils.stopTableEditing(rulesTable);
    }
    rulesTable.addRow(rule);
    updateMainContainerRequiredHeight();
    moveScrollToLastRule();
    updateTable();
    rule.paintHandlers();
    checkButtonsStatus();
  }

  private void updateMainContainerRequiredHeight() {
    int totalRequiredHeight = rulesTable.getRules().size() * ROW_PREFERRED_HEIGHT;
    if (totalRequiredHeight >= rulesTable.getHeight()) {
      mainContainerUpdateDimension(new Dimension(rulesTable.getWidth(), totalRequiredHeight));
    } else if (rulesTable.getRules().size() == 0) {
      mainContainerUpdateDimension(MAIN_CONTAINER_DEFAULT_DIMENSION);
    }
  }

  private void mainContainerUpdateDimension(Dimension requiredSize) {
    rulesTable.setMinimumSize(requiredSize);
    rulesTable.setPreferredSize(requiredSize);
  }

  private void moveScrollToLastRule() {
    if ((rulesTable.getRules().size() * FIELD_PREFERRED_SIZE.height) > rulesTable.getHeight()) {
      JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
      verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }
  }

  private void moveUp() {
    // get the selected rows before stopping editing
    // or the selected rows will be unselected
    int[] selectedRows = rulesTable.getSelectedRows();
    GuiUtils.stopTableEditing(rulesTable);

    if (selectedRows.length > 0 && selectedRows[0] > 0) {
      rulesTable.clearSelection();
      for (int selectedIndex : selectedRows) {
        rulesTable.switchRows(selectedIndex, selectedIndex - 1);
      }

      for (int rowSelected : selectedRows) {
        rulesTable.addRowSelectionInterval(rowSelected - 1, rowSelected - 1);
      }
    }
    checkButtonsStatus();
  }

  private void moveDown() {
    int[] rowsSelected = rulesTable.getSelectedRows();
    GuiUtils.stopTableEditing(rulesTable);

    if (rowsSelected.length > 0
        && rowsSelected[rowsSelected.length - 1] < rulesTable.getRowCount() - 1) {
      rulesTable.clearSelection();
      for (int i = rowsSelected.length - 1; i >= 0; i--) {
        int rowSelected = rowsSelected[i];
        rulesTable.switchRows(rowSelected, rowSelected + 1);
      }
      for (int rowSelected : rowsSelected) {
        rulesTable.addRowSelectionInterval(rowSelected + 1, rowSelected + 1);
      }
    }
  }

  public List<CorrelationRule> getCorrelationRules() {
    return rulesTable.getRules().stream()
        .map(RuleConfiguration::getCorrelationRule)
        .collect(Collectors.toList());
  }
  
  private void clear() {
    componentContainer.setComponentsTextArea("");
    rulesTable.clear();
    responseFilterField.setText("");
  }
  
  public void configure(CorrelationProxyControl model) {
    clear();
    setCorrelationComponents(buildCorrelationComponents(model));
    setResponseFilter(isSiebelTestPlan ? ContentType.TEXT_HTML.getMimeType() : model.getResponseFilter());
    model.getRules()
        .forEach(rule->addRule(buildRuleConfiguration(rule)));
    
    checkButtonsStatus();
  }
  

  private String buildCorrelationComponents(CorrelationProxyControl correlationProxyControl) {
    String correlationComponents = correlationProxyControl.getCorrelationComponents();
    if (correlationComponents.isEmpty()) {
      //if empty could mean that comes from CRM-Siebel.
      correlationComponents = getSiebelCRMComponents(
          correlationProxyControl.getCorrelationRulesTestElement());
      isSiebelTestPlan = !correlationComponents.isEmpty();
    }
    return correlationComponents;
  }

  public List<RuleConfiguration> getRules() {
    return rulesTable.getRules();
  }

  private BufferedImage getSnapshotBufferedImage() {
    Dimension d = getSize();
    BufferedImage snapshotImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = snapshotImage.createGraphics();
    print(g2d);
    g2d.dispose();

    return snapshotImage;
  }

  private void clearLocalConfiguration() {
    loadedTemplates.clear();
    rulesTable.clear();
    setResponseFilter("");
    componentContainer.setComponentsTextArea("");
  }

  public String getCorrelationComponents() {
    return this.componentContainer.getCorrelationComponents();
  }

  private void setCorrelationComponents(String correlationComponents) {
    componentContainer.setComponentsTextArea(componentsRegistry
        .updateActiveComponents(correlationComponents, new ArrayList<>()));
  }

  private String getSiebelCRMComponents(CorrelationRulesTestElement correlationRulesTestElement) {
    StringBuilder componentsBuilder = new StringBuilder();

    if (correlationRulesTestElement == null) {
      return "";
    }

    correlationRulesTestElement.getRules()
        .forEach(r -> {
          Class<? extends CorrelationExtractor<?>> extractorClass = r.getExtractorClass();
          String extractorClassName =
              extractorClass != null ? extractorClass.getCanonicalName() : "";
          Class<? extends CorrelationReplacement<?>> replacementClass = r.getReplacementClass();
          String replacementClassName =
              replacementClass != null ? replacementClass.getCanonicalName() : "";

          if (!RegexCorrelationExtractor.class.equals(extractorClass) &&
              !RegexCorrelationReplacement.class.equals(replacementClass)) {
            if (!extractorClassName.isEmpty() && !componentsBuilder.toString()
                .contains(extractorClassName)) {
              componentsBuilder.append(extractorClassName).append(",\n");
            }
            if (!replacementClassName.isEmpty() && !componentsBuilder.toString()
                .contains(replacementClassName)) {
              componentsBuilder.append(replacementClassName).append(",\n");
            }
          }
        });

    String components = componentsBuilder.toString();
    components =
        components.endsWith(",\n") ? components.substring(0, components.length() - 2) : components;
    return components.isEmpty() ? "" : components;
  }

  public String getResponseFilter() {
    return responseFilterField.getText();
  }

  private void setResponseFilter(String responseFilter) {
    responseFilterField.setText(responseFilter);
  }

  public void validateRulesConsistency(String componentsText) {
    List<RuleConfiguration> rules = getRules();
    List<String> missingSelectedExtensions = getUsedExtensions(rules).stream()
        .filter(extension -> !componentsText.contains(extension.getCanonicalName()))
        .map(Class::getCanonicalName)
        .collect(Collectors.toList());
    
    if (!missingSelectedExtensions.isEmpty()) {
      displayErrors(missingSelectedExtensions);
    }

    String activeComponents = componentsRegistry.updateActiveComponents(componentsText, missingSelectedExtensions);
    rules.forEach(r -> {
      r.getExtractorConfigurationPanel().updateComboOptions(componentsRegistry.buildActiveExtractors());
      r.getReplacementConfigurationPanel().updateComboOptions(componentsRegistry.buildActiveReplacements());
    });
    componentContainer.setComponentsTextArea(activeComponents);
  }
  
  private Set<Class<?>> getUsedExtensions(List<RuleConfiguration> rules) {
    Set<Class<?>> usedExtensions = new HashSet<>();
    rules.forEach(rule ->{
      CorrelationRulePartTestElement<?> selectedItem = rule.getExtractorConfigurationPanel().getSelectedItem();
      if (componentsRegistry.isExtractorExtension(selectedItem.getClass())) {
        usedExtensions.add(selectedItem.getClass());        
      }

      selectedItem = rule.getReplacementConfigurationPanel().getSelectedItem();
      if (componentsRegistry.isReplacementExtension(selectedItem.getClass())) {
        usedExtensions.add(selectedItem.getClass());
      }
    });
    return usedExtensions;
  }

  private void displayErrors(List<String> componentsInUse) {
    if (componentsInUse.size() > 0) {
      JPanel errorsPanel = new JPanel();
      errorsPanel.setLayout(new BorderLayout(5, 5));
      errorsPanel.setBorder(createEmptyBorder(10, 10, 10, 10));
      errorsPanel
          .add(new JLabel("The following component can't be removed since they are been used:"),
              BorderLayout.NORTH);
      JScrollPane scrollPane = new JScrollPane(new JList<>(componentsInUse.toArray()));
      errorsPanel.add(scrollPane, BorderLayout.SOUTH);
      JOptionPane.showMessageDialog(this, errorsPanel);
    }
  }

  public void updateLoadedTemplate(CorrelationTemplate template) {
    if (loadedTemplates.add(template)) {
      LOG.debug("Updated loaded templates. New Total={}. 'Last Template' = {}. ",
          loadedTemplates.size(), template);
    }
  }
  
  @VisibleForTesting
  public void clean() {
    clearContainer();
  }
  

}
