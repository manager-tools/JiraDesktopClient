package com.almworks.util.cache2;

import com.almworks.util.cache.Util;

import java.math.BigInteger;

class FCachedLongObject extends LongKeyedCachedObject {
  private final BigInteger myFactorial;

  public FCachedLongObject(long key) {
    super(key);
    myFactorial = Util.calculate((int)key);
  }

  public BigInteger getFactorial() {
    return myFactorial;
  }
}
