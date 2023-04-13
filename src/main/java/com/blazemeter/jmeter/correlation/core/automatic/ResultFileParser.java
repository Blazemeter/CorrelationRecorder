package com.blazemeter.jmeter.correlation.core.automatic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.visualizers.Visualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultFileParser {
  private static final Logger LOG = LoggerFactory.getLogger(ResultFileParser.class);
  private Configuration configuration = new Configuration();

  public ResultFileParser() {
  }

  public ResultFileParser(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Loads a list of SampleResults from a jtl file. If shouldFilter is true, the results will be
   * filtered using the provided configuration.
   *
   * @param file         the file to load the results from
   * @param shouldFilter whether the results should be filtered or not
   * @return the results loaded from the file
   */
  public List<SampleResult> loadFromFile(File file, boolean shouldFilter) {
    try {
      Collection<SampleResult> results = new ArrayList<>();
      ResultCollector collector = new ResultCollector();
      collector.setFilename(file.getAbsolutePath());
      JMeterElementUtils utils = new JMeterElementUtils(configuration);
      collector.setListener(new Visualizer() {
        public void add(SampleResult sample) {
          if (shouldFilter && utils.canBeFiltered(() -> sample)) {
            return;
          }

          HTTPSampleResult httpSampleResult = (HTTPSampleResult) sample;
          SampleResult[] subResults = httpSampleResult.getSubResults();
          if (subResults.length > 0) {
            for (SampleResult subResult : subResults) {
              results.add(subResult);
            }
            return;
          }

          results.add(sample);
        }

        public boolean isStats() {
          return false;
        }
      });
      collector.loadExistingFile();
      return new ArrayList<>(results);
    } catch (Exception e) {
      LOG.error("Error while loading the result from the file {} ", file.getAbsolutePath(), e);
      e.printStackTrace();
      throw new IllegalArgumentException("Unable to load test results from " + file, e);
    }
  }

  /**
   * Saves a list of SampleResults to a jtl file.
   * It is important to mention that, if the results contain invalid characters (such as
   * non-UTF-8 characters), they will be filtered out.
   *
   * @param samples the list of SampleResults from which the jtl file will be created.
   * @return the path of the jtl file where the results were saved.
   */
  public static String saveToFile(List<SampleResult> samples) {
    ResultCollector collector = new ResultCollector();
    collector.setFilename(FileManagementUtils.getRecordingResultFileName());
    collector.setSaveConfig(new SampleSaveConfiguration(true));
    collector.testStarted();
    samples.forEach(result -> {
      if (!containsHexNullText(result)) {
        collector.sampleOccurred(new SampleEvent(result, "Automatic Correlation"));
      }
    });
    collector.testEnded();
    String traceFilepath = collector.getFilename();
    LOG.info("Recording saved to '{}'", traceFilepath);
    return traceFilepath;
  }

  // JMeter has troubles loading SampleResult's responses that contains the hex value of null,
  // we skip those to avoid exceptions while loading them.
  private static boolean containsHexNullText(SampleResult result) {
    String text = result.getResponseDataAsString();
    String nullHexString = "&#x0";
    boolean hasInvalidNullText = text.contains(nullHexString);
    if (hasInvalidNullText) {
      LOG.warn("The SampleResult '{}' contains the character '{}' "
              + "(Hex for NULL). It will be skipped to prevent errors upon loading. ",
          result.getSampleLabel(), nullHexString);
    }
    return hasInvalidNullText;
  }
}
