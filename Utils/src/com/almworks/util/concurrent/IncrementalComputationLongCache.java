package com.almworks.util.concurrent;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterable;
import com.almworks.integers.LongIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.almworks.util.collections.Functional.cat;

/** Represents concurrently accessed cache of a costly incrementally computed long-valued function, i.e. function {@code f(n) = g(n, f(n-1))}, where {@code g} is computationally expensive.<br/>
 * For each given {@code n}, {@code f(n)} is computed once except for cases of concurrent access: it may be computed as many times as there are concurrent
 * requests for arguments {@code k_i >= n}. After all these requests return, {@code f(n)} is computed, and successive reads are lock-free. */
public class IncrementalComputationLongCache {
  private final Computation myFun;
  private final long myZero;
  /**
   * <ol>
   *   <li>Can only grow.</li>
   *   <li>Is a multiple of {@link #SEG_SZ}.</li>
   * </ol>
   * */
  private final AtomicInteger mySize;
  private final ConcurrentSkipListMap<Integer, long[]> mySegments = new ConcurrentSkipListMap<Integer, long[]>();
  private static final int SEG_SZ = 0x1000;
  
  /** @param zero {@code f(0)} */
  public IncrementalComputationLongCache(Computation fun, long zero) {
    myFun = fun;
    myZero = zero;
    mySize = new AtomicInteger(1);
  }

  /** May call the {@link Computation computation} with range {@code [s, e), s <= n <= e} if the value for {@code n} is not computed yet. 
   * @throws IndexOutOfBoundsException if n < 0
   * @throws NoSuchElementException if there are not enough values returned by {@link Computation#computeRange(int, int, long) computeRange}
   * */
  public long getOrComputeUpTo(int n) throws IndexOutOfBoundsException, NoSuchElementException {
    if (n < 0) throw new IndexOutOfBoundsException(String.valueOf(n));
    int curSize = mySize.get(); 
    if (curSize <= n) {
      int newSize = (n / SEG_SZ + 1)*SEG_SZ;
      int range = newSize - curSize;
      LongArray values = new LongArray(range);
      values.addAllNotMore(myFun.computeRange(curSize, newSize, get(curSize - 1)).iterator(), range);      
      synchronized (mySegments) {
        int newCurSize = mySize.get();
        if (newCurSize <= n) {
          // {newSize >= n >= newCurSize} => {range = newSize - curSize >= newCurSize - curSize}
          Iterable<LongIterator> valuesToAdd = values.subList(newCurSize - curSize, range);
          if (newCurSize == 1) {
            newCurSize = 0;
            valuesToAdd = cat(new LongIterator.Single(myZero), values);
          }
          addAll(newCurSize, valuesToAdd);
          assert mySegments.lastKey() == newSize - SEG_SZ : mySegments.lastKey() + " " + (newSize - SEG_SZ);
          mySize.set(newSize);
        }
      }
    }
    return get(n);
  }

  private long get(int n) {
    return n == 0 ? myZero : mySegments.floorEntry(n).getValue()[n % SEG_SZ];
  }

  /** 
   * @param values amount of elements must be a multiple of {@link #SEG_SZ}
   * @return new value to set to mySize */
  private void addAll(int curSize, Iterable<LongIterator> values) {
    for (Iterator<LongIterator> i = values.iterator(); i.hasNext(); ) {
      long[] seg = new long[SEG_SZ];
      for (int j = 0; j < SEG_SZ; ++j) seg[j] = i.next().value();
      mySegments.putIfAbsent(curSize, seg);
      curSize += SEG_SZ;
    }
  }
  
  
  public interface Computation {
    /** <p>The amount of elements in the returned LongIterable must be not less than {@code endExcl - startIncl}, otherwise getters will throw. 
     * Only first {@code endExcl - startIncl} elements will be retrieved from it.</p>
     * <p>If you intend to use this cache in a multi-thread environment, this computation must be thread-safe. The returned LongIterable is used 
     * only in the calling thread.</p>
     * <p>Also, it must be a <em>function</em> of the specified arguments, i.e. must return the same results provided the same start, end, and prevResult.</p>*/
    LongIterable computeRange(int startIncl, int endExcl, long prevResult);
  }
}
