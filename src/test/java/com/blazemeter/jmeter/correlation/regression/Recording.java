package com.blazemeter.jmeter.correlation.regression;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import com.google.common.base.Stopwatch;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.testelement.WorkBench;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.SearchByClass;

public class Recording implements Closeable {

  private static final int RECORDING_POLL_PERIOD_MILLIS = 3000;
  private static final int RECORDING_STOP_TIMEOUT_MILLIS = 30000;

  private final CorrelationProxyControl proxyControl;
  private final JMeterTreeModel treeModel;

  public Recording(CorrelationProxyControl proxyControl,
      JMeterTreeModel treeModel) {
    this.proxyControl = proxyControl;
    this.treeModel = treeModel;
  }

  public static Recording fromTemplate(Path templatePath)
      throws IOException, IllegalUserActionException {
    HashTree tree = SaveService.loadTree(templatePath.toFile());
    JMeter.convertSubTree(tree);
    JMeterTreeModel treeModel = buildTreeModel(tree);
    CorrelationProxyControl proxy = findTestElement(CorrelationProxyControl.class, tree);
    proxy.setNonGuiTreeModel(treeModel);
    proxy.startProxy();
    return new Recording(proxy, treeModel);
  }

  private static JMeterTreeModel buildTreeModel(HashTree tree) throws IllegalUserActionException {
    JMeterTreeModel treeModel = new JMeterTreeModel(new TestPlan(), new WorkBench());
    JMeterTreeNode root = (JMeterTreeNode) treeModel.getRoot();
    treeModel.addSubTree(tree, root);
    return treeModel;
  }

  private static <T> T findTestElement(Class<T> testElementClass, HashTree tree) {
    SearchByClass<T> proxyControlSearch = new SearchByClass<>(testElementClass);
    tree.traverse(proxyControlSearch);
    Iterator<T> resultsIterator = proxyControlSearch.getSearchResults().iterator();
    if (!resultsIterator.hasNext()) {
      throw new IllegalStateException(
          "Could not find a " + testElementClass.getName() + " in recording template");
    }
    return resultsIterator.next();
  }

  @Override
  public void close() {
    proxyControl.stopProxy();
  }

  public void saveRecordingTo(Path path)
      throws IOException, TimeoutException, InterruptedException {
    close();
    awaitRecordingEnd();
    SaveService
        .saveTree(convertSubTree(treeModel.getTestPlan()), new FileOutputStream(path.toFile()));
  }

  private void awaitRecordingEnd() throws TimeoutException, InterruptedException {
    Stopwatch awaitTime = Stopwatch.createStarted();
    HashTree currentRecording = (HashTree) treeModel.getTestPlan().clone();
    HashTree lastRecording;
    do {
      lastRecording = currentRecording;
      Thread.sleep(RECORDING_POLL_PERIOD_MILLIS);
      currentRecording = (HashTree) treeModel.getTestPlan().clone();
    } while (!currentRecording.equals(lastRecording)
        && awaitTime.elapsed(TimeUnit.MILLISECONDS) < RECORDING_STOP_TIMEOUT_MILLIS);
    if (awaitTime.elapsed(TimeUnit.MILLISECONDS) >= RECORDING_STOP_TIMEOUT_MILLIS) {
      throw new TimeoutException(
          "Timeout waiting for recording to stop adding items to test plan after "
              + RECORDING_STOP_TIMEOUT_MILLIS + "ms.");
    }
  }

  private HashTree convertSubTree(HashTree tree) {
    for (Object o : new LinkedList<>(tree.list())) {
      JMeterTreeNode item = (JMeterTreeNode) o;
      convertSubTree(tree.getTree(item));
      TestElement testElement = item.getTestElement();
      tree.replaceKey(item, testElement);
    }
    return tree;
  }

}
