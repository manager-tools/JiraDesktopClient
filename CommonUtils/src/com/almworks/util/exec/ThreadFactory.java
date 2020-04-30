package com.almworks.util.exec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ThreadFactory {
  public static final ThreadFactory DEFAULT_FACTORY = new DefaultThreadFactory();
  private static int ourThreadCount;

  @NotNull
  public static Thread create(@Nullable ThreadGroup group, String name, Runnable runnable) {
    ThreadFactory factory = Context.get(ThreadFactory.class, DEFAULT_FACTORY);
    return factory.createThread(group, name, runnable);
  }

  @NotNull
  public static Thread create(String name, Runnable runnable) {
    return create(null, name, runnable);
  }

  public static Thread create(Runnable runnable) {
    return create(getNextThreadName(), runnable);
  }

  public static ExecutorService newSingleThreadExecutor(final String name) {
    return Executors.newSingleThreadExecutor(namedThreadFactory(name));
  }

  public static ExecutorService newFixedThreadPool(int nThreads, String name) {
    return Executors.newFixedThreadPool(nThreads, namedThreadFactory(name));
  }

  public static ExecutorService newCachedThreadPool(String name) {
    return Executors.newCachedThreadPool(namedThreadFactory(name));
  }

  private static java.util.concurrent.ThreadFactory namedThreadFactory(final String name) {
    return new java.util.concurrent.ThreadFactory() {
      private final AtomicInteger myCounter = new AtomicInteger(0);

      @Override
      public Thread newThread(Runnable r) {
        return create(null, name + "-" + myCounter.incrementAndGet(), r);
      }
    };
  }

  private static synchronized String getNextThreadName() {
    return "thread#" + (++ourThreadCount);
  }

  public abstract Thread createThread(ThreadGroup group, String name, Runnable runnable);

}
