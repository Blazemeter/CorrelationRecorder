package com.blazemeter.jmeter.correlation.core.templates;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TemplateVersionKeyDeserializerTest {

    private static final String SIEBEL_TEMPLATE_REFERENCE_NAME = "siebel";
    private static final String TEMPLATE_VERSION_ONE = "1.0";
    private static final String TEMPLATE_FILE_SUFFIX = "template.json";
    private static final String SIEBEL_TEMPLATE_VERSION_ONE_NAME = SIEBEL_TEMPLATE_REFERENCE_NAME + "-" + TEMPLATE_VERSION_ONE + "-" + TEMPLATE_FILE_SUFFIX;

    @Test
    public void shouldReturnTemplateVersionWhenDeserializeKey() {
        TemplateVersionKeyDeserializer templateVersionDeserializer = new TemplateVersionKeyDeserializer();
        TemplateVersion tversion = (TemplateVersion) templateVersionDeserializer.deserializeKey(SIEBEL_TEMPLATE_VERSION_ONE_NAME, null);
        assertEquals(SIEBEL_TEMPLATE_REFERENCE_NAME + " " + TEMPLATE_VERSION_ONE, tversion.toString());
        assertEquals(SIEBEL_TEMPLATE_REFERENCE_NAME, tversion.getName());
        assertEquals(TEMPLATE_VERSION_ONE, tversion.getVersion());
    }
}
