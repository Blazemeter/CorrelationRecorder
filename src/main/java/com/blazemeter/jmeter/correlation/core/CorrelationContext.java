package com.blazemeter.jmeter.correlation.core;

import com.blazemeter.jmeter.correlation.CorrelationProxyControl;
import org.apache.jmeter.samplers.SampleResult;

/**
 * Stores information relevant for the recorded flow to help correlating different variables.
 *
 * Contains the basic method for all the Contexts to being able to update and reset the variables
 * shared by their Correlation Components.
 */
public interface CorrelationContext {

  /**
   * Handles the reset of all the variables. Will be called on the {@link
   * CorrelationProxyControl#startProxy()} method
   */
  void reset();

  /**
   * Handles the update of the variables from the result obtained after every request made to the
   * server.
   *
   * @param sampleResult response obtained after a request
   */
  void update(SampleResult sampleResult);
}
