package com.blazemeter.jmeter.correlation.core.automatic;


import com.blazemeter.jmeter.correlation.regression.ClientMock;
import com.blazemeter.jmeter.correlation.regression.ServerMock;
import java.io.IOException;
import java.nio.file.Paths;
import kg.apc.emulators.TestJMeterUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This class is used to resend the requests that were recorded during the recording process, avoiding
 * the need of doing the recording manually again in the browser.
 * <p>
 * There are 2 ways to resend the requests:
 * 1. Using the JTL file path of the recording.
 * 2. Using the Correlation History file path.
 * <p>
 * Also, there is a third method that mocks the requests, in case you want to test any of the
 * automatic features without having to do directly send the requests against the server. This
 * is useful to simulate an specific scenario.
 */
@RunWith(MockitoJUnitRunner.class)
public class NonGuiReSendRequests extends NonGuiAcr {


  @Before
  public void setUp() {
    TestJMeterUtils.createJmeterEnv();
  }

  @Test
  public void shouldReSendTheRecordedTrafficToJMeter() throws IOException {
    correlationHistoryPath = ""; //Add the path to the Correlation History file here.
    if (!hasNeededFiles(correlationHistoryPath)) {
      return;
    }

    history = CorrelationHistory.loadFromFile(correlationHistoryPath);

    if (history == null) {
      return;
    }

    if (!hasNeededFiles(history.getOriginalRecordingTrace(),
        history.getLastReplayTraceFilepath())) {
      return;
    }

    recordingFilePath = history.getOriginalRecordingTrace();
    replayFilePath = history.getLastReplayTraceFilepath();

    System.out.println("Resending the traffic from the original recording to JMeter");
    System.out.println("Original Recording: " + recordingFilePath);

    ServerMock serverMock = ServerMock.fromJtl(Paths.get(recordingFilePath));
    ClientMock clientMock = ClientMock.fromJtl(Paths.get(recordingFilePath));
    System.out.println("Starting the replay");
    clientMock.run();
    System.out.println("Replay finished");
    serverMock.reset();
    System.out.println("Mock server left running for possible replay from JMeter...");
    System.in.read();
  }
}
