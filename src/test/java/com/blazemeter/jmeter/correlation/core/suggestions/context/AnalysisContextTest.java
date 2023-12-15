package com.blazemeter.jmeter.correlation.core.suggestions.context;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class AnalysisContextTest {
  private static final Logger LOG = LoggerFactory.getLogger(AnalysisContextTest.class);

  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private AnalysisContext analysisContext;

  @Before
  public void setUp() throws Exception {
    JMeterTestUtils.setupUpdatedJMeter();
  }

  @Test
  public void shouldReturnEmptyWhenGetRecordingSamplersWithEmptyRecordingTestPlanPath() {
    analysisContext = new AnalysisContext();
    analysisContext.setRecordingTestPlanPath("");
    softly.assertThat(analysisContext.getRecordingSamplers()).isEmpty();
  }

  @Test
  public void shouldReturnEmptyWhenGetRecordingSamplersWithInvalidRecordingTestPlanPath() {
    analysisContext = new AnalysisContext();
    analysisContext.setRecordingTestPlanPath("invalidPath");
    softly.assertThat(analysisContext.getRecordingSamplers()).isEmpty();
  }

//  @Test
//  public void shouldReturnListWhenGetRecordingSamplersWithValidRecordingTestPlanPath() {
//    analysisContext = new AnalysisContext();
//    String filePath = "/recordings/testplans/recordingWithNonces.jmx";
//    Optional<String> path = TestUtils.getFilePathFromResources(filePath, getClass());
//    if (!path.isPresent()) {
//      softly.fail("Test plan file not found in resources " + filePath);
//      return;
//    }
//    analysisContext.setRecordingTestPlanPath(path.get());
//    softly.assertThat(analysisContext.getRecordingSamplers()).isNotEmpty();
//    softly.assertThat(analysisContext.getRecordingSamplers().size()).isEqualTo(5);
//  }

  @Test
  public void shouldReturnEmptyWhenGetRecordingSampleResultsWithEmptyRecordingTraceFilePath() {
    analysisContext = new AnalysisContext();
    analysisContext.setRecordingTraceFilePath("");
    softly.assertThat(analysisContext.getRecordingSampleResults()).isEmpty();
  }

  @Test
  public void shouldReturnEmptyWhenGetRecordingSampleResultsWithInvalidRecordingTraceFilePath() {
    analysisContext = new AnalysisContext();
    analysisContext.setRecordingTraceFilePath("invalidPath");
    softly.assertThat(analysisContext.getRecordingSampleResults()).isEmpty();
  }

//  @Test
//  public void shouldReturnListWhenGetRecordingSampleResultsWithValidRecordingTraceFilePath() {
//    analysisContext = new AnalysisContext();
//    String filePath = "/recordings/recordingTrace/recordingWithNonces.jtl";
//    Optional<String> path = TestUtils.getFilePathFromResources(filePath, getClass());
//    if (!path.isPresent()) {
//      softly.fail("JTL file not found in resources " + filePath);
//      return;
//    }
//    analysisContext.setRecordingTraceFilePath(path.get());
//    softly.assertThat(analysisContext.getRecordingSampleResults()).isNotEmpty();
//    softly.assertThat(analysisContext.getRecordingSampleResults().size()).isEqualTo(5);
//  }
}