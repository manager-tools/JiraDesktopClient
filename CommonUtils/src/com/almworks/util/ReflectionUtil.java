package com.almworks.util;


import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Failure;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ReflectionUtil {
  /**
   * This method is used to neatly instantiate an object. You don't have to deal with not-runtime
   * exceptions. If an object cannot be instantiated, an IllegalStateException is thrown.
   * <p/>
   * <b>NB:</b>{@link Class#newInstance} throws checked exceptions with a reason. Make
   * sure that you don't want to catch them. As a rule of thumb, if success or failure depends on
   * some kind of input, then you have to catch checked exceptions.
   *
   * @param clazz class for instantiation
   * @return new object
   * @see Class#newInstance()
   */
  public static <T> T newInstance(Class<T> clazz) {
    return newInstance(clazz, Failure.class, false);
  }

  /**
   * See {@link #newInstance(Class<? extends T>)}. In this variant, you may specify the kind
   * of runtime error to be thrown if object could not be instantiated. Also, you can try
   * to access private constructor for the class.
   */
  public static <T> T newInstance(Class<T> clazz, Class<? extends RuntimeException> throwIfCannot,
    boolean accessPrivateConstructor) {
    try {
      Constructor<T> constructor = clazz.getDeclaredConstructor(Const.EMPTY_CLASSES);
      T result;
      if (accessPrivateConstructor && !constructor.isAccessible()) {
        constructor.setAccessible(true);
        result = constructor.newInstance(Const.EMPTY_OBJECTS);
        constructor.setAccessible(false);
      } else {
        result = constructor.newInstance(Const.EMPTY_OBJECTS);
      }
      return result;
    } catch (NoSuchMethodException e) {
      throw createException(clazz, throwIfCannot, e);
    } catch (InstantiationException e) {
      throw createException(clazz, throwIfCannot, e);
    } catch (IllegalAccessException e) {
      throw createException(clazz, throwIfCannot, e);
    } catch (InvocationTargetException e) {
      throw createException(clazz, throwIfCannot, e);
    }
  }

  /**
   * This highly unreadable method attempts to create a runtime error according to client's wish.
   * It tries several constructors and throws error itself if all attempts are unsuccessful.
   * We could do this with {@link #newInstance} also, but stack trace could be a weirdo then.
   */
  private static RuntimeException createException(Class<?> clazz, Class<? extends RuntimeException> throwIfCannot,
    Exception cause) {

    RuntimeException exception;
    Constructor<? extends RuntimeException> constructor;
    Object[] parameters = null;
    constructor = getConstructor(throwIfCannot, Const.CLASSES_STRING_THROWABLE);
    if (constructor != null) {
      parameters = new Object[]{"cannot instantiate class " + clazz.getName() + " (" + cause + ")", cause};
    } else {
      constructor = getConstructor(throwIfCannot, Const.CLASSES_STRING);
      if (constructor != null) {
        parameters = new Object[]{"cannot instantiate class " + clazz.getName() + " (" + cause + ")"};
      } else {
        constructor = getConstructor(throwIfCannot, Const.EMPTY_CLASSES);
        if (constructor == null)
          throw new IllegalStateException("could not instantiate runtime error " + clazz);
        parameters = Const.EMPTY_OBJECTS;
      }
    }
    try {
      return constructor.newInstance(parameters);
    } catch (Exception e) {
      throw new Failure("could not instantiate runtime error " + clazz, e);
    }
  }

  private static Constructor getConstructor(Class clazz, Class[] parameters) {
    try {
      return clazz.getConstructor(parameters);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  public static <T> T newInstance(Constructor constructor, Object[] arguments) {
    try {
      return (T) constructor.newInstance(arguments);
    } catch (Exception e) {
      throw new Failure(e);
    }
  }

  /**
   * Returns all interfaces that are implemented by this object and its superclasses.
   */
  public static Class[] getAllInterfaces(Class clazz) {
    Set<Class> result = Collections15.linkedHashSet();
    while (clazz != null) {
      Class[] interfaces = clazz.getInterfaces();
      result.addAll(Arrays.asList(interfaces));
      clazz = clazz.getSuperclass();
    }
    return result.toArray(new Class[result.size()]);
  }

  public static Method findMethod(Class clazz, String name) {
    Method[] methods = clazz.getDeclaredMethods();
    List<Method> result = Collections15.arrayList();
    for (Method method : methods) {
      if (method.getName().equals(name)) result.add(method);
    }
    return result.size() == 1 ? result.get(0) : null;
  }

  public static Object findEnumConstant(String enumName, String constName) {
    try {
      Class<?> cls = Class.forName(enumName);
      if (cls == null) return null;
      Class<? extends Enum> enumCls = cls.asSubclass(Enum.class);
      return Enum.valueOf(enumCls, constName);
    } catch (Exception e) {
      return null;
    }
  }

  public static Object safeInvoke(Object instance, Method method, Object ... arguments) {
    try {
      return method.invoke(instance, arguments);
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean isStatic(Member field) {
    return (field.getModifiers() & Modifier.STATIC) != 0;
  }
}
