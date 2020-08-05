package com.blazemeter.jmeter.correlation.core;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.correlation.TestUtils;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.FunctionCorrelationReplacement;
import com.blazemeter.jmeter.correlation.siebel.SiebelContext;
import com.blazemeter.jmeter.correlation.siebel.SiebelRowIdCorrelationReplacement;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationEngineTest {

  private static final String VALID_RESPONSE_DATA_PATH = "src/test/resources/validResponseData.txt";
  @Mock
  private CorrelationComponentsRegistry registry;
  private CorrelationEngine engine;

  @Before
  public void prepare() {
    engine = new CorrelationEngine();
    when(registry.getContext(SiebelContext.class)).thenReturn(new SiebelContext());
  }

  @Test
  public void shouldContextWhenSetCorrelationRules() {
    engine.setCorrelationRules(Arrays
            .asList(new CorrelationRule("firstRuleRefVar", "(regex)", "(regex)"),
                new CorrelationRule("secondRuleRefVar",
                    new RegexCorrelationExtractor(""), new SiebelRowIdCorrelationReplacement(""))),
        registry);

    assertThat(engine.getCorrelationRules().stream()
        .noneMatch(c -> c.getCorrelationReplacement().getSupportedContext() != null
            && c.getCorrelationReplacement().getContext() == null));
  }

  public void shouldNotSetContextWhenSetCorrelationRulesWhenRuleDontNeed() {
    engine.setCorrelationRules(Arrays
            .asList(new CorrelationRule("firstRuleRefVar", "(regex)", "(regex)"),
                new CorrelationRule("secondRuleRefVar",
                    new RegexCorrelationExtractor(""), new FunctionCorrelationReplacement(""))),
        registry);

    assertThat(engine.getCorrelationRules().stream()
        .noneMatch(c ->
            (c.getCorrelationExtractor().getSupportedContext() == null
                && c.getCorrelationExtractor().getContext() != null)
                || (c.getCorrelationReplacement().getSupportedContext() == null
                && c.getCorrelationReplacement().getContext() != null)));
  }

  @Test
  public void shouldResetContextWhenReset() {
    engine.setCorrelationRules(Collections.singletonList(
        new CorrelationRule("secondRuleRefVar", new RegexCorrelationExtractor(""),
            new SiebelRowIdCorrelationReplacement(""))), registry);

    SampleResult validResponse = new SampleResult();
    validResponse
        .setResponseData(TestUtils.readFile(VALID_RESPONSE_DATA_PATH, Charset.defaultCharset()));
    engine.updateContexts(validResponse);

    String contextUpdated = getContextsToString();
    engine.reset();

    assertNotEquals(contextUpdated, getContextsToString());
  }

  private String getContextsToString() {
    return engine.getInitializedContexts().stream()
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }
}
