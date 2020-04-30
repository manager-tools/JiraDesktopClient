package com.almworks.items.impl.dbadapter;

import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.detach.Lifespan;

public interface PrioritizedListenerSupport<L> {
  @ThreadSafe
  void addListener(Lifespan life, int priority, L listener);

  @ThreadSafe
  void setPriority(int priority, L listener);
}
