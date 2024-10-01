package com.blazemeter.jmeter.correlation.core.templates;

import com.blazemeter.jmeter.correlation.core.templates.repository.TemplateProperties;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ProtocolTest {

    private static final String PROTOCOL_NAME = "Test";
    private static final String CUSTOM_TEMPLATE_REFERENCE_NAME = "custom";
    private static final String TEMPLATE_VERSION_ONE = "1.0";
    private static final String TEMPLATE_FILE_SUFFIX = "template.json";

    @Test
    public void shouldSetTemplatesAndPropertiesWhenSetTemplatesAndProperties() {
        TemplateProperties templateProperties = new TemplateProperties();
        Template.Builder builder = new Template.Builder();
        builder.withId(CUSTOM_TEMPLATE_REFERENCE_NAME).withVersion(TEMPLATE_VERSION_ONE);
        Template template = builder.build();
        Protocol protocol = new Protocol(PROTOCOL_NAME);
        Map<Template, TemplateProperties> templatesAndProperties = new HashMap<>();
        templatesAndProperties.put(template,templateProperties);
        protocol.setTemplatesAndProperties(templatesAndProperties);
        assertEquals(templatesAndProperties, protocol.getTemplatesAndProperties());
    }

    @Test
    public void shouldAddTemplateAndPropertyWhenAddTemplate() {
        TemplateProperties templateProperties = new TemplateProperties();
        Template.Builder builder = new Template.Builder();
        builder.withId(CUSTOM_TEMPLATE_REFERENCE_NAME).withVersion(TEMPLATE_VERSION_ONE);
        Template template = builder.build();
        Protocol protocol = new Protocol(PROTOCOL_NAME);
        protocol.addTemplate(template, templateProperties);
        Map<Template, TemplateProperties> templatesAndProperties = protocol.getTemplatesAndProperties();
        Map.Entry<Template, TemplateProperties> entry = templatesAndProperties.entrySet().iterator().next();
        assertEquals(template, entry.getKey());
        assertEquals(templateProperties, entry.getValue());
    }

    @Test
    public void shouldSetNameWhenSetName() {
        Protocol protocol = new Protocol("");
        protocol.setName(PROTOCOL_NAME);
        assertEquals(PROTOCOL_NAME, protocol.getName());
    }
}
