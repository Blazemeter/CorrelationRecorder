package com.blazemeter.jmeter.correlation.core;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CorrelationRulePartTestElement<T extends CorrelationContext> {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationRulePartTestElement.class);

  protected T context;

  // Ensure backward compatibility, since getDisplayName() was added after v1.0
  public static String getDisplayName(CorrelationRulePartTestElement<?> partTestElement) {
    try {
      return (String) partTestElement.getClass().getMethod("getDisplayName")
          .invoke(partTestElement);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      LOG.warn(
          "Method getDisplayName() not overwritten in custom extensions.\n Make sure to update "
              + "the API to the last version and that all methods are been overwritten.",
          e);
      String type = partTestElement.getType()
          .replace("CorrelationExtractor", "")
          .replace("CorrelationReplacement", "");
      return type.substring(type.lastIndexOf(".") + 1);
    }
  }

  public abstract String getDisplayName();

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

  public abstract String getDescription();

  @Override
  public String toString() {
    return "CorrelationRulePartTestElement{" +
        "name=" + getDisplayName() +
        "params=[" + getParams() + "]"
        + "}";
  }
}
