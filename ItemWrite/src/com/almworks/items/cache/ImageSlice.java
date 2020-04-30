package com.almworks.items.cache;

import com.almworks.integers.IntList;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Generic events:<br>
 * <ul>
 * <li>When new data or change is loaded (because of DB change or new data loader added)</li>
 * <li>When new ICN is passed by DB (even no slice data is affected). This event may skip ICNs, but last DB ICN is notified
 * in some time after last transaction successfully completes</li>
 * <li>All removed event on end of life</li>
 * </ul>
 * Specific slices sends additional events on special cases
 */
// todo document methods
public interface ImageSlice {
  LongList getActualItems();

  <T> T getValue(long item, DataLoader<T> data);

  <T> T getNNValue(long item, DataLoader<T> loader, T defaultValue);

  boolean hasAllValues(long item);

  boolean hasValue(long item, DataLoader<?> data);

  void addData(Collection<? extends DataLoader<?>> loaders);

  void addAttributes(Collection<? extends DBAttribute<?>> attributes);

  void addAttributes(DBAttribute<?>... attributes);

  void addData(DataLoader<?> ... loaders);

  void removeData(Collection<? extends DataLoader<?>> loaders);

  void removeData(DataLoader<?> ... loaders);

  void addListener(Lifespan life, Listener listener);

  int getActualCount();

  long getItem(int index);

  DataLoader<?>[] getActualData();

  DBImage getImage();

  <T> T getValue(long item, DBAttribute<T> data);

  /**
   * Search item by loaded value.<br>
   * Usage:<br>
   * <pre>
   * int index = 0;
   * while ((index = slice.findIndexByValue(index, LOADER, VALUE)) >= 0) {
   *   // do something with item at index
   *   index++;
   * }
   * </pre>
   *
   * @param fromIndex start search from this index, may return this value
   * @return index of found item or -1 if searched up to the end
   */
  <T> int findIndexByValue(int fromIndex, DataLoader<? extends T> loader, T value);

  /**
   * Search for item index by both values.
   * @return index of an item with both values equal to given ones, or -1 if no found up to end
   * @see #findIndexByValue(int, DataLoader, Object)
   */
  <S, T> int findIndexByValue(int fromIndex, DataLoader<S> loader1, S value1, DataLoader<T> loader2, T value2);

  /**
   * Search for item by value.
   * @return the item, or 0 if not found
   * @see #findIndexByValue(int, DataLoader, Object)
   */
  <T> long findItemByValue(DataLoader<T> loader, T value);

  /**
   * Selects all items indexes with value for loader equals to given value.
   * @return all indexes or empty list
   * @see #findIndexByValue(int, DataLoader, Object)
   */
  @NotNull
  <T> IntList selectIndexesByValue(DataLoader<T> loader, T value);

  LoadersSet createAttributeSet(Lifespan life);

  void ensureStarted(Lifespan life);

  interface Listener {
    void onChange(ImageSliceEvent event);
  }
}
