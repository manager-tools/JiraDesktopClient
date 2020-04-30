package com.almworks.items.impl.dbadapter;

import com.almworks.integers.IntList;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.util.threads.AWTRequired;
import org.jetbrains.annotations.Nullable;

/**
 * Item view provides a list of item ids, according to some order and configuration. The configuration includes:
 * <ul>
 * <li>Multiple sorting (with implied sorting by item id at the end);</li>
 * <li>Multiple grouping;</li>
 * <li>Sparse hierarchy from many-to-many relationship;</li>
 * <li>Dense hierarchy from many-to-one relationship.</li>
 * </ul>
 */
public interface ItemView extends PrioritizedListenerSupport<ItemView.Listener> {
  /**
   * Important: until someone listens to the view, it may return empty items.
   * todo add view state, loaded/unloaded
   */
  @AWTRequired
  LongList getItems();

  /**
   * Returns depths of the items according to built tree structure, or null if structure is flat.
   *
   * NB: this interface is experimental; we can easily change getDepths() into getParentIndexes() if that will be
   * more convenient
   * */
  @AWTRequired
  @Nullable
  IntList getDepths();

  /**
   * Returns the distribution of items between groups for all specified groupings, i.e. groupIDs for all items in the view.
   * If the item does not belong to any group, it's group ID is 0. 
   */
  @AWTRequired
  @Nullable
  AbstractGroupsDist getGroups();

  /**
   * @return true if getItems() returns meaningful data (according to some ICN, not necessary the last)
   */
  boolean isLoaded();

  @AWTRequired
  interface Event {
    @Deprecated
    void getRange(int firstIndex, ItemAcceptor acceptor);

    /**
     * @return new index of an item which was at the oldIndex in the old items list, or -1 if item is missing
     */
    int getNewIndex(int oldIndex);

    int getCountDelta();

    // todo remove?
    @Deprecated
    LongIterator getRemovedItems();
  }

  enum ItemState {
    NOT_CHANGED,
    UPDATED,
    INSERTED
  }

  interface ItemAcceptor {
    boolean accept(int index, long item, ItemState state);
  }


  interface Listener {
    void onUpdate(Event event);
  }
}
