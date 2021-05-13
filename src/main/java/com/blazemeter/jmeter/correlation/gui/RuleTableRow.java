package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.gui.CorrelationRulePartPanel.Builder;
import com.blazemeter.jmeter.correlation.gui.common.PlaceHolderTextField;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class RuleTableRow implements Serializable {

  private static final String REFERENCE_VARIABLE_NAME = "RefVar";
  private static final String CORRELATION_REPLACEMENT_SUFFIX = "CorrelationReplacement";
  private static final String CORRELATION_EXTRACTOR_SUFFIX = "CorrelationExtractor";
  private final PlaceHolderTextField referenceVariableField;
  private String name;
  private boolean enabled = true;
  private boolean groupEnabled = true;
  private CorrelationRulePartPanel extractorPanel;
  private CorrelationRulePartPanel replacementPanel;
  private String oldRefVarName;

  public RuleTableRow(int index, Runnable update,
      Consumer<CorrelationRulePartTestElement<?>> displayExtensionConsumer,
      CorrelationComponentsRegistry registry) {
    //Needed for testing purposes
    String ruleNameId = "Rule-" + System.currentTimeMillis();
    setName(ruleNameId);
    referenceVariableField = buildReferenceVariableField(ruleNameId, index);
    oldRefVarName = referenceVariableField.getText();

    Builder rulePartPanelBuilder = new Builder()
        .withTableUpdateRunner(update)
        .withDisplayExtensionConsumer(displayExtensionConsumer)
        .withFieldsListener(() -> {
          registerIgnoreValueCheckboxStatusListeners();
          addAutoUpdateReplacementExpressionListener();
        });

    this.extractorPanel = rulePartPanelBuilder
        .withName(ruleNameId + "-" + CORRELATION_EXTRACTOR_SUFFIX)
        .withOptions(registry.buildActiveExtractorRulePart())
        .build();

    this.replacementPanel = rulePartPanelBuilder
        .withName(ruleNameId + "-" + CORRELATION_REPLACEMENT_SUFFIX)
        .withOptions(registry.buildActiveReplacementRulePart())
        .build();
  }

  private PlaceHolderTextField buildReferenceVariableField(String ruleNameId, int index) {
    final PlaceHolderTextField referenceVariableField;
    referenceVariableField = new PlaceHolderTextField();
    referenceVariableField.setName(ruleNameId + "-referenceVariable");
    referenceVariableField.setText("refVar" + index);
    referenceVariableField.setColumns(10);
    referenceVariableField.setToolTipText(RulesTableGui.VARIABLE_HEADER);
    referenceVariableField.setPlaceHolder(RulesTableGui.VARIABLE_HEADER);
    return referenceVariableField;
  }

  /* Will disable ignoreCheck on RegexReplacement when multiValued is enabled, or when
  regexExtractor match number is lower than zero */
  private void registerIgnoreValueCheckboxStatusListeners() {
    Optional<JCheckBox> foundReplacementIgnoreValue = findReplacementIgnoreValueCheck();
    if (!foundReplacementIgnoreValue.isPresent()) {
      return;
    }
    Optional<PlaceHolderTextField> foundRegexMatchNumber = findRegexMatchNumberField();
    Optional<JCheckBox> foundRegexMultivalued = findRegexMultiValuedCheck();
    if (!foundRegexMatchNumber.isPresent() || !foundRegexMultivalued.isPresent()) {
      return;
    }
    PlaceHolderTextField regexMatchNumberField = foundRegexMatchNumber.get();
    JCheckBox regexMultivaluedCheck = foundRegexMultivalued.get();
    JCheckBox replacementIgnoreValueCheck = foundReplacementIgnoreValue.get();
    Runnable updateIgnoreValue = () -> {
      if (regexMatchNumberField.getText().equals("-1") || regexMultivaluedCheck.isSelected()) {
        replacementIgnoreValueCheck.setEnabled(false);
        replacementIgnoreValueCheck.setSelected(false);
      } else {
        replacementIgnoreValueCheck.setEnabled(true);
      }
    };
    regexMatchNumberField.getDocument()
        .addDocumentListener(buildDocumentListenerWithAction(updateIgnoreValue));
    regexMultivaluedCheck.addActionListener(e -> updateIgnoreValue.run());
  }

  private Optional<JCheckBox> findReplacementIgnoreValueCheck() {
    return replacementPanel.retrieveComponent(JCheckBox.class,
        RegexCorrelationReplacement.REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME);
  }

  private Optional<PlaceHolderTextField> findRegexMatchNumberField() {
    return extractorPanel
        .retrieveComponent(PlaceHolderTextField.class, RegexCorrelationExtractor.MATCH_NUMBER_NAME);
  }

  private Optional<JCheckBox> findRegexMultiValuedCheck() {
    return extractorPanel
        .retrieveComponent(JCheckBox.class, RegexCorrelationExtractor.MULTIVALUED_NAME);
  }

  private static DocumentListener buildDocumentListenerWithAction(Runnable action) {
    return new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        // added in order to avoid an IllegalStateException
        SwingUtilities.invokeLater(action);
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        SwingUtilities.invokeLater(action);
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        SwingUtilities.invokeLater(action);
      }
    };
  }

  private void addAutoUpdateReplacementExpressionListener() {
    Optional<PlaceHolderTextField> foundReplacementString = findReplacementStringField();
    if (!foundReplacementString.isPresent()) {
      return;
    }
    PlaceHolderTextField replacementStringField = foundReplacementString.get();
    referenceVariableField.getDocument().addDocumentListener(
        buildDocumentListenerWithAction(() -> {
          replacementStringField.setText(VariableExpressionUpdater
              .applyAll(oldRefVarName, referenceVariableField.getText(),
                  replacementStringField.getText()));
          oldRefVarName = referenceVariableField.getText();
        })
    );
  }

  private Optional<PlaceHolderTextField> findReplacementStringField() {
    return replacementPanel.retrieveComponent(PlaceHolderTextField.class,
        RegexCorrelationReplacement.REPLACEMENT_STRING_PROPERTY_NAME);
  }

  private static class VariableExpressionUpdater {

    private static final List<VariableExpressionUpdater> UPDATERS = Arrays.asList(
        new VariableExpressionUpdater(Pattern.quote("${"), "(?:#\\d+)?(?:_\\d+)?}"),
        new VariableExpressionUpdater(Pattern.quote("vars.get(\""), "\"\\)"),
        new VariableExpressionUpdater(Pattern.quote("vars[") + "[\"|']", "[\"|']]"));

    private final String prefix;
    private final String suffix;

    private VariableExpressionUpdater(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
    }

    private static String applyAll(String oldRefVarName, String newRefVarName, String expression) {
      String ret = expression;
      for (VariableExpressionUpdater updater : UPDATERS) {
        ret = updater.apply(oldRefVarName, newRefVarName, ret);
      }
      return ret;
    }

    private String apply(String oldVarRef, String newVarRef, String expression) {
      return Pattern.compile("(" + prefix + ")" + Pattern.quote(oldVarRef) + "(" + suffix + ")")
          .matcher(expression)
          .replaceAll("$1" + newVarRef + "$2");
    }

  }

  public void setGroupEnabled(boolean groupEnabled) {
    this.groupEnabled = groupEnabled;
    updateChildrenEnableColors();
  }

  private void updateChildrenEnableColors() {
    boolean useEnableColors = this.enabled && groupEnabled;
    this.extractorPanel.useEnabledColors(useEnableColors);
    this.replacementPanel.useEnabledColors(useEnableColors);
    this.referenceVariableField
        .setForeground(SwingUtils.getEnabledForegroundColor(useEnableColors));
  }

  public JTextField getReferenceVariableField() {
    return referenceVariableField;
  }

  public String getVariableName() {
    return referenceVariableField.getText();
  }

  void setVariableName(String variableNameText) {
    this.referenceVariableField.setText(variableNameText);
  }

  protected CorrelationRulePartPanel getExtractorConfigurationPanel() {
    return extractorPanel;
  }

  protected void setExtractorConfigurationPanel(CorrelationRulePartPanel extractorPanel) {
    this.extractorPanel = extractorPanel;
  }

  protected CorrelationRulePartPanel getReplacementConfigurationPanel() {
    return replacementPanel;
  }

  protected void setReplacementConfigurationPanel(
      CorrelationRulePartPanel replacementPanel) {
    this.replacementPanel = replacementPanel;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CorrelationRule getCorrelationRule() {
    CorrelationRule correlationRule = new CorrelationRule();
    String referenceName = getReferenceVariableField().getText();
    correlationRule.setReferenceName(referenceName);
    correlationRule.setEnabled(isEnabled());

    CorrelationRulePartTestElement<?> selectedExtractor = getExtractorConfigurationPanel()
        .getSelectedItem();
    if (CorrelationComponentsRegistry.NONE_EXTRACTOR != selectedExtractor) {
      CorrelationExtractor<?> extractor = (CorrelationExtractor<?>) selectedExtractor;
      extractor.setVariableName(referenceName);
      extractor.setParams(getExtractorConfigurationPanel().getComponentsValues());
      correlationRule.setCorrelationExtractor(extractor);
    }

    CorrelationRulePartTestElement<?> selectedReplacement = getReplacementConfigurationPanel()
        .getSelectedItem();
    if (CorrelationComponentsRegistry.NONE_REPLACEMENT != selectedReplacement) {
      CorrelationReplacement<?> replacement = (CorrelationReplacement<?>) selectedReplacement;
      replacement.setVariableName(referenceName);
      replacement.setParams(getReplacementConfigurationPanel().getComponentsValues());
      correlationRule.setCorrelationReplacement(replacement);
    }

    return correlationRule;
  }

  public boolean isEnabled() {
    return enabled;
  }

  protected void setEnabled(boolean isEnable) {
    this.enabled = isEnable;
    updateChildrenEnableColors();
  }
  
  public int getNeededHeight() {
    return Math.max(extractorPanel.getCellNeededHeight(),
        replacementPanel.getCellNeededHeight());
  }

  @Override
  public String toString() {
    return "RuleConfiguration= {" + referenceVariableField.getText() +
        ", enabled = " + enabled +
        ", extractor " + extractorPanel.getSelectedItem().getDisplayName() +
        "=" + extractorPanel.getComponentsValues() +
        ", replacement " + CorrelationRulePartTestElement
        .getDisplayName(replacementPanel.getSelectedItem()) +
        "=" + replacementPanel.getComponentsValues() + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RuleTableRow rule = (RuleTableRow) o;
    if (getName().equals(rule.getName())) {
      return true;
    }

    //Need to validate every field in the Rule
    return isEnabled() == rule.isEnabled() &&
        getVariableName().equals(rule.getVariableName()) &&
        getReplacementConfigurationPanel().equals(rule.getReplacementConfigurationPanel())
        && getExtractorConfigurationPanel().equals(rule.getExtractorConfigurationPanel());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(isEnabled(), getVariableName(), getReplacementConfigurationPanel(),
        getExtractorConfigurationPanel());
  }

  @VisibleForTesting
  public void paintHandlers() {
    extractorPanel.addFields();
    replacementPanel.addFields();
  }

  public void setExtractorFromRulePart(CorrelationRulePartTestElement<?> rulePartTestElement) {
    if (rulePartTestElement != null) {
      getExtractorConfigurationPanel().setValuesFromRulePart(rulePartTestElement);
    }
  }

  public void setReplacementFromRulePart(CorrelationRulePartTestElement<?> rulePartTestElement) {
    if (rulePartTestElement != null) {
      getReplacementConfigurationPanel().setValuesFromRulePart(rulePartTestElement);
    }
  }
}
