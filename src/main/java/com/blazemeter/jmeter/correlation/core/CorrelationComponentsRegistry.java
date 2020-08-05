package com.blazemeter.jmeter.correlation.core;

import static java.lang.Class.forName;

import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.FunctionCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationComponentsRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationComponentsRegistry.class);
  private static final String CORRELATION_REPLACEMENT_SUFFIX = "CorrelationReplacement";
  private static final String CORRELATION_EXTRACTOR_SUFFIX = "CorrelationExtractor";

  private Set<Class> customExtractors = new HashSet<>();
  private Set<Class> customReplacements = new HashSet<>();
  private List<Class> defaultExtractors = Collections
      .singletonList(RegexCorrelationExtractor.class);
  private List<Class> defaultReplacements = Arrays
      .asList(FunctionCorrelationReplacement.class, RegexCorrelationReplacement.class);

  public CorrelationComponentsRegistry() {

  }

  public void addComponent(String component)
      throws ClassNotFoundException, IllegalArgumentException {
    Class<?> componentClass = forName(component);
    if (CorrelationContext.class.isAssignableFrom(componentClass)) {
      throw new IllegalArgumentException(
          "You can't add Contexts on this list. Only Correlation Extractors or Replacements are allowed.");
    }

    if (CorrelationReplacement.class.isAssignableFrom(componentClass)) {
      customReplacements.add(componentClass);
    } else if (CorrelationExtractor.class.isAssignableFrom(componentClass)) {
      customExtractors.add(componentClass);
    } else {
      throw new IllegalArgumentException("The class " + componentClass
          + " didn't match any CorrelationReplacement or CorrelationExtractor.");
    }
  }

  public List<String> getComponents() {
    List<Class> allComponents = new ArrayList<>(customExtractors);
    allComponents.addAll(customReplacements);
    return allComponents.stream()
        .map(Class::getCanonicalName)
        .collect(Collectors.toList());
  }

  public CorrelationExtractor getCorrelationExtractor(String correlationExtractorType,
      List<String> correlationParamValues) {
    return (CorrelationExtractor) getCorrelationInstance(
        getExtractorClassName(correlationExtractorType), correlationParamValues);
  }

  private Object getCorrelationInstance(String correlationCanonicalClassName,
      List<String> correlationParamValues) {
    try {
      Class[] paramsTypes = new Class[correlationParamValues.size()];
      Arrays.fill(paramsTypes, String.class);

      Class<?> correlationType = forName(correlationCanonicalClassName);
      return correlationType.getConstructor(paramsTypes)
          .newInstance((Object[]) correlationParamValues.toArray(new String[0]));
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
      LOG.warn("Couldn't build the correlation type '{}'.", correlationCanonicalClassName, e);
      return null;
    }
  }

  /*
  We use this method to find the CanonicalClassName for the Extractor Type,
  since the "type" doesn't contain the nor the "CorrelationExtractor" nor the full route of the class
  */
  private String getExtractorClassName(String extractorSimplifiedClassName) {
    return getCorrelationCanonicalClassName(getExtractors(), extractorSimplifiedClassName,
        CORRELATION_EXTRACTOR_SUFFIX);
  }

  private String getCorrelationCanonicalClassName(List<Class> correlations,
      String correlationSimplifiedName, String suffix) {
    return correlations.stream()
        .filter(c -> c.getCanonicalName()
            .toLowerCase()
            .endsWith((correlationSimplifiedName + suffix).toLowerCase()))
        .map(Class::getCanonicalName)
        .findFirst()
        .orElse(correlationSimplifiedName);
  }

  private List<Class> getExtractors() {
    List<Class> extractors = new ArrayList<>(customExtractors);
    extractors.addAll(defaultExtractors);
    return extractors;
  }

  public CorrelationReplacement getCorrelationReplacement(String replacementSimplifiedClassName,
      List<String> correlationParamValues) {
    return (CorrelationReplacement) getCorrelationInstance(
        getReplacementClassName(replacementSimplifiedClassName), correlationParamValues);
  }

  /*
  We use this method to find the CanonicalClassName for the Replacement Type,
  since the "type" doesn't contain the nor the "CorrelationReplacement" nor the full route of the class
  */
  private String getReplacementClassName(String replacementSimplifiedClassName) {
    return getCorrelationCanonicalClassName(getReplacements(), replacementSimplifiedClassName,
        CORRELATION_REPLACEMENT_SUFFIX);
  }

  private List<Class> getReplacements() {
    List<Class> replacements = new ArrayList<>(customReplacements);
    replacements.addAll(defaultReplacements);
    return replacements;
  }

  public CorrelationRulePartTestElement getCorrelationRulePartTestElement(String type) {
    return (CorrelationRulePartTestElement) getCorrelationInstance(type, new ArrayList<>());
  }

  CorrelationContext getContext(Class<? extends CorrelationContext> contextType) {
    try {

      return contextType.getConstructor().newInstance();
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      LOG.warn("Couldn't build the correlation type='{}'.", contextType, e);
      return null;
    }
  }

  public boolean containsReplacement(String className) {
    List<Class> allReplacements = getReplacements();
    return allReplacements.stream()
        .anyMatch(c -> c.getSimpleName().endsWith(
            className + CORRELATION_REPLACEMENT_SUFFIX));
  }

  public List<String> getReplacementsComponents() {
    List<Class> allReplacements = getReplacements();
    return allReplacements.stream()
        .map(r -> r.getSimpleName().replace(CORRELATION_REPLACEMENT_SUFFIX, ""))
        .collect(Collectors.toList());
  }

  public boolean containsExtractor(String className) {
    List<Class> allExtractors = getExtractors();
    return allExtractors.stream()
        .anyMatch(c -> c.getSimpleName().endsWith(className + CORRELATION_EXTRACTOR_SUFFIX));
  }

  public boolean containsExtractor(Class<? extends CorrelationExtractor> extractorClass) {
    List<Class> allExtractors = getExtractors();
    return allExtractors.stream()
        .anyMatch(c -> c.equals(extractorClass));
  }

  public boolean containsReplacement(Class<? extends CorrelationReplacement> replacementClass) {
    List<Class> allReplacement = getReplacements();
    return allReplacement.stream()
        .anyMatch(c -> c.equals(replacementClass));
  }

  public List<String> getExtractorsComponents() {
    List<Class> allExtractors = getExtractors();
    return allExtractors.stream()
        .map(e -> e.getSimpleName().replace(CORRELATION_EXTRACTOR_SUFFIX, ""))
        .collect(Collectors.toList());
  }

  public void reset() {
    customExtractors.clear();
    customReplacements.clear();
  }
}
