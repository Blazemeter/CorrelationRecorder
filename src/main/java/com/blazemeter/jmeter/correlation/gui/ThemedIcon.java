package com.blazemeter.jmeter.correlation.gui;

import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

public class ThemedIcon {

  private static Map<String, ImageIcon> cachedIcons = new WeakHashMap<>();

  public static ImageIcon fromResourceName(String resourceName) {
    String resourcePath = getThemePath() + "/" + resourceName;
    return cachedIcons
        .computeIfAbsent(resourcePath, p -> new ImageIcon(ThemedIcon.class.getResource(p)));
  }

  private static String getThemePath() {
    return "Darcula".equals(UIManager.getLookAndFeel().getID()) ? "/dark-theme" : "/light-theme";
  }
}
