package com.blazemeter.jmeter.correlation.gui.common;

import com.blazemeter.jmeter.correlation.core.templates.Template;
import java.util.List;
import java.util.stream.Collectors;

public class TemplateVersionUtils {

  public static String getInformationAsHTLM(Template template, boolean showInstalled,
                                            boolean canUse, String repositoryName) {
    StringBuilder content = new StringBuilder();
    content.append("<h2>");
    content.append(template.getId()).append(" (").append(repositoryName).append(")");
    content.append(" v").append(template.getVersion());

    if (template.isInstalled() && showInstalled) {
      content.append(makeItalic(" (Installed)"));
    }
    content.append("</h2>");
    if (isNotEmpty(template.getAuthor())) {
      content.append("Made by " + makeItalic(template.getAuthor()));
      content.append("<br>");
    }

    content.append(makeParagraph(template.getDescription()));

    if (isNotEmpty(template.getChanges())) {
      content.append(makeParagraph("Recent changes: <br> " + template.getChanges()));
    }

    if (isNotEmpty(template.getDependencies())) {
      content.append(makeParagraph("Dependencies: "));
      content.append("<pre> [");
      content.append(template.getDependencies().stream()
          .map(d -> d.getName() + ">=" + d.getVersion()).collect(Collectors.joining(",")));
      content.append("]</pre>");
    }

    if (isNotEmpty(template.getUrl())) {
      content.append(makeParagraph("For more information visit <br> " + template.getUrl()));
    }

    if (template.getSnapshot() != null) {
      content.append(makeParagraph(makeImage(template.getSnapshotPath())));
    }

    return content.toString();
  }

  private static boolean isNotEmpty(String text) {
    return text != null && !text.isEmpty();
  }

  private static boolean isNotEmpty(List listText) {
    return listText != null && !listText.isEmpty();
  }

  private static void addFieldContent(StringBuilder content, String fieldText, String fieldHeader) {
    if (fieldText != null && !fieldText.isEmpty()) {
      content.append(makeParagraph(fieldHeader))
          .append(makeParagraph(fieldText));
    }
  }

  private static String makeHeader(String text) {
    return makeParagraph(makeBold(text));
  }

  private static String makeParagraph(String text) {
    return "<p>" + text + "</p>";
  }

  private static String makeItalic(String text) {
    return "<i>" + text + "</i>";
  }

  private static String makeBold(String text) {
    return "<b>" + text + "</b>";
  }

  private static String makeImage(String path) {
    return "<img src='file:" + path + "'/>";
  }
}
