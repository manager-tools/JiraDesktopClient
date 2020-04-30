package com.almworks.util.ui.actions;

import org.almworks.util.TypedKey;

/**
 * @author dyoma
 */
public class DataRole <T> extends TypedKey<T> {
  private final Class<T> myClass;
  private Class<?> myLastMatchedClass = null;

  protected DataRole(String name, Class<T> aClass) {
    super(name, null, null);
    myClass = aClass;
  }

  public boolean matches(Object object) {
    Class<? extends Object> aClass = object.getClass();
    if (aClass == myClass)
      return true;
    if (aClass == myLastMatchedClass)
      return true;
    boolean matches = myClass.isInstance(object);
    if (matches)
      myLastMatchedClass = aClass;
    return matches;
  }

  public boolean matches(Class clazz) {
    return myClass.isAssignableFrom(clazz);
  }

  public Class<T> getDataClass() {
    return myClass;
  }

  public static <T> DataRole<T> createRole(Class<T> aClass) {
    return new DataRole<T>("DataRole[" + aClass + "]", aClass);
  }

  public static <T> DataRole<T> createRole(Class<T> aClass, String name) {
    return new DataRole<T>("DataRole[" + aClass + "] " + name, aClass);
  }
}
