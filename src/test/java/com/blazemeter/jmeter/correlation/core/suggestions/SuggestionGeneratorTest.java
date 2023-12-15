package com.blazemeter.jmeter.correlation.core.suggestions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import com.blazemeter.jmeter.correlation.core.automatic.CorrelationSuggestion;
import com.blazemeter.jmeter.correlation.core.suggestions.context.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.suggestions.method.CorrelationMethod;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SuggestionGeneratorTest {
  @Mock
  private CorrelationMethod method;
  private SuggestionGenerator suggestionGenerator;

  @Before
  public void setUp() throws IOException {
    suggestionGenerator = new SuggestionGenerator(getCorrelationMethod());
  }

  private CorrelationMethod getCorrelationMethod() {
    return new CorrelationMethod() {
      @Override
      public List<CorrelationSuggestion> generateSuggestions(CorrelationContext context) {
        return new ArrayList<>();
      }

      @Override
      public void applySuggestions(List<CorrelationSuggestion> suggestions) {
        // Do nothing
      }
    };
  }

  @Test
  public void shouldReturnEmptyWhenGenerateSuggestionsWithoutContext() {
    assert suggestionGenerator.generateSuggestions(null).isEmpty();
  }

  @Test
  public void shouldReturnEmptyWhenGenerateSuggestionsWithoutMethod() {
    suggestionGenerator.setCorrelationMethod(null);
    assert suggestionGenerator.generateSuggestions(getBasicCorrelationContext()).isEmpty();
  }

  private CorrelationContext getBasicCorrelationContext() {
    return new CorrelationContext() {

      @Override
      public List<SampleResult> getRecordingSampleResults() {
        return null;
      }

      @Override
      public List<HTTPSamplerProxy> getRecordingSamplers() {
        return null;
      }
    };
  }

  @Test
  public void shouldCallMethodGenerateSuggestions() {
    suggestionGenerator.setCorrelationMethod(method);
    suggestionGenerator.generateSuggestions(getBasicCorrelationContext());
    verify(method).generateSuggestions(any());
  }
}