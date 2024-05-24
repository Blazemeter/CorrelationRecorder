package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import com.blazemeter.jmeter.correlation.core.automatic.Appearances;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import org.apache.jmeter.testelement.TestElement;

public interface ReplacementStrategy {

  CorrelationReplacement<?> generateReplacement(TestElement usage, Appearances appearance,
      String referenceName);

}
