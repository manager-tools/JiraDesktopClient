package com.almworks.util.commons;

import com.almworks.util.collections.Factories;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * A single-object pool. When an object is requested ({@link #getInstance()}) checks if current thread allows pooled
 * objects sharing without synchonization. If yes an pooled object is referenced returns shared reference.
 * Otherwise creates new instance.
 * To release requested instance use {@link #releaseInstance(Object)}
 */
public class AsyncSingleObjectPool<T> {
  private final Factory<Boolean> myOwnThread;
  private final Factory<T> myFactory;
  @Nullable
  private T myObject;
  
  private static final Factory<Boolean> IS_AWT_THREAD = new Factory<Boolean>() {
    public Boolean create() {
      return EventQueue.isDispatchThread();
    }
  };

  public AsyncSingleObjectPool(Factory<Boolean> ownThread, Factory<T> factory) {
    myOwnThread = ownThread;
    myFactory = factory;
  }

  @NotNull
  public T getInstance() {
    T obj = myObject;
    if (obj != null && isMyThread()) {
      myObject = null;
      return obj;
    }
    T t = myFactory.create();
    assert t !=null;
    return t;
  }

  private boolean isMyThread() {
    return myOwnThread.create();
  }

  public void releaseInstance(@Nullable T obj) {
    if (obj != null && myObject == null && isMyThread())
      myObject = obj;
  }

  /**
   * @param aClass
   * @return Object pool that allows sharing pooled instance in AWT event-dispatch threads.
   * New instance is created by calling default constructor of given class.
   */
  public static <T> AsyncSingleObjectPool<T> awtNewInstance(Class<? extends T> aClass) {
    return new AsyncSingleObjectPool<T>(IS_AWT_THREAD, Factories.newInstance(aClass));
  }
}
