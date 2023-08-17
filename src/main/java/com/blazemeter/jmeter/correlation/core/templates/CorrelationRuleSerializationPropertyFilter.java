package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.gui.CorrelationComponentsRegistry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import java.util.List;

public class CorrelationRuleSerializationPropertyFilter extends SimpleBeanPropertyFilter {

  public static final String FILTER_ID = "correlationRuleFilter";

  @Override
  public void serializeAsField(Object object, JsonGenerator jgen, SerializerProvider provider,
                               PropertyWriter writer) throws Exception {
    if (include(writer)) {

      CorrelationRulePartTestElement<?> correlationElement =
          (CorrelationRulePartTestElement<?>) object;

      if (correlationElement.equals(CorrelationComponentsRegistry.NONE_EXTRACTOR)
          || correlationElement.equals(CorrelationComponentsRegistry.NONE_REPLACEMENT)) {
        return;
      }

      List<ParameterDefinition> paramsDefinition = correlationElement.getParamsDefinition();
      for (int i = 0; i < paramsDefinition.size(); i++) {
        if (paramsDefinition.get(i).getName().endsWith(writer.getName())) {
          String currentObjectValue = correlationElement.getParams().get(i);
          String defaultObjectValue = paramsDefinition.get(i)
              .getDefaultValue();

          if (!defaultObjectValue.equals(currentObjectValue)) {
            writer.serializeAsField(object, jgen, provider);
          }
          return;
        }
      }
    } else if (!jgen.canOmitFields()) {
      writer.serializeAsOmittedField(object, jgen, provider);
    }
  }

  @Override
  public void serializeAsElement(Object elementValue, JsonGenerator gen, SerializerProvider prov,
                                 PropertyWriter writer) throws Exception {

  }

  @Override
  public void depositSchemaProperty(PropertyWriter writer, ObjectNode propertiesNode,
                                    SerializerProvider provider) throws JsonMappingException {

  }

  @Override
  public void depositSchemaProperty(PropertyWriter writer, JsonObjectFormatVisitor objectVisitor,
                                    SerializerProvider provider) throws JsonMappingException {

  }

  @Override
  protected boolean include(BeanPropertyWriter writer) {
    return true;
  }

  @Override
  protected boolean include(PropertyWriter writer) {
    return true;
  }
}
