package com.almworks.util.threads;

import com.almworks.util.Env;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadFactory;
import com.almworks.util.ui.errors.AwtExceptionHandler;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

/**
 * @author : Dyoma
 */
public class MainThreadGroup extends ThreadGroup {
  private static MainThreadGroup INSTANCE = null;
  private final FireEventSupport<ExceptionListener> myListeners =
    FireEventSupport.createSynchronized(ExceptionListener.class);

  static {
    AwtExceptionHandler.staticInitialize();
  }

  private MainThreadGroup() {
    super("platform");
    setDaemon(false);
  }

  public Detach addListener(ExceptionListener listener) {
    DetachComposite life = new DetachComposite();
    myListeners.addStraightListener(life, listener);
    return life;
  }

  public void uncaughtException(Thread t, Throwable e) {
    if (e == null || t == null)
      return;
    if (e instanceof ThreadDeath)
      return;
    if (e instanceof RuntimeInterruptedException) {
      Log.warn("thread " + t + " interrupted", e);
      if (t == Thread.currentThread()) {
        // clear interrupted flag
        Thread.currentThread().interrupt();
      }
      return;
    }
    if (myListeners.getListenersCount() > 0) {
      myListeners.getDispatcherSnapshot().onException(t, e);
    }
    Log.error(t, e);
  }

  public void startThread(String name, Runnable runnable) {
    createThread(name, runnable).start();
  }

  public Thread createThread(String name, Runnable runnable) {
    Thread thread = ThreadFactory.create(this, name, runnable);
    ClassLoader loader = MainThreadGroup.class.getClassLoader();
    Log.debug("default class loader: " + loader);
    thread.setContextClassLoader(loader);
    return thread;
  }

  public static synchronized MainThreadGroup getOrCreate() {
    if (INSTANCE == null) {
      if (!Env.getBoolean("mtg.noverify", false)) {
        verify();
      }
      INSTANCE = new MainThreadGroup();
      Thread.setDefaultUncaughtExceptionHandler(INSTANCE); // Be the global exception handler (since Java5)
    }
    return INSTANCE;
  }

  private static void verify() {
    Thread thread = Thread.currentThread();
    ThreadGroup group = thread.getThreadGroup().getParent();
    if (group == null)
      group = thread.getThreadGroup();
    if (!"system".equals(group.getName()))
      Log.error(group.getName());
    if (group.getParent() != null) {
      String message =
        "Thread <" + thread.getName() + "> member of <" + group.getName() + "> failed to create MainThreadGroup";
      throw new RuntimeException(message);
    }
    ThreadGroup[] allGroups = new ThreadGroup[group.activeGroupCount()];
    int returned = group.enumerate(allGroups, false);
    assert returned <= allGroups.length;
    for (int i = 0; i < returned; i++) {
      ThreadGroup threadGroup = allGroups[i];
      if (threadGroup instanceof MainThreadGroup) {
        throw new RuntimeException("Already created");
      }
    }
  }

  public interface ExceptionListener {
    void onException(Thread thread, Throwable exception);
  }
}
