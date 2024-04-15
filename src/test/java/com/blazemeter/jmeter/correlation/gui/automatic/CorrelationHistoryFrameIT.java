package com.blazemeter.jmeter.correlation.gui.automatic;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistory;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationHistoryBaseTest;
import com.blazemeter.jmeter.correlation.gui.automatic.CorrelationHistoryFrame;
import org.apache.jmeter.util.JMeterUtils;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.swing.data.TableCell.row;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SwingTestRunner.class)
public class CorrelationHistoryFrameIT extends CorrelationHistoryBaseTest {

    private CorrelationHistoryFrame historyFrame;
    private FrameFixture frame;
    private CorrelationHistory history;


    @Before
    public void setUp() throws IOException {

        setupHistoryFiles();

        history = CorrelationHistory.loadFromFile(HISTORY_FILE_ID);

        historyFrame = new CorrelationHistoryFrame(history);
        historyFrame.loadSteps(history.getSteps());
        frame = showInFrame(historyFrame.getContentPane());
    }

    @After
    public void tearDown() {
        frame.cleanUp();
        frame = null;
    }

    @Test
    public void shouldHaveNoRowSelectedWhenCreated() {
        assertEquals(0,frame.table("historyTable").target().getSelectedRowCount());
    }

    @Test
    public void shouldDisplayMessageWhenRestoreButtonClickedWithMoreThanOneRowSelected() {
        frame.table("historyTable").cell(row(0).column(0)).click();
        frame.table("historyTable").cell(row(1).column(0)).click();
        frame.button("restoreStepButton").click();
        frame.optionPane().requireMessage("You can't restore more than one iteration at a time.");
    }

    @Test
    public void shouldDisplayMessageWhenRestoreButtonClickedWithNoRowSelected() {
        frame.button("restoreStepButton").click();
        frame.optionPane().requireMessage("Please select one iteration to restore");
    }

    @Test
    public void shouldEnableDeleteButtonWithMoreThanOneRowSelected() {
        frame.table("historyTable").cell(row(0).column(0)).click();
        frame.table("historyTable").cell(row(1).column(0)).click();
        assertTrue(frame.button("deleteStepsButton").isEnabled());
    }

    @Test
    public void shouldShowWarningWhenDeleteButtonIsClicked() {
        frame.table("historyTable").cell(row(0).column(0)).click();
        frame.table("historyTable").cell(row(1).column(0)).click();
        frame.button("deleteStepsButton").click();
        frame.optionPane().requireMessage("You are about to delete one or more history iterations.\n" +
                "Do you want to continue? \n");

    }

    @Test
    public void shouldShowSuccessMessageWhenZipButtonIsClicked() {
        frame.button("zipSaveButton").click();
        frame.optionPane().requireMessage(Pattern.compile("History zipped at: \n.*"));
    }
    @Test
    public void shouldCreateCheckpointWhenButtonIsClicked() {
        String testPlanFilepath = "Recording" + File.separator+ "testPlanFilepath";
        CorrelationHistory.setSaveCurrentTestPlan(() -> testPlanFilepath);
        String recordingFilepath = "Recording" + File.separator+ "recordingFilepath";
        CorrelationHistory.setRecordingFilePathSupplier(() -> recordingFilepath);

        frame.button("createIterationButton").click();

        assertEquals(4, frame.table("historyTable").rowCount());
    }
    @Test
    public void shouldEditHistoryIterationValues() {
        frame.table("historyTable").cell(row(0).column(2)).doubleClick()
                .enterValue("new value");
        frame.table("historyTable").cell(row(0).column(3)).doubleClick()
                .enterValue("new value");

        assertEquals("new value", frame.table("historyTable").valueAt(row(0).column(2)));
        assertEquals("new value", frame.table("historyTable").valueAt(row(0).column(3)));
    }


}
