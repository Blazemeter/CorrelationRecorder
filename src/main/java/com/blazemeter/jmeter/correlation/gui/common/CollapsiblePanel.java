package com.blazemeter.jmeter.correlation.gui.common;

import com.blazemeter.jmeter.commons.SwingUtils;
import com.blazemeter.jmeter.correlation.gui.RulesContainer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class CollapsiblePanel extends JPanel {

  private final Header header;
  private final JComponent contentComponent;
  private boolean collapsed;

  private CollapsiblePanel(Builder builder) {
    setName(builder.namePrefix + "-collapsiblePanel");
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.header = new Header(builder);
    add(this.header);
    this.contentComponent = builder.content;
    add(builder.content);
    if (builder.isCollapsed) {
      toggleCollapsed();
    }
  }

  public void toggleCollapsed() {
    collapsed = !collapsed;
    header.toggleCollapsed();
    contentComponent.setVisible(!collapsed);
  }

  public boolean isCollapsed() {
    return collapsed;
  }

  public JPanel getHeaderPanel() {
    return this.header;
  }

  public void setEnabled(boolean enabled) {
    header.setEnabled(enabled);
  }

  public String getId() {
    return header.getPanelTitle();
  }

  private class Header extends JPanel {

    private final JTextField name;
    private final JPanel buttonsPanel;
    private final ImageIcon collapsedIcon = ThemedIcon.fromResourceName("collapsed.png");
    private final ImageIcon expandedIcon = ThemedIcon.fromResourceName("expanded.png");
    private final JButton collapseButton;
    private boolean enable;

    private Header(Builder builder) {
      //Used to avoid issues while testing
      setName(builder.namePrefix + "-collapsiblePanel-header");
      setBorder(BorderFactory.createLineBorder(Color.GRAY));
      setBackground(UIManager.getColor("Label.background"));
      setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
      this.enable = builder.isEnabled;
      if (builder.enablingListener != null) {
        /*
        we pass each field of builder instead of builder as parameter to avoid tying this instance
        listeners to builder, which might change after creating this object.
         */
        add(buildEnableCheckBox(builder.enablingListener));
        addGap();
      }
      name = buildNameField(builder.title, builder.editableTitle);
      addFocusListener(buildDisableNameOnFocusLostListener(name));
      add(name);
      if (builder.editableTitle) {
        addGap();
        add(buildEditTitleIcon(name));
        addGap();
      }
      add(Box.createHorizontalGlue());
      buttonsPanel = buildButtonsPanel(builder.buttons);
      add(buttonsPanel);
      add(Box.createHorizontalGlue());
      collapseButton = buildCollapseButton(builder.collapsingListener);
      add(collapseButton);
      int height = calcHeight(builder.buttons);
      setPreferredSize(new Dimension(RulesContainer.MAIN_CONTAINER_WIDTH - 10, height));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    private JCheckBox buildEnableCheckBox(Consumer<Boolean> enablingListener) {
      JCheckBox ret = SwingUtils
          .createComponent(getName() + "-disableCheck", new JCheckBox());
      ret.setSelected(this.enable);
      ret.addItemListener(e -> {
        this.enable = !this.enable;
        ret.setSelected(this.enable);
        enablingListener.accept(this.enable);
      });
      return ret;
    }

    private void addGap() {
      add(Box.createRigidArea(new Dimension(5, 0)));
    }

    private JTextField buildNameField(String title, boolean editable) {
      JTextField ret = new JTextField(title, 10);
      ret.setName(getName() + "-title");
      ret.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
      ret.setEditable(false);
      if (editable) {
        ret.addFocusListener(buildDisableNameOnFocusLostListener(ret));
        ret.addActionListener(e -> ret.setEditable(false));
      }
      return ret;
    }

    private FocusAdapter buildDisableNameOnFocusLostListener(JTextField name) {
      return new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          name.setEditable(false);
        }
      };
    }

    private JLabel buildEditTitleIcon(JTextField nameField) {
      JLabel ret = new JLabel();
      ret.setIcon(ThemedIcon.fromResourceName("pencil-edit.png"));
      ret.setName(getName() + "-editTitleIcon");
      ret.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          nameField.setEditable(true);
          nameField.requestFocus();
        }

        public void mouseEntered(MouseEvent e) {
          setCursor(Cursor.HAND_CURSOR);
        }

        public void mouseExited(MouseEvent e) {
          setCursor(Cursor.DEFAULT_CURSOR);
        }
      });
      return ret;
    }

    private JPanel buildButtonsPanel(List<JButton> buttons) {
      JPanel ret = new JPanel();
      ret.setLayout(new BoxLayout(ret, BoxLayout.LINE_AXIS));
      ret.setName(getName() + "-buttonsPanel");
      for (JButton button : buttons) {
        ret.add(button);
        ret.add(Box.createRigidArea(new Dimension(10, 0)));
      }
      return ret;
    }

    private JButton buildCollapseButton(Runnable collapsingListeners) {
      JButton ret = new JButton(expandedIcon);
      ret.setName(getName() + "-collapseButton");
      //Making the button looks like a label
      ret.setFocusPainted(false);
      ret.setMargin(new Insets(0, 0, 0, 0));
      ret.setContentAreaFilled(false);
      ret.setBorderPainted(false);
      ret.setOpaque(false);
      ret.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          CollapsiblePanel.this.toggleCollapsed();
          collapsingListeners.run();
        }

        public void mouseEntered(MouseEvent e) {
          setCursor(Cursor.HAND_CURSOR);
        }

        public void mouseExited(MouseEvent e) {
          setCursor(Cursor.DEFAULT_CURSOR);
        }
      });
      return ret;
    }

    private int calcHeight(List<JButton> buttons) {
      return buttons.stream()
          .mapToInt(button -> (int) button.getMinimumSize().getHeight())
          .max()
          .orElse(RulesContainer.ROW_PREFERRED_HEIGHT);
    }

    public void setEnabled(boolean enabled) {
      name.setEditable(true);
      name.setForeground(
          enabled ? new JTextField().getForeground() : new JTextField().getDisabledTextColor());
      name.setEditable(false);
    }

    private String getPanelTitle() {
      return name.getText();
    }

    private void setCursor(int cursor) {
      this.setCursor(Cursor.getPredefinedCursor(cursor));
    }

    private void toggleCollapsed() {
      boolean isCollapsed = collapseButton.getIcon().equals(expandedIcon);
      buttonsPanel.setVisible(!isCollapsed);
      collapseButton.setIcon(isCollapsed ? collapsedIcon : expandedIcon);
    }

    @Override
    public int getHeight() {
      return RulesContainer.ROW_PREFERRED_HEIGHT;
    }

  }

  public static final class Builder {

    private String namePrefix = "";
    private String title = "";
    private boolean editableTitle = false;
    private List<JButton> buttons = Collections.emptyList();
    private Runnable collapsingListener;
    private boolean isEnabled = true;
    private boolean isCollapsed = false;
    private Consumer<Boolean> enablingListener;
    private JComponent content;

    public Builder() {
    }

    public Builder withNamePrefix(String prefix) {
      this.namePrefix = prefix;
      return this;
    }

    public Builder withTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder withEditableTitle() {
      this.editableTitle = true;
      return this;
    }

    public Builder withButtons(List<JButton> buttons) {
      this.buttons = buttons;
      return this;
    }

    public Builder withCollapsingListener(Runnable collapsingListener) {
      this.collapsingListener = collapsingListener;
      return this;
    }

    public Builder withEnablingListener(Consumer<Boolean> enablingListener) {
      this.enablingListener = enablingListener;
      return this;
    }

    public Builder withEnabled(boolean isEnabled) {
      this.isEnabled = isEnabled;
      return this;
    }

    public Builder withCollapsed(boolean isCollapsed) {
      this.isCollapsed = isCollapsed;
      return this;
    }

    public Builder withContent(JComponent content) {
      this.content = content;
      return this;
    }

    public CollapsiblePanel build() {
      return new CollapsiblePanel(this);
    }

  }
}
