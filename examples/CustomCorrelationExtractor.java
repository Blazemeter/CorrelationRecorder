import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import com.blazemeter.jmeter.correlation.core.ParameterDefinition;
import com.blazemeter.jmeter.correlation.core.ResultField;
import com.blazemeter.jmeter.correlation.core.extractors.CorrelationExtractor;
import com.blazemeter.jmeter.correlation.gui.CorrelationRuleTestElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterVariables;

/**
 * This is the structure of a Correlation Extractor Extension. Extension from a CorrelationExtractor
 * allows the Plugin to categorize it as a Correlation Extractor, defining when it will be applied
 * during the correlating flow.
 */
public class CustomCorrelationExtractor<T extends CorrelationContext> extends
    CorrelationExtractor<T> {

  /**
   * Always use the "EXTRACTOR_PREFIX" before the property name, to be consistent with the expected
   * properties
   */
  private static final String FIELD_ONE_PROPERTY_NAME = EXTRACTOR_PREFIX + "fieldOne";
  private static final String FIELD_TWO_PROPERTY_NAME = EXTRACTOR_PREFIX + "fieldTwo";

  /**
   * Default values aren't necessary but, can be defined and assigned in the default extractor
   */
  private static final String FIELD_ONE_DEFAULT_VALUE = "fieldOneDefault";

  /**
   * Defining a Map for the <code>availableValues</code> param on the {@link ParameterDefinition}
   * will make it be rendered as a JComboBox. The Key will be the Displayed text and the value will
   * the selected value. Setting this parameter as <code>null</code> will make it be rendered as a
   * JTextField
   */
  private static final Map<String, String> fieldTwoValuesToDisplay = new HashMap<String, String>() {{
    put("Blue", "#3371FF ");
    put("Red", "#FF5733");
  }};

  private static final ParameterDefinition fieldOneDefinition = new ParameterDefinition("fieldOne",
      "Field One", FIELD_ONE_DEFAULT_VALUE, null);
  private static final ParameterDefinition fieldTwoDefinition = new ParameterDefinition("fieldTwo",
      "Field Two", "1", fieldTwoValuesToDisplay);

  private static final ParameterDefinition targetDefinition = new ParameterDefinition(
      TARGET_FIELD_NAME, TARGET_FIELD_DESCRIPTION, ResultField.BODY.getCode(),
      ResultField.getNamesToCodesMapping());


  /**
   * As long as you do the conversion from String to your field class, they can of any type. Eg:
   * integer, boolean, or even an inner class.
   */
  private String fieldOne;
  private int fieldTwo;

  /**
   * Default constructor added in order to satisfy the JSON conversion. You always need to implement
   * this method
   */
  public CustomCorrelationExtractor() {
    fieldOne = FIELD_ONE_DEFAULT_VALUE;
  }

  /**
   * The constructor that is going to be used to build the CorrelationExtractor when is loaded from
   * a TestPlan or a CorrelationTemplate, always will use Strings as params.
   *
   * @param fieldOne first String field for the class
   * @param fieldTwo second String field for the class that you need to parse
   */
  public CustomCorrelationExtractor(String fieldOne, String fieldTwo, String target) {
    this.fieldOne = fieldOne;
    this.fieldTwo = Integer.parseInt(fieldTwo);
    this.target = ResultField.valueOf(target);
  }

  /**
   * Returns the values set in the fields for this Correlation Extractor Extesion
   *
   * @return list of String equivalent values of your fields.
   */
  @Override
  public List<String> getParams() {
    return new ArrayList<>(Arrays.asList(fieldOne, Integer.toString(fieldTwo), target.getCode()));
  }

  /**
   * Set the values in the Correlation Extractor Extension, in the same order as those were returned
   * in the getParamsDefinition.
   *
   * @param params of String representations of the parameters to configure this
   * CorrelationExtractor
   */
  @Override
  public void setParams(List<String> params) {
    fieldOne = params.size() > 0 ? params.get(0) : FIELD_ONE_DEFAULT_VALUE;

    if (params.size() > 1) {
      fieldTwo = Integer.parseInt(params.get(1));
    }

    target = params.size() > 2 ? ResultField.valueOf(params.get(2)) : ResultField.BODY;
  }

  /**
   * This method returns the fields needed to configure the Correlation Extractor Extension. The
   * order in which those are returned here will always be the same when a setParam recives the
   * array. Be consisten.
   *
   * @return list of the definitions of the fields to configure the CorrelationExtractor
   */
  @Override
  public List<ParameterDefinition> getParamsDefinition() {
    return Arrays.asList(fieldOneDefinition, fieldTwoDefinition,
        targetDefinition);
  }

  /**
   * Store the value of the fields into a {@link CorrelationRuleTestElement}. The names of the
   * properties need to be consistent with the ones used in the update method.
   *
   * @param testElem CorrelationRuleTestElement where the fields will be stored
   */
  public void updateTestElem(CorrelationRuleTestElement testElem) {
    testElem.setProperty(FIELD_ONE_PROPERTY_NAME, fieldOne);
    testElem.setProperty(FIELD_TWO_PROPERTY_NAME, fieldTwo);
    testElem.setProperty(TARGET_FIELD_NAME, target.getCode());
  }

  /**
   * Load the values from a {@link CorrelationRuleTestElement} into the fields. The names of the
   * properties need to be consistent with the ones used in the updateTestElem method
   *
   * @param testElem CorrelationRuleTestElement from which the values are obtained
   */
  public void update(CorrelationRuleTestElement testElem) {
    fieldOne = testElem.getPropertyAsString(FIELD_ONE_PROPERTY_NAME);
    fieldTwo = testElem.getPropertyAsInt(FIELD_TWO_PROPERTY_NAME);
    target = ResultField
        .valueOf(testElem.getPropertyAsString(TARGET_FIELD_NAME, ResultField.BODY.getCode()));
  }

  /**
   * Every request and it's response will be included in the {@link SampleResult}. Overwriting this
   * methods allows you to implement the custom logic for the extraction of the desired values.
   *
   * In case the conditions you program are matched, it is possible to add different kind of
   * Components to the children list to extract the desired value during replay and, while in the
   * recording, put the matched value into the JMeterVariables shared variables with the key for
   * future references. It is suggested to use the <code>variableName</code> as key for easier
   * detection.
   *
   * Some components can be:
   * <li>RegexExtractor (PostProcessor)</li>
   * <li>JSR223 PreProcessor</li>
   *
   * @param sampler recorded sampler containing the information of the request
   * @param children list of children added to the sampler
   * @param result response obtained from the server after the request
   * @param vars stored variables shared between request during the recording
   * @see <a href="https://jmeter.apache.org/api/org/apache/jmeter/extractor/RegexExtractor.html">
   * RegexExtractor</a>
   * @see <a href="https://jmeter.apache.org/usermanual/component_reference.html#JSR223_PreProcessor">
   * JSR223 PreProcessor</a>
   * @see <a href="https://jmeter.apache.org/usermanual/component_reference.html">Components
   * Reference</a>
   */
  @Override
  public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result,
      JMeterVariables vars) {
    /*
     * Here goes the logic used to: g
     * 1. Get the information from the response
     * 2. Optain the desire value
     * 3. Process it
     * 4. Store it
     *
     * Children can be added and will be displayed on the sampler during the recording
     * */

  }
}
