package com.almworks.util.concurrent;

import com.almworks.integers.LongIterable;
import com.almworks.integers.LongProgression;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Const;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class IncrementalComputationLongCacheTests extends BaseTestCase {
  private IncrementalComputationLongCache cache;  
  private Random r;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    setWriteToStdout(true);
    r = getRandom();
  }

  @Override
  public void tearDown() throws Exception {
    cache = null;
    r = null;
    super.tearDown();
  }

  public void testSingleThread() {
    Succ succ = new Succ();
    cache = new IncrementalComputationLongCache(succ, Succ.ZERO);
    for (int i = 0; i < 10000; ++i) {
      computeOnce(1);
    }
    assertEquals(0, cache.getOrComputeUpTo(0));
    assertEquals(succ.getMax(), cache.getOrComputeUpTo(succ.getMax()));
  }

  private void computeOnce(int nThreads) {
    // Be prepared that all threads will compute all values at once
    // let's be prepared to max 200M consumption; for each thread, if maximum is x, it consumes O(8x) bytes  
    int k = r.nextInt(Math.min((int)(Const.MEBIBYTE * 200L / 8 / nThreads), 5000000));
    long ret = cache.getOrComputeUpTo(k);
    assertEquals((long)k, ret);
  }

  public void testMultipleThreads() throws InterruptedException, IOException {
    MultiThreadSucc succ = new MultiThreadSucc();
    cache = new IncrementalComputationLongCache(succ, 0L);
    final int nThreads = Runtime.getRuntime().availableProcessors();
    final CountDownLatch cdl = new CountDownLatch(nThreads);
    for (int i = 0; i < nThreads; ++i) new Thread(new Runnable() {
      @Override
      public void run() {
        for (int j = 0; j < 10000; ++j) {
          computeOnce(nThreads);
        }
        cdl.countDown();
      }
    }).start();
    cdl.await();
    assertEquals(succ.getMax(), cache.getOrComputeUpTo(succ.getMax()));
    System.out.println(succ.getMax());
  }
  
  private class Succ implements IncrementalComputationLongCache.Computation {
    public static final long ZERO = 0L;
    private int myMax = -1;
    
    @Override
    public LongIterable computeRange(int startIncl, int endExcl, long prevResult) {
      assertTrue(myMax + " " + startIncl, myMax < startIncl);
      myMax = startIncl;
      return LongProgression.arithmetic(prevResult + 1, endExcl - startIncl);
    }

    public int getMax() {
      return myMax;
    }
  }
  
 
  private static class MultiThreadSucc implements IncrementalComputationLongCache.Computation {
    private final AtomicInteger myMax = new AtomicInteger(0);

    @Override
    public LongIterable computeRange(int startIncl, int endExcl, long prevResult) {
      int max = myMax.getAndSet(startIncl);
      // This contract (f(n) is computed only once) was removed so that we don't have to call callback under synchronized section
//      assertTrue(max + " " + startIncl, max < startIncl);
      return LongProgression.arithmetic(prevResult + 1, endExcl - startIncl);
    }
    
    public int getMax() {
      return myMax.get();
    }
  }
}
