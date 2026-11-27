package com.blazemeter.jmeter.correlation.gui.automatic;

import com.blazemeter.jmeter.commons.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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

  private JRadioButton correlateByRules;

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
    // Swing radio buttons, not java.awt.Checkbox: heavyweight AWT components ignore the look and
    // feel and get cut out by any lightweight component layered above them.
    correlateByRules = new JRadioButton("Existing Correlation Templates", true);
    correlateByRules.setName("correlateByRules");
    JRadioButton correlateByComparison =
        new JRadioButton("Automatic comparison and variable detection", false);
    correlateByComparison.setName("correlateByComparison");
    ButtonGroup methodGroup = new ButtonGroup();
    methodGroup.add(correlateByRules);
    methodGroup.add(correlateByComparison);
    methodsPanel.add(correlateByRules);
    methodsPanel.add(correlateByComparison);
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
    this.getWizard().setVisible(false);
    if (correlateByRules.isSelected()) {
      displayTemplateSelectionPanel();
    } else {
      displaySuggestionsPanel();
    }
  }
}
