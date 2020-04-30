package com.almworks.items.sync;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Represents conflict state of an item and allows to resolve the conflict.<br>
 * Item is in <strong>conflict</strong> iff at least one attribute has different local/server value, marked as conflict
 * during {@link ItemAutoMerge#preProcess(ModifiableDiff)}.<br>
 * To resolve the conflict for specific attribute auto merge code should provide <strong>resolution value</strong>.<br>
 * The resolution is applied if only all attribute conflicts are resolved. Otherwise item is not changed. 
 */
public interface AutoMergeData {
  /**
   * @return true iff there is no conflict: local and server changes don't intersect or there is resolution for all
   * attributes from intersection.
   */
  boolean isConflictResolved();

  long getItem();

  /**
   * @return Attributes which are changed locally and on server and has no resolution yet
   */
  Collection<DBAttribute<?>> getUnresolved();

  /**
   * @return local changes
   */
  ItemDiff getLocal();

  /**
   * @return server changes
   */
  ItemDiff getServer();

  /**
   * Resolves conflict for an attribute with specified resolution value.
   */
  <T> void setResolution(DBAttribute<T> attribute, @Nullable T value);

  /**
   * Resolves conflict with last server value. Sets remote value as resolution.<br>
   * If all local changes are replaced with remote values then local changes are discarded.
   * This method provides conflict resolution too.
   * @see #resolveToLocal(com.almworks.items.api.DBAttribute[])
   */
  void discardEdit(DBAttribute<?>... attributes);

  /**
   * Resolves conflict with last local value. Sets local value as resolution.<br>
   * This method works the same way as {@link #discardEdit(com.almworks.items.api.DBAttribute[])} but takes last local
   * value as resolution
   * @see #discardEdit(com.almworks.items.api.DBAttribute[])
   */
  void resolveToLocal(DBAttribute<?>... attributes);

  /**
   * Utility to set resolution for list or set composition attribute.
   * @param attribute long list or set attribute
   * @param resolution resolution value. If attribute composition is list the order of value is preserved.
   */
  void setCompositeResolution(DBAttribute<? extends Collection<? extends Long>> attribute, LongList resolution);

  void setCompositeResolution(DBAttribute<? extends Collection<? extends String>> attribute, Collection<String> resolution);

  DBReader getReader();

  <T> boolean isNewEqual(DBAttribute<T> attribute);

  List<HistoryRecord> getHistory();

  void removeHistoryRecord(int recordId);

  boolean hasHistory();
}
