package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplatesRepositoriesRegistryHandler;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.gui.common.StringUtils;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.templates.CorrelationTemplateFrame;
import com.blazemeter.jmeter.correlation.gui.templates.CorrelationTemplatesFrame;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesContainer extends JPanel implements ActionListener {

  public static final int MAIN_CONTAINER_WIDTH = 800;
  private static final int FIELD_PREFERRED_HEIGHT = 25;
  protected static final Dimension FIELD_PREFERRED_SIZE = new Dimension(150,
      FIELD_PREFERRED_HEIGHT);
  public static final int ROW_PREFERRED_HEIGHT = FIELD_PREFERRED_HEIGHT + 10;
  private static final Logger LOG = LoggerFactory.getLogger(RulesContainer.class);
  private static final String SAVE = "save";
  private static final String LOAD = "load";
  private static final String CLEAR = "clear";
  private static final String TEMPLATE_ACTIONS_BUTTON_SUFFIX_TEXT = " Template";

  private final CorrelationTemplatesRegistryHandler templatesRegistryHandler;
  private final CorrelationTemplatesRepositoriesRegistryHandler repositoriesRegistryHandler;
  private final Runnable modelUpdater;
  private final Set<TemplateVersion> loadedTemplates;
  private final GroupsContainer groupsContainer;
  private final ResponseFilterPanel responseFilterPanel;
  private CorrelationTemplateFrame templateFrame;
  private CorrelationTemplatesFrame loadFrame;
  private boolean isSiebelTestPlan;

  public RulesContainer(CorrelationTemplatesRegistryHandler correlationTemplatesRegistryHandler,
      Runnable modelUpdater) {
    this.modelUpdater = modelUpdater;
    templatesRegistryHandler = correlationTemplatesRegistryHandler;
    repositoriesRegistryHandler =
        (CorrelationTemplatesRepositoriesRegistryHandler) correlationTemplatesRegistryHandler;
    loadedTemplates = new HashSet<>();
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(makeTemplateButtonPanel());
    groupsContainer = new GroupsContainer(modelUpdater);
    add(groupsContainer);
    responseFilterPanel = new ResponseFilterPanel();
    add(responseFilterPanel);
  }

  @VisibleForTesting
  public void setLoadFrame(CorrelationTemplatesFrame loadFrame) {
    this.loadFrame = loadFrame;
  }

  @VisibleForTesting
  public void setTemplateFrame(CorrelationTemplateFrame templateFrame) {
    this.templateFrame = templateFrame;
  }

  private JPanel makeTemplateButtonPanel() {
    JButton exportButton = makeButton("export", SAVE);
    exportButton.setText(JMeterUtils.getResString("save") + TEMPLATE_ACTIONS_BUTTON_SUFFIX_TEXT);

    JButton loadButton = makeButton("load", LOAD);
    loadButton.setText(JMeterUtils.getResString("load") + TEMPLATE_ACTIONS_BUTTON_SUFFIX_TEXT);

    JButton clearButton = makeButton("clear", CLEAR);
    clearButton.setText(JMeterUtils.getResString("clear"));

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    buttonPanel.setMinimumSize(new Dimension(100, 200));
    buttonPanel.add(loadButton);
    buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
    buttonPanel.add(exportButton);
    buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
    buttonPanel.add(clearButton);
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

  @Override
  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();
    switch (action) {
      case CLEAR:
        clearContainer();
        groupsContainer.doGroupsUIRefresh();
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

  public List<RulesGroup> getRulesGroups() {
    return groupsContainer.getGroupPanels().stream()
        .map(GroupPanel::getRulesGroup)
        .collect(Collectors.toList());
  }

  public void configure(CorrelationProxyControl model) {
    clearLocalConfiguration();
    updateCustomExtensions(buildCorrelationComponents(model));
    setResponseFilter(isSiebelTestPlan ? ContentType.TEXT_HTML.getMimeType()
        : model.getResponseFilter());
    if (model.getGroups() != null && !model.getGroups().isEmpty()) {
      groupsContainer.configure(model.getGroups());
    }
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
    groupsContainer.clear();
    setResponseFilter("");
    updateCustomExtensions("");
  }

  public String getCorrelationComponents() {
    return CorrelationComponentsRegistry.getInstance().getCorrelationComponents();
  }

  private void updateCustomExtensions(String correlationComponents) {
    CorrelationComponentsRegistry.getInstance()
        .updateActiveComponents(correlationComponents, new ArrayList<>());
  }

  private String getSiebelCRMComponents(CorrelationRulesTestElement correlationRulesTestElement) {
    if (correlationRulesTestElement == null) {
      return "";
    }
    return correlationRulesTestElement.getRules().stream()
        .flatMap(r -> Stream.<Class<?>>of(r.getExtractorClass(), r.getReplacementClass()))
        .filter(c -> c != null && c != RegexCorrelationExtractor.class
            && c != RegexCorrelationReplacement.class)
        .collect(Collectors.toCollection(LinkedHashSet::new)).stream()
        .map(Class::getCanonicalName)
        .collect(Collectors.joining(",\n"));
  }

  public String getResponseFilter() {
    return responseFilterPanel.getResponseFilter();
  }

  private void setResponseFilter(String responseFilter) {
    responseFilterPanel.setResponseFilter(responseFilter);
  }

  public void updateLoadedTemplate(TemplateVersion template) {
    if (loadedTemplates.add(template)) {
      LOG.debug("Updated loaded templates. New Total={}. 'Last Template' = {}. ",
          loadedTemplates.size(), template);
    }
  }

  @Override
  public String toString() {
    return "RulesContainer{" +
        "loadedTemplates=" + loadedTemplates +
        ", groupsContainer=" + groupsContainer +
        ", isSiebelTestPlan=" + isSiebelTestPlan +
        '}';
  }
}
