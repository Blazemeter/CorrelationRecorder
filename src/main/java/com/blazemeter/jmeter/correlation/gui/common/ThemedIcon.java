package com.blazemeter.jmeter.correlation.gui.common;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import org.apache.jmeter.gui.action.LookAndFeelCommand;

public class ThemedIcon {

  private static final Map<String, ImageIcon> CACHED_ICONS = new WeakHashMap<>();
  private static final Pattern DARK_THEME_PATTERN = Pattern
      .compile("Intellij|HighContrastLight|HighContrastDark|Darcula|Motif|OneDark|SolarizedDark");

  public static ImageIcon fromResourceName(String resourceName) {
    String resourcePath = getThemePath() + "/" + resourceName;
    return CACHED_ICONS
        .computeIfAbsent(resourcePath, p -> new ImageIcon(ThemedIcon.class.getResource(p)));
  }

  private static String getThemePath() {
    return isDark() ? "/dark-theme" : "/light-theme";
  }

  private static boolean isDark() {
    return DARK_THEME_PATTERN.matcher(LookAndFeelCommand.getJMeterLaf()).find();
  }
}
