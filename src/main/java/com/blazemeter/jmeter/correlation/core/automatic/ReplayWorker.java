package com.blazemeter.jmeter.correlation.core.automatic;

import static org.slf4j.LoggerFactory.getLogger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingWorker;
import org.slf4j.Logger;

public class ReplayWorker extends SwingWorker<ReplayReport, Void> implements
    PropertyChangeListener {

  private static final Logger LOG = getLogger(ReplayWorker.class);
  public Runnable onDoneMethod;
  public Consumer<Exception> onFailureMethod;

  public Supplier<ReplayReport> methodToRun;

  public ReplayWorkerArrivalContext replayWorkerContext =
      ReplayWorkerArrivalContext.REPLAY_TEST_PLAN;
  public ReplayReport replayReport;

  public ReplayWorker() {
    super();
    addPropertyChangeListener(this);
  }

  @Override
  protected ReplayReport doInBackground() {
    try {
      // Maybe we should store this in a variable and return it when prompted?
      replayReport = getMethodToRun().get();
      return replayReport;
    } catch (Exception ex) {
      LOG.error("Error while processing replaying test plan.", ex);
      cancel(true);
      getOnFailureMethod().accept(ex);
      return null;
    } finally {
      LOG.info("Replay finished.");
    }
  }

  public Runnable getOnDoneMethod() {
    return onDoneMethod;
  }

  public void setOnDoneMethod(Runnable onDoneMethod) {
    this.onDoneMethod = onDoneMethod;
  }

  public Consumer<Exception> getOnFailureMethod() {
    return onFailureMethod;
  }

  public void setOnFailureMethod(Consumer<Exception> onFailureMethod) {
    this.onFailureMethod = onFailureMethod;
  }

  public Supplier<ReplayReport> getMethodToRun() {
    return methodToRun;
  }

  public void setMethodToRun(Supplier<ReplayReport> methodToRun) {
    this.methodToRun = methodToRun;
  }

  public ReplayReport getReplayReport() {
    return replayReport;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    String name = evt.getPropertyName();
    if ("state".equals(name)) {
      if (evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
        getOnDoneMethod().run();
      }
    }
  }

  public enum ReplayWorkerArrivalContext {
    REPLAY_TEST_PLAN, CORRELATION_METHOD
  }

  public ReplayWorkerArrivalContext getReplayWorkerArrivalContext() {
    return replayWorkerContext;
  }

  public void setReplayWorkerArrivalContext(ReplayWorkerArrivalContext replayWorkerContext) {
    this.replayWorkerContext = replayWorkerContext;
  }

}
