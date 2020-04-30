package com.almworks.util.cache2;

public abstract class LongKeyedProxy<T extends LongKeyedObject> extends LongKeyed {
  private T myInstance = null;

  protected LongKeyedProxy(long key) {
    super(key);
  }

  protected abstract NoCache<T> getCache();

  protected final T delegate() {
    T instance = myInstance;
    if (instance == null) {
      myInstance = instance = getCache().get(key());
    }
    assert instance != null : key();
    return instance;
  }
}
