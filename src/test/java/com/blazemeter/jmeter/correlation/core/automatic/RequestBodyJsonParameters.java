package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.Arrays;
import java.util.Collection;

public class RequestBodyJsonParameters {

  // This method provides the parameters for the Request Body JSON test
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"{\"Name\":\"value\"}", "value"},
        {"{\"Another\":123}", "123"},
        {"{\"Last\":\"test\"}", "test"},
        {"{\"Only\":\"value\"}", "value"},
        {null, null},
        {"{\"Invalid\":\"value\"}", null},
        {"{\"Array\":[1, 2, 3]}", "[1, 2, 3]"},
        {"{\"Nested\":{\"Inner\":\"value\"}}", "{\"Inner\":\"value\"}"},
    });
  }
}

