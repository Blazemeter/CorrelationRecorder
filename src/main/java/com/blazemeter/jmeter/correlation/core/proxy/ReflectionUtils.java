package com.blazemeter.jmeter.correlation.core.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReflectionUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ReflectionUtils.class);

  private ReflectionUtils() {
  }

  public static Field getField(Class<?> fieldClass, String fieldName) {
    try {
      Field f = fieldClass.getDeclaredField(fieldName);
      f.setAccessible(true);
      return f;
    } catch (NoSuchFieldException | SecurityException e) {
      LOG.error("Problem getting {}.{} field", fieldClass.getSimpleName(), fieldName, e);
      return null;
    }
  }

  public static Method getMethod(Class<?> methodClass, String methodName,
      Class<?>... parameterTypes) {
    try {
      Method m = methodClass.getDeclaredMethod(methodName, parameterTypes);
      m.setAccessible(true);
      return m;
    } catch (NoSuchMethodException | SecurityException e) {
      LOG.error("Problem getting {}.{} method", methodClass.getSimpleName(), methodName, e);
      return null;
    }
  }

  public static void checkMethods(Class<?> methodsClass, Method... methods) {
    for (Method m : methods) {
      if (m == null) {
        throw buildMissingClassMemberException(methodsClass, "method");
      }
    }
  }

  private static IllegalStateException buildMissingClassMemberException(Class<?> methodClass,
      String memberType) {
    return new IllegalStateException(
        "Problem accessing " + methodClass.getSimpleName() + " " + memberType + ". "
            + "Seems this version of JMeter is not yet supported. "
            + "Open a ticket in plugin repository with log report and JMeter version and try "
            + "another JMeter version.");
  }

  public static void checkFields(Class<?> fieldsClass, Field... fields) {
    for (Field f : fields) {
      if (f == null) {
        throw buildMissingClassMemberException(fieldsClass, "field");
      }
    }
  }

}
