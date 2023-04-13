package com.blazemeter.jmeter.correlation.gui.automatic;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.action.KeyStrokes;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.GuiUtils;

public class TextBoxPopupDialog implements ActionListener {
  private static final String CANCEL_COMMAND = "cancel_dialog";
  private static final String SAVE_CLOSE_COMMAND = "save_close_dialog";
  private static final String CLOSE_COMMAND = "close_dialog";
  private JDialog dialog;
  private JEditorPane textBox;
  private String originalText;
  private boolean editable = false;
  private JFrame parentFrame;

  public TextBoxPopupDialog() {
    this.init("");
  }

  public TextBoxPopupDialog(String text) {
    this.init(text);
  }

  public TextBoxPopupDialog(String text, boolean editable) {
    this.editable = editable;
    this.init(text);
  }

  private void init(String text) {
    this.createDialogBox();
    this.setTextBox(text);
    this.dialog.setVisible(true);
    this.dialog.toFront();
  }

  private void createDialogBox() {
    this.parentFrame = GuiPackage.getInstance().getMainFrame();
    String title =
        this.editable ? JMeterUtils.getResString(
            "textbox_title_edit") : JMeterUtils.getResString("textbox_title_view");
    this.dialog = new JDialog(this.parentFrame, title, true);
    JPanel content = (JPanel) this.dialog.getContentPane();
    content.registerKeyboardAction(this, KeyStrokes.ESC, 2);
    this.textBox = new JEditorPane();
    this.textBox.setEditable(this.editable);
    JScrollPane textBoxScrollPane = GuiUtils.makeScrollPane(this.textBox);
    JPanel btnBar = new JPanel();
    btnBar.setLayout(new FlowLayout(2));
    JButton cancelBtn;
    if (this.editable) {
      cancelBtn = new JButton(JMeterUtils.getResString("textbox_cancel"));
      cancelBtn.setActionCommand("cancel_dialog");
      cancelBtn.addActionListener(this);
      JButton saveBtn = new JButton(JMeterUtils.getResString("textbox_save_close"));
      saveBtn.setActionCommand("save_close_dialog");
      saveBtn.addActionListener(this);
      btnBar.add(cancelBtn);
      btnBar.add(saveBtn);
    } else {
      cancelBtn = new JButton(JMeterUtils.getResString("textbox_close"));
      cancelBtn.setActionCommand("close_dialog");
      cancelBtn.addActionListener(this);
      btnBar.add(cancelBtn);
    }

    Container panel = this.dialog.getContentPane();
    this.dialog.setMinimumSize(new Dimension(400, 250));
    panel.add(textBoxScrollPane, "Center");
    panel.add(btnBar, "South");
    Point p = this.parentFrame.getLocationOnScreen();
    Dimension d1 = this.parentFrame.getSize();
    Dimension d2 = this.dialog.getSize();
    this.dialog.setLocation(p.x + (d1.width - d2.width) / 2, p.y + (d1.height - d2.height) / 2);
    this.dialog.setAlwaysOnTop(true);
    this.dialog.pack();
  }

  private void closeDialog() {
    this.dialog.setVisible(false);
  }

  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();
    if ("cancel_dialog".equals(command)) {
      this.closeDialog();
      this.setTextBox(this.originalText);
    } else {
      this.closeDialog();
    }

  }

  public void setTextBox(String text) {
    this.originalText = text;
    this.textBox.setText(text);
  }

  public String getTextBox() {
    return this.textBox.getText();
  }

  public static class TextBoxDoubleClickPressed extends MouseAdapter {
    private JTable table = null;

    public TextBoxDoubleClickPressed(JTable table) {
      this.table = table;
    }

    public void mousePressed(MouseEvent e) {
      if (e.getClickCount() == 2) {
        TableModel tm = this.table.getModel();
        Object value = tm.getValueAt(this.table.getSelectedRow(), this.table.getSelectedColumn());
        if (value instanceof String) {
          if (this.table.getCellEditor() != null) {
            this.table.getCellEditor().cancelCellEditing();
          }

          org.apache.jmeter.gui.util.TextBoxDialoger
              tbd = new org.apache.jmeter.gui.util.TextBoxDialoger(value.toString(), true);
          tm.setValueAt(tbd.getTextBox(), this.table.getSelectedRow(),
              this.table.getSelectedColumn());
        }
      }

    }
  }

  public static class TextBoxDoubleClick extends MouseAdapter {
    private JTable table = null;

    public TextBoxDoubleClick(JTable table) {
      this.table = table;
    }

    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
        TableModel tm = this.table.getModel();
        Object value = tm.getValueAt(this.table.getSelectedRow(), this.table.getSelectedColumn());
        new TextBoxPopupDialog(value.toString(), false);
      }

    }
  }
}
