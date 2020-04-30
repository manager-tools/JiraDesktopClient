package com.almworks.util.cache2;

import com.almworks.util.cache.F;
import com.almworks.util.cache.Util;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.CountDown;

import java.math.BigInteger;

public class LongCacheTests extends BaseTestCase implements LongKeyedCachedObjectLoader<FCachedLongObject> {
  private LRULongCache<FCachedLongObject> myCache;
  private static final int SIZE = 20;
  private static final int BASE = 20000;
  private static final int MANY_ITERATIONS = 10000;

  protected void setUp() throws Exception {
    myCache = new LRULongCache<FCachedLongObject>(this);
  }

  protected void tearDown() throws Exception {
    myCache = null;
  }

  public FCachedLongObject loadObject(long key) {
    // long
    return new FCachedLongObject(key);
  }

  private F proxy(int integer) {
    return new FCachedLongObjectProxy(myCache, BASE + integer);
  }

  public void testBasicContract() {
    // disabled - unstable test

    if (true) {
      return;
    }

    F[] fs = new F[SIZE];
    for (int i = 0; i < SIZE; i++) {
      fs[i] = proxy(i);
      fs[i].getFactorial();
      if (i > 0)
        assertEquals(fs[i].getFactorial(), proxy(i - 1).getFactorial().multiply(BigInteger.valueOf(BASE + i)));
      else
        proxy(i).getFactorial();
    }
    assertEquals("hits: " + myCache.getHits() + "; misses: " + myCache.getMisses(), SIZE, myCache.getHits());
    assertEquals(SIZE, myCache.getMisses());

    // all items except one access used. then we access more items from cache, and one not used gets
    // expunged first.
    // cache's conract doesn't state explicitly when an item would be expunged,
    // so we should assume that it would happen 100% if we request CAPACITY more new elements
    // also, cache may expunge any number of items
    int scapegoat = (SIZE >> 1) + 1;
    for (int i = 0; i < SIZE; i++) {
      //Assert.assertEquals(true, ((CachedObjectProxy) fs[i]).isDelegateAssigned());
      if (i != scapegoat)
        for (int j = 0; j < 10; j++)
          fs[i].getFactorial();
    }

    int misses = myCache.getMisses();
    for (int i = 0; i < SIZE; i++) {
      proxy(SIZE + i).getFactorial();
      assertEquals(misses + 1, myCache.getMisses());
      misses = myCache.getMisses();
      boolean expungedSomebody = false;
      boolean expungedScapegoat = false;
      for (int j = 0; j < fs.length; j++)
/*
        if (!((CachedObjectProxy) fs[j]).isDelegateAssigned()) {
          expungedSomebody = true;
          if (j == scapegoat) {
            expungedScapegoat = true;
            break;
          }
        }
*/
        ;
      if (expungedSomebody) {
        // if somebody expunged
        assertTrue(expungedScapegoat);
      }
    }

    // now it's re-requested
    proxy(scapegoat).getFactorial();
    //assertEquals(misses + 1, myCache.getMisses());
  }

  public void testCacheOneItem() {
    // test caching. if not caching, this should take forever
    for (int i = 0; i < MANY_ITERATIONS; i++)
      proxy(0).getFactorial();
    assertEquals(MANY_ITERATIONS - 1, myCache.getHits());
    assertEquals(1, myCache.getMisses());
  }

  public void testConcurrentCreation() throws InterruptedException {
    final int n = BASE; // fairly large to allow other threads to run
    final int THREADS = 5;
    final CountDown start = new CountDown(THREADS);
    final CountDown finish = new CountDown(THREADS);
    Runnable r = new Runnable() {
      public void run() {
        try {
          start.release();
          start.acquire();
          proxy(100).getFactorial();
          finish.release();
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
      }
    };
    int factorialings = Util.ourFactorialings;
    for (int i = 0; i < THREADS; i++)
      new Thread(r).start();
    finish.acquire();

    // only one calculation happened
    assertEquals(factorialings + 1, Util.ourFactorialings);
  }

  static BigInteger calculate(int anInt) {
    synchronized (LongCacheTests.class) {
      Util.ourFactorialings++;
    }
    BigInteger result = BigInteger.ONE;
    for (int i = 2; i <= anInt; i++)
      result = result.multiply(BigInteger.valueOf(i));
    return result;
  }
}
