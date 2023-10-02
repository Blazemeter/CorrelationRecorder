package com.blazemeter.jmeter.correlation.gui.templates;

import com.blazemeter.jmeter.correlation.core.templates.CorrelationTemplateVersions;
import com.blazemeter.jmeter.correlation.gui.common.PlaceHolderTextField;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

public class PlaceHolderComboBox extends JComboBox<CorrelationTemplateVersions> {

  public PlaceHolderComboBox() {
    super();
    setEditable(true);

    PlaceHolderTextField placeholderTextField = new PlaceHolderTextField();
    placeholderTextField.setPlaceHolder("Type a new template name or select existing");
    placeholderTextField.setBorder(null);
    placeholderTextField.setName("templateName");
    setEditor(new PlaceholderComboBoxEditor(placeholderTextField));
    setSelectedItem(null);
    setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value,
                                                    int index, boolean isSelected,
                                                    boolean cellHasFocus) {
        Component component =
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof CorrelationTemplateVersions) {
          setText(((CorrelationTemplateVersions) value).getName());
        }
        return component;
      }
    });
  }

  public void setTemplates(List<CorrelationTemplateVersions> templates) {
    DefaultComboBoxModel<CorrelationTemplateVersions> model =
        new DefaultComboBoxModel<>(templates.toArray(new CorrelationTemplateVersions[0]));
    setModel(model);
    setSelectedItem(null);
  }

  private class PlaceholderComboBoxEditor implements ComboBoxEditor {
    private PlaceHolderTextField textField;

    PlaceholderComboBoxEditor(PlaceHolderTextField textField) {
      this.textField = textField;
    }

    @Override
    public Component getEditorComponent() {
      return textField;
    }

    @Override
    public void setItem(Object anObject) {
      if (anObject instanceof CorrelationTemplateVersions) {
        textField.setText(((CorrelationTemplateVersions) anObject).getName());
      } else {
        textField.setText("");
      }
    }

    @Override
    public Object getItem() {
      String text = textField.getText();
      for (int i = 0; i < getItemCount(); i++) {
        CorrelationTemplateVersions template = getItemAt(i);
        if (template.getName().equals(text)) {
          return template;
        }
      }
      return null;
    }

    @Override
    public void selectAll() {
      textField.selectAll();
    }

    @Override
    public void addActionListener(ActionListener l) {
      textField.addActionListener(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
      textField.removeActionListener(l);
    }

  }

  public void resetToDefault() {
    removeAllItems();
    setSelectedItem(null);
  }

  public List<String> getSelectedTemplateVersions() {
    return getSelectedTemplate().getVersions();
  }

  public CorrelationTemplateVersions getSelectedTemplate() {
    Object item = getEditor().getItem();

    PlaceHolderTextField editorComponent = (PlaceHolderTextField) getEditor().getEditorComponent();
    String text = editorComponent.getText();
    if (text.isEmpty()) {
      CorrelationTemplateVersions templateVersions = new CorrelationTemplateVersions();
      templateVersions.setName("");
      templateVersions.addVersion("0.0.0");
      return templateVersions;
    } else {
      if (item instanceof CorrelationTemplateVersions) {
        return (CorrelationTemplateVersions) item;
      } else {
        CorrelationTemplateVersions templateVersions = new CorrelationTemplateVersions();
        templateVersions.setName(text);
        templateVersions.addVersion("0.0.0");
        return templateVersions;
      }
    }
  }

  public void addEditorLostFocusListener(ActionListener l) {
    getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        l.actionPerformed(null);
      }
    });
  }
}
