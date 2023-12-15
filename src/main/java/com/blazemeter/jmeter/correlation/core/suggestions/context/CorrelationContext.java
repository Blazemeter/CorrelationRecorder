package com.blazemeter.jmeter.correlation.core.suggestions.context;

import java.util.List;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.samplers.SampleResult;

/**
 * The CorrelationContext interface provides a contract for objects that hold the context
 * for correlation methods.
 * The context includes a list of SampleResult objects from the recording and a list of
 * HTTPSamplerBase objects from the recording.
 */
public interface CorrelationContext {

  /**
   * This method retrieves a list of SampleResult objects from the recording.
   * A SampleResult represents the result of a single sample or request.
   * @return a list of SampleResult objects from the recording.
   */
  List<SampleResult> getRecordingSampleResults();

  /**
   * This method retrieves a list of HTTPSamplerBase objects from the recording.
   * An HTTPSamplerBase represents a sampler that sends HTTP requests.
   * @return a list of HTTPSamplerBase objects from the recording.
   */
  List<HTTPSamplerProxy> getRecordingSamplers();
}
