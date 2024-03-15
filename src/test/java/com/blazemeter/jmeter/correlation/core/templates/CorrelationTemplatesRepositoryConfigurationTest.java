

package com.blazemeter.jmeter.correlation.core.templates;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CorrelationTemplatesRepositoryConfigurationTest {

    private static final String CONFIG_NAME = "Test";
    private static final String CONFIG_URL = "www.google.com";


    @Test
    public void shouldSetNameWhenSetName() {
        CorrelationTemplatesRepositoryConfiguration correlationTemplatesRepositoryConfiguration =
                new CorrelationTemplatesRepositoryConfiguration();
        correlationTemplatesRepositoryConfiguration.setName(CONFIG_NAME);
        assertEquals(CONFIG_NAME, correlationTemplatesRepositoryConfiguration.getName());
    }

    @Test
    public void shouldSetUrlWhenSetUrl() {
        CorrelationTemplatesRepositoryConfiguration correlationTemplatesRepositoryConfiguration =
                new CorrelationTemplatesRepositoryConfiguration();
        correlationTemplatesRepositoryConfiguration.setUrl(CONFIG_URL);
        assertEquals(CONFIG_URL, correlationTemplatesRepositoryConfiguration.getUrl());
    }

    @Test
    public void shouldReturnStringWhenToString() {
        CorrelationTemplatesRepositoryConfiguration correlationTemplatesRepositoryConfiguration =
                new CorrelationTemplatesRepositoryConfiguration(CONFIG_NAME, CONFIG_URL);
        String expectedString = "CorrelationTemplatesRepositoryConfiguration{name='Test', "
                                            +"url='www.google.com', "
                                            +"installedTemplates={}}";
        assertEquals(expectedString, correlationTemplatesRepositoryConfiguration.toString());
    }
}
