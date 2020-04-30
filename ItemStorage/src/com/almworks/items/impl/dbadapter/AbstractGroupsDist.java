package com.almworks.items.impl.dbadapter;

import com.almworks.integers.IntIterator;
import com.almworks.integers.IntList;
import com.almworks.integers.IntSameValuesList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.NoSuchElementException;

import static org.almworks.util.Collections15.arrayList;

/**
 * Describes distribution of items between groups for multiple grouping.
 * One may think of it as a 2D-array where <i>a</i>[<i>i</i>][<i>j</i>] is group ID in the grouping j for the item with index i in the items list.
 * @author igor baltiyskiy
 */
// optimized implementation is poswsible that holds a single list of numbers per each item index. This number is item's group coordinates written in the K-base system, where K = max{groupings, max{groupIDs}}
// although it slows down getGrouping(i), which is necessary for inserting items (on update). See FindInsertIndex.
public abstract class AbstractGroupsDist {
  /**
   * Returns distribution of items between groups in the specified grouping.
   * @param i grouping number
   * @return list of group IDs of items in the specified grouping
   */
  @NotNull
  public IntList getGrouping(int i) {
    return getBackingList().get(i);
  }

  @NotNull
  public List<IntList> groupingsList() {
    return arrayList(getBackingList());
  }

  /**
   * Returns group ID in the specified grouping for the specified item index.
   * @param grouping grouping number in which the group ID is asked
   * @param idx index of the item in the items list for which to return the group ID
   * @return
   */
  public int getGroup(int idx, int grouping) {
    return getGrouping(grouping).get(idx);
  }

  /**
   * Number of groupings.
   */
  public int groupingsCount() {
    return getBackingList().size();
  }

  /**
   * Iterator starts from the specified index. Each iteration returns index of the item which is the first in the closest starting group.
   * @param start starting index
   * @return iterator over item indices that jumps to the next starting group.
   */
  @NotNull
  public Iterator iterator(int start) {
    return new ReadonlyGroupIterator(start, this);
  }

  /**
   * Returns iterator that runs from the beginning. The method is equivalent of calling iterator(0).
   * For the iterator semantics, see {@link #iterator(int)}.
   * @return iterator over item indices that jumps to the next starting groups and runs from the beginning.
   */
  @NotNull
  public Iterator iterator() {
    return iterator(0);    
  }

  /**
   * Returns the list of groupings, each of which is {@link IntList} containing groupIDs for items in this grouping.
   * @return
   */
  @NotNull
  protected abstract List<? extends IntList> getBackingList();

  /**
   * If the group distribution is decorated, applies the decorated changes. Otherwise, returns {@code this}.
   * @return equivalent non-decorated groups distribution.
   */
  @NotNull
  public abstract AbstractGroupsDist undecorate();

  /**
   * Decorator inheritors may use this default implementation of {@link #undecorate}.
   * @return
   */
  protected static AbstractGroupsDist defaultUndecorate(@NotNull AbstractGroupsDist groups) {
    assert groups != null;
    List<IntList> undecoratedGroups = arrayList(groups.groupingsCount());
    for (int i = 0; i < groups.groupingsCount(); ++i) {
      IntSameValuesList grouping = new IntSameValuesList();
      grouping.addAll(groups.getGrouping(i));
      undecoratedGroups.add(grouping);
    }
    return new GroupsDist(undecoratedGroups);
  }

  public interface Iterator extends IntIterator {
    /**
     * Returns the index of the grouping in which a new group started.
     * @see #nextValue()
     * @return
     */
    int startedGroupIndex();

      /**
     * Returns the index of the first item of the new started group.
     * @return
     * @throws java.util.NoSuchElementException if we've traversed to the poisition where no more new groups start
     */
    int nextValue() throws NoSuchElementException;
  }

}
