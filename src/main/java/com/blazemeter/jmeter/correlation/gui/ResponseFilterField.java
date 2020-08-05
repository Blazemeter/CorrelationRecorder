package com.blazemeter.jmeter.correlation.gui;

import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JTextField;

public class ResponseFilterField extends JTextField {

  private Consumer<String> filteringHandler;

  public ResponseFilterField(String name, Dimension preferredSize,
      Consumer<String> filteringHandler) {
    setName(name);
    setPreferredSize(preferredSize);
    addFocusListener(makeLostFocusListener());
    this.filteringHandler = filteringHandler;
  }

  private FocusAdapter makeLostFocusListener() {
    return new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        removeRepeatedFromString(getText());
      }
    };
  }

  private void removeRepeatedFromString(String text) {
    Set<String> removed = new HashSet<>();
    Set<String> filtered = new HashSet<>();
    for (String filter : text.split(",")) {
      if (filter.trim().isEmpty()) {
        continue;
      }

      if (!filtered.add(filter.trim())) {
        removed.add(filter.trim());
      }
    }
    filteringHandler.accept(removed.isEmpty() ? ""
        : "The following filters were repeated and removed: " + removed.stream().map(String::trim)
            .collect(Collectors.joining(", ")) + ".");
    setText(filtered.stream().map(String::trim).collect(Collectors.joining(", ")));
  }
}
