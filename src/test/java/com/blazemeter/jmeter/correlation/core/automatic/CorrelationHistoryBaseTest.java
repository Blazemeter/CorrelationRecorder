package com.blazemeter.jmeter.correlation.core.automatic;

import com.blazemeter.jmeter.correlation.TestUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CorrelationHistoryBaseTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected static final String HISTORY_FILE_ID = "test";
    protected static final String HISTORY_FILE_NAME = "history-" + HISTORY_FILE_ID + ".json";
    protected static final String RECORDING_FILE_NAME_TEMPLATE = "recording-%s.jtl";
    protected static final String RECORDING_RESOURCE_PATH = "jtl" + File.separator + "recording" + File.separator + "recording-1698345455862.jtl";

    protected static final String REPLY_FILE_NAME_TEMPLATE = "replay-%s.jtl";
    protected static final String REPLAY_RESOURCE_PATH = "jtl" + File.separator + "replay" + File.separator + "replay-1698345458421.jtl";
    protected static final String SNAPSHOT_FILE_NAME_TEMPLATE = "snapshot-%s.jmx";
    protected static final String SNAPSHOT_RESOURCE_PATH = "recordings" + File.separator + "testplans" + File.separator + "recordingWithNonces.jmx";

    protected List<String> expectedZipFiles;

    private void copyFile(File binFolder, String folder, String destinationName, String resourcePath) throws IOException {
        Path destFolder = Paths.get(binFolder.getAbsolutePath(), File.separator ,folder);
        Files.createDirectories(destFolder);
        File historyFile = TestUtils.findTestFile(resourcePath);
        Path destinationPath = destFolder.resolve(destinationName);
        Files.copy(historyFile.toPath(), destinationPath);
    }

    protected void setupHistoryFiles() throws IOException {
        String binPath = folder.getRoot().toPath().toAbsolutePath().toString();
        File binFolder = folder.newFolder("bin");
        JMeterUtils.setJMeterHome(binPath);
        expectedZipFiles = new ArrayList<>();
        copyFile(binFolder, "History", HISTORY_FILE_NAME, HISTORY_FILE_NAME);
        expectedZipFiles.add("History/" + HISTORY_FILE_NAME);
        copyFile(binFolder, "Recording", String.format(SNAPSHOT_FILE_NAME_TEMPLATE, 1), SNAPSHOT_RESOURCE_PATH);
        expectedZipFiles.add("Recording/"+ String.format(SNAPSHOT_FILE_NAME_TEMPLATE, 1));
        copyFile(binFolder, "Recording", String.format(RECORDING_FILE_NAME_TEMPLATE, 1), RECORDING_RESOURCE_PATH);
        expectedZipFiles.add("Recording/" + String.format(RECORDING_FILE_NAME_TEMPLATE, 1));
        copyFile(binFolder, "Recording", String.format(SNAPSHOT_FILE_NAME_TEMPLATE, 2), SNAPSHOT_RESOURCE_PATH);
        expectedZipFiles.add("Recording/"+ String.format(SNAPSHOT_FILE_NAME_TEMPLATE, 2));
        copyFile(binFolder, "Replay", String.format(REPLY_FILE_NAME_TEMPLATE, 2), REPLAY_RESOURCE_PATH);
        expectedZipFiles.add("Replay/" + String.format(REPLY_FILE_NAME_TEMPLATE, 2));
        copyFile(binFolder, "Recording", String.format(SNAPSHOT_FILE_NAME_TEMPLATE, 3), SNAPSHOT_RESOURCE_PATH);
        expectedZipFiles.add("Recording/"+ String.format(SNAPSHOT_FILE_NAME_TEMPLATE, 3));
    }

}
