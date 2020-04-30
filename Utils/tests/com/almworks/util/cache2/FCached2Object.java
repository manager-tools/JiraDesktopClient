package com.almworks.util.cache2;

import com.almworks.util.cache.IntegerKey;
import com.almworks.util.cache.Util;

import java.math.BigInteger;

class FCached2Object extends CachedObject<IntegerKey> {
  private final BigInteger myFactorial;

  public FCached2Object(IntegerKey integerKey) {
    super(integerKey);
    myFactorial = Util.calculate(integerKey.getInt());
  }

  public BigInteger getFactorial() {
    return myFactorial;
  }
}
