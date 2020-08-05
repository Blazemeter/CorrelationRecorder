package com.blazemeter.jmeter.correlation.gui;

import static com.blazemeter.jmeter.correlation.gui.RulePanel.DEFAULT_COMBO_VALUE;
import static javax.swing.BorderFactory.createEmptyBorder;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplate;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateFrame;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesFrame;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.gui.RulePanel.ContainerPanelsHandler;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesContainer extends JPanel implements ActionListener {

  private static final Logger LOG = LoggerFactory.getLogger(RulesContainer.class);
  private static final String ADD = "add";
  private static final String DELETE = "delete";
  private static final String UP = "up";
  private static final String DOWN = "down";
  private static final String SAVE = "save";
  private static final String LOAD = "load";
  private static final String CLEAR = "clear";
  private static final String TEMPLATE_ACTIONS_BUTTON_SUFFIX_TEXT = " Template";
  private static final Dimension MAIN_CONTAINER_DEFAULT_DIMENSION = new Dimension(800, 110);
  private static final Dimension MAIN_CONTAINER_SCROLL_DIMENSION = new Dimension(700, 200);
  private static final Dimension FIELD_PREFERRED_SIZE = new Dimension(150, 35);
  private static final int DEFAULT_EXTRA_GAP_BETWEEN_RULES = 5;
  private final CorrelationTemplatesRegistryHandler templatesRegistryHandler;
  private final CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistryHandler;
  private final CorrelationComponentsRegistry componentsRepository;
  private final Runnable modelUpdater;
  private RulePanel lastActivePanel;
  private ArrayList<RulePanel> rules;
  private JPanel mainContainer;
  private JButton deleteButton;
  private JButton upButton;
  private JButton downButton;
  private JScrollPane scrollPane;
  private ResponseFilterField responseFilterField;
  private JLabel responseFilterNotificationLabel;
  private CorrelationTemplateFrame templateFrame;
  private CorrelationTemplatesFrame loadFrame;
  private ComponentContainer componentContainer;
  private Set<CorrelationTemplate> loadedTemplates;
  private boolean isSiebelTestplan;

  public RulesContainer(CorrelationTemplatesRegistryHandler correlationTemplatesRegistryHandler,
      Runnable modelUpdater) {
    templatesRegistryHandler = correlationTemplatesRegistryHandler;
    repositoriesRegistryHandler =
        (CorrelationTemplatesRepositoriesRegistryHandler) correlationTemplatesRegistryHandler;
    //Used to avoid creating unnecessary interfaces
    this.modelUpdater = modelUpdater;

    componentsRepository = new CorrelationComponentsRegistry();
    rules = new ArrayList<>();
    loadedTemplates = new HashSet<>();

    makeContainer();
  }

  private void makeContainer() {
    setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);

    JScrollPane mainPanel = makeMainPanel();
    JPanel templateButtonPanel = makeTemplateButtonPanel();
    JPanel buttonPanel = makeRulesButtonsPanel();
    CollapsiblePanel componentPanel = makeAdvancedComponentPanel();

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
        .addComponent(mainPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
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
        .addComponent(mainPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
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

    reDraw();
  }


  private JScrollPane makeMainPanel() {
    mainContainer = new JPanel(new GridBagLayout());
    mainContainer.setPreferredSize(MAIN_CONTAINER_DEFAULT_DIMENSION);
    mainContainer.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    mainContainer.add(new JPanel(), getFirstAdd());

    scrollPane = new JScrollPane();
    scrollPane.setViewportView(mainContainer);
    scrollPane.setPreferredSize(MAIN_CONTAINER_SCROLL_DIMENSION);
    scrollPane.createVerticalScrollBar();
    scrollPane.createHorizontalScrollBar();

    return scrollPane;
  }

  private GridBagConstraints getFirstAdd() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.weightx = 1;
    constraints.weighty = 1;
    return constraints;
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
    int totalRules = rules.size();
    deleteButton.setEnabled(totalRules > 0);
    boolean morePositionsAvailable = totalRules > 1;
    upButton.setEnabled(
        morePositionsAvailable && (lastActivePanel == null || rules.indexOf(lastActivePanel) > 0));
    downButton.setEnabled(morePositionsAvailable && (lastActivePanel == null
        || rules.indexOf(lastActivePanel) < totalRules - 1));
    reDrawScroll();
  }

  public void reDrawScroll() {
    scrollPane.repaint();
    scrollPane.revalidate();
  }

  private CollapsiblePanel makeAdvancedComponentPanel() {
    componentContainer =
        new ComponentContainer(this::validateRulesConsistency);
    CollapsiblePanel collapsiblePane = SwingUtils
        .createComponent("CollapsiblePane", new CollapsiblePanel("Custom Extensions"));
    collapsiblePane.setMinimumSize(new Dimension(800, 0));
    collapsiblePane.add(componentContainer);
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
              getSnapshotBufferedImage(), this::updateLoadedTemplate);
        }
        if (rulesAreIncomplete()) {
          JOptionPane.showMessageDialog(this,
              "Rules are incomplete or empty, please fill them before continue saving.");
          break;
        }
        displaySaveTemplateFrame();
        break;
      case LOAD:
        if (loadFrame == null) {
          loadFrame = new CorrelationTemplatesFrame(templatesRegistryHandler,
              repositoriesRegistryHandler, this::updateLoadedTemplate);
        }
        LOG.info("[Before loading templates] Rules locally {}.", rules.size());
        loadFrame.showFrame();
        break;
      default:
        throw new UnsupportedOperationException(action);
    }
    checkButtonsStatus();
    reDraw();
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
  }

  private void displaySaveTemplateFrame() {
    templateFrame.clear();
    if (!loadedTemplates.isEmpty()) {
      templateFrame.setLoadedTemplates(loadedTemplates);
    }
    templateFrame.setTemplateSnapshot(getSnapshotBufferedImage());
    templateFrame.showFrame();
  }

  private boolean rulesAreIncomplete() {
    if (rules.isEmpty()) {
      return true;
    }

    return rules.stream()
        .anyMatch(r -> (r.getReferenceVariableField().getText().isEmpty() ||
            r.getExtractorHandler().getComboBox().getSelectedItem().equals(DEFAULT_COMBO_VALUE) &&
                r.getReplacementHandler().getComboBox().getSelectedItem()
                    .equals(DEFAULT_COMBO_VALUE)));
  }

  private void deleteRule() {
    if (lastActivePanel != null) {
      mainContainer.remove(lastActivePanel);
      rules.remove(lastActivePanel);
      lastActivePanel = null;
      reDrawScroll();
    }
  }

  private void addRule() {
    addRule(new RulePanel(this));
  }

  public Dimension getSizePanelThatContainsRules() {
    return mainContainer.getSize();
  }

  public void updatePanelThatContainsRules(int width) {
    /*
     * setSize will be ignored by parent component layout
     * So we need to force the min and the preferred in order
     * to resize to the minimum size that allows the components
     * in the "rule" to be displayed horizontally
     * */
    Dimension requiredSize = new Dimension(width + 10, mainContainer.getHeight());
    mainContainerUpdateDimension(requiredSize);
  }

  private void mainContainerUpdateDimension(Dimension requiredSize) {
    mainContainer.setMinimumSize(requiredSize);
    mainContainer.setPreferredSize(requiredSize);
  }

  private void addRule(RulePanel rule) {
    rules.add(rule);
    updateMainContainerRequiredHeight();
    mainContainer.add(rule, getAddingConstraints(), rules.size() - 1);
    moveScrollToLastRule();
    checkButtonsStatus();
  }

  public void updateMainContainerRequiredHeight() {
    int totalRequiredHeight =
        (rules.size() + 1) * (RulePanel.RULE_HEIGHT + DEFAULT_EXTRA_GAP_BETWEEN_RULES);
    if (totalRequiredHeight >= mainContainer.getHeight()) {
      mainContainerUpdateDimension(new Dimension(mainContainer.getWidth(), totalRequiredHeight));
    } else if (rules.size() == 0) {
      mainContainerUpdateDimension(MAIN_CONTAINER_DEFAULT_DIMENSION);
    }
  }

  private GridBagConstraints getAddingConstraints() {
    GridBagConstraints addingConstrains = new GridBagConstraints();
    addingConstrains.gridwidth = GridBagConstraints.REMAINDER;
    addingConstrains.weightx = 1;
    addingConstrains.fill = GridBagConstraints.HORIZONTAL;
    return addingConstrains;
  }

  private void moveScrollToLastRule() {
    JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
    verticalScrollBar.setValue(verticalScrollBar.getMaximum());
  }

  private void moveUp() {
    int position = rules.indexOf(lastActivePanel);
    if (position > 0) {
      movePanel(position, position - 1);
    }
  }

  private void movePanel(int origin, int dest) {
    Collections.swap(rules, origin, dest);
    rules.forEach(r -> {
      r.removeFocusBackground();
      mainContainer.remove(r);
      mainContainer.add(r, getAddingConstraints(), rules.size() - 1);
    });
    lastActivePanel.setFocusBackground();
  }

  private void moveDown() {
    int position = rules.indexOf(lastActivePanel);
    if (position < rules.size() - 1) {
      movePanel(position, position + 1);
    }
  }

  public void updateLastActivePanel(RulePanel activePanel) {
    if (lastActivePanel != null) {
      lastActivePanel.removeFocusBackground();
    }
    //This handles the unclick/removes focus for the actual active rule
    if (lastActivePanel != null && lastActivePanel.getName().equals(activePanel.getName())) {
      lastActivePanel = null;
    } else {
      lastActivePanel = activePanel;
      lastActivePanel.setFocusBackground();
    }
    checkButtonsStatus();
  }

  private void reDraw() {
    scrollPane.repaint();
    mainContainer.repaint();
    repaint();
  }

  public List<CorrelationRule> getCorrelationRules() {
    return rules.stream().filter(RulePanel::isComplete).map(this::getCorrelationRule)
        .collect(Collectors.toList());
  }

  private void setCorrelationRules(CorrelationProxyControl correlationProxyControl) {
    if (correlationProxyControl.getCorrelationRulesTestElement() != null) {
      for (JMeterProperty jMeterProperty : correlationProxyControl
          .getCorrelationRulesTestElement()) {
        setCorrelationRules((CorrelationRuleTestElement) jMeterProperty.getObjectValue());
      }
    }
    reDraw();
  }

  private void setCorrelationRules(CorrelationRuleTestElement ruleTestElement) {
    RulePanel rulePanel = new RulePanel(this);

    String referenceName = ruleTestElement.getReferenceName();
    rulePanel.setReferenceVariableNameText(referenceName);

    LOG.info("> RefVar {}", referenceName);
    CorrelationRulePartTestElement extractorValues = ruleTestElement.getExtractorRulePart();
    //Rules are allowed to be standalone extractors/replacements
    if (extractorValues != null) {
      rulePanel.setValuesFromRulePart(extractorValues, rulePanel.getExtractorHandler());
    }

    CorrelationRulePartTestElement replacementValues = ruleTestElement.getReplacementRulePart();
    if (replacementValues != null) {
      rulePanel.setValuesFromRulePart(replacementValues, rulePanel.getReplacementHandler());
    }

    if (rules.stream().noneMatch(r -> r.equals(rulePanel))) {
      LOG.info("Added Rule with {} {} {}", referenceName,
          extractorValues != null ? extractorValues.getType() : "",
          replacementValues != null ? replacementValues.getType() : "");
      addRule(rulePanel);
    }
  }

  private CorrelationRule getCorrelationRule(RulePanel rulePanel) {
    String referenceName = rulePanel.getReferenceVariableField().getText();
    CorrelationReplacement correlationReplacement = createReplacement(
        rulePanel.getReplacementHandler());
    CorrelationExtractor correlationExtractor = createExtractor(rulePanel.getExtractorHandler());

    return new CorrelationRule(referenceName, correlationExtractor, correlationReplacement);
  }

  private CorrelationReplacement createReplacement(ContainerPanelsHandler handler) {

    String selectedCorrelationType = getComboBoxSelectedValue(handler.getComboBox());
    if (selectedCorrelationType.isEmpty() || selectedCorrelationType
        .equals(DEFAULT_COMBO_VALUE)) {
      return null;
    }
    return componentsRepository
        .getCorrelationReplacement(selectedCorrelationType, getParams(handler));
  }

  private String getComboBoxSelectedValue(JComboBox comboBox) {
    return comboBox != null && comboBox.getSelectedItem() != null ? comboBox.getSelectedItem()
        .toString() : DEFAULT_COMBO_VALUE;
  }

  private List<String> getParams(ContainerPanelsHandler handler) {
    return handler.getListComponents().stream().map(
        c -> c instanceof JTextField ? ((JTextField) c).getText()
            : getComboBoxSelectedValue(((JComboBox) c))).collect(Collectors.toList());
  }

  private CorrelationExtractor createExtractor(ContainerPanelsHandler handler) {
    String selectedCorrelationType = getComboBoxSelectedValue(handler.getComboBox());
    if (selectedCorrelationType.isEmpty() || selectedCorrelationType
        .equals(DEFAULT_COMBO_VALUE)) {
      return null;
    }
    return componentsRepository
        .getCorrelationExtractor(selectedCorrelationType, getParams(handler));
  }

  public void configure(CorrelationProxyControl correlationProxyControl) {
    String correlationComponents = buildCorrelationComponents(correlationProxyControl);

    setCorrelationComponents(correlationComponents);
    setResponseFilter(isSiebelTestplan ?
        ContentType.TEXT_HTML.getMimeType() : correlationProxyControl.getResponseFilter());
    setCorrelationRules(correlationProxyControl);
    updateMainContainerRequiredHeight();
  }

  private String buildCorrelationComponents(CorrelationProxyControl correlationProxyControl) {
    String correlationComponents = correlationProxyControl.getCorrelationComponents();
    if (correlationComponents.isEmpty()) {
      //if empty could mean that comes from CRM-Siebel.
      correlationComponents = getSiebelCRMComponents(
          correlationProxyControl.getCorrelationRulesTestElement());
      isSiebelTestplan = !correlationComponents.isEmpty();
    }
    return correlationComponents;
  }

  private List<String> parseCorrelationComponentString(String correlationComponents) {
    if (correlationComponents.isEmpty()) {
      return new ArrayList<>();
    }
    return Arrays.asList(correlationComponents
        .replace("\n", "")
        .replace("\r", "")
        .split(","));
  }

  public ArrayList<RulePanel> getRules() {
    return rules;
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
    loadedTemplates = new HashSet<>();
    clearRules();
    setResponseFilter("");
    componentContainer.setComponentsTextArea("");
  }

  public void clearRules() {
    rules.forEach(r -> mainContainer.remove(r));
    rules.clear();
    componentContainer.clear();
    /*
     * We need to return the mainContainer to its original size
     * If not, it will have a size unnecessarily wide
     * For an empty container
     * */
    updatePanelThatContainsRules(MAIN_CONTAINER_DEFAULT_DIMENSION.width);
    updateMainContainerRequiredHeight();
    reDrawScroll();
  }

  public String getCorrelationComponents() {
    return this.componentContainer.getCorrelationComponents();
  }

  private void setCorrelationComponents(String correlationComponents) {
    componentContainer.setComponentsTextArea(correlationComponents);
    List<String> components = parseCorrelationComponentString(correlationComponents);
    components.forEach(c -> {
      try {
        componentsRepository.addComponent(c);
      } catch (IllegalArgumentException | ClassNotFoundException e) {
        LOG.error("There was an issue trying to add the component {}. ", c, e);
      }
    });
  }

  private String getSiebelCRMComponents(
      CorrelationRulesTestElement correlationRulesTestElement) {
    StringBuilder componentsBuilder = new StringBuilder();

    if (correlationRulesTestElement == null) {
      return "";
    }
    correlationRulesTestElement.getRules()
        .forEach(r -> {
          String extractorClassName = r.getExtractorClass() != null ?
              r.getExtractorClass().getCanonicalName() : "";
          String replacementClassName = r.getReplacementClass() != null ?
              r.getReplacementClass().getCanonicalName() : "";
          if (!RegexCorrelationExtractor.class.getCanonicalName().equals(extractorClassName)
              && !RegexCorrelationReplacement.class.getCanonicalName()
              .equals(replacementClassName)) {
            if (!extractorClassName.isEmpty() && !componentsBuilder.toString()
                .contains(extractorClassName)) {
              componentsBuilder.append(extractorClassName);
              componentsBuilder.append(",");
              componentsBuilder.append("\n");
            }
            if (!replacementClassName.isEmpty() && !componentsBuilder.toString()
                .contains(replacementClassName)) {
              componentsBuilder.append(replacementClassName);
              componentsBuilder.append(",");
              componentsBuilder.append("\n");
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

  public void setResponseFilter(String responseFilter) {
    responseFilterField.setText(responseFilter);
  }

  private void resetCorrelationComponents() {
    componentsRepository.reset();
  }

  private void addCorrelationComponents(String correlationComponents) {
    List<String> components = parseCorrelationComponentString(correlationComponents);
    components.forEach(c -> {
      try {
        componentsRepository.addComponent(c);
      } catch (IllegalArgumentException | ClassNotFoundException e) {
        LOG.error("There was an issue trying to add the component {}. ", c, e);
      }
    });
  }

  private boolean containsExtractor(String simpleClassName) {
    return componentsRepository.containsExtractor(simpleClassName);
  }

  private boolean containsReplacement(String simpleClassName) {
    return componentsRepository.containsReplacement(simpleClassName);
  }

  public List<String> getExtractors() {
    return componentsRepository.getExtractorsComponents();
  }

  public List<String> getReplacements() {
    return componentsRepository.getReplacementsComponents();
  }

  public List<ParameterDefinition> getExtractorParamsDefinition(String extractorName) {
    CorrelationRulePartTestElement correlationRulePartTestElement = componentsRepository
        .getCorrelationExtractor(extractorName, new ArrayList<>());
    return correlationRulePartTestElement != null ? correlationRulePartTestElement
        .getParamsDefinition() : new ArrayList<>();
  }

  List<ParameterDefinition> getReplacementParamsDefinition(String replacementName) {
    CorrelationRulePartTestElement correlationRulePartTestElement = componentsRepository
        .getCorrelationReplacement(replacementName, new ArrayList<>());
    return correlationRulePartTestElement != null ? correlationRulePartTestElement
        .getParamsDefinition() : new ArrayList<>();
  }

  public void validateRulesConsistency(String componentsText) {
    resetCorrelationComponents();
    addCorrelationComponents(componentsText);

    ArrayList<RulePanel> rules = getRules();
    if (rules == null) {
      return;
    }

    List<String> comboBoxValidationErrors = validateRulePanels(rules);
    if (!comboBoxValidationErrors.isEmpty()) {
      displayErrors(comboBoxValidationErrors);
      return;
    }

    rules.forEach(r -> {
      setCorrelationComboValues(r.getExtractorHandler(), getExtractors());
      setCorrelationComboValues(r.getReplacementHandler(), getReplacements());
    });
  }

  private List<String> validateRulePanels(ArrayList<RulePanel> rules) {
    List<String> rulePanelsErrors = new ArrayList<>();
    rules.forEach(r -> {
      String referenceVar = r.getReferenceVariableField().getText();

      if (!DEFAULT_COMBO_VALUE.equals(r.getExtractorHandler().getComboBox().getSelectedItem())) {
        String extractorValidationResult = validateCorrelationContainer(referenceVar,
            r.getExtractorHandler(), this::containsExtractor);
        if (!extractorValidationResult.isEmpty()) {
          rulePanelsErrors.add(extractorValidationResult);
        }
      }

      if (!DEFAULT_COMBO_VALUE.equals(r.getReplacementHandler().getComboBox().getSelectedItem())) {
        String replacementValidationResult = validateCorrelationContainer(referenceVar,
            r.getReplacementHandler(), this::containsReplacement);
        if (!replacementValidationResult.isEmpty()) {
          rulePanelsErrors.add(replacementValidationResult);
        }
      }
    });
    return rulePanelsErrors;
  }

  private String validateCorrelationContainer(String referenceVar,
      ContainerPanelsHandler correlationPartContainer, Function<String, Boolean> contained) {
    String selectedCorrelation = (String) correlationPartContainer.getComboBox().getSelectedItem();
    if (!contained.apply(selectedCorrelation)) {
      return "The rule with reference variable '" + referenceVar + "' depends on the "
          + selectedCorrelation
          + " component, which isn't considered in the allowed.";
    }

    return "";
  }

  private void setCorrelationComboValues(ContainerPanelsHandler correlationPartContainer,
      List<String> componentNames) {
    JComboBox correlationPartCombo = correlationPartContainer.getComboBox();
    ItemListener[] listeners = correlationPartCombo.getItemListeners();
    Arrays.stream(listeners).forEach(correlationPartCombo::removeItemListener);
    Object selectedItem = correlationPartCombo.getSelectedItem();

    correlationPartCombo.removeAllItems();
    correlationPartCombo.addItem(DEFAULT_COMBO_VALUE);
    componentNames.forEach(correlationPartCombo::addItem);
    correlationPartCombo.setSelectedItem(selectedItem);
    Arrays.stream(listeners).forEach(correlationPartCombo::addItemListener);
  }

  private void displayErrors(List<String> rulesErrors) {
    if (rulesErrors.size() > 0) {
      JPanel errorsPanel = new JPanel();
      errorsPanel.setLayout(new BorderLayout(5, 5));
      errorsPanel.setBorder(createEmptyBorder(10, 10, 10, 10));
      errorsPanel
          .add(new JLabel("After validating all the components, the following errors were found:"),
              BorderLayout.NORTH);
      JScrollPane scrollPane = new JScrollPane(new JList(rulesErrors.toArray()));
      errorsPanel.add(scrollPane, BorderLayout.SOUTH);
      JOptionPane.showMessageDialog(null, errorsPanel);
    }
  }

  public void updateLoadedTemplate(CorrelationTemplate template) {
    if (loadedTemplates.add(template)) {
      LOG.info("Updated loaded templates. New Total={}. 'Last Template' = {}. ",
          loadedTemplates.size(), template);
    }
  }

  @VisibleForTesting
  public void setLastLoadedTemplates(Set<CorrelationTemplate> loadedTemplates) {
    this.loadedTemplates = loadedTemplates;
  }

  @VisibleForTesting
  public Set<CorrelationTemplate> getLoadedTemplates() {
    return loadedTemplates;
  }

  @VisibleForTesting
  public void setComponents(String components) {
    this.componentContainer.setComponentsTextArea(components);
  }
}
