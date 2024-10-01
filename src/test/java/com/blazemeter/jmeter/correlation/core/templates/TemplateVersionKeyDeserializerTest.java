package com.blazemeter.jmeter.correlation.core.templates;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TemplateVersionKeyDeserializerTest {

    private static final String TEST_TEMPLATE_REFERENCE_NAME = "test";
    private static final String TEMPLATE_VERSION_ONE = "1.0";
    private static final String TEMPLATE_FILE_SUFFIX = "template.json";
    private static final String TEST_TEMPLATE_VERSION_ONE_NAME = TEST_TEMPLATE_REFERENCE_NAME + "-" + TEMPLATE_VERSION_ONE + "-" + TEMPLATE_FILE_SUFFIX;

    @Test
    public void shouldReturnTemplateVersionWhenDeserializeKey() {
        TemplateVersionKeyDeserializer templateVersionDeserializer = new TemplateVersionKeyDeserializer();
        TemplateVersion templateVersion = (TemplateVersion) templateVersionDeserializer.deserializeKey(
            TEST_TEMPLATE_VERSION_ONE_NAME, null);
        assertEquals(TEST_TEMPLATE_REFERENCE_NAME + " " + TEMPLATE_VERSION_ONE, templateVersion.toString());
        assertEquals(TEST_TEMPLATE_REFERENCE_NAME, templateVersion.getName());
        assertEquals(TEMPLATE_VERSION_ONE, templateVersion.getVersion());
    }
}
