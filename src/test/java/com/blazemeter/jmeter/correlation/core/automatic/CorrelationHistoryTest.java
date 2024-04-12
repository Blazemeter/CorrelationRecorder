package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.TestUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CorrelationHistoryTest extends CorrelationHistoryBaseTest{

    @Before
    public void setUp() throws IOException {
        setupHistoryFiles();
    }

    @Test
    public void shouldLoadHistoryFromFile() throws IOException {
        CorrelationHistory history = CorrelationHistory.loadFromFile(HISTORY_FILE_ID);
        assertEquals(history.getSteps().size(), 3);
        assertEquals(history.getHistoryId(),HISTORY_FILE_ID);
    }



    @Test
    public void shouldDeleteStepsWhenDeleteSteps() throws IOException {
        CorrelationHistory history = CorrelationHistory.loadFromFile(HISTORY_FILE_ID);
        assertEquals(history.getSteps().size(), 3);
        List<CorrelationHistory.Step> steps = history.getSteps();
        history.deleteSteps(steps);
        assertEquals(history.getSteps().size(), 0);
    }

    @Test
    public void shouldAddOriginalRecordingStep() throws IOException {
        CorrelationHistory history = new CorrelationHistory();
        history.addOriginalRecordingStep("Recording"+ File.separator+"originalRecordingFilepath",
                "Recording"+ File.separator+"originalRecordingTraceFilepath");
        assertEquals("Recording"+ File.separator+"originalRecordingFilepath", history.getOriginalRecordingFilepath());
        assertEquals("Recording"+ File.separator+"originalRecordingTraceFilepath", history.getOriginalRecordingTrace());
    }
    @Test
    public void shouldAddOriginalRecordingStepWhenStepsAreNotEmpty() throws IOException {
        CorrelationHistory history = new CorrelationHistory();
        history.addOriginalRecordingStep("Recording"+ File.separator+"test",
                "Recording"+ File.separator+"test");
        history.addOriginalRecordingStep("Recording"+ File.separator+"originalRecordingFilepath",
                "Recording"+ File.separator+"originalRecordingTraceFilepath");
        assertEquals("Recording"+ File.separator+"originalRecordingFilepath", history.getOriginalRecordingFilepath());
        assertEquals("Recording"+ File.separator+"originalRecordingTraceFilepath", history.getOriginalRecordingTrace());
    }

    private void addSuccessfulReplay(boolean errors) {
        CorrelationHistory history = new CorrelationHistory();
        history.addSuccessfulReplay("Recording"+ File.separator+"testPlanFilepath","Recording"+ File.separator+"replayTraceFilepath",
                errors);
        assertEquals("Recording"+ File.separator+"testPlanFilepath", history.getLastTestPlanFilepath());
        assertEquals("Recording"+ File.separator+"replayTraceFilepath", history.getLastReplayTraceFilepath());
    }

    @Test
    public void shouldAddSuccessfulReplayWhenNoErrors() throws IOException {
        addSuccessfulReplay(false);
    }
    @Test
    public void shouldAddSuccessfulReplayWithErrors() throws IOException {
        addSuccessfulReplay(true);
    }

    @Test
    public void shouldAddFailedReplay() throws IOException {
        CorrelationHistory history = new CorrelationHistory();
        history.addFailedReplay("Recording"+ File.separator+"testPlanFilepath","Replay"+ File.separator+"replayTraceFilepath", 0);
        assertEquals("Recording"+ File.separator+"testPlanFilepath", history.getLastTestPlanFilepath());
        assertEquals("Replay"+ File.separator+"replayTraceFilepath", history.getLastReplayTraceFilepath());
    }

    @Test
    public void shouldAddAnalisisStep() throws IOException {
        CorrelationHistory history = new CorrelationHistory();
        history.addAnalysisStep("Test","Recording"+ File.separator+"testPlanFilepath","Replay"+ File.separator+"replayTraceFilepath");
        assertEquals("Recording"+ File.separator+"testPlanFilepath", history.getLastTestPlanFilepath());
        assertEquals("Replay"+ File.separator+"replayTraceFilepath", history.getLastReplayTraceFilepath());
    }

    @Test
    public void shouldAddRestoreStep() throws IOException {
        CorrelationHistory history = new CorrelationHistory();
        CorrelationHistory.Step newStep = new CorrelationHistory.Step(
                "Restored iteration with timestamp: %s", "2024/05/10");
        newStep.setTestPlanFilepath("testPlanFilepath");
        newStep.setRecordingTraceFilepath("recordingTraceFilepath");
        newStep.setReplayTraceFilepath("replayTraceFilepath");
        history.addRestoredStep(newStep);
        assertEquals("testPlanFilepath", history.getLastTestPlanFilepath());
        assertEquals("replayTraceFilepath", history.getLastReplayTraceFilepath());
    }

    @Test
    public void shouldGetOriginalRecordingFilepathWhenNoStepAdded() throws IOException {
        String testPlanFilepath = "Recording"+ File.separator+"testPlanFilepath";
        CorrelationHistory.setSaveCurrentTestPlan(() -> testPlanFilepath);
        String recordingFilepath = "Recording"+ File.separator+"recordingFilepath";
        CorrelationHistory.setRecordingFilePathSupplier(() -> recordingFilepath);

        CorrelationHistory history = new CorrelationHistory();
        assertEquals(testPlanFilepath, history.getOriginalRecordingFilepath());
    }

    @Test
    public void shouldGetOriginalRecordingTraceWhenNoStepAdded() throws IOException {
        String testPlanFilepath = "Recording"+ File.separator+"testPlanFilepath";
        CorrelationHistory.setSaveCurrentTestPlan(() -> testPlanFilepath);
        String recordingFilepath = "Recording"+ File.separator+"recordingFilepath";
        CorrelationHistory.setRecordingFilePathSupplier(() -> recordingFilepath);

        CorrelationHistory history = new CorrelationHistory();
        assertEquals(recordingFilepath, history.getOriginalRecordingTrace());
    }

    @Test
    public void shouldReturnStringSupplierWhenGetLastTestPlanFilepathWithEmptySteps() throws IOException {
        Supplier<String> stringSupplier = () -> "testPlanFilepath";
        CorrelationHistory history = new CorrelationHistory();
        CorrelationHistory.setSaveCurrentTestPlan(stringSupplier);
        assertEquals("testPlanFilepath", history.getLastTestPlanFilepath());
    }

    @Test
    public void shouldReturnEmptyStringWhenGetLastReplayTraceFilepathWithEmptySteps() throws IOException {
        Supplier<String> stringSupplier = () -> "testPlanFilepath";
        CorrelationHistory history = new CorrelationHistory();
        CorrelationHistory.setSaveCurrentTestPlan(stringSupplier);
        assertEquals("",history.getLastReplayTraceFilepath());
    }

    @Test
    public void stepShouldAddCurrentTestPlanWhenAddCurrentTestPlan() throws IOException {
        Supplier<String> stringSupplier = () -> "Recording"+ File.separator+"testPlanFilepath";
        CorrelationHistory.setSaveCurrentTestPlan(stringSupplier);
        CorrelationHistory.Step step = new CorrelationHistory.Step("Hello World");
        step.addCurrentTestPlan();
        assertEquals("Recording"+ File.separator+"testPlanFilepath",step.getTestPlanFilepath());
    }

    @Test
    public void stepShouldReturnTimestampWhenGetTimestamp() throws IOException {
        CorrelationHistory history = CorrelationHistory.loadFromFile(HISTORY_FILE_ID);
        CorrelationHistory.Step step = history.getSteps().get(0);
        assertEquals("2024-01-22T11:26:20.230",step.getTimestamp());
    }

    @Test
    public void stepShouldSetTimestampWhenSetTimestamp() throws IOException {
        CorrelationHistory history = CorrelationHistory.loadFromFile(HISTORY_FILE_ID);
        CorrelationHistory.Step step = history.getSteps().get(0);
        step.setTimestamp("123");
        assertEquals("123",step.getTimestamp());
    }

    @Test
    public void stepShouldReturnStepMessageWhenGetStepMessage() throws IOException {
        CorrelationHistory history = CorrelationHistory.loadFromFile(HISTORY_FILE_ID);
        CorrelationHistory.Step step = history.getSteps().get(0);
        assertEquals("Original Recording",step.getStepMessage());
    }

    @Test
    public void shouldReturnStringWhenHistoryToString() throws IOException {
        CorrelationHistory history = CorrelationHistory.loadFromFile(HISTORY_FILE_ID);
        String historyString = "CorrelationHistory {"
                + ", filepath='" + FileManagementUtils.getHistoryPath(HISTORY_FILE_ID) + '\''
                + ", totalSteps=" + history.getSteps().size()
                + ", steps=" + history.getSteps() + '}';
        assertEquals(history.toString(),historyString);
    }

    @Test
    public void shouldCreateZipHistoryFileWhenZipHistory() throws IOException {
        CorrelationHistory history = CorrelationHistory.loadFromFile(HISTORY_FILE_ID);
        String zipFile = history.zipHistory();
        assertEquals(FileManagementUtils.getHistoryZipFilePath(history.getHistoryId()), zipFile);

        List<String> zipFiles = new ArrayList<>();
        ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(Paths.get(zipFile)));
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            if (!zipEntry.isDirectory()) {
                zipFiles.add(zipEntry.getName());
            }
        }

        assertEquals(expectedZipFiles, zipFiles);
    }

}
