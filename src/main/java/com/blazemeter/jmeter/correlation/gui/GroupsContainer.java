package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.RulesGroup;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.common.SwingUtils.ButtonBuilder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class GroupsContainer extends JPanel implements ActionListener {

  private static final String ADD = "add";
  private static final String DELETE = "delete";
  private static final String UP = "up";
  private static final String DOWN = "down";
  private final GroupsTableGui table;
  private final Runnable modelUpdate;
  private JButton deleteButton;
  private JButton upButton;
  private JButton downButton;

  public GroupsContainer(Runnable modelUpdater) {
    setLayout(new BorderLayout());
    this.modelUpdate = modelUpdater;
    this.table = SwingUtils
        .createComponent("groupsTable", new GroupsTableGui(this::checkButtonsStatus));
    add(new JScrollPane(table), BorderLayout.CENTER);
    add(buildGroupButtonPanel(), BorderLayout.SOUTH);
  }

  private JPanel buildGroupButtonPanel() {

    ButtonBuilder base = new SwingUtils.ButtonBuilder()
        .withActionListener(this);

    upButton = base.withName("up").withAction(UP).build();
    downButton = base.withName("down").withAction(DOWN).build();
    deleteButton = base.withName("delete").withAction(DELETE).build();

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(new JLabel("Groups: "));
    buttonPanel.add(base.withName("add").withAction(ADD).build());
    buttonPanel.add(deleteButton);
    buttonPanel.add(upButton);
    buttonPanel.add(downButton);
    buttonPanel.setMinimumSize(new Dimension(100, 200));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    return buttonPanel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();
    switch (action) {
      case DELETE:
        deleteGroup();
        break;
      case ADD:
        addGroup();
        break;
      case UP:
        moveUp();
        break;
      case DOWN:
        moveDown();
        break;
      default:
        throw new UnsupportedOperationException(action);
    }
    modelUpdate.run();
    doGroupsUIRefresh();
  }

  public void addGroup() {
    addGroup(new GroupPanel(findNextAvailableTitle("Group-" + (getGroupPanels().size() + 1), 0),
        true, this::doGroupsUIRefresh, modelUpdate));
  }

  public void addGroup(GroupPanel groupPanel) {
    table.addRow(groupPanel);
  }

  private String findNextAvailableTitle(String title, int repeatedTimes) {
    String renamedTitle = title + (repeatedTimes > 0 ? " (" + repeatedTimes + ")" : "");

    if (getGroupPanels().stream().anyMatch(g -> g.getRulesGroup().getId().equals(renamedTitle))) {
      return findNextAvailableTitle(title, repeatedTimes + 1);
    }

    return renamedTitle;
  }

  public void doGroupsUIRefresh() {
    doGroupsResize();
    doGroupsRepaint();
  }

  public void doGroupsResize() {
    int neededHeight = 0;
    int neededWidth = 1100;
    int rowCount = 0;
    for (GroupPanel group : getGroupPanels()) {
      int rowRequiredHeight = group.getRequiredHeight();
      neededHeight += rowRequiredHeight;
      table.setRowHeight(rowCount, rowRequiredHeight);

      neededWidth = Math.max(neededWidth, group.getRequiredWidth());
      rowCount++;
    }

    Dimension groupsTableNeededSize = new Dimension(neededWidth, neededHeight);
    table.setPreferredSize(groupsTableNeededSize);
    table.setSize(groupsTableNeededSize);

    //Now we update the sizes of all tables
    for (GroupPanel group : getGroupPanels()) {
      int groupWidth = group.getRequiredWidth();
      if (neededWidth != groupWidth) {
        group.updateGroupDimensions(new Dimension(neededWidth, group.getRequiredHeight()));
      }
    }
  }

  private void doGroupsRepaint() {
    revalidate();
    repaint();
  }

  private void moveUp() {
    table.moveUp();
  }

  private void moveDown() {
    table.moveDown();
  }

  private void deleteGroup() {
    table.delete();
  }

  public void configure(List<RulesGroup> groups) {
    groups.forEach(group -> addGroup(new GroupPanel(group, this::doGroupsUIRefresh, modelUpdate)));
    doGroupsUIRefresh();
  }

  public void clear() {
    table.clear();
  }

  public List<GroupPanel> getGroupPanels() {
    return table.getGroups();
  }

  public void checkButtonsStatus() {
    deleteButton.setEnabled(table.getValues().size() > 0);
    int[] selectedRows = table.getSelectedRows();
    upButton.setEnabled(selectedRows.length > 0 && selectedRows[0] > 0);
    downButton.setEnabled(selectedRows.length > 0
        && selectedRows[selectedRows.length - 1] < table.getValues().size() - 1);
  }

  @Override
  public String toString() {
    return "GroupsContainer{" +
        "table=" + table +
        '}';
  }
}
