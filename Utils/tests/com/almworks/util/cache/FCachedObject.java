package com.almworks.util.cache;

import java.math.BigInteger;

/**
 * :todoc:
 *
 * @author sereda
 */
class FCachedObject extends CachedObject<IntegerKey> {
  private final BigInteger myFactorial;

  public FCachedObject(IntegerKey integerKey) {
    super(integerKey);
    myFactorial = Util.calculate(integerKey.getInt());
  }

  public BigInteger getFactorial() {
    return myFactorial;
  }
}
