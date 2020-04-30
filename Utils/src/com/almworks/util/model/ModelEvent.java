package com.almworks.util.model;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ModelEvent <M, K, V> {
  private final M myModel;
  private final K myKey;
  private final V myOldValue;
  private final V myNewValue;

  protected ModelEvent(M model, K key, V oldValue, V newValue) {
    myModel = model;
    myKey = key;
    myOldValue = oldValue;
    myNewValue = newValue;
  }

  public M getModel() {
    return myModel;
  }

  public K getKey() {
    return myKey;
  }

  public V getOldValue() {
    return myOldValue;
  }

  public V getNewValue() {
    return myNewValue;
  }

  public static <M, K, V> ModelEvent create(M model, K key, V oldValue, V newValue) {
    return new ModelEvent<M, K, V>(model, key, oldValue, newValue);
  }
}
