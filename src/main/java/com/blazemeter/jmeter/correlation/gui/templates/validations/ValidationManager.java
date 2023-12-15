package com.blazemeter.jmeter.correlation.gui.templates.validations;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

public class ValidationManager {

  private final Set<Component> interactedFields = new HashSet<>();
  private final Map<Component, ComponentValidation<?>> validations = new HashMap<>();
  private final Map<Integer, Set<Component>> groups = new HashMap<>();
  private JButton saveButton;
  private Runnable afterValidation;

  public <T extends JTextComponent> void register(int group, T component, JLabel errorLabel,
                                                  List<Condition> validationRules) {

    addComponentToGroup(group, component);
    ComponentValidation<T> validation =
        new BaseValidation<>(component, errorLabel, validationRules);
    validations.put(component, validation);

    // If the field has a default value, assume the user has interacted with it.
    if (!component.getText().isEmpty()) {
      interactedFields.add(component);
    }

    component.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        interactedFields.add(component);
        validation.updateValidationStates();
        validation.applyFormat();

        updateSaveButtonState();
        triggerAfterValidation();
      }
    });
  }

  public void register(int group, JComboBox<?> comboBox, JLabel errorLabel,
                       List<Condition> validationRules) {
    addComponentToGroup(group, comboBox);
    JTextComponent editor = (JTextComponent) comboBox.getEditor().getEditorComponent();
    ComponentValidation<JTextComponent> validation =
        new BaseValidation<>(editor, errorLabel, validationRules);
    validations.put(editor, validation);

    // If the JComboBox has a default selection, assume the user has interacted with it.
    if (comboBox.getSelectedItem() != null) {
      interactedFields.add(editor);
    }

    editor.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        interactedFields.add(editor);
        validation.applyFormat();

        updateSaveButtonState();
        triggerAfterValidation();
      }
    });
  }

  private void addComponentToGroup(int group, Component component) {
    groups.putIfAbsent(group, new HashSet<>());
    groups.get(group).add(component);
  }

  private void triggerAfterValidation() {
    if (afterValidation != null) {
      afterValidation.run();
    }
  }

  private void updateSaveButtonState() {
    if (saveButton == null) {
      return;
    }
    boolean valid = isValid();
    boolean allValid = true;
    for (ComponentValidation<?> compValidation : validations.values()) {
      boolean componentInvalid = !compValidation.isValid();
      JTextComponent field = compValidation.getField();
      if (componentInvalid) {
        allValid = false;
        break;  // If any interacted field is invalid, we can stop checking.
      }
    }
    saveButton.setEnabled(allValid);
  }

  public boolean isValid() {
    for (int group : groups.keySet()) {
      if (!isValid(group)) {
        return false;
      }
    }
    return true;
  }

  public boolean isValid(int group) {
    if (!groups.containsKey(group)) {
      return false;
    }
    Set<Component> componentsGroup = groups.get(group);
    for (Map.Entry<Component, ComponentValidation<?>> entry : validations.entrySet()) {
      if (!componentsGroup.contains(entry.getKey())) {
        continue;
      }
      entry.getValue().updateValidationStates();
      if (!entry.getValue().isValid()) {
        return false;
      }
    }
    return true;
  }

  public boolean isErrorVisible(int group) {
    if (!groups.containsKey(group)) {
      return false;
    }
    Set<Component> componentsGroup = groups.get(group);
    for (Map.Entry<Component, ComponentValidation<?>> entry : validations.entrySet()) {
      if (!componentsGroup.contains(entry.getKey())) {
        continue;
      }
      if (entry.getValue().getErrorVisible()) {
        return true;
      }
    }
    return false;
  }

  public boolean validateAll() {
    for (ComponentValidation<?> validation : validations.values()) {
      validation.updateValidationStates();
      validation.applyFormat();
    }
    updateSaveButtonState();
    triggerAfterValidation();
    return isValid();
  }

  public JButton getSaveButton() {
    return saveButton;
  }

  public void setSaveButton(JButton saveButton) {
    this.saveButton = saveButton;
  }

  public void setAfterValidation(Runnable afterValidation) {
    this.afterValidation = afterValidation;
  }

  public void resetAll() {
    validations.values().forEach(ComponentValidation::reset);
    interactedFields.clear();
    if (saveButton != null) {
      saveButton.setEnabled(false);
    }
  }
}

