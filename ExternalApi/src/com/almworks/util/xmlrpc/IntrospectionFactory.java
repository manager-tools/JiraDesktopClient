package com.almworks.util.xmlrpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;

/**
 * Introspected class should conform to the following constraints:
 * 1. Have public constructor that accepts Vector
 * 2. Have public static method that accepts Void and returns String
 */
class IntrospectionFactory implements IncomingMessageFactory {
  private final String myRpcMethodName;
  private final Constructor myConstructor;
  private final Object[] myAdditionalParameters;

  public IntrospectionFactory(Class clazz) {
    this(clazz, null);
  }

  public IntrospectionFactory(Class clazz, Object[] additionalParameters) {
    if (!IncomingMessage.class.isAssignableFrom(clazz))
      throw new IllegalArgumentException("not a message: " + clazz);
    myRpcMethodName = fetchName(clazz);
    if (myRpcMethodName == null)
      throw new IllegalArgumentException("no method: " + clazz);
    myConstructor = fetchConstructor(clazz, additionalParameters);
    if (myConstructor == null)
      throw new IllegalArgumentException("no constructor: " + clazz);
    myAdditionalParameters = additionalParameters;
  }

  private static Constructor fetchConstructor(Class clazz, Object[] additionalParameters) {
    int requiredParameters = additionalParameters == null ? 1 : 1 + additionalParameters.length;
    Constructor[] constructors = clazz.getDeclaredConstructors();
    for (Constructor constructor : constructors) {
      int modifiers = constructor.getModifiers();
      if ((modifiers & Modifier.PUBLIC) != Modifier.PUBLIC)
        continue;
      Class[] parameters = constructor.getParameterTypes();
      if (parameters.length != requiredParameters)
        continue;
      if (!Vector.class.equals(parameters[0]))
        continue;
      if (additionalParameters != null) {
        boolean matches = true;
        for (int i = 1; i < parameters.length; i++) {
          if (!parameters[i].isInstance(additionalParameters[i - 1])) {
            matches = false;
            break;
          }
        }
        if (!matches)
          continue;
      }
      try {
        constructor.setAccessible(true);
      } catch (SecurityException e) {
        // ignore
      }
      return constructor;
    }
    return null;
  }

  private static String fetchName(Class clazz) {
    Method[] methods = clazz.getDeclaredMethods();
    for (Method method : methods) {
      int modifiers = method.getModifiers();
      int required = Modifier.PUBLIC | Modifier.STATIC;
      if ((modifiers & required) != required)
        continue;
      if (!String.class.equals(method.getReturnType()))
        continue;
      Class<?>[] parameters = method.getParameterTypes();
      if (parameters.length != 1)
        continue;
      if (!Void.class.equals(parameters[0]))
        continue;
      try {
        method.setAccessible(true);
      } catch (SecurityException e) {
        // ignore
      }
      try {
        Object result = method.invoke(null, new Object[] {null});
        if (!(result instanceof String))
          continue;
        return (String) result;
      } catch (Exception e) {
        continue;
      }
    }
    return null;
  }

  public String getRpcMethodName() {
    return myRpcMethodName;
  }

  public IncomingMessage createMessage(Vector parameters) throws Exception {
    try {
      if (myAdditionalParameters == null) {
        return (IncomingMessage) myConstructor.newInstance(parameters);
      } else {
        Object[] p = new Object[myAdditionalParameters.length + 1];
        p[0] = parameters;
        for (int i = 1; i < p.length; i++)
          p[i] = myAdditionalParameters[i - 1];
        return (IncomingMessage) myConstructor.newInstance(p);
      }
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        Throwable targetException = ((InvocationTargetException) e).getTargetException();
        if (targetException instanceof Exception)
          e = (Exception) targetException;
      }
      throw e;
    }
  }
}
