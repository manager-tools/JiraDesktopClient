package com.almworks.util.cache;

import java.math.BigInteger;

/**
 * :todoc:
 *
 * @author sereda
 */
public class Util {
  public static int ourFactorialings = 0;

  public static BigInteger calculate(int anInt) {
    synchronized (CacheTests.class) {
      ourFactorialings++;
    }
    BigInteger result = BigInteger.ONE;
    for (int i = 2; i <= anInt; i++)
      result = result.multiply(BigInteger.valueOf(i));
    return result;
  }
}
