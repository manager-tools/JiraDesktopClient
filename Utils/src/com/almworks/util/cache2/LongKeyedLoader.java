package com.almworks.util.cache2;

public interface LongKeyedLoader<T extends LongKeyed> {
  T loadObject(long key);
}
