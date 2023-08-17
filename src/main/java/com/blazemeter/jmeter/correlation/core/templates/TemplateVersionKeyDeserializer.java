package com.blazemeter.jmeter.correlation.core.templates;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

public class TemplateVersionKeyDeserializer extends KeyDeserializer {

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) {
    String[] parts = key.split("-");
    TemplateVersion templateVersion = new TemplateVersion();
    templateVersion.setName(parts[0]);
    templateVersion.setVersion(parts[1]);
    return templateVersion;
  }
}

