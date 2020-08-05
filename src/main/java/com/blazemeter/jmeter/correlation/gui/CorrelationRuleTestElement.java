package com.blazemeter.jmeter.correlation.gui;

import com.blazemeter.jmeter.correlation.core.CorrelationComponentsRegistry;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.helger.commons.annotation.VisibleForTesting;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationRuleTestElement extends AbstractTestElement implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationRuleTestElement.class);
  private static final Class<? extends CorrelationExtractor> DEFAULT_EXTRACTOR_TYPE = RegexCorrelationExtractor.class;
  private static final Class<? extends CorrelationReplacement> DEFAULT_REPLACEMENT_TYPE = RegexCorrelationReplacement.class;
  private static final CorrelationComponentsRegistry registry = new CorrelationComponentsRegistry();
  private static final String REFERENCE_NAME = "CorrelationRule.referenceName";
  private static final String EXTRACTOR_CLASS = "CorrelationRule.CorrelationExtractorClass";
  private static final String REPLACEMENT_CLASS = "CorrelationRule.CorrelationReplacementClass";
  //For backward compatibility
  public static Map<String, String> conversion;

  static {
    conversion = new HashMap<>();
    conversion.put("CorrelationRule.CorrelationExtractor.target", "CorrelationRule.targetField");
    conversion.put("CorrelationRule.CorrelationExtractor.matchNr", "CorrelationRule.matchNumber");
    conversion.put("CorrelationRule.CorrelationExtractor.groupNr", "CorrelationRule.groupNumber");
    conversion.put("CorrelationRule.CorrelationExtractor.regex", "CorrelationRule.extractorRegex");
    conversion
        .put("CorrelationRule.CorrelationReplacement.regex", "CorrelationRule.replacementRegex");
    conversion.put(EXTRACTOR_CLASS, "CorrelationRule.extractorClass");
    conversion.put(REPLACEMENT_CLASS, "CorrelationRule.replacementClass");
    
  }

  private CorrelationRulePartTestElement extractorDefinitions;
  private CorrelationRulePartTestElement replacementDefinitions;
  private boolean isOldTesplan;
  private Function<String, String> propertyProvider = property -> isOldTesplan ?
      conversion.get(property) : property;
  
  public CorrelationRuleTestElement() {
    extractorDefinitions = null;
    replacementDefinitions = null;
  }

  @VisibleForTesting
  public CorrelationRuleTestElement(String referenceName, CorrelationRulePartTestElement extractor,
      CorrelationRulePartTestElement replacement) {

    setReferenceName(referenceName);
    this.extractorDefinitions = extractor;
    this.replacementDefinitions = replacement;

    setProperty(REFERENCE_NAME, referenceName);
    setPropertiesForCorrelationPart(extractor);
    setPropertiesForCorrelationPart(replacement);
  }

  private void setPropertiesForCorrelationPart(CorrelationRulePartTestElement part) {
    String type = part instanceof CorrelationReplacement
        ? "CorrelationReplacement" : "CorrelationExtractor";
    setProperty("CorrelationRule." + type + "Class", part.getClass().getName());
    List<ParameterDefinition> definitions = part.getParamsDefinition();
    List<String> params = part.getParams();

    if (params != null && definitions != null) {
      IntStream.range(0, definitions.size()).forEach(i -> {
        setProperty(definitions.get(i).getName(), params.get(i));
      });
    } else {
      LOG.warn("Failed to generate the Correlation rule for {}. Definitions: {}. Params: {}.", type,
          definitions,
          params);
    }
  }

  public String getReferenceName() {
    return getPropertyAsString(REFERENCE_NAME);
  }

  public void setReferenceName(String referenceName) {
    setProperty(new StringProperty(REFERENCE_NAME, referenceName));
  }

  public Class<? extends CorrelationExtractor> getExtractorClass() {
    String extractorClass = getPropertyAsString(EXTRACTOR_CLASS);

    if (extractorClass == null || extractorClass.isEmpty()) {
      extractorClass = getPropertyAsString(conversion.get(EXTRACTOR_CLASS));
      if (extractorClass.isEmpty()) {
        LOG.info("No Correlation Extractor Selected");
        return null;
      }
      extractorClass = !extractorClass.contains("CorrelationExtractor") ? extractorClass.replace(
          "Extractor", "CorrelationExtractor") : extractorClass;
      isOldTesplan = true;

    }

    try {
      return (Class<? extends CorrelationExtractor>) Class.forName(extractorClass);
    } catch (ClassNotFoundException e) {
      LOG.error("Unsupported extractor type {}", extractorClass, e);
      return null;
    }
  }

  public void setExtractorClass(Class<? extends CorrelationExtractor> extractorClass) {
    setProperty(EXTRACTOR_CLASS, extractorClass.getCanonicalName());
  }

  public Class<? extends CorrelationReplacement> getReplacementClass() {
    String replacementClass = getPropertyAsString(REPLACEMENT_CLASS);

    if (replacementClass == null || replacementClass.isEmpty()) {
      replacementClass = getPropertyAsString(
          conversion.get(REPLACEMENT_CLASS)).replace("Replacement", "CorrelationReplacement");
      if (replacementClass.isEmpty()) {
        LOG.info("No Correlation Replacement Selected");
        return null;
      }
      isOldTesplan = true;
    }

    try {
      return (Class<? extends CorrelationReplacement>) Class.forName(replacementClass);
    } catch (ClassNotFoundException e) {
      LOG.error("Unsupported replacement type {}", replacementClass, e);
      return null;
    }
  }

  public void setReplacementClass(Class<? extends CorrelationReplacement> replacementClass) {
    setProperty(REPLACEMENT_CLASS, replacementClass.getCanonicalName());
  }

  public CorrelationRulePartTestElement getExtractorRulePart() {
    String correlationClassName = getExtractorClass() != null ?
        getExtractorClass().getCanonicalName() : "";
    
    if (correlationClassName.isEmpty()) {
      LOG.info(">> !The rule with reference variable name {} doesn't have a Correlation Extractor.",
          getReferenceName());
      return null;
    }

    return getCorrelationRulePart(correlationClassName);
  }
  
  private CorrelationRulePartTestElement getCorrelationRulePart(String correlationClassName) {
    CorrelationRulePartTestElement correlationInstance = registry
        .getCorrelationRulePartTestElement(correlationClassName);
    correlationInstance
        .setParams(((List<ParameterDefinition>) correlationInstance.getParamsDefinition()).stream()
            .map(p -> getPropertyAsString(propertyProvider.apply(p.getName())))
            .collect(Collectors.toList()));
    return correlationInstance;
  }

  public CorrelationRulePartTestElement getReplacementRulePart() {
    String correlationClassName = getReplacementClass() != null ?
        getReplacementClass().getCanonicalName() : "";
    
    if (correlationClassName.isEmpty()) {
      LOG.info("The rule with refVar {} doesn't have a class for the Replacement.",
          getReferenceName());
      return null;
    }

    return getCorrelationRulePart(correlationClassName);
  }

  @Override
  public String toString() {
    CorrelationExtractor correlationExtractor = registry
        .getCorrelationExtractor(getPropertyAsString(EXTRACTOR_CLASS), new ArrayList<>());
    CorrelationReplacement correlationReplacement = registry
        .getCorrelationReplacement(getPropertyAsString(REPLACEMENT_CLASS), new ArrayList<>());

    return "TestElement [referenceName=" + getPropertyAsString(REFERENCE_NAME)
        + ", extractorClass=" + correlationExtractor.getClass().getName()
        + ", extractorParams={" + (
        ((List<ParameterDefinition>) correlationExtractor.getParamsDefinition()).stream()
            .map(p -> getPropertyAsString(p.getName())).collect(Collectors.joining(",")) + "}"
            + ", extractorParamsDefinition={" + ((List<ParameterDefinition>) correlationExtractor
            .getParamsDefinition()).stream()
            .map(ParameterDefinition::toString).collect(Collectors.joining(",")) + "}"
            + ", replacementClass=" + correlationReplacement.getClass().getName()
            + ", replacementParams={" + ((List<ParameterDefinition>) correlationReplacement
            .getParamsDefinition()).stream()
            .map(p -> getPropertyAsString(p.getName())).collect(Collectors.joining(",")) + "}"
            + ", extractorParamsDefinition={" + ((List<ParameterDefinition>) correlationReplacement
            .getParamsDefinition()).stream()
            .map(ParameterDefinition::toString).collect(Collectors.joining(","))) + "}"
        + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    CorrelationRuleTestElement that = (CorrelationRuleTestElement) o;
    return Objects.equals(extractorDefinitions, that.extractorDefinitions) &&
        Objects.equals(replacementDefinitions, that.replacementDefinitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), extractorDefinitions, replacementDefinitions);
  }
}
