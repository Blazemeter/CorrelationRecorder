package com.blazemeter.jmeter.correlation.gui.templates;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

public class UpdateRepositoriesWorker extends SwingWorker {

  JDialog dialog = null;
  JProgressBar progressBar = null;

  JLabel label = null;

  protected UpdateRepositoriesWorker() {
    this.addPropertyChangeListener(evt -> {
      if (evt.getPropertyName().equals("progress")) {
        progressBar.setValue(getProgress());
      } else if (evt.getPropertyName().equals("state")) {
        if (evt.getNewValue() == StateValue.STARTED) {
          dialog = new JDialog();
          dialog.setUndecorated(true);
          dialog.getRootPane().setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
          dialog.setResizable(false);

          progressBar = new JProgressBar();
          progressBar.setIndeterminate(false);
          JPanel panel = new JPanel(new BorderLayout());
          panel.add(progressBar, BorderLayout.CENTER);
          label = new JLabel("Updating catalog...", SwingConstants.CENTER);
          label.setBorder(new EmptyBorder(25, 50, 25, 50));
          panel.add(label, BorderLayout.PAGE_START);
          dialog.add(panel);
          dialog.pack();
          dialog.setSize(320, 80);

          Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
          // calculate the new location of the window
          int x = (dim.width - dialog.getSize().width) / 2;
          int y = (dim.height - dialog.getSize().height) / 2;
          dialog.setLocation(x, y);
          this.dialog.setAlwaysOnTop(true);

          dialog.setVisible(true);

        } else if (evt.getNewValue() == StateValue.DONE) {
          dialog.dispose();
        }
      }
    });
  }

  @Override
  protected Object doInBackground() throws Exception {
    return null;
  }

  @Override
  protected void process(List chunks) {
    for (Object chunk : chunks) {
      if (isCancelled()) {
        break;
      }
      label.setText((String) chunk);
    }
  }

}
