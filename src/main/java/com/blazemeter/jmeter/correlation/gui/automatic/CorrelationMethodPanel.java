package com.blazemeter.jmeter.correlation.gui.automatic;

import com.blazemeter.jmeter.commons.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Contains the list with the possible correlation methods: 1. By Correlation Rules 2. By Comparison
 * Within the methods, this should allow to get the selection done by the user.
 */

/**
 * This class will allow the user to select which kind of Auto Correlation Method to use.
 * It will display a list of the available methods, and allow the user to select one.
 * After, the user will select Continue and one of two things will happen:
 * 1. If the method selected is "Correlate by Replay" the triggered method will be
 * CorrelationSuggestionsDialog#replayRecordingAndGenerateSuggestions()
 * 2. If the method selected is "Correlate by Rules" the triggered method will be
 */
public class CorrelationMethodPanel extends WizardStepPanel implements ActionListener {

  private static final long serialVersionUID = 1L;
  private static final String CONTINUE = "continue";

  private Checkbox correlateByRules;

  public CorrelationMethodPanel(CorrelationWizard wizard) {
    super(wizard);
    init();
  }

  private void init() {
    String informationMessage =
        "Select a method for correlation";
    JLabel correlationMethodsLabel = new JLabel(informationMessage);
    JPanel methodsPanel = buildMethodsPanel();
    JPanel buttonPanel = buildButtonPanel();
    setBorder(new EmptyBorder(5, 15, 5, 15));
    setLayout(new BorderLayout());
    add(correlationMethodsLabel, BorderLayout.NORTH);
    add(methodsPanel, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  private JPanel buildMethodsPanel() {
    JPanel methodsPanel = new JPanel();
    methodsPanel.setLayout(new GridLayout(0, 1));
    methodsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
    CheckboxGroup methodGroup = new CheckboxGroup();
    correlateByRules = new Checkbox("Existing Correlation Templates", methodGroup,
        true);
    methodsPanel.add(correlateByRules);
    methodsPanel.add(new Checkbox("Automatic comparison and variable detection",
        methodGroup, false));
    methodsPanel.setMaximumSize(new Dimension(500, 300));
    return methodsPanel;
  }

  private JPanel buildButtonPanel() {
    JPanel buttonPanel = new JPanel();
    SwingUtils.ButtonBuilder builder = new SwingUtils.ButtonBuilder()
        .withActionListener(this);

    JButton continueButton = builder.withText("Continue")
        .withAction(CONTINUE)
        .withName("continueButton")
        .build();

    buttonPanel.add(continueButton);
    return buttonPanel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    switch (e.getActionCommand()) {
      case CONTINUE:
        continueToNextStep();
        break;
      default:
        throw new IllegalArgumentException("Unknown action: " + e.getActionCommand());
    }
  }

  private void continueToNextStep() {
    if (correlateByRules.getState()) {
      displayTemplateSelectionPanel();
    } else {
      displaySuggestionsPanel();
    }
  }
}
