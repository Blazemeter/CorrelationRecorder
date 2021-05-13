package com.blazemeter.jmeter.correlation.gui;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.swing.fixture.Containers.showInFrame;

import com.blazemeter.jmeter.correlation.SwingTestRunner;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SwingTestRunner.class)
public class GroupsContainerIT {

  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Mock
  private Runnable modelUpdate;
  private FrameFixture frame;
  private GroupsContainer container;
  private JTableFixture table;

  @Before
  public void setup() {
    container = new GroupsContainer(modelUpdate);
    frame = showInFrame(container);
    table = frame.table("groupsTable");
    table.replaceCellWriter(new CustomCellWriter(frame.robot()));
  }

  @After
  public void tearDown() {
    frame.cleanUp();
    frame = null;
  }

  @Test
  public void shouldAddGroupPanelWhenAddPressed() {
    addGroup();
    assertThat(container.getGroupPanels().size()).isEqualTo(1);
  }

  private void addGroup() {
    frame.button("addButton").click();
  }

  @Test
  public void shouldMoveRuleUpWhenUpPressed() {
    addGroup();
    addGroup();
    GroupPanel secondGroup = container.getGroupPanels().get(1);
    clickGroupByIndex(1);
    findUpButton().click();
    assertThat(container.getGroupPanels().get(0)).isSameAs(secondGroup);
  }

  private void clickGroupByIndex(int index) {
    table.target().setRowSelectionInterval(index, index);
  }

  private JButtonFixture findUpButton() {
    return frame.button("upButton");
  }

  @Test
  public void shouldDisableDownButtonWhenSelectLastGroup() {
    addGroup();
    addGroup();
    clickGroupByIndex(1);
    findDownButton().requireDisabled();
  }

  private JButtonFixture findDownButton() {
    return frame.button("downButton");
  }

  @Test
  public void shouldEnableDownButtonWhenSelectFirstGroupWithMultipleGroups() {
    addGroup();
    addGroup();
    clickGroupByIndex(0);
    findDownButton().requireEnabled();
  }

  @Test
  public void shouldAddGroupWhenAddPressed() {
    addGroup();
    assertThat(table.contents()[0]).isNotEqualTo(new String[]{null, null, null});
  }

  @Test
  public void shouldMoveGroupDownWhenDownPressed() {
    addGroup();
    addGroup();
    GroupPanel firstGroup = container.getGroupPanels().get(0);
    clickGroupByIndex(0);
    findDownButton().click();
    assertThat(container.getGroupPanels().get(1)).isSameAs(firstGroup);
  }

  @Test
  public void shouldDeleteGroupWhenDeletePressed() {
    addGroup();
    addGroup();
    clickGroupByIndex(1);
    frame.button("deleteButton").click();
    assertThat(container.getGroupPanels().size()).isEqualTo(1);
  }

  @Test
  public void shouldUpButtonBeDisabledWhenMovedRuleReachTheTopOfTheList() {
    addGroup();
    addGroup();
    clickGroupByIndex(1);
    findUpButton().click();
    findUpButton().requireDisabled();
  }

  @Test
  public void shouldEnableUpButtonWhenMoreThanOneRowAndNotFirstRowSelected() {
    addGroup();
    addGroup();
    clickGroupByIndex(1);
    findUpButton().requireEnabled();
  }
}
