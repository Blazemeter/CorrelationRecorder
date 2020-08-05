package com.blazemeter.jmeter.correlation.core;

import java.util.List;

public abstract class CorrelationRulePartTestElement<T extends CorrelationContext> {

  protected T context;

  public abstract String getType();

  public abstract List<String> getParams();

  public abstract void setParams(List<String> params);

  public abstract List<ParameterDefinition> getParamsDefinition();

  public Class<? extends CorrelationContext> getSupportedContext() {
    return null;
  }

  public T getContext() {
    return context;
  }

  public void setContext(T context) {
    this.context = context;
  }
}
