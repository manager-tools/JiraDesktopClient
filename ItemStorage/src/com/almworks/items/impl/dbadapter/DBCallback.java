package com.almworks.items.impl.dbadapter;

// DB-13
public interface DBCallback {
  void dbSuccess();

  void dbFailure(Throwable throwable);
}
