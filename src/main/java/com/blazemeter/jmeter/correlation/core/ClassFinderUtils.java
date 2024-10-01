package com.blazemeter.jmeter.correlation.core;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.reflect.ClassFinder;

public class ClassFinderUtils {

  private static String inJUnitTest = null;
  private static String basePath;

  static {
    try {
      // Quick and fast way to retrieve $JmeterHome/lib/ext
      basePath = new File(URLDecoder.decode(ClassFinderUtils.class
          .getProtectionDomain().getCodeSource()
          .getLocation().getFile(), "UTF-8"))
          .getAbsolutePath();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isJUnitTest() {
    if (inJUnitTest == null) {
      for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
        if (element.getClassName().startsWith("org.junit.")) {
          inJUnitTest = String.valueOf(true);
          break;
        }
      }
      if (inJUnitTest == null) {
        inJUnitTest = String.valueOf(false);
      }
    }
    return Boolean.parseBoolean(inJUnitTest);
  }

  public static List<String> findClassesThatExtendOnLibExt(Class refClass) throws IOException {
    List<String> findPaths = new ArrayList<>();

    if (isJUnitTest()) {
      // Ad the jar of ACR to the find path
      findPaths.add(basePath);
    }
    // Add the /lib/ext and others "extension" folder to the search
    findPaths.addAll(Arrays.asList(JMeterUtils.getSearchPaths()));
    return findClassesThatExtend(refClass, findPaths);
  }

  public static List<String> findClassesThatExtend(Class refClass, List<String> findPaths)
      throws IOException {
    String[] findPathsArr = new String[findPaths.size()];
    findPathsArr = findPaths.toArray(findPathsArr);
    return ClassFinder.findClassesThatExtend(
        findPathsArr,
        new Class[]{refClass});
  }

}
