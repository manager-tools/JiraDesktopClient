package com.almworks.util.collections;

import com.almworks.util.ReflectionUtil;
import com.almworks.util.commons.Factory;

import java.lang.reflect.Constructor;

/**
 * @author : Dyoma
 */
public class Factories {
  public static <T> Factory<T> newInstance(final Class<? extends T> aClass) {
    return new Factory<T>() {
      public T create() {
        return ReflectionUtil.newInstance(aClass);
      }
    };
  }

  public static <T> Factory<T> singleton(final T prototype) {
    return new Factory<T>() {
      public T create() {
        return prototype;
      }
    };
  }

  public static <T> Factory<T> newInstance(final Constructor constructor, final Object[] arguments) {
    return new Factory<T>() {
      public T create() {
        return ReflectionUtil.<T>newInstance(constructor, arguments);
      }
    };
  }
}
