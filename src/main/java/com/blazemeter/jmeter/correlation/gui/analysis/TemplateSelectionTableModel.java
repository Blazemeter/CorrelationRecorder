package com.blazemeter.jmeter.correlation.gui.analysis;

import com.blazemeter.jmeter.correlation.core.templates.Protocol;
import com.blazemeter.jmeter.correlation.core.templates.Repository;
import com.blazemeter.jmeter.correlation.core.templates.Template;
import com.blazemeter.jmeter.correlation.core.templates.TemplateVersion;
import com.blazemeter.jmeter.correlation.core.templates.repository.Properties;
import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.swing.table.AbstractTableModel;


/**
 * The model for the TemplateSelectionTable. Added to simplify how we obtain the
 * selected TemplateVersion.
 * The versionList uses a TemplateVersionsTableItem to represent the row in the table.
 */
public class TemplateSelectionTableModel extends AbstractTableModel {
  private final String[] columnNames = {"Select", "Repository", "Name", "Version"};
  private final List<TemplateVersionsTableItem> versionList = new ArrayList<>();
  private Map<Template, TemplateProperties> templatesAndProperties = new HashMap<>();
  private Map<String, String> repositoryIdToName = new HashMap<>();

  public TemplateSelectionTableModel() {
  }

  @Override
  public int getRowCount() {
    return versionList.size();
  }

  @Override
  public int getColumnCount() {
    return columnNames.length;
  }

  @Override
  public String getColumnName(int column) {
    return columnNames[column];
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if (columnIndex == 0) {
      return Boolean.class;
    } else if (columnIndex == 3) {
      return TemplateVersionsTableItem.class;
    } else {
      return String.class;
    }
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    if (columnIndex == 0) {
      return canUseTemplate(rowIndex);
    }
    return columnIndex == 3;
  }

  public boolean canUseTemplate(int rowIndex) {
    Template version = versionList.get(rowIndex).getSelectedTemplateVersion();
    Properties properties = new Properties();
    properties.putAll(templatesAndProperties.get(version));
    return properties.canUse();
  }

  public boolean canUseTemplate(Template selected) {
    Properties properties = new Properties();
    properties.putAll(templatesAndProperties.get(selected));
    return properties.canUse();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    TemplateVersionsTableItem templateRow = versionList.get(rowIndex);
    if (columnIndex == 0) {
      return templateRow.isSelected();
    } else if (columnIndex == 1) {
      return templateRow.getSelectedTemplateVersion().getRepositoryId();
    } else if (columnIndex == 2) {
      return templateRow.getName();
    } else {
      return templateRow;
    }
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    TemplateVersionsTableItem templateRow = versionList.get(rowIndex);
    if (columnIndex == 0) {
      templateRow.setSelected((Boolean) aValue);
    } else if (columnIndex == 3) {
      TemplateVersionsTableItem value = (TemplateVersionsTableItem) aValue;
      templateRow.setSelectedVersion(value.getSelectedVersion());
    }
  }

  private static Predicate<TemplateVersionsTableItem> areEquals(Template template) {
    return item -> item.getName().equals(template.getId())
        && item.getSelectedTemplateVersion().getRepositoryId()
        .equals(template.getRepositoryId());
  }

  public Template getSelectedTemplateAt(int row) {
    if (row > versionList.size()) {
      return null;
    }
    return versionList.get(row).getSelectedTemplateVersion();
  }

  public boolean notSelectedTemplates() {
    return versionList.stream().noneMatch(TemplateVersionsTableItem::isSelected);
  }

  public List<Template> getSelectedTemplates() {
    return versionList.stream()
        .filter(TemplateVersionsTableItem::isSelected)
        .map(TemplateVersionsTableItem::getSelectedTemplateVersion)
        .collect(java.util.stream.Collectors.toList());
  }

  public void updateTableContentWithRepositories(Map<String, Repository> repositoryMap) {
    templatesAndProperties.clear();
    versionList.clear();

    // We will iterate over the repositoryList
    for (Map.Entry<String, Repository> repositoryEntry : repositoryMap.entrySet()) {
      // We will iterate over the versionList
      Repository repository = repositoryEntry.getValue();
      repositoryIdToName.put(repositoryEntry.getKey(), repository.getDisplayName());
      Map<String, Protocol> protocols = repository.getProtocols();
      for (Map.Entry<String, Protocol> protocolEntry : protocols.entrySet()) {
        Protocol protocol = protocolEntry.getValue();

        Map<Template, TemplateProperties> templatesInfo = protocol.getTemplatesAndProperties();

        // We keep a list of all the templates and their properties to simplify the search later
        templatesAndProperties.putAll(templatesInfo);

        // Get the first element of the map
        Set<Map.Entry<Template, TemplateProperties>> templatesEntry = templatesInfo.entrySet();
        Template firstTemplate = templatesEntry.iterator().next().getKey();

        TemplateVersionsTableItem item = new TemplateVersionsTableItem(firstTemplate);
        for (Map.Entry<Template, TemplateProperties> templatePropertiesEntry : templatesEntry) {
          Template template = templatePropertiesEntry.getKey();
          if (firstTemplate.equals(template)) {
            continue;
          }
          item.addVersion(template);
        }

        // We keep a list of all the TemplateVersionsTableItem to simplify the display
        versionList.add(item);
      }
    }
  }

  public Map<String, List<TemplateVersion>> getSelectedTemplateWithRepositoryMap() {
    List<Template> selectedTemplates = getSelectedTemplates();
    Map<String, List<TemplateVersion>> repositoryAndTemplates = new HashMap<>();

    for (Template template : selectedTemplates) {
      String repositoryId = template.getRepositoryId();
      if (!repositoryAndTemplates.containsKey(repositoryId)) {
        repositoryAndTemplates.put(repositoryId, new ArrayList<>());
      }
      repositoryAndTemplates.get(repositoryId).add(template.toTemplateVersion());
    }
    return repositoryAndTemplates;
  }

  public String getRepositoryDisplayName(String repositoryId) {
    return repositoryIdToName.get(repositoryId);
  }
}
