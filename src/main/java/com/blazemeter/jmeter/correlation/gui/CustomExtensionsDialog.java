package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.DescriptionContent;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.gui.common.RulePartType;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import com.google.common.base.Objects;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomExtensionsDialog extends JDialog implements ActionListener {

  private static final String REMOVE = "remove";
  private static final String ADD = "add";
  private static final Dimension LIST_PREFERRED_SIZE = new Dimension(150, 200);
  private static final Logger LOG = LoggerFactory.getLogger(CustomExtensionsDialog.class);
  private final Runnable updateComboOptions;
  private final CorrelationComponentsRegistry registry;
  private DefaultListModel<ExtensionItem> activeModel;
  private DefaultListModel<ExtensionItem> availableModel;
  private JList<ExtensionItem> activeList;
  private JList<ExtensionItem> availableList;
  private JTextPane descriptionTextPane;
  private JLabel informativeLabel;
  private JButton removeExtensionButton;
  private JButton addExtensionButton;
  private RulePartType type;

  public CustomExtensionsDialog(Runnable updateComboOptions, JPanel parent) {
    super((JFrame) SwingUtilities.getWindowAncestor(parent), true);
    setName("customExtensionDialog");
    this.registry = CorrelationComponentsRegistry.getInstance();
    this.updateComboOptions = updateComboOptions;
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    buildMainPanel();
    pack();
  }

  private void buildMainPanel() {
    prepareButtons();
    prepareLists();

    VerticalPanel leftPane = buildVerticalPanel(activeList, "activeScrollPane");
    VerticalPanel rightPanel = buildVerticalPanel(availableList, "availableScrollPane");

    descriptionTextPane = SwingUtils.buildJTextPane("displayInfoPanel", LOG);
    JScrollPane descriptionScroll = SwingUtils.buildScrollPanel(descriptionTextPane,
        "descriptionScroll", new Dimension(600, 350));

    JLabel activeExtensionsLabel = new JLabel("Active");
    JLabel availableExtensionsLabel = new JLabel("Available");
    informativeLabel = new JLabel("");

    JPanel mainPanel = new JPanel();
    mainPanel.setPreferredSize(new Dimension(700, 650));
    GroupLayout layout = new GroupLayout(mainPanel);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);
    mainPanel.setLayout(layout);
    add(mainPanel);

    layout.setHorizontalGroup(layout.createParallelGroup()
        .addComponent(informativeLabel)
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup()
                .addComponent(activeExtensionsLabel)
                .addComponent(leftPane))
            .addGroup(layout.createParallelGroup()
                .addComponent(addExtensionButton)
                .addComponent(removeExtensionButton))
            .addGroup(layout.createParallelGroup()
                .addComponent(availableExtensionsLabel)
                .addComponent(rightPanel)))
        .addComponent(descriptionScroll)
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addComponent(informativeLabel)
        .addGroup(layout.createParallelGroup(Alignment.CENTER)
            .addGroup(layout.createSequentialGroup()
                .addComponent(activeExtensionsLabel)
                .addComponent(leftPane))
            .addGroup(layout.createSequentialGroup()
                .addComponent(removeExtensionButton)
                .addComponent(addExtensionButton))
            .addGroup(layout.createSequentialGroup()
                .addComponent(availableExtensionsLabel)
                .addComponent(rightPanel)))
        .addComponent(descriptionScroll)
    );
  }

  private void prepareButtons() {
    removeExtensionButton = SwingUtils.buildJButton("removeExtension", ">>", REMOVE, this);
    removeExtensionButton.setEnabled(false);
    removeExtensionButton.setToolTipText("Select an extension before removing it");

    addExtensionButton = SwingUtils.buildJButton("addExtension", "<<", ADD, this);
    addExtensionButton.setEnabled(false);
    addExtensionButton.setToolTipText("Select an extension before adding it");
  }

  private void prepareLists() {
    activeModel = new DefaultListModel<>();
    activeList = SwingUtils.buildJList("activeExtensionList", activeModel, LIST_PREFERRED_SIZE);
    activeList.setCellRenderer(new ExtensionItemRenderer());
    activeList.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        triggerItemSelection(activeList, e);
      }
    });

    availableModel = new DefaultListModel<>();
    availableList = SwingUtils
        .buildJList("availableExtensionList", availableModel, LIST_PREFERRED_SIZE);
    availableList.setCellRenderer(new ExtensionItemRenderer());
    availableList.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        triggerItemSelection(availableList, e);
      }
    });
  }

  private void triggerItemSelection(JList<ExtensionItem> selected, MouseEvent e) {
    if (selected.getSelectedIndex() != -1) {
      boolean isActive = selected.equals(activeList);
      removeExtensionButton.setEnabled(isActive);
      addExtensionButton.setEnabled(!isActive);

      ExtensionItem item = ((JList<ExtensionItem>) e.getSource()).getSelectedValue();
      updateDescriptionContent(DescriptionContent.getFromClass(item.getExtension().getClass()));
      if (isActive) {
        availableList.clearSelection();
        removeExtensionButton.setEnabled(!item.isActive());
        removeExtensionButton.setToolTipText(!item.isActive() ? "Remove selected extension" 
            : "The selected extension can't be removed since is being used.");
      } else {
        activeList.clearSelection();
        addExtensionButton.setToolTipText("Add selected extension");
      }
    }
  }

  private VerticalPanel buildVerticalPanel(JList list, String name) {
    VerticalPanel panel = new VerticalPanel();
    panel.add(SwingUtils.buildScrollPanel(list, name, LIST_PREFERRED_SIZE), BorderLayout.CENTER);
    return panel;
  }

  public void updateDescriptionContent(String descriptionText) {
    descriptionTextPane.setText(descriptionText);
    descriptionTextPane.setCaretPosition(0);
    pack();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals(ADD) && availableList.getSelectedIndex() != -1) {
      ExtensionItem selectedValue = availableList.getSelectedValue();
      availableModel.remove(availableList.getSelectedIndex());
      activeModel.addElement(selectedValue);
      registry.addCustomExtension(selectedValue.getExtension());
      addExtensionButton.setEnabled(false);
    } else if (e.getActionCommand().equals(REMOVE)
        && activeList.getSelectedIndex() != -1) {
      ExtensionItem selectedValue = activeList.getSelectedValue();
      activeModel.remove(activeList.getSelectedIndex());
      availableModel.addElement(selectedValue);
      registry.removeCustomExtension(selectedValue.getExtension());
      removeExtensionButton.setEnabled(false);
    }

    activeList.setModel(activeModel);
    availableList.setModel(availableModel);

    updateComboOptions.run();
  }
  
  public void buildExtensions(Set<Class<? extends CorrelationRulePartTestElement>> usedExtensions,
      RulePartType type) {
    this.type = type;

    setTitle("Additional " + type + "s");
    updateInformativeLabel(type.toString());

    Set<CorrelationRulePartTestElement<?>> activeExtensions = registry
        .filterNonCustomExtensionsFrom(usedExtensions);

    activeModel = new DefaultListModel<>();
    activeExtensions.stream()
        .filter(c -> type == RulePartType.fromComponent(c))
        .forEach(e -> activeModel.addElement(new ExtensionItem(e, true)));

    getCustomExtensions().forEach(e -> {
      //Only show the ones that aren't "active" (loaded and being used by rules)
      if (activeExtensions.add(e)) {
        activeModel.addElement(new ExtensionItem(e, false));
      }
    });
    this.activeList.clearSelection();
    this.activeList.setModel(activeModel);

    availableModel = new DefaultListModel<>();
    registry.getAvailableExtensions().stream()
        .filter(c -> type == RulePartType.fromComponent(c))
        .forEach(e -> availableModel.addElement(new ExtensionItem(e, false)));

    this.availableList.clearSelection();
    this.availableList.setModel(availableModel);

    pack();
  }

  private List<CorrelationRulePartTestElement<?>> getCustomExtensions() {
    return type == RulePartType.EXTRACTOR ? registry.buildCustomExtractorRuleParts()
        : registry.buildCustomReplacementRuleParts();
  }

  public void updateInformativeLabel(String type) {
    informativeLabel.setText("<html> Here you can either add new " + type
        + " from the available list or remove from the active list. If an active " + type
        + " is being used by any Correlation Rule, you won't be able to remove it.  </html>");
  }

  private static class ExtensionItemRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
        boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      ExtensionItem item = (ExtensionItem) value;
      CorrelationRulePartTestElement<?> extension = item.getExtension();
      this.setText(CorrelationRulePartTestElement.getDisplayName(extension) + " " + (
          extension instanceof CorrelationExtractor
              ? RulePartType.EXTRACTOR : RulePartType.REPLACEMENT));
      this.setForeground(SwingUtils.getEnabledForegroundColor(!item.active));
      this.setToolTipText(item.isActive() ? "This extension is being used" : null);

      return this;
    }
  }

  protected static class ExtensionItem {

    private final CorrelationRulePartTestElement<?> extension;
    private final boolean active;

    public ExtensionItem(CorrelationRulePartTestElement<?> extension, boolean active) {
      this.extension = extension;
      this.active = active;
    }

    public CorrelationRulePartTestElement<?> getExtension() {
      return extension;
    }

    public boolean isActive() {
      return active;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ExtensionItem item = (ExtensionItem) o;
      return isActive() == item.isActive() &&
          Objects.equal(CorrelationRulePartTestElement.getDisplayName(getExtension()),
              CorrelationRulePartTestElement.getDisplayName(item.getExtension()));
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getExtension(), isActive());
    }

    @Override
    public String toString() {
      return "ExtensionItem{" +
          "extension=" + CorrelationRulePartTestElement.getDisplayName(extension) +
          ", active=" + active +
          '}';
    }
  }
}
