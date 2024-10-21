package com.blazemeter.jmeter.correlation.core.suggestions;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public interface InterruptibleWorkerAgreement extends PropertyChangeListener {

  String ON_FAILURE_ENDED_PROPERTY = "END_BUSY_WAIT";

  default WindowAdapter getWindowAdapter() {
    return new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        onInterruption();
      }
    };
  }

  void onInterruption();

  @Override
  default void propertyChange(PropertyChangeEvent evt) {
    onWorkerPropertyChange(evt);
  }

  void onWorkerPropertyChange(PropertyChangeEvent evt);
}
