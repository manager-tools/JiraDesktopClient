package com.almworks.util.cache2;

import com.almworks.util.cache.Keyed;
import org.almworks.util.Collections15;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;

public abstract class CachedObjectProxy <K, T extends CachedObject<K>> extends Keyed<K> {
  private volatile Reference<T> myReference = null;

//  private static DebugDichotomy count = new DebugDichotomy("proxy.count", "", 10000);

  private static final Map<StackKey, Integer> callers = Collections15.hashMap();

  protected CachedObjectProxy(K key) {
    super(key);
/*
PROFILING
    count.a();
    synchronized(CachedObjectProxy.class) {
      StackKey stack = new StackKey(new Throwable().getStackTrace());
      Integer count = callers.access(stack);
      count = count == null ? new Integer(1) : new Integer(count.intValue() + 1);
      callers.put(stack, count);

      if (CachedObjectProxy.count.getA() % 50000 == 0) {
        int max = 0;
        StackKey maxKey = null;
        for (Iterator<Map.Entry<StackKey,Integer>> ii = callers.entrySet().iterator(); ii.hasNext();) {
          Map.Entry<StackKey,Integer> entry = ii.next();
          int c = entry.getValue();
          if (c > max) {
            max = c;
            maxKey = entry.getKey();
          }
        }
        if (maxKey != null) {
          System.out.println(max + " times:");
          StringBuffer result = new StringBuffer(1000);
          DecentFormatter.printStackTrace(maxKey.myStack, result);
          System.out.print(result.toString());
          System.out.println("========================+++=========================");
        }
        System.exit(1);
      }
    }
*/
  }

  private static class StackKey {
    private final StackTraceElement[] myStack;
    private int myHashCode;

    public StackKey(StackTraceElement[] stack) {
      myStack = stack;
      myHashCode = 11;
      for (int i = 0; i < stack.length; i++)
        myHashCode = myHashCode * 11 + stack[i].hashCode();
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof StackKey))
        return false;
      StackKey that = (StackKey) obj;
      return Arrays.equals(myStack, that.myStack);
    }

    public int hashCode() {
      return myHashCode;
    }


  }


  protected abstract Cache<K, T> getCache();

//  private static DebugDichotomy stats = new DebugDichotomy("refed", "tocache", 1000);

  protected final T delegate() {
    // Quick unsynchronized guess.
    // Even if myReference is changed concurrently, I will either access a valid
    // object, or fall through to synchronized block.
    Reference<T> ref = myReference;
    if (ref != null) {
      T delegate = ref.get();
      if (delegate != null) {
        delegate.accessed();
//        stats.a();
        return delegate;
      }
    }
    return delegate2();
  }

  private T delegate2() {
    // don't care if myReference is overwritten
    T delegate = getCache().get(myKey);
    myReference = new WeakReference<T>(delegate);
    return delegate;
  }
/*
  private T delegate2() {
    synchronized (this) {
      if (myReference != null) {
        T delegate = myReference.get();
        if (delegate != null) {
          delegate.accessed();
//          stats.a();
          return delegate;
        }
      }
      T delegate = getCache().get(myKey);
      myReference = new WeakReference<T>(delegate);
//      stats.b();
      return delegate;
    }
  }
*/
}
