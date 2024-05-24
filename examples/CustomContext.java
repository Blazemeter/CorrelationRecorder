import com.blazemeter.jmeter.correlation.core.CorrelationContext;
import org.apache.jmeter.samplers.SampleResult;


/**
 * This is the structure for a basic Correlation Context Implementation.
 * All the values that want to be shared between a Correlation
 * Extensions will be stored using this class.
 * The update method is the one that gets the information
 * */
public class CustomContext implements CorrelationContext {

  /**
   * Define the variables that are going to be shared between the CorrelationExtractors and
   * CorrelationReplacements
   */
  private final Map<String, String> sharedFieldOne = new HashMap<>();
  private int sharedFieldTwo = 0;
  private Integer counter;

  /**
   * Resets the shared variables to their default values.
   *
   * This method is always called when the JMeter starts recording and the Proxy is started.
   */
  @Override
  public void reset() {
    sharedFieldOne = new HashMap<>();
    sharedFieldTwo = 0;
    counter = 0;
  }

  /**
   * Define the logic for updating the shared values.
   *
   * This method is always called when the {@link CorrelationEngine} is processing the responses
   * from the server.
   *
   * Look for the implementation on {@link ../src/main/java/com/blazemeter/jmeter/correlation/siebel/SiebelContext}
   */
  @Override
  public void update(SampleResult sampleResult) {
    /*
     * Here goes to logic used to update values comming from the sampleResult.
     * For Example: Lets count if the is any <script> tag on the response
     * using the `sampleResult.getResponseDataAsString()` method.
     *
     * Read the java doc for a more detailed example
     * */

    String responseAsString = sampleResult.getResponseDataAsString();
    if (responseAsString.contains("<script>"))
      counter++;
  }
}
}



