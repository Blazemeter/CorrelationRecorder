package com.blazemeter.jmeter.correlation.core.templates;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CorrelationTemplateVersionsTest {

    private static final String CONFIG_NAME = "Test";
    private static final String TEMPLATE_V1 = "1.1";
    private static final String TEMPLATE_V2 = "1.2";
    private static final String TEMPLATE_V3 = "1.3";


    @Test
    public void shouldReturnNameWhenGetName() {
        List<String> versionList = new ArrayList<>(Arrays.asList(TEMPLATE_V1, TEMPLATE_V2));
        CorrelationTemplateVersions correlationTemplateVersions = new CorrelationTemplateVersions(CONFIG_NAME, versionList);
        assertEquals(CONFIG_NAME, correlationTemplateVersions.getName());
    }

    @Test
    public void shouldReturnTrueWhenEqualsWithItself() {
        List<String> versionList = new ArrayList<>(Arrays.asList(TEMPLATE_V1, TEMPLATE_V2));
        CorrelationTemplateVersions correlationTemplateVersions = new CorrelationTemplateVersions(CONFIG_NAME, versionList);
        assertTrue(correlationTemplateVersions.equals(correlationTemplateVersions));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithNull() {
        List<String> versionList = new ArrayList<>(Arrays.asList(TEMPLATE_V1, TEMPLATE_V2));
        CorrelationTemplateVersions correlationTemplateVersions = new CorrelationTemplateVersions(CONFIG_NAME, versionList);
        assertFalse(correlationTemplateVersions.equals(null));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithObjectWithDifferentClass() {
        List<String> versionList = new ArrayList<>(Arrays.asList(TEMPLATE_V1, TEMPLATE_V2));
        CorrelationTemplateVersions correlationTemplateVersions = new CorrelationTemplateVersions(CONFIG_NAME, versionList);
        assertFalse(correlationTemplateVersions.equals("null"));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithObjectWithDifferentName() {
        List<String> versionList = new ArrayList<>(Arrays.asList(TEMPLATE_V1, TEMPLATE_V2));
        CorrelationTemplateVersions correlationTemplateVersions = new CorrelationTemplateVersions(CONFIG_NAME, versionList);
        CorrelationTemplateVersions correlationTemplateVersions2 = new CorrelationTemplateVersions(CONFIG_NAME+2, versionList);
        assertFalse(correlationTemplateVersions.equals(correlationTemplateVersions2));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithObjectWithDifferentVerionList() {
        List<String> versionList = new ArrayList<>(Arrays.asList(TEMPLATE_V1, TEMPLATE_V2));
        CorrelationTemplateVersions correlationTemplateVersions = new CorrelationTemplateVersions(CONFIG_NAME, versionList);
        CorrelationTemplateVersions correlationTemplateVersions2 = new CorrelationTemplateVersions(CONFIG_NAME+2,  new ArrayList<>(Arrays.asList(TEMPLATE_V3)));
        assertFalse(correlationTemplateVersions.equals(correlationTemplateVersions2));
    }

    @Test
    public void shouldReturnTrueWhenEqualsWithEqualObject() {
        List<String> versionList = new ArrayList<>(Arrays.asList(TEMPLATE_V1, TEMPLATE_V2));
        CorrelationTemplateVersions correlationTemplateVersions = new CorrelationTemplateVersions(CONFIG_NAME, versionList);
        CorrelationTemplateVersions correlationTemplateVersions2 = new CorrelationTemplateVersions(CONFIG_NAME, versionList);
        assertTrue(correlationTemplateVersions.equals(correlationTemplateVersions2));
    }
}
