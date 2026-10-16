package com.blazemeter.jmeter.correlation.gui.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows whether the JMeter properties that the correlation needs have their expected values: a
 * check for each one that has it, a warning with its effect for each one that does not, and an
 * action that sets them in user.properties and restarts JMeter.
 */
public class RecommendedJMeterPropertiesDialog extends JDialog {

  private static final Logger LOG =
      LoggerFactory.getLogger(RecommendedJMeterPropertiesDialog.class);
  private static final String TITLE = "BlazeMeter Auto Correlation Recorder - JMeter Properties";
  private static final Color VALID_COLOR = new Color(0x2E, 0x9E, 0x4F);
  private static final Color WARNING_COLOR = new Color(0xE0, 0x9B, 0x1A);

  /**
   * What applying does, so tests can replace the confirmation, the file write and the restart.
   */
  public interface Applier {

    boolean confirm(Component parent, List<JMeterPropertyRequirement> unmet);

    void apply(Component parent, List<JMeterPropertyRequirement> unmet);
  }

  private final List<JMeterPropertyRequirement> requirements;
  private final Applier applier;

  public RecommendedJMeterPropertiesDialog(Window owner) {
    this(owner, RecommendedJMeterProperties.REQUIREMENTS, new UserPropertiesApplier());
  }

  public RecommendedJMeterPropertiesDialog(Window owner,
      List<JMeterPropertyRequirement> requirements, Applier applier) {
    super(owner, TITLE, ModalityType.APPLICATION_MODAL);
    this.requirements = requirements;
    this.applier = applier;
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setContentPane(buildContent());
    pack();
    setMinimumSize(new Dimension(560, getHeight()));
    setLocationRelativeTo(owner);
  }

  private JPanel buildContent() {
    List<JMeterPropertyRequirement> unmet = unmet();
    JPanel content = new JPanel(new BorderLayout(0, 12));
    content.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));

    JLabel summary = new JLabel(unmet.isEmpty()
        ? "All JMeter properties have the values the correlation needs."
        : unmet.size() + " of " + requirements.size() + " JMeter properties do not have the "
            + "value the correlation needs. Recording and replaying can differ from what the "
            + "correlation expects.");
    summary.setName("summary");
    content.add(wrap(summary), BorderLayout.NORTH);

    JPanel rows = new JPanel(new GridBagLayout());
    int row = 0;
    for (JMeterPropertyRequirement requirement : requirements) {
      row = addRow(rows, row, requirement);
    }
    content.add(rows, BorderLayout.CENTER);

    JButton apply = new JButton("Apply and Restart JMeter");
    apply.setName("applyButton");
    apply.setEnabled(!unmet.isEmpty());
    apply.addActionListener(e -> {
      List<JMeterPropertyRequirement> toSet = unmet();
      if (applier.confirm(this, toSet)) {
        dispose();
        applier.apply(getOwner(), toSet);
      }
    });
    JButton close = new JButton("Close");
    close.setName("closeButton");
    close.addActionListener(e -> dispose());
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    buttons.add(apply);
    buttons.add(close);
    content.add(buttons, BorderLayout.SOUTH);
    return content;
  }

  private int addRow(JPanel rows, int row, JMeterPropertyRequirement requirement) {
    boolean met = requirement.isMet();
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = row;
    c.anchor = GridBagConstraints.WEST;
    c.insets = new Insets(6, 0, 0, 10);

    JLabel status = new JLabel(new StatusIcon(met));
    status.setName("status." + requirement.getName());
    status.setToolTipText(met ? "Has the expected value" : "Does not have the expected value");
    c.gridx = 0;
    rows.add(status, c);

    JLabel name = new JLabel(requirement.getName());
    name.setFont(name.getFont().deriveFont(Font.BOLD));
    c.gridx = 1;
    rows.add(name, c);

    JLabel current = new JLabel("Current: " + requirement.describeEffectiveValue());
    current.setName("current." + requirement.getName());
    c.gridx = 2;
    rows.add(current, c);

    JLabel expected = new JLabel("Expected: " + requirement.getExpectedValue());
    c.gridx = 3;
    c.weightx = 1;
    rows.add(expected, c);

    if (met) {
      return row + 1;
    }
    // HTML with a fixed width wraps the text and reports its real height, so the dialog fits it
    JLabel effect = new JLabel("<html><body style='width: 420px'>"
        + requirement.getEffectWhenNotExpected() + "</body></html>");
    effect.setName("effect." + requirement.getName());
    GridBagConstraints e = new GridBagConstraints();
    e.gridy = row + 1;
    e.gridx = 1;
    e.gridwidth = 3;
    e.fill = GridBagConstraints.HORIZONTAL;
    e.insets = new Insets(2, 0, 4, 0);
    rows.add(effect, e);
    return row + 2;
  }

  private List<JMeterPropertyRequirement> unmet() {
    return requirements.stream().filter(r -> !r.isMet()).collect(Collectors.toList());
  }

  private static JPanel wrap(JLabel label) {
    // a label in HTML wraps inside the dialog's width instead of widening it
    label.setText("<html><body style='width: 460px'>" + label.getText() + "</body></html>");
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(label, BorderLayout.CENTER);
    return panel;
  }

  /**
   * A check for a valid property and a warning triangle for one that is not, drawn so they look
   * the same in every look and feel.
   */
  private static final class StatusIcon implements Icon {

    private static final int SIZE = 16;
    private final boolean valid;

    private StatusIcon(boolean valid) {
      this.valid = valid;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.translate(x, y);
      if (valid) {
        g2.setColor(VALID_COLOR);
        g2.fillOval(0, 0, SIZE, SIZE);
        g2.setColor(Color.WHITE);
        g2.setStroke(new java.awt.BasicStroke(2f));
        g2.drawPolyline(new int[] {4, 7, 12}, new int[] {8, 11, 5}, 3);
      } else {
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(SIZE / 2.0, 0);
        triangle.lineTo(SIZE, SIZE);
        triangle.lineTo(0, SIZE);
        triangle.closePath();
        g2.setColor(WARNING_COLOR);
        g2.fill(triangle);
        g2.setColor(Color.BLACK);
        g2.fillRect(SIZE / 2 - 1, 5, 2, 6);
        g2.fillRect(SIZE / 2 - 1, 12, 2, 2);
      }
      g2.dispose();
    }

    @Override
    public int getIconWidth() {
      return SIZE;
    }

    @Override
    public int getIconHeight() {
      return SIZE;
    }
  }

  /**
   * Asks before replacing the previous values, writes user.properties and restarts JMeter.
   */
  public static class UserPropertiesApplier implements Applier {

    @Override
    public boolean confirm(Component parent, List<JMeterPropertyRequirement> unmet) {
      StringBuilder message = new StringBuilder("These properties will be set in ")
          .append(RecommendedJMeterProperties.userPropertiesFile().getAbsolutePath())
          .append(":\n\n");
      for (JMeterPropertyRequirement requirement : unmet) {
        message.append("  ").append(requirement.getName()).append(": ")
            .append(requirement.describeEffectiveValue()).append(" -> ")
            .append(requirement.getExpectedValue()).append('\n');
      }
      message.append("\nTheir previous values in that file will be lost.\n")
          .append("JMeter will restart to load them; you will be asked to save a modified "
              + "test plan.");
      return JOptionPane.showConfirmDialog(parent, message.toString(), TITLE,
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    @Override
    public void apply(Component parent, List<JMeterPropertyRequirement> unmet) {
      try {
        RecommendedJMeterProperties.persist(RecommendedJMeterProperties.userPropertiesFile(),
            unmet);
      } catch (IOException e) {
        LOG.error("Could not write the JMeter properties", e);
        JOptionPane.showMessageDialog(parent, "The properties could not be written: "
            + e.getMessage() + "\nNothing was changed.", TITLE, JOptionPane.ERROR_MESSAGE);
        return;
      }
      RecommendedJMeterProperties.applyToRuntime(unmet);
      try {
        JMeterRestart.restart();
        // restart returns only when the exit was cancelled
        JOptionPane.showMessageDialog(parent, "The properties were saved. JMeter reads them "
            + "when it starts: restart JMeter to load them.", TITLE,
            JOptionPane.INFORMATION_MESSAGE);
      } catch (IOException | RuntimeException e) {
        LOG.error("Could not restart JMeter", e);
        JOptionPane.showMessageDialog(parent, "The properties were saved, but JMeter could not "
                + "restart: " + e.getMessage() + "\nRestart JMeter to load them.", TITLE,
            JOptionPane.WARNING_MESSAGE);
      }
    }
  }
}
