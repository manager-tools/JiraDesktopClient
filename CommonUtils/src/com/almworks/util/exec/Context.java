package com.almworks.util.exec;

import com.almworks.util.Env;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.awt.AppContext;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Context is a facility for passing down the stack parameters.
 * Underlying data storage is ThreadLocal variables.
 * Context is also known by ThreadGates, and all parameters make it through gates to another thread.
 */
public final class Context {
  static final boolean DEBUG = Env.getBoolean("debug.context");

  /**
   * Global context is accessible from any thread
   */
  private static final CopyOnWriteArrayList<ContextFrame> ourGlobals = new CopyOnWriteArrayList<ContextFrame>();

  /**
   * Thread-local storage of a context
   */
  private static final ThreadLocal<ThreadContext> ourThreadContext = new ThreadLocal<ThreadContext>() {
    protected ThreadContext initialValue() {
      return new ThreadContext();
    }
  };


  /**
   * A cache for the last thread-local context.
   * It is <b>specifically non-volatile</b>, because if JVM caches the value for the current
   * thread, that would only make hit ratio better.
   */
  private static ThreadContext ourCachedThreadContext;

  private static volatile boolean hasAppContext = true;
  /**
   * A special helper for understaing if the thread is awt event queue (it is faster than isDispatchThread)
   */
  public static final ThreadLocal<Boolean> ourThreadAWT = new ThreadLocal<Boolean>() {
    protected Boolean initialValue() {
      if (hasAppContext) {
        // this is done to avoid instantiation of AWT event queue just to check that the current thread is not event queue
        try {
          boolean awtStarted = AppContext.getAppContext().get(AppContext.EVENT_QUEUE_KEY) != null;
          if (!awtStarted)
            return false;
        } catch (NoClassDefFoundError e) {
          hasAppContext = false;
        }
      }
      return EventQueue.isDispatchThread();
    }
  };

  private static volatile boolean ourDepthErrorShown;

  //===== GETTERS =====

  @NotNull
  public static <T> T get(TypedKey<T> key, @NotNull T defaultValue) {
    T value = get(key);
    return value != null ? value : defaultValue;
  }

  @NotNull
  public static <T> T get(Class<T> objectClass, @NotNull T defaultValue) {
    T value = get(objectClass);
    return value != null ? value : defaultValue;
  }

  // debug get

  @Nullable
  public static <T> T get(Class<T> objectClass) {
    return getImpl(objectClass, null);
  }

  @Nullable
  public static <T> T get(TypedKey<T> key) {
    return getImpl(null, key);
  }


  private static <T> T getImpl(Class<T> objectClass, TypedKey<T> key) {
    try {
      ThreadContext threadContext = getThreadContext();
      threadContext.lock(true);
      try {
        T result = threadContext.getObject(objectClass, key);
        if (result == null) {
          result = getGlobal(objectClass, key);
        }
        return result;
      } finally {
        threadContext.lock(false);
      }
    } catch (ContextDepthException e) {
      if (!ourDepthErrorShown) {
        ourDepthErrorShown = true;
        Log.error(e);
      } else {
        Log.warn(e);
      }
      return null;
    }
  }

  private static <T> T getGlobal(Class<T> objectClass, TypedKey<T> key) throws ContextDepthException {
    for (ContextFrame global : ourGlobals) {
      T result = ContextFrame.getObject(global, objectClass, key, 0);
      if (result != null)
        return result;
    }
    return null;
  }

  @NotNull
  public static <T> T require(Class<T> objectClass) {
    T value = get(objectClass);
    validate(value, objectClass);
    //noinspection ConstantConditions
    return value;
  }

  @NotNull
  public static <T> T require(TypedKey<T> key) {
    T value = get(key);
    validate(value, key);
    //noinspection ConstantConditions
    return value;
  }

  public static boolean isAWT() {
    return ourThreadAWT.get();
  }

  public static void clearIsAWT() {
    ourThreadAWT.remove();
  }

  @Nullable
  static ContextFrame getTopFrame() {
    return getThreadContext().getTopFrame();
  }

  private static void validate(Object value, Object request) {
    if (value == null) {
      throw new IllegalStateException(String.valueOf(request));
    }
  }


  private static ThreadContext getThreadContext() {
    // todo single context data for AWT thread(s)
    Thread current = Thread.currentThread();
    ThreadContext threadContext = ourCachedThreadContext;
    if (threadContext != null && threadContext.getThread() == current) {
      return threadContext;
    }
    if (current instanceof GoodThread) {
      threadContext = ((GoodThread) current).getContextData();
    } else {
      threadContext = ourThreadContext.get();
    }
    ourCachedThreadContext = threadContext;
    return threadContext;
  }


  public static void add(ContextDataProvider provider, @Nullable String name) {
    getThreadContext().push(provider, true, name);
  }

  public static void replace(ContextDataProvider provider, @Nullable String name) {
    getThreadContext().push(provider, false, name);
  }

  public static void pop() {
    getThreadContext().pop();
  }

  public static void clear() {
    replace(ContextDataProvider.EMPTY, "clear");
  }

  public static int savepoint() {
    ContextFrame frame = getThreadContext().getTopFrame();
    int id = frame == null ? -1 : frame.getId();
    return id;
  }

  public static void restoreSavepoint(int savepoint) {
    getThreadContext().restoreTo(savepoint);
  }

  public static int globalize() {
    ContextFrame topFrame = getTopFrame();
    if (topFrame != null) {
      ourGlobals.add(0, topFrame);
      return topFrame.getId();
    } else {
      return -1;
    }
  }

  public static void unglobalize(int id) {
    if (id >= 0) {
      for (ContextFrame global : ourGlobals) {
        if (global.getId() == id) {
          ourGlobals.remove(global);
          return;
        }
      }
    }
  }
}
