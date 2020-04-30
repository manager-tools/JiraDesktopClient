package com.almworks.items.impl.sqlite;

import com.almworks.util.threads.ThreadSafe;

public interface DatabaseExecutor {
  @ThreadSafe
  void execute(DatabaseJob job);
}
