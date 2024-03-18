package com.blazemeter.jmeter.correlation.core.templates;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CorrelationTemplateDependencyTest {

    private static final String CORRELATION_TEMPLATE_DEPENDENCY_NAME = "Test";
    private static final String CORRELATION_TEMPLATE_DEPENDENCY_VERSION = "1.0";
    private static final String CORRELATION_TEMPLATE_DEPENDENCY_URL = "www.google.com";

    private static final String CORRELATION_TEMPLATE_DEPENDENCY_NAME_2 = "Test2";
    private static final String CORRELATION_TEMPLATE_DEPENDENCY_VERSION_2 = "2.0";
    private static final String CORRELATION_TEMPLATE_DEPENDENCY_URL_2 = "www.yahoo.com";
    @Test
    public void shouldSetNameWhenSetName() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency();
        correlationTemplateDependency.setName(CORRELATION_TEMPLATE_DEPENDENCY_NAME);
        assertEquals(CORRELATION_TEMPLATE_DEPENDENCY_NAME,correlationTemplateDependency.getName());
    }

    @Test
    public void shouldSetVersionWhenSetVersion() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency();
        correlationTemplateDependency.setVersion(CORRELATION_TEMPLATE_DEPENDENCY_VERSION);
        assertEquals(CORRELATION_TEMPLATE_DEPENDENCY_VERSION,correlationTemplateDependency.getVersion());
    }

    @Test
    public void shouldSetUrlWhenSetUrl() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency();
        correlationTemplateDependency.setUrl(CORRELATION_TEMPLATE_DEPENDENCY_URL);
        assertEquals(CORRELATION_TEMPLATE_DEPENDENCY_URL,correlationTemplateDependency.getUrl());
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithDifferentKindOfObject() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        assertFalse(correlationTemplateDependency.equals(""));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithObjectWithDifferentName() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        CorrelationTemplateDependency correlationTemplateDependency2 = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME_2, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        assertFalse(correlationTemplateDependency.equals(correlationTemplateDependency2));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithObjectWithDifferentVersion() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        CorrelationTemplateDependency correlationTemplateDependency2 = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION_2, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        assertFalse(correlationTemplateDependency.equals(correlationTemplateDependency2));
    }

    @Test
    public void shouldReturnFalseWhenEqualsWithObjectWithDifferentUrl() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        CorrelationTemplateDependency correlationTemplateDependency2 = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL_2);
        assertFalse(correlationTemplateDependency.equals(correlationTemplateDependency2));
    }

    @Test
    public void shouldReturnTrueWhenEqualsWithSameObject() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        assertTrue(correlationTemplateDependency.equals(correlationTemplateDependency));
    }

    @Test
    public void shouldReturnTrueWhenEqualsWithObjectWithSameInfo() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        CorrelationTemplateDependency correlationTemplateDependency2 = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        assertTrue(correlationTemplateDependency.equals(correlationTemplateDependency2));
    }

    @Test
    public void shouldReturnCorrectStringWhenToString() {
        CorrelationTemplateDependency correlationTemplateDependency = new CorrelationTemplateDependency(
                CORRELATION_TEMPLATE_DEPENDENCY_NAME, CORRELATION_TEMPLATE_DEPENDENCY_VERSION, CORRELATION_TEMPLATE_DEPENDENCY_URL);
        assertEquals("CorrelationTemplateDependency{name='Test', version='1.0', url='www.google.com'}",
                correlationTemplateDependency.toString());
    }
}
