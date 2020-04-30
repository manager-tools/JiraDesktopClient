package com.almworks.util.cache2;

import com.almworks.util.cache.F;
import com.almworks.util.cache.IntegerKey;

import java.math.BigInteger;

/**
 * :todoc:
 *
 * @author sereda
 */
class FCached2ObjectProxy extends CachedObjectProxy<IntegerKey, FCached2Object> implements F {
  private final Cache<IntegerKey, FCached2Object> myCache;

  public FCached2ObjectProxy(Cache<IntegerKey, FCached2Object> cache, IntegerKey integerKey) {
    super(integerKey);
    myCache = cache;
  }

  public Cache<IntegerKey, FCached2Object> getCache() {
    return myCache;
  }

  public BigInteger getFactorial() {
    return delegate().getFactorial();
  }

  public int getNumber() {
    return myKey.getInt();
  }
}
