package com.almworks.util.cache2;

import com.almworks.util.cache.F;

import java.math.BigInteger;

class FCachedLongObjectProxy extends LongKeyedCachedObjectProxy<FCachedLongObject> implements F {
  private final LRULongCache<FCachedLongObject> myCache;

  public FCachedLongObjectProxy(LRULongCache<FCachedLongObject> cache, long key) {
    super(key);
    myCache = cache;
  }

  public LRULongCache<FCachedLongObject> getCache() {
    return myCache;
  }

  public BigInteger getFactorial() {
    return delegate().getFactorial();
  }

  public int getNumber() {
    return (int)myKey;
  }
}
