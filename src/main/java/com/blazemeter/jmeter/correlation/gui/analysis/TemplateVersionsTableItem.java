package com.blazemeter.jmeter.correlation.gui.analysis;

import com.blazemeter.jmeter.correlation.core.templates.Template;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class represents the row in the TemplateSelectionTable.
 * It contains the name of the template, the list of the versions associated with that template,
 * and the selected version (represented as a TemplateVersion).
 */
public class TemplateVersionsTableItem {
  private boolean selected = false;
  private transient String name;
  private List<Template> templates = new ArrayList<>();
  private Template selectedTemplate;

  //Constructor added to avoid issues with the serialization
  public TemplateVersionsTableItem() {

  }

  public TemplateVersionsTableItem(Template version) {
    this.name = version.getId();
    this.templates.add(version);
    this.selectedTemplate = version;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public List<String> getVersions() {
    return templates.stream()
        .map(Template::getVersion)
        .collect(Collectors.toList());
  }

  public void addVersion(Template version) {
    this.templates.add(version);
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public boolean isSelected() {
    return selected;
  }

  public String getSelectedVersion() {
    return selectedTemplate.getVersion();
  }

  public void setSelectedVersion(String selectedVersion) {
    this.selectedTemplate = templates.stream()
        .filter(templateVersion -> templateVersion.getVersion().equals(selectedVersion))
        .findFirst()
        .orElse(null);

    if (this.selectedTemplate == null) {
      throw new IllegalArgumentException("Version " + selectedVersion + " not found");
    }
  }

  public Template getSelectedTemplateVersion() {
    return selectedTemplate;
  }

  @Override
  public String toString() {
    return "TemplateVersionsTableItem {"
        + "name=" + name
        + "versions=" + templates
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TemplateVersionsTableItem that = (TemplateVersionsTableItem) o;
    return Objects.equals(name, that.getName())
        && Objects.equals(templates, that.getVersions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, templates);
  }
}
