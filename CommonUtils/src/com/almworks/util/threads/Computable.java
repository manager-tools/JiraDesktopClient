package com.almworks.util.threads;

/**
 * @author : Dyoma
 */
public interface Computable <T> {
  T compute();

  abstract class Once <T> implements Computable<T> {
    private boolean myCalculated = false;
    private T myValue;

    public T compute() {
      synchronized(this) {
        if (!myCalculated) {
          myValue = doCompute();
          myCalculated = true;
        }
        return myValue;
      }
    }

    protected abstract T doCompute();
  }
}
