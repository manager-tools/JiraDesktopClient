package com.almworks.util.tests;


import org.almworks.util.Collections15;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class MethodResolver {
  private final Class myClass;
  private final String myName;
  private Class[] myArgumentTypes = null;
  private Class myReturnType = null;
  public static final Map<Class, Class> PRIMITIVE_TO_OBJECT;

  static {
    HashMap<Class, Class> map = Collections15.hashMap();
    map.put(boolean.class, Boolean.class);
    map.put(int.class, Integer.class);
    map.put(long.class, Long.class);
    PRIMITIVE_TO_OBJECT = Collections.unmodifiableMap(map);
  }

  public MethodResolver(Class aClass, String name) {
    myClass = aClass;
    myName = name;
  }

  public MethodResolver setReturnType(Class type) {
    myReturnType = type;
    return this;
  }

  public MethodResolver setReturnValue(Object value) {
    if (value != null)
      setReturnType(value.getClass());
    return this;
  }

  public MethodResolver setVoidReturn() {
    setReturnType(void.class);
    return this;
  }

  public MethodResolver setArgumentTypes(Class[] argumentTypes) {
    myArgumentTypes = argumentTypes;
    return this;
  }

  public MethodResolver setArgumentValues(Object[] arguments) {
    setArgumentCount(arguments.length);
    for (int i = 0; i < arguments.length; i++) {
      Object argument = arguments[i];
      setArgumentType(i, argument != null ? argument.getClass() : null);
    }
    return this;
  }

  public MethodResolver setArgumentCount(int count) {
    Class[] classes = new Class[count];
    if (myArgumentTypes != null) {
      assert myArgumentTypes.length <= classes.length;
      System.arraycopy(myArgumentTypes, 0, classes, 0, myArgumentTypes.length);
    }
    myArgumentTypes = classes;
    return this;
  }

  public MethodResolver setArgumentType(int index, Class type) {
    if (myArgumentTypes == null || index >= myArgumentTypes.length)
      setArgumentCount(index + 1);
    myArgumentTypes[index] = type;
    return this;
  }

  public Method resolve() {
    Method[] methods = myClass.getMethods();
    Method candidate = null;
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      if (!method.getName().equals(myName))
        continue;
      if (!areTypesMatches(method.getParameterTypes()))
        continue;
      if (!isReturnTypeMatches(method.getReturnType()))
        continue;
      if (candidate != null) {
        Method overridenResolution = resolveOverriden(candidate, method);
        if (overridenResolution == null)
          throw new RuntimeException("At least two method matches:\n" + candidate + " and\n" + method);
        method = overridenResolution;
      }
      candidate = method;
    }
    if (candidate == null)
      throw new RuntimeException(
        "Can't resolve: " + myReturnType + " " + myName + "(" + (myArgumentTypes != null ? Arrays.asList(myArgumentTypes).toString() : "?") + ")");
    return candidate;
  }

  private boolean isReturnTypeMatches(Class returnType) {
    if (myReturnType == null)
      return true;
    if (myReturnType == returnType)
      return true;
    if (returnType == void.class)
      return false;
    if (returnType.isPrimitive()) {
      if (myReturnType.isPrimitive())
        return returnType.equals(myReturnType);
      else
        return PRIMITIVE_TO_OBJECT.get(returnType).equals(myReturnType);
    }
    assert !myReturnType.isPrimitive() && !returnType.isPrimitive() : "Not implemented";
    return returnType.isAssignableFrom(myReturnType);
  }

  private Method resolveOverriden(Method method1, Method method2) {
    if (myArgumentTypes == null)
      return null;
    Class[] types1 = method1.getParameterTypes();
    Class[] types2 = method2.getParameterTypes();
    assert types1.length == types2.length;
    assert types1.length == myArgumentTypes.length;
    Method resolution = null;
    for (int i = 0; i < myArgumentTypes.length; i++) {
      Class argumentType = myArgumentTypes[i];
      if (argumentType == null)
        continue;
      boolean better1 = types2[i].isAssignableFrom(types1[i]);
      boolean better2 = types1[i].isAssignableFrom(types2[i]);
      assert !better1 || !better2;
      if (resolution == method1 && better2)
        return null;
      if (resolution == method2 && better1)
        return null;
      if (better1)
        resolution = method1;
      if (better2)
        resolution = method2;
    }
    return resolution;
  }

  private boolean areTypesMatches(Class[] parameterTypes) {
    if (myArgumentTypes == null)
      return true;
    if (myArgumentTypes.length != parameterTypes.length)
      return false;
    for (int i = 0; i < myArgumentTypes.length; i++) {
      if (!matchType(myArgumentTypes[i], parameterTypes[i])) {
        return false;
      }
    }
    return true;
  }

  private static boolean matchType(Class assumed, Class method) {
    if (assumed == null)
      return true;
    if (method.isAssignableFrom(assumed))
      return true;
    if (method.isPrimitive()) {
      if (method.equals(boolean.class) && assumed.equals(Boolean.class))
        return true;
      if (method.equals(long.class) && assumed.equals(Long.class))
        return true;
      // todo
    }
    return false;
  }


  public boolean hasReturnType() {
    return myReturnType != null;
  }
}
