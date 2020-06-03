package com.blazemeter.jmeter.correlation.core;

import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.oro.text.MalformedCachePatternException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationEngine {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationEngine.class);
  private JMeterVariables vars = new JMeterVariables();
  private List<CorrelationContext> initializedContexts = new ArrayList<>();
  private List<CorrelationRule> rules;

  public CorrelationEngine() {
    rules = new ArrayList<>();
  }

  public void setCorrelationRules(List<CorrelationRule> rules,
      CorrelationComponentsRegistry registry) {
    rules.forEach(r -> {
      updateCorrelationContext(r.getCorrelationExtractor(), registry);
      updateCorrelationContext(r.getCorrelationReplacement(), registry);
    });
    this.rules = rules;
  }

  private void updateCorrelationContext(CorrelationRulePartTestElement rulePartTestElement,
      CorrelationComponentsRegistry registry) {
    if (rulePartTestElement != null && rulePartTestElement.getSupportedContext() != null) {
      rulePartTestElement
          .setContext(getSupportedContext(rulePartTestElement.getSupportedContext(), registry));
    }
  }

  private CorrelationContext getSupportedContext(
      Class<? extends CorrelationContext> requestedContextClass,
      CorrelationComponentsRegistry registry) {
    Optional<CorrelationContext> supportedContext = initializedContexts.stream()
        .filter(c -> c.getClass().equals(requestedContextClass))
        .findFirst();
    if (supportedContext.isPresent()) {
      return supportedContext.get();
    } else {
      CorrelationContext context = registry.getContext(requestedContextClass);
      initializedContexts.add(context);
      return context;
    }
  }

  public void reset() {
    vars = new JMeterVariables();
    initializedContexts.forEach(CorrelationContext::reset);
  }

  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
      String responseFilter) {
    rules.stream()
        .filter(r -> r.getCorrelationReplacement() != null)
        .forEach(r -> r.applyReplacements(sampler, children, result, vars));
    
    initializedContexts.forEach(c -> c.update(result));

    if (isContentTypeAllowed(result, responseFilter)) {
      rules.stream()
          .filter(r -> r.getCorrelationExtractor() != null)
          .forEach(r -> r.addExtractors(sampler, children, result, vars));
    }
  }

  private boolean isContentTypeAllowed(SampleResult result, String filterRegex) {
    if (filterRegex == null || filterRegex.isEmpty()) {
      return true;
    }

    String sampleContentType = result.getContentType();
    if (sampleContentType == null || sampleContentType.isEmpty()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No Content-type found for : {}.", result.getUrlAsString());
      }
      return true;
    }

    LOG.debug("Content-type to filter: {}.", sampleContentType);

    String[] responseTypeFilter = filterRegex.split(",");
    for (String filter : responseTypeFilter) {
      try {
        if (!JMeterUtils.getMatcher()
            .contains(sampleContentType, JMeterUtils.getPatternCache().getPattern(filter, 32784))) {
          return false;
        }
      } catch (MalformedCachePatternException ex) {
        LOG.warn("Skipped invalid content pattern: {}", filterRegex, ex);
        return false;
      }
    }

    return true;
  }

  @VisibleForTesting
  public List<CorrelationRule> getCorrelationRules() {
    return rules;
  }

  @VisibleForTesting
  public List<CorrelationContext> getInitializedContexts() {
    return initializedContexts;
  }

  @VisibleForTesting
  public void updateContexts(SampleResult sampleResult) {
    initializedContexts.forEach(c -> c.update(sampleResult));
  }
}
