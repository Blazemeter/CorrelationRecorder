package com.blazemeter.jmeter.correlation.siebel;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.AbstractFunction;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiebelArrayFunction extends AbstractFunction {

  private static final Logger LOG = LoggerFactory.getLogger(SiebelArrayFunction.class);
  private static final List<String> DESC = new LinkedList<>();
  private static final String KEY = "__splitStarArray"; // $NON-NLS-1$

  static {
    DESC.add(JMeterUtils.getResString("split_function_string")); //$NON-NLS-1$
    DESC.add(JMeterUtils.getResString("function_name_param")); //$NON-NLS-1$
  }

  private Object[] values;

  public static void split(String stringToSplit, String varNamePrefix, JMeterVariables vars) {
    int i = 0;
    int variableIndex = 0;
    try {
      while (i < stringToSplit.length()) {
        variableIndex++;
        int indexOfStar = stringToSplit.indexOf("*", i);
        int indexOfStarPlusOne = indexOfStar + 1;
        int length = Integer.parseInt(stringToSplit.substring(i, indexOfStar));
        String varName = varNamePrefix + "_" + variableIndex;
        String varValue = stringToSplit.substring(indexOfStarPlusOne,
            indexOfStarPlusOne + length);
        vars.put(varName, varValue);
        if (LOG.isDebugEnabled()) {
          LOG.debug("${{}} = {}", varName, varValue);
        }
        i = indexOfStarPlusOne + length;
      }
      vars.put(varNamePrefix + "_n", String.valueOf(variableIndex));
    } catch (StringIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Given string does not comply star array format.");
    } catch (NullPointerException e) {
      throw new IllegalArgumentException("Input string cannot be null.");
    }
  }

  @Override
  public List<String> getArgumentDesc() {
    return DESC;
  }

  @Override
  public String execute(SampleResult previousResult, Sampler currentSampler) {
    JMeterVariables vars = getVariables();
    String stringToSplit = ((CompoundVariable) values[0]).execute();
    String varNamePrefix = ((CompoundVariable) values[1]).execute().trim();
    split(stringToSplit, varNamePrefix, vars);
    return stringToSplit;
  }

  @Override
  public String getReferenceKey() {
    return KEY;
  }

  @Override
  public void setParameters(Collection<CompoundVariable> parameters)
      throws InvalidVariableException {
    checkParameterCount(parameters, 2);
    values = parameters.toArray();
  }
}
