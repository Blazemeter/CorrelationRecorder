package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExtractionStrategyTest {

  @Test
  public void shouldReturnedUrlDecodedResultWhenSearchValue() {

    Pair<LocationType, String> result =
        ExtractionStrategy.getLocationAndValue("Hello World", "Hello%20World",
            LocationType.COOKIE);
    Pair<LocationType, String> expected = new ImmutablePair<>(LocationType.COOKIE, "Hello World");
    assertEquals(expected, result);
  }

  @Test
  public void shouldReturnedUrlEncodedResultWhenSearchValue() {
    // Note: java.net.URLEncode use the old "plus" (+) for spaces and not the %20
    // JMeter use the same implementation
    Pair<LocationType, String> result =
        ExtractionStrategy.getLocationAndValue("Hello+World", "Hello World",
            LocationType.COOKIE);
    Pair<LocationType, String> expected = new ImmutablePair<>(LocationType.COOKIE, "Hello+World");
    assertEquals(expected, result);
  }
}
