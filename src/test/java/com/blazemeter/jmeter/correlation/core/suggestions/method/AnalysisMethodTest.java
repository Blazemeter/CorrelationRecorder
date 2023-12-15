package com.blazemeter.jmeter.correlation.core.suggestions.method;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.suggestions.context.AnalysisContext;
import com.blazemeter.jmeter.correlation.core.suggestions.context.AnalysisContextTest;
import java.util.Optional;
import junit.framework.TestCase;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class AnalysisMethodTest extends TestCase {

  private static final Logger LOG = LoggerFactory.getLogger(AnalysisContextTest.class);

  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private AnalysisContext analysisContext;
  private AnalysisMethod method;

  @Before
  public void setUp() throws Exception {
    JMeterTestUtils.setupUpdatedJMeter();
  }

  @Test
  public void shouldGenerateSuggestions() {
    analysisContext = new AnalysisContext();

    String filePath = "/recordings/recordingTrace/recordingWithNonces.jtl";
    Optional<String> path = TestUtils.getFilePathFromResources(filePath, getClass());
    if (!path.isPresent()) {
      softly.fail("JTL file not found in resources " + filePath);
      return;
    }
    analysisContext.setRecordingTraceFilePath(path.get());

    String recordingFilePath = "/recordings/testplans/recordingWithNonces.jmx";
    Optional<String> recordingPath = TestUtils.getFilePathFromResources(recordingFilePath, getClass());
    if (!recordingPath.isPresent()) {
      softly.fail("Test plan file not found in resources " + filePath);
      return;
    }
    analysisContext.setRecordingTestPlanPath(recordingPath.get());

    method = new AnalysisMethod();
    method.generateSuggestions(analysisContext);
  }
}