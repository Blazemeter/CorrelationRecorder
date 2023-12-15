package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import java.util.HashMap;
import java.util.Map;

public class Repository {
  private String name;
  private String displayName;
  private Map<String, Protocol> protocolsMap = new HashMap<>();

  public Repository(String name) {
    this.name = name;
  }

  public void addTemplate(Template template, TemplateProperties properties) {
    String protocolName = template.getId();
    Protocol protocol = protocolsMap.get(protocolName);
    if (protocol == null) {
      protocol = new Protocol(protocolName);
      protocolsMap.put(protocolName, protocol);
    }
    protocol.addTemplate(template, properties);
  }

  public void addProtocol(Protocol protocol) {
    String protocolName = protocol.getName();
  }

  public Protocol getProtocol(Protocol protocol) {
    return protocolsMap.get(protocol.getName());
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, Protocol> getProtocols() {
    return protocolsMap;
  }

  public void setProtocolsMap(
      Map<String, Protocol> protocolsMap) {
    this.protocolsMap = protocolsMap;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}
