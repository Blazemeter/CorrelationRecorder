package com.blazemeter.jmeter.correlation.core;

import static java.lang.Class.forName;

import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.FunctionCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.templates.DescriptionContent;
import com.blazemeter.jmeter.correlation.gui.InvalidRulePartElementException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationComponentsRegistry {
  
  public static final CorrelationRulePartTestElement<?> NONE_EXTRACTOR = new EventsSelectionItem() {
    @Override
    public String getDescription() {
      return DescriptionContent.getFromName(CorrelationComponentsRegistry.class, "NoneExtractor");
    }
  };

  public static final CorrelationRulePartTestElement<?> NONE_REPLACEMENT = new EventsSelectionItem() {
    @Override
    public String getDescription() {
      return DescriptionContent.getFromName(CorrelationComponentsRegistry.class, "NoneReplacement");
    }
  };

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationComponentsRegistry.class);
  private final Set<Class<?>> customExtractors = new HashSet<>();
  private final Set<Class<?>> customReplacements = new HashSet<>();
  private final List<Class<?>> defaultExtractors = Collections
      .singletonList(RegexCorrelationExtractor.class);
  private final List<Class<?>> defaultReplacements = Collections
      .singletonList(RegexCorrelationReplacement.class);
  private final List<String> deprecatedComponents = Collections.singletonList(
      FunctionCorrelationReplacement.class.getCanonicalName());

  public CorrelationComponentsRegistry() {

  }

  public List<CorrelationRulePartTestElement<?>> buildActiveExtractors() {
    return buildActiveRulePartTestElement(customExtractors, defaultExtractors, NONE_EXTRACTOR);
  }

  public List<CorrelationRulePartTestElement<?>> buildActiveReplacements() {
    return buildActiveRulePartTestElement(customReplacements, defaultReplacements, NONE_REPLACEMENT);
  }

  private List<CorrelationRulePartTestElement<?>> buildActiveRulePartTestElement(
      Set<Class<?>> customs, List<Class<?>> defaults, CorrelationRulePartTestElement<?> none) {
    List<CorrelationRulePartTestElement<?>> partTestElements = new ArrayList<>();
    partTestElements.add(none);
    
    List<Class<?>> actives = new ArrayList<>(customs);
    actives.addAll(defaults);

    for (Class<?> clazz : actives) {
      partTestElements.add(getCorrelationRulePartTestElement(clazz.getCanonicalName()));
    }

    return partTestElements;
  }

  public CorrelationRulePartTestElement<?> getCorrelationRulePartTestElement(String className) {
    try {
      return (CorrelationRulePartTestElement<?>) getInstance(Class.forName(className));
    } catch (ClassNotFoundException e) {
      LOG.error("Couldn't build the correlation type '{}'.", className, e);
      return null;
    }
  }

  private Object getInstance(Class<?> correlationPartClass) {
    try {
      return correlationPartClass.getConstructor().newInstance();
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      LOG.warn("Couldn't build the correlation type '{}'.", correlationPartClass, e);
      return null;
    }
  }

  public CorrelationContext getContext(Class<? extends CorrelationContext> contextType) {
    try {
      return contextType.getConstructor().newInstance();
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      LOG.warn("Couldn't build the correlation type='{}'.", contextType, e);
      return null;
    }
  }

  public String updateActiveComponents(String components,
      List<String> missingSelectedExtensions) {
    reset();
    //Deprecated components shouldn't appear nor in ComboBoxes nor in ComponentsContainer
    List<String> validComponents = removeDeprecatedComponents(components);
    validComponents.addAll(missingSelectedExtensions);
    validComponents.forEach(component -> {
      try {
        addComponent(component);
      } catch (IllegalArgumentException | ClassNotFoundException e) {
        LOG.error("There was an issue trying to add the component {}. ", component, e);
      }
    });
    
    return String.join(",\n", validComponents);
  }

  private List<String> removeDeprecatedComponents(String components) {
    if (components.isEmpty()) {
      return new ArrayList<>();
    }
    
    String[] parsedComponents = components.replace("\n", "")
        .replace("\r", "").split(",");
    List<String> validComponents = new ArrayList<>();
    for (String component: parsedComponents) {
      if (deprecatedComponents.stream().noneMatch(dc -> dc.equals(component))) {
        validComponents.add(component);
      }
    }

    return validComponents;
  }

  private void reset() {
    customExtractors.clear();
    customReplacements.clear();
  }

  private void addComponent(String component)
      throws ClassNotFoundException, IllegalArgumentException {
    Class<?> componentClass = forName(component);
    if (CorrelationContext.class.isAssignableFrom(componentClass)) {
      throw new IllegalArgumentException(
          "You can't add Contexts on this list. Only Correlation Extractors or Replacements are "
              + "allowed.");
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

  public boolean isReplacementActive(CorrelationRulePartTestElement<?> component) {
    return buildActiveReplacements().stream()
        .anyMatch(replacement -> replacement.getClass()
            .equals(component.getClass()));
  }

  public boolean isExtractorActive(CorrelationRulePartTestElement<?> component) {
    return buildActiveExtractors().stream()
        .anyMatch(extractor -> extractor.getClass()
            .equals(component.getClass()));
  }

  public CorrelationExtractor<?> getCorrelationExtractor(
      Class<? extends CorrelationExtractor<?>> extractorClass)
      throws InvalidRulePartElementException {
    //Only implemented for the cases where no Extractor is selected
    if (extractorClass == null) {
      return null;
    }

    CorrelationExtractor<?> extractor = (CorrelationExtractor<?>) getInstance(extractorClass);
    if (isExtractorActive(extractor)) {
      return extractor;
    }

    throw new InvalidRulePartElementException("Couldn't build the Correlation Extractor for "
        + "Class " + extractorClass);
  }

  public CorrelationReplacement<?> getCorrelationReplacement(
      Class<? extends CorrelationReplacement<?>> replacementClass)
      throws InvalidRulePartElementException {
    //Only implemented for the cases where no Replacement is selected
    if (replacementClass == null) {
      return null;
    }

    CorrelationReplacement<?> replacement = (CorrelationReplacement<?>) getInstance(
        replacementClass);

    if (isReplacementActive(replacement)) {
      return replacement;
    }

    throw new InvalidRulePartElementException("Couldn't build the Correlation Replacement for "
        + "Class " + replacementClass);
  }

  public boolean isExtractorExtension(Class<? extends CorrelationRulePartTestElement> itemClass) {
    return !defaultExtractors.contains(itemClass) && !NONE_EXTRACTOR.getClass().equals(itemClass);
  }

  public boolean isReplacementExtension(Class<? extends CorrelationRulePartTestElement> itemClass) {
    return !defaultReplacements.contains(itemClass)&& !NONE_REPLACEMENT.getClass().equals(itemClass);
  }


  /*
   * Implemented to be able to show "empty" RulePartTestElements that
   * trigger actions like "None"
   * */
  private abstract static class EventsSelectionItem extends CorrelationRulePartTestElement {

    @Override
    public String getDisplayName() {
      return "None";
    }

    @Override
    public abstract String getDescription();

    @Override
    public String getType() {
      return "";
    }

    @Override
    public List<String> getParams() {
      return Collections.emptyList();
    }

    @Override
    public void setParams(List params) {
    }

    @Override
    public List<ParameterDefinition> getParamsDefinition() {
      return Collections.emptyList();
    }
  }
}
