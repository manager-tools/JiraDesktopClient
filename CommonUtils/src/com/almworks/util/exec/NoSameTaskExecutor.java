package com.almworks.util.exec;

import java.util.List;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: dyoma
 * Date: Mar 17, 2010
 * Time: 6:26:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoSameTaskExecutor extends AbstractExecutorService {
  private static final Object LOCK = new Object();
  private final ExecutorService myExecutorService;
  private final ConcurrentHashMap<Runnable, Object> mySubmittedTasks = new ConcurrentHashMap<Runnable, Object>();

  public NoSameTaskExecutor(ExecutorService executorService) {
    myExecutorService = executorService;
  }

  public static ExecutorService newFixedThreadPool(int nThreads, String name) {
    return new NoSameTaskExecutor(ThreadFactory.newFixedThreadPool(nThreads, name));
  }

  public static ExecutorService newCachedThreadPool(String name) {
    return new NoSameTaskExecutor(ThreadFactory.newCachedThreadPool(name));
  }

  public void shutdown() {
    myExecutorService.shutdown();
  }

  public List<Runnable> shutdownNow() {
    return myExecutorService.shutdownNow();
  }

  public boolean isShutdown() {
    return myExecutorService.isShutdown();
  }

  public boolean isTerminated() {
    return myExecutorService.isTerminated();
  }

  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return myExecutorService.awaitTermination(timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    myExecutorService.execute(command);
  }

  @Override
  public Future<?> submit(final Runnable task) {
    if (task == null) throw new NullPointerException();
    Object prev =  LOCK;
    while (prev == LOCK) {
      prev = mySubmittedTasks.putIfAbsent(task, LOCK);
      if (prev instanceof Future) return (Future<?>) prev;
    }
    try {
      Future<?> future = myExecutorService.submit(new Runnable() {
        @Override
        public void run() {
          try {
            task.run();
          } finally {
            mySubmittedTasks.remove(task);
          }
        }
      });
      if (future != null) mySubmittedTasks.replace(task, LOCK, future);
      return future;
    } finally {
      mySubmittedTasks.remove(task, LOCK);
    }
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    throw new UnsupportedOperationException();
  }
}
