package com.blazemeter.jmeter.correlation.gui.templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.mockito.Mockito.doThrow;
import com.blazemeter.jmeter.correlation.SwingTestRunner;
import java.awt.Component;
import java.io.IOException;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.driver.AbstractJTableCellWriter;
import org.assertj.swing.driver.JTableTextComponentEditorCellWriter;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JLabelFixture;
import org.assertj.swing.fixture.JTableCellFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SwingTestRunner.class)
public class RepositoriesConfigFrameTestIT extends BaseTest {

  private static final String[][] INITIAL_REPOSITORIES = {
      new String[] {CENTRAL_REPOSITORY_NAME, CENTRAL_REPOSITORY_URL},
      new String[] {FIRST_REPOSITORY_NAME, FIRST_REPOSITORY_URL},
      new String[] {SECOND_REPOSITORY_NAME, SECOND_REPOSITORY_URL},
  };
  private static final String ERROR_DIALOG_TITLE = "Error saving repositories";

  private FrameFixture frame;
  private JTableFixture repositoriesTable;
  private JButtonFixture saveButton;
  private JButtonFixture removeButton;
  private JButtonFixture addButton;
  private JLabelFixture helperIcon;
  private RepositoriesConfigFrame configFrame;

  @Before
  public void setup() {
    baseSetup();
    configFrame =
        new RepositoriesConfigFrame(repositoriesRegistry, new JDialog());
    frame = showInFrame(configFrame.getContentPane());
    repositoriesTable = frame.table("repositoriesTable");
    repositoriesTable.replaceCellWriter(new MyCellWriter(frame.robot()));
    saveButton = frame.button("repositorySaveButton");
    removeButton = frame.button("repositoryRemoveButton");
    addButton = frame.button("repositoryAddButton");
    helperIcon = frame.label("helperIcon");
  }

  private static class MyCellWriter extends AbstractJTableCellWriter {

    private final JTableTextComponentEditorCellWriter textComponentWriter;

    private MyCellWriter(Robot robot) {
      super(robot);
      textComponentWriter = new JTableTextComponentEditorCellWriter(robot);
    }

    @Override
    public void startCellEditing(JTable table, int row, int column) {
      Component editor = editorForCell(table, row, column);
      if (editor instanceof JTextComponent) {
        textComponentWriter.startCellEditing(table, row, column);
      }
    }

    @Override
    public void enterValue(JTable table, int row, int column, String value) {
    }

  }

  @After
  public void tearDown() {
    if (frame != null) {
      frame.cleanUp();
    }
  }

  @Test
  public void shouldDisableSaveButtonWhenOpen() {
    saveButton.requireDisabled();
  }

  @Test
  public void shouldAddEmptyRowAtTheEndWhenClickAddButton() {
    addButton.click();
    String[][] tableContents = repositoriesTable.contents();
    assertThat(tableContents[tableContents.length - 1]).isEqualTo(new String[] {"", ""});
  }

  @Test
  public void shouldDisplayMessageWhenRepositoriesSaved() {
    addRepositoryAndSave(NEW_REPO_ID, NEW_REPO_URL);
    frame.optionPane().requireMessage("All repositories has been saved correctly.");
  }

  private void addRepositoryAndSave(String id, String url) {
    addRepository(id,url);
    saveButton.click();
  }

  private void addRepository(String id, String url) {
    addButton.click();
    fillCell(id, repositoriesTable.rowCount() - 1, 0);
    fillCell(url, repositoriesTable.rowCount() - 1, 1);
  }

  private void fillCell(String text, int row, int column) {
    JTableCellFixture cell = repositoriesTable.cell(TableCell.row(row).column(column));
    cell.startEditing();
    ((JTextComponent) cell.editor()).setText(text);
    cell.stopEditing();
  }

  @Test
  public void shouldShowErrorDialogWhenSaveRepositoriesRepeatedIDRepositories() {
    addRepository(FIRST_REPOSITORY_NAME, NEW_REPO_URL);
    addRepositoryAndSave(FIRST_REPOSITORY_NAME, "URL4");
    frame.optionPane().requireTitle(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldShowErrorDialogWhenSaveRepositoriesRepeatedUrlRepositories() {
    addRepository(FIRST_REPOSITORY_NAME, FIRST_REPOSITORY_URL);
    addRepositoryAndSave(NEW_REPO_ID, FIRST_REPOSITORY_URL);
    frame.optionPane().requireTitle(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldShowErrorDialogWhenSaveRepositoryWithEmptyIDRepositories() {
    addRepositoryAndSave("", NEW_REPO_URL);
    frame.optionPane().requireTitle(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldShowErrorDialogWhenSaveRepositoryWithEmptyUrlRepositories() {
    addRepositoryAndSave(NEW_REPO_ID, "");
    frame.optionPane().requireTitle(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldShowErrorDialogWhenSaveRepositoryWithIdWithSpecialCharacters() {
    addRepositoryAndSave("New%Repo+", "URL");
    frame.optionPane().requireTitle(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldShowErrorWhenSaveRepositoryWithMalformedJSON() throws IOException {
    doThrow(new IOException()).when(repositoriesRegistry).saveRepository("repo", "https://google.com");
    addRepositoryAndSave("repo", "https://google.com");
    frame.optionPane().requireTitle(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldRemoveAddedRepositoriesWhenErrorAndClickOk() {
    addInitialRepositories();
    addRepositoryAndSave(FIRST_REPOSITORY_NAME, FIRST_REPOSITORY_URL);
    clickOk();
    assertThat(repositoriesTable.contents()).isEqualTo(INITIAL_REPOSITORIES);
  }

  private void clickOk() {
    frame.optionPane().okButton().click();
  }

  private void addInitialRepositories(){
    for (String[] repo: INITIAL_REPOSITORIES) {
      addRepository(repo[0], repo[1]);
    }
    saveButton.click();
    clickOk();
  }

  private void addDuplicateRepository() {
    addRepositoryAndSave(FIRST_REPOSITORY_NAME, FIRST_REPOSITORY_URL);
  }

  @Test
  public void shouldDisableSaveButtonWhenErrorAndClickOk() {
    addDuplicateRepository();
    clickOk();
    saveButton.requireDisabled();
  }

  @Test
  public void shouldKeepAddedRepositoriesWhenErrorAndClickCancel() {
    addInitialRepositories();
    addRepositoryAndSave(FIRST_REPOSITORY_NAME, FIRST_REPOSITORY_URL);
    clickCancel();
    assertThat(repositoriesTable.contents()).isEqualTo(new String[][] {
        new String[] {CENTRAL_REPOSITORY_NAME, CENTRAL_REPOSITORY_URL},
        new String[] {FIRST_REPOSITORY_NAME, FIRST_REPOSITORY_URL},
        new String[] {SECOND_REPOSITORY_NAME, SECOND_REPOSITORY_URL},
        new String[] {FIRST_REPOSITORY_NAME, FIRST_REPOSITORY_URL}
    });
  }

  private void clickCancel() {
    frame.optionPane().cancelButton().click();
  }

  @Test
  public void shouldEnableSaveButtonWhenErrorAndClickCancel() {
    addInitialRepositories();
    addDuplicateRepository();
    clickCancel();
    saveButton.requireEnabled();
  }

  @Test
  public void shouldRemoveInputFromTableWhenClickRemoveButton() {
    addInitialRepositories();
    repositoriesTable.click(TableCell.row(1).column(1), MouseButton.LEFT_BUTTON);
    removeButton.click();
    assertThat(repositoriesTable.contents()).isEqualTo(new String[][] {
        new String[] {CENTRAL_REPOSITORY_NAME, CENTRAL_REPOSITORY_URL},
        new String[] {SECOND_REPOSITORY_NAME, SECOND_REPOSITORY_URL}
    });
  }

  @Test
  public void shouldUpdateRepositoryWhenChangeUrlAndSave() {
    addInitialRepositories();
    this.fillCell(NEW_REPO_URL, 1, 1);
    saveButton.click();
    assertThat(repositoriesTable.contents()).isEqualTo(new String[][] {
        new String[] {CENTRAL_REPOSITORY_NAME, CENTRAL_REPOSITORY_URL},
        new String[] {FIRST_REPOSITORY_NAME, NEW_REPO_URL},
        new String[] {SECOND_REPOSITORY_NAME, SECOND_REPOSITORY_URL}
    });
  }

  @Test
  public void shouldFirstRowBeNotEditableWhenLoaded() {
    addInitialRepositories();
    configFrame.updateRepositoriesTable();
    // where central repository is going to be placed
    repositoriesTable.cell(TableCell.row(0).column(0)).requireNotEditable();
  }

  @Test
  public void shouldAddHelperIconWhenLoaded() {
    assertThat(helperIcon.isEnabled());
  }

  @Test
  public void shouldDisplayJDialogWhenClickOnHelperIcon() {
    helperIcon.click();
    assertThat(frame.dialog("helperDialog").isEnabled());
  }

}
