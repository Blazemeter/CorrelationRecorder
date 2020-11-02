package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationRule;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class RuleConfiguration implements Serializable {

  private static final String REFERENCE_VARIABLE_NAME = "RefVar";
  private static final String CORRELATION_REPLACEMENT_SUFFIX = "CorrelationReplacement";
  private static final String CORRELATION_EXTRACTOR_SUFFIX = "CorrelationExtractor";
  private static final Function<Boolean, Color> COLOR_PROVIDER = isEnable -> isEnable ?
      new JTextField().getForeground() : new JTextField().getDisabledTextColor();
  private final PlaceHolderTextField referenceVariableField;
  private final VariableExpressionUpdater variableExpressionUpdater =
      new VariableExpressionUpdater();
  private String name;
  private boolean enable;
  private ConfigurationPanel extractorConfigurationPanel;
  private ConfigurationPanel replacementConfigurationPanel;
  private String oldRefVarName;

  public RuleConfiguration(int index, Runnable update, CorrelationComponentsRegistry registry) {
    enable = true;
    //Needed for testing purposes
    String ruleNameId = "Rule-" + System.currentTimeMillis();
    setName(ruleNameId);
    referenceVariableField = new PlaceHolderTextField();
    referenceVariableField.setName(ruleNameId + "-referenceVariable");
    referenceVariableField.setText("refVar" + index);
    referenceVariableField.setPreferredSize(RulesContainer.FIELD_PREFERRED_SIZE);
    referenceVariableField.setToolTipText(RulesTable.VARIABLE_HEADER);
    referenceVariableField.setPlaceHolder(REFERENCE_VARIABLE_NAME);
    oldRefVarName = "refVar" + index;
    extractorConfigurationPanel = new ConfigurationPanel(update,
        ruleNameId + "-" + CORRELATION_EXTRACTOR_SUFFIX, registry.buildActiveExtractors());
    replacementConfigurationPanel = new ConfigurationPanel(update,
        ruleNameId + "-" + CORRELATION_REPLACEMENT_SUFFIX, registry.buildActiveReplacements());
    extractorConfigurationPanel.setUpdateFieldListeners(() -> setUpdateStateFieldListeners(update));
    replacementConfigurationPanel
        .setUpdateFieldListeners(() -> setUpdateStateFieldListeners(update));

  }

  private void setUpdateStateFieldListeners(Runnable guiUpdate) {
    registerIgnoreValueCheckboxStatusListeners(guiUpdate);
    setupDynamicFieldReplaceIfPresent(guiUpdate);
  }

  /* Will disable ignoreCheck on RegexReplacement when multiValued is enabled, or when 
  regexExtractor match number is lower than zero */
  private void registerIgnoreValueCheckboxStatusListeners(Runnable guiUpdate) {
    replacementConfigurationPanel
        .retrieveComponent(JCheckBox.class,
            RegexCorrelationReplacement.REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME)
        .ifPresent(ignoreCheckComponent -> {
          Optional<? extends JComponent> regexMatchNumber =
              extractorConfigurationPanel
                  .retrieveComponent(PlaceHolderTextField.class,
                      RegexCorrelationExtractor.MATCH_NUMBER_NAME);
          Optional<? extends JComponent> regexMultivalued =
              extractorConfigurationPanel
                  .retrieveComponent(JCheckBox.class, RegexCorrelationExtractor.MULTIVALUED_NAME);
          if (regexMatchNumber.isPresent() && regexMultivalued.isPresent()) {
            Consumer<Boolean> updateIgnoreValue = (condition) -> {
              if (condition) {
                ignoreCheckComponent.setEnabled(false);
                ((JCheckBox) ignoreCheckComponent).setSelected(false);
              } else {
                ignoreCheckComponent.setEnabled(true);
              }
              guiUpdate.run();
            };
            PlaceHolderTextField regexMatchNrField = (PlaceHolderTextField) regexMatchNumber.get();
            JCheckBox regexMultivaluedCheck = (JCheckBox) regexMultivalued.get();
            BooleanSupplier disableCondition = () ->
                regexMatchNrField.getText().equals("-1") || regexMultivaluedCheck.isSelected();
            regexMatchNrField.getDocument().addDocumentListener(
                buildDocumentListenerWithSequelAction(() -> updateIgnoreValue.accept(
                    disableCondition.getAsBoolean())));
            regexMultivaluedCheck.addActionListener(
                e -> updateIgnoreValue.accept(disableCondition.getAsBoolean()));
          }
        });
  }

  private void setupDynamicFieldReplaceIfPresent(Runnable guiUpdate) {
    replacementConfigurationPanel
        .retrieveComponent(PlaceHolderTextField.class,
            RegexCorrelationReplacement.REPLACEMENT_STRING_PROPERTY_NAME)
        .ifPresent(replacementStrField -> referenceVariableField.getDocument().addDocumentListener(
            buildDocumentListenerWithSequelAction(() -> {
              String replaceStringText = ((PlaceHolderTextField) replacementStrField).getText();
              variableExpressionUpdater.setVariableName(oldRefVarName);
              if (variableExpressionUpdater.anyMatch(replaceStringText)) {
                ((PlaceHolderTextField) replacementStrField)
                    .setText(variableExpressionUpdater
                        .buildReplaceExpressionWithNewRefVar(replaceStringText,
                            referenceVariableField.getText()));
              }
              oldRefVarName = referenceVariableField.getText();
              guiUpdate.run();
            })
        ));
  }

  private static DocumentListener buildDocumentListenerWithSequelAction(Runnable action) {
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

  public JTextField getReferenceVariableField() {
    return referenceVariableField;
  }

  public String getVariableName() {
    return referenceVariableField.getText();
  }

  protected ConfigurationPanel getExtractorConfigurationPanel() {
    return extractorConfigurationPanel;
  }

  protected void setExtractorConfigurationPanel(ConfigurationPanel extractorConfigurationPanel) {
    this.extractorConfigurationPanel = extractorConfigurationPanel;
  }

  protected ConfigurationPanel getReplacementConfigurationPanel() {
    return replacementConfigurationPanel;
  }

  protected void setReplacementConfigurationPanel(
      ConfigurationPanel replacementConfigurationPanel) {
    this.replacementConfigurationPanel = replacementConfigurationPanel;
  }

  void setVariableName(String variableNameText) {
    this.referenceVariableField.setText(variableNameText);
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
    correlationRule.setEnabled(isEnable());

    if (!getExtractorConfigurationPanel().isNoneSelected()) {
      CorrelationExtractor<?> selectedExtractor =
          (CorrelationExtractor<?>) getExtractorConfigurationPanel()
              .getSelectedItem();
      selectedExtractor.setVariableName(referenceName);
      selectedExtractor.setParams(getExtractorConfigurationPanel().getValues());
      correlationRule.setCorrelationExtractor(selectedExtractor);
    }

    if (!getReplacementConfigurationPanel().isNoneSelected()) {
      CorrelationReplacement<?> selectedReplacement =
          (CorrelationReplacement<?>) getReplacementConfigurationPanel()
              .getSelectedItem();
      selectedReplacement.setVariableName(referenceName);
      selectedReplacement.setParams(getReplacementConfigurationPanel().getValues());
      correlationRule.setCorrelationReplacement(selectedReplacement);
    }

    return correlationRule;
  }

  @Override
  public String toString() {
    return "RuleConfiguration= {" + referenceVariableField.getText() +
        ", enabled = " + enable + 
        ", extractor " + extractorConfigurationPanel.getSelectedItem().getDisplayName() +
        "=" + extractorConfigurationPanel.getValues() +
        ", replacement " + replacementConfigurationPanel.getSelectedItem().getDisplayName() +
        "=" + replacementConfigurationPanel.getValues() + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RuleConfiguration rule = (RuleConfiguration) o;
    if (getName().equals(rule.getName())) {
      return true;
    }

    //Need to validate every field in the Rule
    return isEnable() == rule.isEnable() &&
        getVariableName().equals(rule.getVariableName()) &&
        getReplacementConfigurationPanel().equals(rule.getReplacementConfigurationPanel())
        && getExtractorConfigurationPanel().equals(rule.getExtractorConfigurationPanel());
  }

  public void paintHandlers() {
    extractorConfigurationPanel.addFields();
    replacementConfigurationPanel.addFields();
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

  public boolean isEnable() {
    return enable;
  }

  protected void setEnable(boolean isEnable) {
    this.enable = isEnable;
    this.extractorConfigurationPanel.setEnabled(isEnable);
    this.replacementConfigurationPanel.setEnabled(isEnable);
    this.referenceVariableField.setForeground(COLOR_PROVIDER.apply(isEnable));
  }

  private static class VariableExpressionUpdater {

    private final List<Function<String, Pattern>> providers = new ArrayList<>();
    private String variableName;
    private static final String JMETER_VARIABLE_PREFIX = "${";
    private static final String GROOVY_LIST_VARIABLE_PREFIX = "vars.get(\"";
    private static final String GROOVY_ARRAY_VARIABLE_PREFIX = "vars[";

    public VariableExpressionUpdater() {
      providers.addAll(Arrays.asList(
          (var) -> Pattern
              .compile("(" + Pattern.quote(JMETER_VARIABLE_PREFIX) + ")" + Pattern.quote(var) +
                  "((?:#\\d+)?(?:_\\d+)?})"),
          (var) -> Pattern
              .compile("(" + Pattern.quote(GROOVY_LIST_VARIABLE_PREFIX) + ")" + Pattern.quote(var) +
                  "(\"\\))"),
          (var) -> Pattern
              .compile(
                  "(" + Pattern.quote(GROOVY_ARRAY_VARIABLE_PREFIX) + "[\"|'])" + Pattern.quote(var)
                      + "([\"|']])")));
    }

    private void setVariableName(String variableName) {
      this.variableName = variableName;
    }

    private boolean anyMatch(String text) {
      return providers.stream()
          .anyMatch(p -> p.apply(variableName).matcher(text).find());
    }

    private String getStringMatch(String text) {
      return providers.stream()
          .filter(p -> p.apply(variableName).matcher(text).find())
          .map(p -> {
            Matcher matcher = p.apply(variableName).matcher(text);
            if (matcher.find()) {
              return matcher.group(0);
            }
            /* Won't return null, since the matcher stream that reaches
             this point matcher will have at least a match. */
            return null;
          })
          .findFirst()
          // Should never reach this exception, since anyMatch should be called first
          .orElseThrow(() -> new IllegalArgumentException("Could not find match"));
    }

    private Matcher getMatcher(String text) {
      return providers.stream()
          .filter(p -> p.apply(variableName).matcher(text).find())
          .map(p -> p.apply(variableName).matcher(text))
          .findAny()
          //Should never reach this exception , since anyMatch should be called first
          .orElseThrow(() -> new IllegalArgumentException("Could not find matcher"));
    }

    public String buildReplaceExpressionWithNewRefVar(String replaceStringText,
        String currentRefVar) {
      String matcher = getStringMatch(replaceStringText);
      Matcher replacementMatcher = getMatcher(replaceStringText);
      if (variableName.isEmpty()) {
        String replacement = buildReplacementForEmptyRefVar(currentRefVar, matcher);
        return replaceStringText.replace(matcher, replacement);
      } else {
        return replacementMatcher.replaceAll("$1" + currentRefVar + "$2");
      }
    }

    private String buildReplacementForEmptyRefVar(String currentRefVar, String matcher) {
      Matcher prefixMatcher =
          Pattern.compile(Pattern.quote(JMETER_VARIABLE_PREFIX) + "|" + Pattern
              .quote(GROOVY_ARRAY_VARIABLE_PREFIX) + "|" + Pattern
              .quote(GROOVY_LIST_VARIABLE_PREFIX)).matcher(matcher);

      if (prefixMatcher.find()) {
        switch (prefixMatcher.group(0)) {
          case JMETER_VARIABLE_PREFIX:
            return matcher.replace(JMETER_VARIABLE_PREFIX, JMETER_VARIABLE_PREFIX + currentRefVar);

          case GROOVY_ARRAY_VARIABLE_PREFIX:
            char quoteType = matcher.charAt(GROOVY_ARRAY_VARIABLE_PREFIX.length()) == '\'' ?
                '\'' : '\"';
            return matcher.replace(GROOVY_ARRAY_VARIABLE_PREFIX + quoteType,
                GROOVY_ARRAY_VARIABLE_PREFIX + quoteType + currentRefVar);

          case GROOVY_LIST_VARIABLE_PREFIX:
            return matcher.replace(GROOVY_LIST_VARIABLE_PREFIX,
                GROOVY_LIST_VARIABLE_PREFIX + currentRefVar);
        }
      }
      return matcher;
    }
  }
}
