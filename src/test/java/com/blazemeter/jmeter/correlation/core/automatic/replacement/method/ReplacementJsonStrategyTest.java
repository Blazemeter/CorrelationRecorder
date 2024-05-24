package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import static org.fest.assertions.Assertions.assertThat;

import com.blazemeter.jmeter.correlation.JMeterTestUtils;
import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.method.JsonBodyExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.JsonCorrelationReplacement;
import java.io.IOException;
import java.util.Collections;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.testelement.TestElement;
import org.junit.Before;
import org.junit.Test;

public class ReplacementJsonStrategyTest {

  private static final String RESOURCES_PATH = "/responses/json/";
  private ReplacementJsonStrategy replacementJsonStrategy;

  @Before
  public void setup() throws IOException {
    replacementJsonStrategy = new ReplacementJsonStrategy();
  }

  @Test
  public void shouldReturnNullWhenCantFindPath() throws IOException {
    String json = TestUtils.getFileContent(RESOURCES_PATH + "mendixJson.json", getClass());
    HTTPSamplerBase sampler = new HTTPSampler();
    sampler.addArgument("", json);
    Appearances appearances = new Appearances("NotValid", "", sampler);
    CorrelationReplacement<?> actual = replacementJsonStrategy.generateReplacement(sampler, appearances, "csrftoken");
    assertThat(actual).isNull();
  }

  @Test
  public void shouldReturnNullWhenUsesSamplerWithArguments() {
    HTTPSamplerBase sampler = new HTTPSampler();
    sampler.addArgument("Key1", "Value1");
    sampler.addArgument("Key2", "Value2");
    CorrelationReplacement<?> actual = replacementJsonStrategy.generateReplacement(new HeaderManager(), null, "csrftoken");
    assertThat(actual).isNull();
  }

  @Test
  public void shouldReturnNullWhenSamplerIsNotHTTPSampler() {
    CorrelationReplacement<?> actual = replacementJsonStrategy.generateReplacement(new HeaderManager(), null, "csrftoken");
    assertThat(actual).isNull();
  }

  @Test
  public void shouldReturnCorrelationReplacementWhenFindPath() throws IOException {
    String json = TestUtils.getFileContent(RESOURCES_PATH + "mendixJson.json", getClass());
    HTTPSamplerBase sampler = new HTTPSampler();
    sampler.addArgument("", json);
    Appearances appearances = new Appearances("a6e9ad8f-cc16-42b0-b189-8dd1ea531cf4", "", sampler);
    CorrelationReplacement<?> actual = replacementJsonStrategy.generateReplacement(sampler, appearances, "csrftoken");
    JsonCorrelationReplacement<?> expected = new JsonCorrelationReplacement<>("$.csrftoken");
    expected.setVariableName("csrftoken");
    assertThat(actual).isEqualTo(expected);
  }

}
