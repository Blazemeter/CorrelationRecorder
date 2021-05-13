package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.gui.common.ThemedIcon;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlazemeterLabsLogo extends JLabel {

  private static final Logger LOG = LoggerFactory.getLogger(BlazemeterLabsLogo.class);
  private static final ImageIcon BLAZEMETER_LOGO = ThemedIcon
      .fromResourceName("blazemeter-labs-logo.png");

  public BlazemeterLabsLogo() {
    super(BLAZEMETER_LOGO);
    setBrowseOnClick("https://github.com/Blazemeter/CorrelationRecorder");
  }

  @Override
  public void paint(Graphics g) {
    setIcon(BLAZEMETER_LOGO);
    super.paint(g);
  }

  private void setBrowseOnClick(String url) {
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        if (Desktop.isDesktopSupported()) {
          try {
            Desktop.getDesktop().browse(new URI(url));
          } catch (IOException | URISyntaxException exception) {
            LOG.error("Problem when accessing repository", exception);
          }
        }
      }
    });
  }
}
