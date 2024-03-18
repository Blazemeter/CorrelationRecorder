package com.blazemeter.jmeter.correlation.core.templates;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RepositoryTest {

    private static final String REPOSITORY_NAME = "Test";

    @Test
    public void shouldCreateRepositoryWithNameWhenRepository() {
        Repository repo = new Repository(REPOSITORY_NAME);
        assertEquals(REPOSITORY_NAME, repo.getName());
    }

}
