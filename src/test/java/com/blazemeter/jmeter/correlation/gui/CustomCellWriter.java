package com.blazemeter.jmeter.correlation.gui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import org.assertj.swing.core.Robot;
import org.assertj.swing.driver.AbstractJTableCellWriter;
import org.assertj.swing.driver.JTableTextComponentEditorCellWriter;

public class CustomCellWriter extends AbstractJTableCellWriter {

  private final JTableTextComponentEditorCellWriter textComponentWriter;

  public CustomCellWriter(Robot robot) {
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
