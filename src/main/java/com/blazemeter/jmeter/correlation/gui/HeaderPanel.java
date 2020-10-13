package com.blazemeter.jmeter.correlation.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class HeaderPanel extends JPanel {

  private Container parent;
  private JLabel collapsedIcon;
  private JLabel expandedIcon;

  public HeaderPanel(String title) {
    setBorder(new JTextField().getBorder());
    setBackground((Color) UIManager.get("Panel.background"));
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    setName("headerPanel");

    JTextField name = new JTextField(title, 10);
    name.setName("headerPanelTitle");
    name.setEditable(false);
    name.setForeground((Color) UIManager.get("Label.foreground"));
    name.setBackground((Color) UIManager.get("Label.background"));
    name.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
    loadCollapsibleIcons();
    
    add(name);
    add(Box.createHorizontalGlue());
    add(collapsedIcon);
    add(expandedIcon);

    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        getParentNode((Container) e.getSource());
        if (parent != null) {
          parent.dispatchEvent(e);
        }
      }
      public void mouseEntered(MouseEvent e) {
        setCursor(Cursor.HAND_CURSOR);
      }
      public void mouseExited(MouseEvent e) {
        setCursor(Cursor.DEFAULT_CURSOR);
      }
    });
  }

  private void loadCollapsibleIcons() {
    collapsedIcon = new JLabel();
    collapsedIcon.setIcon(ThemedIcon.fromResourceName("expanded.png"));
    collapsedIcon.setName(getName() + "-collapsedIcon");

    expandedIcon = new JLabel();
    expandedIcon.setIcon(ThemedIcon.fromResourceName("collapsed.png"));
    expandedIcon.setName(getName()+"-expandedIcon");
    expandedIcon.setVisible(false);
  }

  private void getParentNode(Container panel) {
    if (panel == null) {
      return;
    }
    if ((panel instanceof CollapsiblePanelsList)) {
      parent = panel;
    }

    getParentNode(panel.getParent());
  }

  private void setCursor(int cursor) {
    this.setCursor(Cursor.getPredefinedCursor(cursor));
  }

  public void toggleCollapsed() {
    boolean isCollapsed = collapsedIcon.isVisible();
    collapsedIcon.setVisible(!isCollapsed);
    expandedIcon.setVisible(isCollapsed);
  }
}
