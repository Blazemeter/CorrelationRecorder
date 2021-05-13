package com.blazemeter.jmeter.correlation.core.proxy;

import java.util.UUID;
import org.apache.jmeter.modifiers.JSR223PreProcessor;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;

public class Jsr223PreProcessorFactory {

  private Jsr223PreProcessorFactory() {
  }

  public static JSR223PreProcessor fromNameAndScript(String name, String script) {
    JSR223PreProcessor ret = new JSR223PreProcessor();
    ret.setProperty(JSR223PreProcessor.GUI_CLASS, TestBeanGUI.class.getName());
    ret.setName(name);
    ret.setProperty("cacheKey", UUID.randomUUID().toString());
    ret.setProperty("script", script);
    return ret;
  }

}
