package com.blazemeter.jmeter.correlation.gui;

import static java.lang.Class.forName;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.DescriptionContent;
import com.blazemeter.jmeter.correlation.core.InvalidRulePartElementException;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.extractors.RegexCorrelationExtractor;
import com.blazemeter.jmeter.correlation.core.replacements.CorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.FunctionCorrelationReplacement;
import com.blazemeter.jmeter.correlation.core.replacements.RegexCorrelationReplacement;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationComponentsRegistry {

  public static final CorrelationRulePartTestElement<?> NONE_EXTRACTOR = new EventsSelectionItem(
      "None", "NoneExtractor");
  public static final CorrelationRulePartTestElement<?> NONE_REPLACEMENT = new EventsSelectionItem(
      "None", "NoneReplacement");
  public static final CorrelationRulePartTestElement<?> MORE_EXTRACTOR = new EventsSelectionItem(
      "More...", "MoreExtractor");
  public static final CorrelationRulePartTestElement<?> MORE_REPLACEMENT = new EventsSelectionItem(
      "More...", "MoreReplacement");

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationComponentsRegistry.class);
  private static CorrelationComponentsRegistry instance;
  private final Set<Class<?>> customExtractors = new HashSet<>();
  private final Set<Class<?>> customReplacements = new HashSet<>();
  private final List<Class<?>> defaultExtractors = Collections
      .singletonList(RegexCorrelationExtractor.class);
  private final List<Class<?>> defaultReplacements = Collections
      .singletonList(RegexCorrelationReplacement.class);
  private final List<String> deprecatedComponents = Collections.singletonList(
      FunctionCorrelationReplacement.class.getCanonicalName());

  @VisibleForTesting
  protected Function<Class<?>, List<String>> classFinderFunction = (clazz) -> {
    try {
      return JMeterUtils.findClassesThatExtend(clazz);
    } catch (IOException e) {
      LOG.error("There was an error trying to search for the classes that extends {}", clazz, e);
      return Collections.emptyList();
    }
  };

  private CorrelationComponentsRegistry() {

  }

  public static synchronized CorrelationComponentsRegistry getInstance() {
    if (instance == null) {
      instance = new CorrelationComponentsRegistry();
    }
    return instance;
  }

  public Set<Class<?>> getAllCustomExtensionClasses() {
    Set<Class<?>> extensions = new HashSet<>(customExtractors);
    extensions.addAll(customReplacements);
    return extensions;
  }

  public List<CorrelationRulePartTestElement<?>> buildCustomExtractorRuleParts() {
    return customExtractors.stream()
        .map(ext -> buildRulePartFromClassName(ext.getCanonicalName()))
        .collect(Collectors.toList());
  }

  public List<CorrelationRulePartTestElement<?>> buildCustomReplacementRuleParts() {
    return customReplacements.stream()
        .map(ext -> buildRulePartFromClassName(ext.getCanonicalName()))
        .collect(Collectors.toList());
  }

  public List<CorrelationRulePartTestElement<?>> buildActiveExtractorRulePart() {
    List<CorrelationRulePartTestElement<?>> partTestElements = buildActiveRulePartTestElement(
        customExtractors, defaultExtractors);
    partTestElements.add(0, NONE_EXTRACTOR);
    partTestElements.add(MORE_EXTRACTOR);
    return partTestElements;
  }

  public List<CorrelationRulePartTestElement<?>> buildActiveReplacementRulePart() {
    List<CorrelationRulePartTestElement<?>> partTestElements = buildActiveRulePartTestElement(
        customReplacements, defaultReplacements);
    partTestElements.add(0, NONE_REPLACEMENT);
    partTestElements.add(MORE_REPLACEMENT);
    return partTestElements;
  }

  private List<CorrelationRulePartTestElement<?>> buildActiveRulePartTestElement(
      Set<Class<?>> customs, List<Class<?>> defaults) {

    List<CorrelationRulePartTestElement<?>> partTestElements = new ArrayList<>();
    List<Class<?>> actives = new ArrayList<>(customs);
    actives.addAll(defaults);
    for (Class<?> clazz : actives) {
      partTestElements.add(buildRulePartFromClassName(clazz.getCanonicalName()));
    }
    return partTestElements;
  }

  public CorrelationRulePartTestElement<?> buildRulePartFromClassName(String className) {
    try {
      return (CorrelationRulePartTestElement<?>) getClassInstance(Class.forName(className));
    } catch (ClassNotFoundException e) {
      LOG.error("Couldn't build the correlation type '{}'.", className, e);
      return null;
    }
  }

  private Object getClassInstance(Class<?> correlationPartClass) {
    try {
      return correlationPartClass.getConstructor().newInstance();
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
             | InvocationTargetException e) {
      LOG.warn("Couldn't build the correlation type '{}'.", correlationPartClass, e);
      return null;
    }
  }

  public CorrelationContext getContext(Class<? extends CorrelationContext> contextType) {
    try {
      return contextType.getConstructor().newInstance();
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
             | InvocationTargetException e) {
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

    return String.join(",", validComponents);
  }

  public void addCustomExtension(CorrelationRulePartTestElement<?> extension) {
    try {
      addComponent(extension.getClass().getCanonicalName());
    } catch (IllegalArgumentException | ClassNotFoundException e) {
      LOG.error("There was an issue trying to add the component {}. ", extension.getClass(), e);
    }
  }

  public void removeCustomExtension(CorrelationRulePartTestElement<?> extension) {
    if (extension instanceof CorrelationExtractor) {
      customExtractors.remove(extension.getClass());
    } else if (extension instanceof CorrelationReplacement) {
      customReplacements.remove(extension.getClass());
    }
  }

  private List<String> removeDeprecatedComponents(String components) {
    if (components.isEmpty()) {
      return new ArrayList<>();
    }

    String[] parsedComponents = components.replace("\n", "")
        .replace("\r", "").split(",");
    List<String> validComponents = new ArrayList<>();
    for (String component : parsedComponents) {
      if (deprecatedComponents.stream().noneMatch(dc -> dc.equals(component))) {
        validComponents.add(component);
      }
    }

    return validComponents;
  }

  public void reset() {
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
    return buildActiveReplacementRulePart().stream()
        .anyMatch(replacement -> replacement.getClass().equals(component.getClass()));
  }

  public boolean isExtractorActive(CorrelationRulePartTestElement<?> component) {
    return buildActiveExtractorRulePart().stream()
        .anyMatch(extractor -> extractor.getClass().equals(component.getClass()));
  }

  public CorrelationExtractor<?> getCorrelationExtractor(
      Class<? extends CorrelationExtractor<?>> extractorClass)
      throws InvalidRulePartElementException {
    //Only implemented for the cases where no Extractor is selected
    if (extractorClass == null) {
      return null;
    }

    CorrelationExtractor<?> extractor = (CorrelationExtractor<?>) getClassInstance(extractorClass);
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

    CorrelationReplacement<?> replacement = (CorrelationReplacement<?>) getClassInstance(
        replacementClass);

    if (isReplacementActive(replacement)) {
      return replacement;
    }

    throw new InvalidRulePartElementException("Couldn't build the Correlation Replacement for "
        + "Class " + replacementClass);
  }

  public Set<CorrelationRulePartTestElement<?>> filterNonCustomExtensionsFrom(
      Set<Class<? extends CorrelationRulePartTestElement>> extensions) {
    Set<CorrelationRulePartTestElement<?>> customExtensions = new HashSet<>();
    for (Class<? extends CorrelationRulePartTestElement> ext : extensions) {
      //Is considered Active if it isn't a default Extractor, Replacement or Deprecated Extension
      if (isCustomExtension(ext) && !isDeprecated(ext)) {
        customExtensions.add(buildRulePartFromClassName(ext.getCanonicalName()));
      }
    }
    return customExtensions;
  }

  public boolean isCustomExtension(Class<? extends CorrelationRulePartTestElement> itemClass) {
    return !defaultExtractors.contains(itemClass) && !NONE_EXTRACTOR.getClass().equals(itemClass) &&
        !defaultReplacements.contains(itemClass) && !NONE_REPLACEMENT.getClass().equals(itemClass);
  }

  private boolean isDeprecated(Class<? extends CorrelationRulePartTestElement> itemClass) {
    return deprecatedComponents.contains(itemClass.getCanonicalName());
  }

  //Returns Extensions that are Custom, not installed and not deprecated
  public List<CorrelationRulePartTestElement<?>> getAvailableExtensions() {

    List<String> availableExtensions = classFinderFunction
        .apply(CorrelationRulePartTestElement.class);
    List<CorrelationRulePartTestElement<?>> filteredAvailable = availableExtensions.stream()
        .filter(ext -> !deprecatedComponents.contains(ext)
            && defaultExtractors.stream().noneMatch(e -> e.getCanonicalName().equals(ext))
            && defaultReplacements.stream().noneMatch(r -> r.getCanonicalName().equals(ext))
            && customExtractors.stream().noneMatch(e -> e.getCanonicalName().equals(ext))
            && customReplacements.stream().noneMatch(r -> r.getCanonicalName().equals(ext)))
        .map(this::buildRulePartFromClassName)
        .collect(Collectors.toList());

    return filteredAvailable;
  }

  public String getCorrelationComponents() {
    return getAllCustomExtensionClasses().stream()
        .map(Class::getCanonicalName)
        .collect(Collectors.joining(","));
  }

  public void setClassFinderFunction(Function<Class<?>, List<String>> classFinderFunction) {
    this.classFinderFunction = classFinderFunction;
  }

  /*
   * Implemented to be able to show "empty" RulePartTestElements that
   * trigger actions like "None"
   * */
  public static class EventsSelectionItem extends
      CorrelationRulePartTestElement<CorrelationContext> {

    private String displayName = "";
    private String descriptionName = "";

    @SuppressWarnings("WeakerAccess")
    public EventsSelectionItem(String displayName, String descriptionName) {
      this.displayName = displayName;
      this.descriptionName = descriptionName;
    }

    @Override
    public String getDisplayName() {
      return displayName;
    }

    @Override
    public String getDescription() {
      return DescriptionContent.getFromName(CorrelationComponentsRegistry.class, descriptionName);
    }

    @Override
    public String getType() {
      return "";
    }

    @Override
    public List<String> getParams() {
      return Collections.emptyList();
    }

    @Override
    public void setParams(List<String> params) {
    }

    @Override
    public List<ParameterDefinition> getParamsDefinition() {
      return Collections.emptyList();
    }
  }
}
