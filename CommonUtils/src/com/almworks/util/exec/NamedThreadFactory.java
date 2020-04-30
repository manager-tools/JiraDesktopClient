package com.almworks.util.exec;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory {
  private static final AtomicInteger ourCounter = new AtomicInteger(0);

  public static ThreadFactory create(String name) {
    SecurityManager s = System.getSecurityManager();
    String namePrefix = name + "-" + ourCounter.getAndIncrement();
    AtomicInteger poolCounter = new AtomicInteger(0);
    ThreadGroup group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    return r -> {
      Thread t = new Thread(group, r, namePrefix + "-thread-" + poolCounter.incrementAndGet(), 0);
      if (t.isDaemon())
        t.setDaemon(false);
      if (t.getPriority() != Thread.NORM_PRIORITY)
        t.setPriority(Thread.NORM_PRIORITY);
      return t;
    };
  }
}
