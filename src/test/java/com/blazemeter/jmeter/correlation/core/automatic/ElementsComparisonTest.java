package com.blazemeter.jmeter.correlation.core.automatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.ResultField;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.functions.RegexFunction;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ElementsComparisonTest {
  private final ElementsComparison elementsComparison = new ElementsComparison();
  private final String data = "header-1=QWE;\n" +
      "header-2=ABC;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;B;C;\n" +
      "header-3=XYW;\n" +
      "csrf_token: \n&quot;459581b016d5b6bed071a02&quot;ABCDEF\n" +
      "session_id: &quot;qweasdxcvfgh&quot;\n";


  @Test
  public void shouldGenerateLazyRegexExtractor() throws InvalidVariableException {
    String value = "ABC";
    String valueName = "header-2";

    String contextString = elementsComparison.getContextString(data, value);
    RegexCorrelationExtractor<?> correlationExtractor = elementsComparison
        .generateExtractor(valueName, value, contextString, ResultField.BODY);

    assertEquals(value, getMatchedString(getRegex(correlationExtractor), data));
  }

  private String getMatchedString(String regex, String responseData)
      throws InvalidVariableException {
    SampleResult result = new SampleResult();
    result.setResponseData(responseData, null);

    JMeterContext context = JMeterContextService.getContext();
    context.setVariables(new JMeterVariables());
    context.setPreviousResult(result);

    Collection<CompoundVariable> params = makeParams(regex, "$1$", "1");
    RegexFunction variable = new RegexFunction();
    variable.setParameters(params);
    return variable.execute(result, null);
  }

  private static Collection<CompoundVariable> makeParams(String... params) {
    return Stream.of(params)
        .map(CompoundVariable::new)
        .collect(Collectors.toList());
  }

  private String getRegex(RegexCorrelationExtractor<?> correlationExtractor) {
    //The first element of the params is the Regex
    return correlationExtractor.getParams().get(0);
  }
  @Test
  public void shouldGenerateRegexSupportingNewLines() throws InvalidVariableException {
    String rawHeader = "Cache-Control: no-cache, must-revalidate, max-age=0\n" +
        "X-WP-Nonce: e617becc44\n" +
        "Allow: GET, POST, PUT, PATCH\n";
    String value = "e617becc44";
    String valueName = "X-WP-Nonce";

    String contextString = elementsComparison.getContextString(rawHeader, value);
    RegexCorrelationExtractor<?> correlationExtractor = elementsComparison
        .generateExtractor(valueName, value, contextString, ResultField.RESPONSE_HEADERS);
    String matchedString = getMatchedString(getRegex(correlationExtractor), rawHeader);
    assertThat(matchedString).isEqualTo(value);
  }
}