package com.blazemeter.jmeter.correlation.core.templates;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.assertj.swing.timing.Pause.pause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.driver.AbstractJTableCellWriter;
import org.assertj.swing.driver.JTableTextComponentEditorCellWriter;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.AbstractWindowFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTableCellFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationTemplatesRepositoryConfigFrameTestIT {

  private static final String LOCAL_REPOSITORY_NAME = "local";
  private static final String FIRST_REPOSITORY_NAME = "R1";
  private static final String FIRST_REPOSITORY_URL = "URL1";
  private static final String SECOND_REPOSITORY_NAME = "R2";
  private static final String SECOND_REPOSITORY_URL = "URL2";
  private static final String REPOSITORY_SAVE_BUTTON_NAME = "repositorySaveButton";
  private static final String REPOSITORY_ADD_BUTTON_NAME = "repositoryAddButton";
  private static final String REPOSITORY_REMOVE_BUTTON_NAME = "repositoryRemoveButton";
  private static final String REPOSITORY_TABLE_NAME = "repositoriesTable";
  private static final String ERROR_DIALOG_TITLE = "Error saving repositories";
  private static final String OK_BUTTON_TEXT = UIManager.getString("OptionPane.okButtonText");
  private static final String CANCEL_BUTTON_TEXT = UIManager
      .getString("OptionPane.cancelButtonText");
  private static final String NEW_REPO_ID = "R3";
  private static final String NEW_REPO_URL = "URL3";

  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Mock
  private CorrelationTemplatesRepositoriesRegistryHandler repositoryHandler;
  @Mock
  private CorrelationTemplatesRepository firstRepository;
  @Mock
  private CorrelationTemplatesRepository secondRepository;
  @Mock
  private CorrelationTemplatesRepository localRepository;

  private FrameFixture frame;
  private JTableFixture repositoriesTable;
  private JButtonFixture saveButton;
  private JButtonFixture removeButton;
  private JButtonFixture addButton;

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Before
  public void setup() {
    prepareRepositories();
    CorrelationTemplatesRepositoryConfigFrame configFrame = new CorrelationTemplatesRepositoryConfigFrame(
        repositoryHandler);
    frame = showInFrame(configFrame.getContentPane());
    repositoriesTable = frame.table(REPOSITORY_TABLE_NAME);
    repositoriesTable.replaceCellWriter(new MyCellWriter(frame.robot()));
    saveButton = frame.button(REPOSITORY_SAVE_BUTTON_NAME);
    removeButton = frame.button(REPOSITORY_REMOVE_BUTTON_NAME);
    addButton = frame.button(REPOSITORY_ADD_BUTTON_NAME);
  }

  private void prepareRepositories() {
    when(localRepository.getName()).thenReturn(LOCAL_REPOSITORY_NAME);
    when(firstRepository.getName()).thenReturn(FIRST_REPOSITORY_NAME);
    when(secondRepository.getName()).thenReturn(SECOND_REPOSITORY_NAME);
    when(repositoryHandler.getCorrelationRepositories())
        .thenReturn(Arrays.asList(localRepository, firstRepository, secondRepository));
    when(repositoryHandler.getRepositoryURL(FIRST_REPOSITORY_NAME))
        .thenReturn(FIRST_REPOSITORY_URL);
    when(repositoryHandler.getRepositoryURL(SECOND_REPOSITORY_NAME))
        .thenReturn(SECOND_REPOSITORY_URL);
  }

  @Test
  public void shouldShowRepositoriesListExceptLocalWhenOpen() {
    softly.assertThat(repositoriesTable.rowCount()).isEqualTo(2);
    softly.assertThat(repositoriesTable.contents()[0][0]).isEqualTo(FIRST_REPOSITORY_NAME);
    softly.assertThat(repositoriesTable.contents()[0][1]).isEqualTo(FIRST_REPOSITORY_URL);
    softly.assertThat(repositoriesTable.contents()[1][0]).isEqualTo(SECOND_REPOSITORY_NAME);
    softly.assertThat(repositoriesTable.contents()[1][1]).isEqualTo(SECOND_REPOSITORY_URL);
  }

  @Test
  public void shouldDisableSaveButtonWhenOpen() {
    assertFalse(saveButton.isEnabled());
  }

  @Test
  public void shouldAddEmptyRowAtTheEndWhenClickAddButton() {
    addButton.click();
    softly.assertThat(repositoriesTable.contents()[repositoriesTable.rowCount() - 1][0])
        .isEqualTo("");
    softly.assertThat(repositoriesTable.contents()[repositoriesTable.rowCount() - 1][1])
        .isEqualTo("");
  }

  @Test
  public void shouldDisplayMessageWhenRepositoriesSaved() {
    addRepository(NEW_REPO_ID, NEW_REPO_URL);
    saveButton.click();
    JOptionPaneFinder.findOptionPane().using(frame.robot())
        .requireMessage(
            "All repositories has been saved correctly.").okButton();
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
    addRepository(NEW_REPO_ID, NEW_REPO_URL);
    addRepository(NEW_REPO_ID, "URL4");
    saveButton.click();
    assertThat(frame.dialog().target().getTitle()).isEqualTo(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldShowErrorDialogWhenSaveRepositoriesRepeatedUrlRepositories() {
    addRepository(NEW_REPO_ID, NEW_REPO_URL);
    addRepository("R4", NEW_REPO_URL);
    saveButton.click();
    assertThat(frame.dialog().target().getTitle()).isEqualTo(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldShowErrorDialogWhenSaveRepositoryWithEmptyIDRepositories() {
    addRepository("", NEW_REPO_URL);
    saveButton.click();
    assertThat(frame.dialog().target().getTitle()).isEqualTo(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldShowErrorDialogWhenSaveRepositoryWithEmptyUrlRepositories() {
    addRepository(NEW_REPO_ID, "");
    saveButton.click();
    assertThat(frame.dialog().target().getTitle()).isEqualTo(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldShowErrorDialogWhenSaveRepositoryWithIdWithSpecialCharacters() {
    addRepository("New%Repo+", "URL");
    saveButton.click();
    assertThat(frame.dialog().target().getTitle()).isEqualTo(ERROR_DIALOG_TITLE);
  }

  @Test
  public void shouldRemoveAddedRepositoriesWhenRepeatedErrorAndClickOk() {
    addDuplicatedRepositoriesAndSave(OK_BUTTON_TEXT);
    String[][] notExpected = {{NEW_REPO_ID, NEW_REPO_URL}};
    assertThat(repositoriesTable.contents()).doesNotContain(notExpected);
  }

  @Test
  public void shouldDisableSaveButtonWhenRepeatedErrorAndClickOk() {
    addDuplicatedRepositoriesAndSave(OK_BUTTON_TEXT);
    assertFalse(saveButton.isEnabled());
  }

  @Test
  public void shouldKeepAddedRepositoriesWhenRepeatedErrorAndClickCancel() {
    addDuplicatedRepositoriesAndSave(CANCEL_BUTTON_TEXT);
    String[][] expected = {{NEW_REPO_ID, NEW_REPO_URL}};
    assertThat(repositoriesTable.contents()).contains(expected);
  }

  @Test
  public void shouldEnableSaveButtonWhenRepeatedErrorAndClickOk() {
    addDuplicatedRepositoriesAndSave(CANCEL_BUTTON_TEXT);
    assertTrue(saveButton.isEnabled());
  }

  private void addDuplicatedRepositoriesAndSave(String dialogOption) {
    addRepository(NEW_REPO_ID, NEW_REPO_URL);
    addRepository(NEW_REPO_ID, NEW_REPO_URL);
    saveButton.click();
    getButtonByText(frame.dialog(), dialogOption).click();
  }

  private JButtonFixture getButtonByText(AbstractWindowFixture source, String text) {
    return source.button(new GenericTypeMatcher<JButton>(JButton.class) {
      @Override
      protected boolean isMatching(JButton component) {
        return component.getText().equals(text);
      }
    });
  }

  @Test
  public void shouldRemoveAddedRepositoriesWhenEmptyErrorAndClickOk() {
    addEmptyRepositoriesAndSave(OK_BUTTON_TEXT);
    String[][] notExpected = {{"", NEW_REPO_URL}};
    assertThat(repositoriesTable.contents()).doesNotContain(notExpected);
  }

  @Test
  public void shouldDisableSaveButtonWhenEmptyErrorAndClickOk() {
    addEmptyRepositoriesAndSave(OK_BUTTON_TEXT);
    assertFalse(saveButton.isEnabled());
  }

  @Test
  public void shouldKeepAddedRepositoriesWhenEmptyErrorAndClickCancel() {
    addEmptyRepositoriesAndSave(CANCEL_BUTTON_TEXT);
    String[][] expected = {{"", NEW_REPO_URL}};
    assertThat(repositoriesTable.contents()).contains(expected);
  }

  @Test
  public void shouldEnableSaveButtonWhenEmptyErrorAndClickOk() {
    addEmptyRepositoriesAndSave(CANCEL_BUTTON_TEXT);
    assertTrue(saveButton.isEnabled());
  }

  private void addEmptyRepositoriesAndSave(String dialogOption) {
    addRepository("", NEW_REPO_URL);
    saveButton.click();
    getButtonByText(frame.dialog(), dialogOption).click();
  }

  @Test
  public void shouldDisableSaveButtonWhenRepositoryAddErrorAndClickOk() throws IOException {
    addErrorRepositoriesAndSave(OK_BUTTON_TEXT);
    assertFalse(saveButton.isEnabled());
  }

  @Test
  public void shouldEnableSaveButtonWhenRepositoryAddErrorAndClickCancel() throws IOException {
    addErrorRepositoriesAndSave(CANCEL_BUTTON_TEXT);
    assertTrue(saveButton.isEnabled());
  }

  @Test
  public void shouldKeepAddedRepositoryWhenRepositoryAddErrorAndClickCancel() throws IOException {
    addErrorRepositoriesAndSave(CANCEL_BUTTON_TEXT);
    String[][] expected = {{NEW_REPO_ID, NEW_REPO_URL}};
    assertThat(repositoriesTable.contents()).contains(expected);
  }

  @Test
  public void shouldRemoveAddedRepositoryWhenRepositoryAddErrorAndClickOk() throws IOException {
    addErrorRepositoriesAndSave(OK_BUTTON_TEXT);
    String[][] notExpected = {{NEW_REPO_ID, NEW_REPO_URL}};
    assertThat(repositoriesTable.contents()).doesNotContain(notExpected);
  }

  private void addErrorRepositoriesAndSave(String dialogOption) throws IOException {
    doThrow(new IOException()).when(repositoryHandler).saveRepository(anyString(), anyString());
    addRepository(NEW_REPO_ID, NEW_REPO_URL);
    saveButton.click();
    getButtonByText(frame.dialog(), dialogOption).click();
  }

  @Test
  public void shouldRemoveInputFromTableWhenClickRemoveButton() {
    selectCell(1, 1);
    removeButton.click();
    String[][] notExpected = {{SECOND_REPOSITORY_NAME, SECOND_REPOSITORY_URL}};
    assertThat(repositoriesTable.contents()).doesNotContain(notExpected);
  }

  private void selectCell(int row, int column) {
    TableCell cell = TableCell.row(row).column(column);
    repositoriesTable.click(cell, MouseButton.LEFT_BUTTON);
  }

  @Test
  public void shouldUpdateRepositoryWhenChangeUrlAndSave() {
    this.fillCell(NEW_REPO_URL, 1, 1);
    saveButton.click();
    String[][] expected = {{SECOND_REPOSITORY_NAME, NEW_REPO_URL}};
    assertThat(repositoriesTable.contents()).contains(expected);
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
  
  @Test
  public void shouldHaveFirstRepositoryImmutableWhenAlways() {
    // where central repository is going to be placed
    repositoriesTable.cell(TableCell.row(0).column(0)).requireNotEditable();
  }
}
