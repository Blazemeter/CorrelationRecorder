package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.Arrays;
import java.util.Collection;

public class RequestHeaderFieldsParameters {

  // This method provides the parameters for the Request Header Fields test
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"Name: value", "value"},
        {"Another: 123", "123"},
        {"Last: test", "test"},
        {"Only: value", "value"},
        {null, null},
        {"InvalidHeader: value", null},
        {"MultiValue: value1, value2, value3", "value1, value2, value3"},
        {"SpecialChars: @#$%^&*()_+-=[]{}|;:'\",.<>/?`~", "@#$%^&*()_+-=[]{}|;:'\",.<>/?`~"},
    });
  }
}

