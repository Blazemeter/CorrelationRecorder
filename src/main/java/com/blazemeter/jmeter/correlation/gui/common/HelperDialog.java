package com.blazemeter.jmeter.correlation.gui.common;

import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelperDialog extends JDialog {

  private static final Logger LOG = LoggerFactory.getLogger(HelperDialog.class);
  private JTextPane descriptionTextPane;

  public HelperDialog(Container parent) {
    setLocationRelativeTo(parent);
    setAlwaysOnTop(true);
    prepareHelper();
  }

  public HelperDialog(JDialog owner) {
    super(owner, true);
    setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
    setLocationRelativeTo(owner.getContentPane());
    setAlwaysOnTop(true);
    prepareHelper();
  }

  private void prepareHelper() {
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    prepareContainer();
    updateDialogContent("Default Content");
  }

  private void prepareContainer() {
    descriptionTextPane = new JTextPane();
    descriptionTextPane.setEditable(false);
    descriptionTextPane.setContentType("text/html");
    descriptionTextPane.setName("displayInfoPanel");
    descriptionTextPane.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && Desktop.isDesktopSupported()) {
        try {
          Desktop.getDesktop().browse(e.getURL().toURI());
        } catch (IOException | URISyntaxException ex) {
          LOG.error("There was an issue trying to open the url {}", e.getURL(), ex);
        }
      }
    });

    JPanel descriptionContainer = new JPanel();
    descriptionContainer.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
    descriptionContainer.add(descriptionTextPane);
    JScrollPane descriptionScroll = new JScrollPane(descriptionContainer,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    descriptionScroll.setPreferredSize(new Dimension(920, 400));
    add(descriptionScroll);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowDeactivated(WindowEvent e) {
        //Using this event since the windowsLostFocus never triggers
        if (e.getOppositeWindow() != null || !((JDialog) e.getSource()).isFocused()) {
          HelperDialog.this.dispose();
        }
        super.windowDeactivated(e);
      }

      @Override
      public void windowLostFocus(WindowEvent e) {
        if (e.getSource() instanceof JDialog && !((JDialog) e.getSource()).isFocused()) {
          setVisible(false);
        }
        super.windowLostFocus(e);
      }
    });
  }

  public void updateDialogContent(String descriptionText) {
    descriptionTextPane.setText(descriptionText);
    descriptionTextPane.setCaretPosition(0);
    pack();
  }

  @VisibleForTesting
  public String getDisplayedText() {
    return descriptionTextPane.getText();
  }
}
