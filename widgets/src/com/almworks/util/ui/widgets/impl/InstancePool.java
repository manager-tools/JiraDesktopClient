package com.almworks.util.ui.widgets.impl;


import com.almworks.util.commons.Factory;
import com.almworks.util.ui.widgets.genutil.Log;

import java.util.ArrayList;

public class InstancePool<T> {
  private static final Log<InstancePool> log = Log.get(InstancePool.class);
  private final Factory<T> myFactory;
  private T myInstance = null;

  public InstancePool(Factory<T> factory) {
    myFactory = factory;
  }

  public T getInstance() {
    T instance = myInstance;
    if (instance != null) {
      myInstance = null;
      return instance;
    }
    return myFactory.create();
  }

  public void release(T instance) {
    if (myInstance != null) return;
    myInstance = instance;
  }

  public static <T> InstancePool<ArrayList<T>> listPool() {
    return new InstancePool<ArrayList<T>>(new Factory<ArrayList<T>>() {
      @Override
      public ArrayList<T> create() {
        return new ArrayList<T>();
      }
    });
  }
}
