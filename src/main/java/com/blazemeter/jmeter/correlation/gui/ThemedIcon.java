package com.blazemeter.jmeter.correlation.gui;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import org.apache.jmeter.gui.action.LookAndFeelCommand;

public class ThemedIcon {

  private static final Map<String, ImageIcon> cachedIcons = new WeakHashMap<>();
  private static final Pattern darkThemePattern = Pattern
      .compile("Intellij|HighContrastLight|HighContrastDark|Darcula|Motif|OneDark|SolarizedDark");

  public static ImageIcon fromResourceName(String resourceName) {
    String resourcePath = getThemePath() + "/" + resourceName;
    return cachedIcons
        .computeIfAbsent(resourcePath, p -> new ImageIcon(ThemedIcon.class.getResource(p)));
  }

  private static String getThemePath() {
    return isDark() ? "/dark-theme" : "/light-theme";
  }

  private static boolean isDark() {
    return darkThemePattern.matcher(LookAndFeelCommand.getJMeterLaf()).find();
  }
}
