package com.almworks.util.ui.macosx;

import com.almworks.util.Env;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Log;
import org.almworks.util.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AppIntegrationFeature {
  static final String EAWT_APPLICATION = "com.apple.eawt.Application";
  static final Class<?>[] NO_ARGS = {};

  private static final Map<Class<?>, Object> FEATURES = Collections.synchronizedMap(new HashMap<Class<?>, Object>());
  static {
    if (Env.isMac()) {
      detectFeatures();
    }
  }

  private static synchronized void detectFeatures() {
    FEATURES.put(BasicHandlersFeature.class, BasicHandlersFeature.create());
    FEATURES.put(DefaultMenuBarFeature.class, DefaultMenuBarFeature.create());
    FEATURES.put(OpenUriHandlerFeature.class, OpenUriHandlerFeature.create());
    FEATURES.put(FullScreenFeature.class, FullScreenFeature.create());
  }
  
  public static <T extends AppIntegrationFeature> T getFeature(Class<T> clazz) {
    return Util.castNullable(clazz, FEATURES.get(clazz));
  }
  
  static Class<?> getClass(String className) throws CantPerformException {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new CantPerformException(e);
    } catch (LinkageError e) {
      throw new CantPerformException(e);
    }
  }

  static Method getMethod(String className, Object target, String methodName, Class<?>[] argClasses) throws CantPerformException {
    final Class<?> clazz;
    if(className == null) {
      if(target == null) {
        throw new IllegalArgumentException();
      }
      clazz = target.getClass();
    } else {
      clazz = getClass(className);
    }

    try {
      return clazz.getMethod(methodName, argClasses);
    } catch (NoSuchMethodException e) {
      throw new CantPerformException(e);
    } catch (SecurityException e) {
      throw new CantPerformException(e);
    }
  }
  
  static Object reflectivelyCall(
    String className, Object target, String methodName,
    Class<?>[] argClasses, Object... args) throws CantPerformException
  {
    final Method method = getMethod(className, target, methodName, argClasses);
    try {
      return method.invoke(target, args);
    } catch (IllegalAccessException e) {
      throw new CantPerformException(e);
    } catch (InvocationTargetException e) {
      throw new CantPerformException(e.getCause());
    }
  }

  static Object getEawtApplication() throws CantPerformException {
    return reflectivelyCall(EAWT_APPLICATION, null, "getApplication", NO_ARGS);
  }

  static boolean classExists(String className) {
    try {
      getClass(className);
      return true;
    } catch (CantPerformException e) {
      Log.debug("AppIF: can't find class " + className);
      return false;
    }
  }
  
  static boolean methodExists(String className, String methodName, Class<?>... argClasses) {
    try {
      getMethod(className, null, methodName, argClasses);
      return true;
    } catch (CantPerformException e) {
      StringBuilder message = new StringBuilder();
      message.append("AppIF: can't find method ").append(className).append(".").append(methodName).append("(");
      for (int i = 0; i < argClasses.length; i++) {
        message.append(i > 0 ? ", " : "").append(argClasses[i].getName());
      }
      message.append(")");
      Log.debug(message.toString());
      return false;
    }
  }

  static void warn(CantPerformException e) {
    if (Env.isMac()) {
      Log.warn(e.getCause());
    }
  }
}
