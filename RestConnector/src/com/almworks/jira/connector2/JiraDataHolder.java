package com.almworks.jira.connector2;

import com.almworks.util.commons.Factory;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dyoma
 */
public abstract class JiraDataHolder {
  protected final Map<TypedKey<?>, ?> myUserParameters = new HashMap<TypedKey<?>, Object>();
  private final Map<TypedKey<?>, Factory<?>> myFactories = Collections15.hashMap();

  public synchronized <T> T getValue(TypedKey<T> key) {
    T value = key.getFrom(myUserParameters);
    if (value == null) {
      Factory<T> factory = (Factory<T>) myFactories.get(key);
      if (factory != null) {
        value = factory.create();
        if (value != null) {
          key.putTo(myUserParameters, value);
        }
      }
    }
    return value;
  }

  public synchronized <T> T getValueNoCreation(TypedKey<T> key) {
    return key.getFrom(myUserParameters);
  }

  public synchronized <T> T putValue(TypedKey<T> key, T value) {
    return key.putTo(myUserParameters, value);
  }

  public synchronized <T> T putWeakValue(TypedKey<WeakReference<T>> key, T value) {
    WeakReference<T> reference = value != null ? new WeakReference<T>(value) : null;
    WeakReference<T> oldReference = putValue(key, reference);
    return oldReference != null ? oldReference.get() : null;
  }

  public synchronized <T> T getWeakValue(TypedKey<WeakReference<T>> key) {
    WeakReference<T> reference = getValue(key);
    if (reference == null) {
      return null;
    } else {
      T value = reference.get();
      if (value == null) {
        myUserParameters.remove(key);
      }
      return value;
    }
  }

  public synchronized <T> void installFactory(TypedKey<T> key, Factory<? extends T> factory) {
    myFactories.put(key, factory);
  }

  public void clear() {
    myFactories.clear();
    myUserParameters.clear();
  }
}
