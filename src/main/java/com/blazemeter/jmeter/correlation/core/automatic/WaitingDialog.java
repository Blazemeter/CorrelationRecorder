package com.blazemeter.jmeter.correlation.core.automatic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class WaitingDialog {

  protected static JDialog runDialog;
  private static JLabel label;
  private static Window parent;

  private static JDialog makeWaitingFrame(String title, String message, Component component) {
    parent = getWindowFromComponent(component);
    JDialog runDialog = new JDialog(parent, title);
    runDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    runDialog.setResizable(true);
    runDialog.add(makeProgressBar(), BorderLayout.SOUTH);
    label = makeLabel(message);
    runDialog.add(label);
    runDialog.pack();
    if (parent != null) {
      runDialog.setLocationRelativeTo(parent);
    } else {
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      // calculate the new location of the window
      int x = (dim.width - runDialog.getSize().width) / 2;
      int y = (dim.height - runDialog.getSize().height) / 2;
      runDialog.setLocation(x, y);
    }
    runDialog.setAutoRequestFocus(true);
    return runDialog;
  }

  private static Window getWindowFromComponent(Component component) {
    if (component == null) {
      return null;
    }
    if (component.isVisible() && (component instanceof Frame || component instanceof Dialog)) {
      return (Window) component;
    }
    return getWindowFromComponent(component.getParent());
  }

  public static JProgressBar makeProgressBar() {
    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    return progressBar;
  }

  public static JLabel makeLabel(String message) {
    JLabel label = new JLabel(message, SwingConstants.CENTER);
    label.setBorder(new EmptyBorder(25, 50, 25, 50));
    return label;
  }

  public static void displayWaitingScreen(String title, String message, Component component) {
    disposeWaitingDialog();
    runDialog = makeWaitingFrame(title, message, component);
    runDialog.setVisible(true);
    runDialog.setAlwaysOnTop(true);
    runDialog.toFront();
  }

  public static void displayWaitingScreen(String title, String message) {
    displayWaitingScreen(title, message, null);
  }

  public static void disposeWaitingDialog() {
    if (runDialog != null) {
      runDialog.dispose();
      runDialog.setVisible(false);
      runDialog.setAlwaysOnTop(false);
    }
  }

  public static void addWindowAdapter(WindowAdapter adapter) {
    runDialog.addWindowListener(adapter);
  }

  public static void changeWaitingMessage(String message) {
    if (label != null && runDialog != null) {
      label.setText(message);
      runDialog.repaint();
      runDialog.pack();
      if (parent != null) {
        runDialog.setLocationRelativeTo(parent);
      }
    }
  }
}
