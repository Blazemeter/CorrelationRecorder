package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.ArrayList;
import java.util.List;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.visualizers.SummaryReport;
import org.apache.jmeter.visualizers.ViewResultsFullVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomResultCollector extends ResultCollector {
  private static final Logger LOG = LoggerFactory.getLogger(CustomResultCollector.class);
  private final CustomSummariser summariser;

  public CustomResultCollector() {
    this(new CustomSummariser("Automatic Replay Summariser"));
  }

  public CustomResultCollector(CustomSummariser summariser) {
    super(summariser);
    this.summariser = summariser;
    setProperty(TestElement.NAME, "Result Collector - Custom Made");
    setProperty(TestElement.GUI_CLASS, ViewResultsFullVisualizer.class.getName());
    setProperty(TestElement.TEST_CLASS, ResultCollector.class.getName());
    SampleSaveConfiguration saveConfig = new SampleSaveConfiguration(true);
    setSaveConfig(saveConfig);
  }

  public boolean hasErrors() {
    return !summariser.getErrors().isEmpty();
  }

  public List<SampleResult> getResults() {
    return summariser.getResults();
  }

  public List<SampleResult> getErrors() {
    return summariser.getErrors();
  }

  public int getErrorsCount() {
    return summariser.getErrors().size();
  }

  private static class CustomSummariser extends Summariser {
    private final List<SampleResult> results = new ArrayList<>();
    private final List<SampleResult> errors = new ArrayList<>();

    CustomSummariser(String name) {
      super(name);
      setProperty(TestElement.GUI_CLASS, SummaryReport.class.getName());
      setProperty(TestElement.TEST_CLASS, ResultCollector.class.getName());
    }

    public List<SampleResult> getResults() {
      return results;
    }

    public List<SampleResult> getErrors() {
      return errors;
    }

    @Override
    public void testStarted(String host) {
      super.testStarted(host);
      results.clear();
      errors.clear();
    }

    @Override
    public void sampleStarted(SampleEvent e) {
      super.sampleStarted(e);
    }

    @Override
    public void sampleStopped(SampleEvent e) {
      super.sampleStopped(e);
    }

    @Override
    public void sampleOccurred(SampleEvent e) {
      super.sampleOccurred(e);
      SampleResult result = e.getResult();
      System.out.println(results.size() + " ) "
          + (result.isSuccessful() ? "(S)" : "(F)")
          + " - Result: " + result.getSampleLabel());
      results.add(result);
      String replayStatus = "Success";
      if (!result.isSuccessful()) {
        errors.add(result);
        replayStatus = "Failed";
      }

      LOG.info("TestPlan Replay: {}: {}", replayStatus, result.getSampleLabel());
    }
  }
}
