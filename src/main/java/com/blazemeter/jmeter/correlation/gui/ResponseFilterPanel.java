package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ResponseFilterPanel extends JPanel {

  private static final String INSTRUCTIONS_TEXT =
      "* This field allows to filter the responses by their MIME type (using regular expressions)."
          + " You can add more than one separating them using commas. If left empty, no "
          + "filtering will be applied.";
  private final JTextField field;
  private final JLabel notificationLabel;

  public ResponseFilterPanel() {
    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);
    JLabel label = buildResponseFilterLabel();
    field = buildField();
    notificationLabel = buildNotificationLabel();

    layout.setHorizontalGroup(layout.createParallelGroup()
        .addGroup(layout.createSequentialGroup()
            .addComponent(label)
            .addComponent(field))
        .addComponent(notificationLabel)
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(label)
            /*
            Since the Collapsible panel forces the sizes of other components when it expands,
            this filter needs to have a "max" height fixed.
            */
            .addComponent(field)
        )
        .addComponent(notificationLabel)
    );
  }

  private JLabel buildNotificationLabel() {
    final JLabel notificationLabel;
    notificationLabel = SwingUtils
        .createComponent("responseFilterNotification", new JLabel());
    notificationLabel.setText(INSTRUCTIONS_TEXT);
    return notificationLabel;
  }

  private JLabel buildResponseFilterLabel() {
    JLabel responseFilterLabel = SwingUtils
        .createComponent("responseFilterLabel", new JLabel("Response Filters: "));
    responseFilterLabel.setToolTipText("Response filter's regex");
    return responseFilterLabel;
  }

  private JTextField buildField() {
    final JTextField ret = SwingUtils
        .createComponent("responseFilterField", new JTextField());
    SwingUtils.setFieldMinimumAndPreferredColumns(ret, 5, 10);
    ret.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        removeRepeatedFromString(ret.getText());
      }
    });
    return ret;
  }

  private void removeRepeatedFromString(String text) {
    Set<String> removed = new HashSet<>();
    Set<String> filtered = new HashSet<>();
    for (String filter : text.split(",")) {
      String trimmedFilter = filter.trim();
      if (trimmedFilter.isEmpty()) {
        continue;
      }
      if (!filtered.add(trimmedFilter)) {
        removed.add(trimmedFilter);
      }
    }
    notificationLabel.setText(buildLabelText(removed));
    field.setText(filtered.stream().map(String::trim).collect(Collectors.joining(", ")));
  }

  private String buildLabelText(Set<String> removed) {
    String notificationText = removed.isEmpty() ? ""
        : "The following filters were repeated and removed: " + removed.stream().map(String::trim)
            .collect(Collectors.joining(", ")) + ".";
    return "<html> " + INSTRUCTIONS_TEXT + "<br>"
        + notificationText + "</html>";
  }

  public void setResponseFilter(String responseFilter) {
    field.setText(responseFilter);
  }

  public String getResponseFilter() {
    return field.getText();
  }

}
