package com.blazemeter.jmeter.correlation.core.suggestions.method;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.automatic.Configuration;
import com.blazemeter.jmeter.correlation.core.automatic.DynamicElement;
import com.blazemeter.jmeter.correlation.core.suggestions.context.ComparisonContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DynamicElementHandlerTest {

  private static final String FIRST_PARAM_NAME = "param1";
  private static final String SECOND_PARAM_NAME = "param2";
  private static final String FIRST_VALUE = "value1";
  private static final String SECOND_VALUE = "value2";
  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private ComparisonContext context; // This should be mocked

  private DynamicElementHandler dynamicElementHandler;

  @Before
  public void setUp() {
    context = new ComparisonContext(new Configuration());
    dynamicElementHandler = new DynamicElementHandler(context);
  }

  @Test
  public void shouldReturnEmptyDynamicElementsWhenGetDynamicElementsWithEmptyMaps() {
    List<DynamicElement>
        dynamicElements = dynamicElementHandler.getDynamicElements(
        new HashMap<>(), new HashMap<>());

    softly.assertThat(dynamicElements).isEmpty();
  }

  @Test
  public void shouldReturnDynamicElementsWhenGetDynamicElementsWithNonSharedParams() {
    Map<String, List<Appearances>> originalMap = new HashMap<>();
    Map<String, List<Appearances>> replayMap = new HashMap<>();

    JMeterTestUtils.HttpSamplerBuilder
        builder = new JMeterTestUtils.HttpSamplerBuilder("GET", "test.com", "/")
        .withArgument(FIRST_PARAM_NAME, FIRST_VALUE)
        .withArgument(SECOND_PARAM_NAME, SECOND_VALUE);

    HTTPSampler build = builder.build();
    originalMap.put(
        FIRST_PARAM_NAME,
        Collections.singletonList(new Appearances(FIRST_PARAM_NAME, FIRST_VALUE, build)));
    originalMap.put(
        SECOND_PARAM_NAME,
        Collections.singletonList(new Appearances(SECOND_PARAM_NAME, SECOND_VALUE, build)));
    replayMap.put(
        FIRST_PARAM_NAME,
        Collections.singletonList(new Appearances(FIRST_PARAM_NAME, SECOND_VALUE, build)));

    List<DynamicElement> dynamicElements =
        dynamicElementHandler.getDynamicElements(originalMap, replayMap);

    softly.assertThat(dynamicElements).isNotEmpty();
    softly.assertThat(dynamicElements).hasSize(2);
    softly.assertThat(dynamicElements.get(0).getName()).isEqualTo(SECOND_PARAM_NAME);
    softly.assertThat(dynamicElements.get(1).getName()).isEqualTo(FIRST_PARAM_NAME);
  }
}