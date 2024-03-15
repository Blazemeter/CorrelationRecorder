package com.blazemeter.jmeter.correlation.core.templates;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CorrelationTemplatesRepositoryTest {

    private static final String REPOSITORY_NAME = "Test";

    private static final String TEMPLATE_NAME = "Template";

    private static final String TEMPLATE_V1 = "1.1";

    private static final String TEMPLATE_V2 = "1.2";


    @Test
    public void shouldAddTemplateWhenAddTemplate() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        correlationTemplatesRepository.addTemplate(TEMPLATE_NAME, TEMPLATE_V1);
        Map.Entry<String, CorrelationTemplateVersions> entry = correlationTemplatesRepository.getTemplates().entrySet().iterator().next();
        assertEquals(TEMPLATE_NAME, entry.getKey());
        assertEquals("[" + TEMPLATE_V1 + "]", entry.getValue().getVersions().toString());
    }

    @Test
    public void shouldEditTemplateVersionWhenAddTemplateWithNewTemplateVersion() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        correlationTemplatesRepository.addTemplate(TEMPLATE_NAME, TEMPLATE_V1);
        correlationTemplatesRepository.addTemplate(TEMPLATE_NAME, TEMPLATE_V2);
        Map.Entry<String, CorrelationTemplateVersions> entry = correlationTemplatesRepository.getTemplates().entrySet().iterator().next();
        assertEquals(TEMPLATE_NAME, entry.getKey());
        assertEquals("[" + TEMPLATE_V1 + ", " + TEMPLATE_V2 + "]", entry.getValue().getVersions().toString());
    }

    @Test
    public void shouldDoNothingWhenAddTemplateWithExistingTemplateVersion() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        correlationTemplatesRepository.addTemplate(TEMPLATE_NAME, TEMPLATE_V1);
        correlationTemplatesRepository.addTemplate(TEMPLATE_NAME, TEMPLATE_V1);
        Map.Entry<String, CorrelationTemplateVersions> entry = correlationTemplatesRepository.getTemplates().entrySet().iterator().next();
        assertEquals(TEMPLATE_NAME, entry.getKey());
        assertEquals("[" + TEMPLATE_V1 + "]", entry.getValue().getVersions().toString());
    }

    @Test
    public void shouldAddTemplateVersionWhenAddTemplateWithTamplateVersion() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        correlationTemplatesRepository.addTemplate(TEMPLATE_NAME, new CorrelationTemplateVersions(TEMPLATE_V1));
        Map.Entry<String, CorrelationTemplateVersions> entry = correlationTemplatesRepository.getTemplates().entrySet().iterator().next();
        assertEquals(TEMPLATE_NAME, entry.getKey());
        assertEquals("[" + TEMPLATE_V1 + "]", entry.getValue().getVersions().toString());
    }

    @Test
    public void shouldSetNameWhenSetName() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository("");
        correlationTemplatesRepository.setName(REPOSITORY_NAME);
        assertEquals(REPOSITORY_NAME, correlationTemplatesRepository.getName());
    }

    @Test
    public void shouldReturnTrueWhenEqualsWithItself() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        assertTrue(correlationTemplatesRepository.equals(correlationTemplatesRepository));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithNullObject() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        assertFalse(correlationTemplatesRepository.equals(null));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithObjectFromDifferentClass() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        assertFalse(correlationTemplatesRepository.equals("null"));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithDifferentNameObject() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        CorrelationTemplatesRepository correlationTemplatesRepository2 =
                new CorrelationTemplatesRepository(REPOSITORY_NAME + 2);
        assertFalse(correlationTemplatesRepository.equals(correlationTemplatesRepository2));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithDifferentTemplatesObject() {
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        correlationTemplatesRepository.addTemplate(TEMPLATE_NAME, new CorrelationTemplateVersions(TEMPLATE_V1));
        CorrelationTemplatesRepository correlationTemplatesRepository2 =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        correlationTemplatesRepository2.addTemplate(TEMPLATE_NAME, new CorrelationTemplateVersions(TEMPLATE_V2));
        assertFalse(correlationTemplatesRepository.equals(correlationTemplatesRepository2));
    }

    @Test
    public void shouldReturnTrueWhenEqualsWithEqualObjects() {
        Map<String, CorrelationTemplateVersions> templatesVersions = new HashMap<>();
        templatesVersions.put(TEMPLATE_NAME, new CorrelationTemplateVersions(TEMPLATE_V1));
        CorrelationTemplatesRepository correlationTemplatesRepository =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        correlationTemplatesRepository.setTemplates(templatesVersions);
        CorrelationTemplatesRepository correlationTemplatesRepository2 =
                new CorrelationTemplatesRepository(REPOSITORY_NAME);
        correlationTemplatesRepository2.setTemplates(templatesVersions);
        assertTrue(correlationTemplatesRepository.equals(correlationTemplatesRepository2));
    }



}
