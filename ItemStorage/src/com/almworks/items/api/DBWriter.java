package com.almworks.items.api;

import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.jetbrains.annotations.Nullable;


public interface DBWriter extends DBReader {
  <T> void setValue(long item, DBAttribute<T> attribute, @Nullable T value);

  /**
   * @return new item ID
   */
  long nextItem();

  /**
   * Removes all values from the item and all snapshots and removes it
   * from all indices, which is equal to his deletion
   * @param item
   */
  void clearItem(long item);

  long materialize(DBIdentifiedObject object);

  void clearAttribute(DBAttribute<?> attribute);

  void finallyDo(ThreadGate gate, Procedure<Boolean> procedure);
}
