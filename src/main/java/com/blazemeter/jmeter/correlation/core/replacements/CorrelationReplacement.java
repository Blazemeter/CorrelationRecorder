package com.blazemeter.jmeter.correlation.core.replacements;

import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.CorrelationRulePartTestElement;
import com.blazemeter.jmeter.correlation.core.DescriptionContent;
import com.blazemeter.jmeter.correlation.core.templates.CorrelationRuleSerializationPropertyFilter;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.MultiProperty;
import org.apache.jmeter.testelement.property.NumberProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeInfo(use = Id.CLASS, property = "type")
@JsonFilter(CorrelationRuleSerializationPropertyFilter.FILTER_ID)
/**
 * Serves as a base for all the Correlation Replacements.
 *
 * Contains all the methods from loading, saving and processing requests from the server.
 *
 * For a more detailed explanation on Correlation Replacements, their usages and methods, please
 * read the
 * <a href="https://github.com/Blazemeter/CorrelationRecorder/blob/master/README.md">readme</a>.
 *
 * Along side {@link com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor}
 * and the Reference Variable, it is a part of the
 * {@link com.blazemeter.jmeter.correlation.core.CorrelationRule}
 */
public abstract class CorrelationReplacement<T extends CorrelationContext> extends
    CorrelationRulePartTestElement<T> {

  protected static final String PROPERTIES_PREFIX =
      "CorrelationRule.CorrelationReplacement.";
  public static final String REPLACEMENT_STRING_PROPERTY_NAME = PROPERTIES_PREFIX +
      "replacementString";
  public static final String REPLACEMENT_IGNORE_VALUE_PROPERTY_NAME = PROPERTIES_PREFIX +
      "ignoreValue";
  protected static final String REGEX_DEFAULT_VALUE = "param=\"(.+?)\"";

  protected static final String REPLACEMENT_STRING_DEFAULT_VALUE = "";

  protected static final String FUNCTION_REF_PREFIX = "${"; //$NON-NLS-1$
  /**
   * Functions are wrapped in ${ and }.
   */
  protected static final String FUNCTION_REF_SUFFIX = "}"; //$NON-NLS-1$

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationReplacement.class);

  protected String variableName;

  protected String replacementString = REPLACEMENT_STRING_DEFAULT_VALUE;

  protected Function<String, String> expressionEvaluator =
      (expression) -> new CompoundVariable(expression).execute();


  /**
   * Default constructor added in order to satisfy the JSON conversion.
   *
   * <p>Implementing a Custom Correlation Extractor requires to mimic this behavior
   */
  public CorrelationReplacement() {
  }

  @Override
  public String getDescription() {
    return DescriptionContent.getFromClass(getClass());
  }

  public String getVariableName() {
    return variableName;
  }

  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  /**
   * Handles the saving of values of the Correlation Replacement into a
   * {@link CorrelationRuleTestElement} for later storage in Test Plans and CorrelationTemplates.
   *
   * <p>This method has to be overwritten when implementing custom Correlation Replacements,
   * otherwise, only the class name will be saved
   *
   * @param testElem CorrelationRuleTestElement where the fields will be stored
   */
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    testElem.setReplacementClass((Class<? extends CorrelationReplacement<?>>) getClass());
  }

  /**
   * Process every request sent to the server and apply replacements over specific values.
   *
   * <p>The logic of correlating values during the recording is contained here.
   *
   * <p>Both the properties in the recorded sampler and its children will be processed
   *
   * @param sampler  recorded sampler containing the information of the request
   * @param children list of children added to the sampler (if the condition is matched, components
   *                 will be added to it to correlate the obtained values)
   * @param result   result containing information about request and associated response from
   *                 server
   * @param vars     stored variables shared between requests during recording
   */
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
                      JMeterVariables vars) {
    replaceTestElementProperties(sampler, vars);
    for (TestElement child : children) {
      if (child instanceof ConfigTestElement) {
        replaceTestElementProperties(child, vars);
      }
    }
  }

  /**
   * Method that performs recursive calls to replace the arguments of the TestElement provided.
   *
   * <p>This method works applying replacement, in a recursive way, to parameter and headers
   * assignments, inside the TestElement. If one value exists in the JMeterVariables for the
   * Reference Variable Name, and does match the condition configured for this Correlation
   * Replacement, the value will be replaced in the String as <code>${referenceVariableName}</code>,
   * as many times as the logic in the condition allows it.
   *
   * @param el   test element to check and match the properties
   * @param vars stored variables from the recording
   */
  private void replaceTestElementProperties(TestElement el, JMeterVariables vars) {
    List<JMeterProperty> props = new LinkedList<>();
    PropertyIterator propertyIterator = el.propertyIterator();
    boolean propChanged = false;
    while (propertyIterator.hasNext()) {
      JMeterProperty orgProp = propertyIterator.next();
      JMeterProperty val = replaceProperty(orgProp, vars);
      if (!val.equals(orgProp) && !propChanged) {
        propChanged = true;
      }
      props.add(val);
    }
    // Only when some property change
    if (propChanged) {
      el.clear();
      for (JMeterProperty jmp : props) {
        el.setProperty(jmp);
      }
    }
  }

  private JMeterProperty replaceProperty(JMeterProperty prop, JMeterVariables vars) {
    if (prop instanceof StringProperty) {
      // Must not convert TestElement.gui_class etc
      if (!prop.getName().equals(TestElement.GUI_CLASS)
          && !prop.getName().equals(TestElement.TEST_CLASS)) {
        prop = replaceSimpleProp(prop, vars);
      }
    } else if (prop instanceof NumberProperty) {
      prop = replaceSimpleProp(prop, vars);
    } else if (prop instanceof MultiProperty) {
      if (prop instanceof TestElementProperty) {
        TestElementProperty testElementProperty = (TestElementProperty) prop;
        if (testElementProperty.getObjectValue() instanceof Argument) {
          replaceArgument((Argument) testElementProperty.getObjectValue(), vars);
          return prop;
        } else if (testElementProperty.getObjectValue() instanceof Header) {
          replaceHeader((Header) testElementProperty.getObjectValue(), vars);
          return prop;
        }
      }
      MultiProperty multiVal = (MultiProperty) prop;
      PropertyIterator propertyIterator = multiVal.iterator();
      Collection<JMeterProperty> newValues = new ArrayList<>();
      boolean propChanged = false;
      while (propertyIterator.hasNext()) {
        JMeterProperty orgProp = propertyIterator.next();
        JMeterProperty val = replaceProperty(orgProp, vars);
        if (!orgProp.equals(val) && !propChanged) {
          propChanged = true;
        }
        newValues.add(val);
      }
      if (propChanged) {
        multiVal.clear();
        for (JMeterProperty jmp : newValues) {
          multiVal.addProperty(jmp);
        }
      }
    }
    return prop;
  }

  private JMeterProperty replaceSimpleProp(JMeterProperty prop, JMeterVariables vars) {
    String input = prop.getStringValue();
    if (input == null) {
      return prop;
    }
    String newImput = replaceString(input, vars);
    if (newImput.equals(input)) {
      return prop;
    } else {
      return new StringProperty(prop.getName(), newImput);
    }
  }

  /**
   * Handles the process of the validation and replacements of the property's string with the stored
   * values. When implementing custom Correlation Replacements, this method needs to be
   * implemented.
   *
   * @param input property's string to check and replace
   * @param vars  stored variables shared between request during the recording
   * @return the resultant input after been processed
   */
  protected abstract String replaceString(String input, JMeterVariables vars);

  private void replaceArgument(Argument arg, JMeterVariables vars) {
    String input = arg.getValue();
    if (input == null) {
      return;
    }
    String argName = arg.getName();
    /*
      To normalize the replacement on arguments for HTTP requests, we include the argument name and
      '=' to the input, apply the replacement logic, and remove it afterward. This doesn't applies
      when the argument has no name (eg: Data Body is a JSON/XML).
    */
    String prefix = argName.isEmpty() ? "" : argName + "=";
    String newInput = replaceString(prefix + input, vars).replace(prefix, "");
    if (!newInput.equals(input)) {
      arg.setValue(newInput);
    }
    /*
    In order to comply backward compatibility from <=v2.5 keys (arg name) is also processed by
     the replacement
     */
    String newName = replaceString(argName, vars);
    if (!newName.equals(argName)) {
      arg.setName(newName);
    }
  }

  private void replaceHeader(Header header, JMeterVariables vars) {
    String input = header.getValue();
    if (input == null) {
      return;
    }
    String headName = header.getName();
    String newInput = replaceString(headName + ": " + input, vars)
        .replace(headName + ": ", "");
    if (!newInput.equals(input)) {
      header.setValue(newInput);
    }
    /*
    In order to comply backward compatibility from <=v2.5 keys (header name) is also processed by
     the replacement
     */
    String newName = replaceString(headName, vars);
    if (!newName.equals(headName)) {
      header.setName(newName);
    }
  }

  @Override
  public String getType() {
    return this.getClass().getCanonicalName();
  }

  @Override
  public String toString() {
    return "CorrelationReplacement{"
        + " replacementClass=" + getClass().getName()
        + " , paramValues='" + getParams() + "'" + "}}";
  }

  /**
   * Handles the loading of values from Test Plans and CorrelationTemplates.
   *
   * <p>Gets the values using the property names used to store it on the <code>updateTestElem
   * (CorrelationRuleTestElement testElem)</code> method. This method has to be overwritten when
   * implementing custom Correlation Extractors
   *
   * @param ruleTestElement CorrelationRuleTestElement that contains the values
   */
  public abstract void update(CorrelationRuleTestElement ruleTestElement);

  Function<String, String> replaceExpressionProvider() {
    return s -> replacementString == null
        || !java.util.regex.Pattern.compile("(\\$\\{.+?})").matcher(replacementString).matches()
        || replacementString.isEmpty()
        ? FUNCTION_REF_PREFIX + s + FUNCTION_REF_SUFFIX : s;
  }

  String computeStringReplacement(String varName) {
    String rawReplacementString = buildReplacementStringForMultivalued(varName);
    String computed = expressionEvaluator.apply(rawReplacementString);
    return computed;
  }

  String buildReplacementStringForMultivalued(String varNameMatch) {
    if (replacementString != null && replacementString.contains(variableName)) {
      return replacementString.replace(variableName, varNameMatch);
    }
    return replacementString;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CorrelationReplacement)) {
      return false;
    }
    CorrelationReplacement<?> that = (CorrelationReplacement<?>) o;
    return Objects.equals(variableName, that.variableName) && Objects.equals(
        replacementString, that.replacementString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(variableName, replacementString);
  }
}
