package com.almworks.util.cache;

import java.math.BigInteger;

/**
 * :todoc:
 *
 * @author sereda
 */
class FCachedObjectProxy extends CachedObjectProxy<IntegerKey, FCachedObject> implements F {
  public FCachedObjectProxy(Cache<IntegerKey, FCachedObject> cache, IntegerKey integerKey) {
    super(cache, integerKey);
  }

  public BigInteger getFactorial() {
    return delegate().getFactorial();
  }

  public int getNumber() {
    return myKey.getInt();
  }
}
