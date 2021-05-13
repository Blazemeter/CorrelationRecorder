package com.blazemeter.jmeter.correlation;

import kg.apc.emulators.TestJMeterUtils;

/**
 * We need this class since we can't directly invoke {@link TestJMeterUtils#createJmeterEnv()}
 * because some times we get "Caused by: java.nio.file.FileAlreadyExistsException:
 * /tmp/jpgc/ut4571494352889939980/ss.props" (both in mac and ubuntu in gitlab).
 */
public class JMeterTestUtils {

  private static boolean jmeterEnvInitialized = false;

  private JMeterTestUtils() {
  }

  public static void setupJmeterEnv() {
    if (!jmeterEnvInitialized) {
      jmeterEnvInitialized = true;
      TestJMeterUtils.createJmeterEnv();
    }
  }

}
