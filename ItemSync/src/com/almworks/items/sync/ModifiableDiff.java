package com.almworks.items.sync;

import com.almworks.items.api.DBAttribute;

import java.util.Collection;
import java.util.List;

public interface ModifiableDiff extends ItemDiff {

  void addChange(DBAttribute<?>... attributes);

  void addChange(Collection<? extends DBAttribute<?>> attributes);

  /**
   * @return copy of current history steps. The returned list is a copy so client code may iterate it and modify history during iteration.
   */
  List<HistoryRecord> getHistory();

  void removeHistoryRecord(int index);
}
