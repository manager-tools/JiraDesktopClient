package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.integers.WritableLongList;

/**
 * DBEvent contains aggregate information on changed items. DBEvent is received by a DBListener and is never null.
 */
public class DBEvent {
  public static final DBEvent EMPTY = new DBEvent(LongList.EMPTY, LongList.EMPTY, LongList.EMPTY, LongList.EMPTY);

  private final LongList myChangedSorted;
  private final LongList myAddedSorted;
  private final LongList myRemovedSorted;
  private final LongList myAffectedSorted;

  private LongList myAddedAndChangedSorted;
  private LongList myRemovedAndChangedSorted;

  private DBEvent(LongList changedSorted, LongList addedSorted, LongList removedSorted, LongList affectedSorted) {
    assert checkInvariants(changedSorted, addedSorted, removedSorted, affectedSorted);
    myChangedSorted = changedSorted;
    myAddedSorted = addedSorted;
    myRemovedSorted = removedSorted;
    myAffectedSorted = affectedSorted;
  }

  private boolean checkInvariants(LongList changed, LongList added, LongList removed, LongList affected) {
    assert changed.isSortedUnique() : changed;
    assert added.isSortedUnique() : added;
    assert removed.isSortedUnique() : removed;
    assert affected.isSortedUnique() : affected;
    checkIncludes(changed, added, true);
    checkIncludes(changed, removed, true);
    checkIncludes(added, changed, true);
    checkIncludes(added, removed, true);
    checkIncludes(removed, changed, true);
    checkIncludes(removed, added, true);
    checkIncludes(changed, affected, false);
    checkIncludes(added, affected, false);
    checkIncludes(removed, affected, false);
    LongSetBuilder builder = new LongSetBuilder();
    builder.addAll(changed);
    builder.addAll(added);
    builder.addAll(removed);
    checkIncludes(builder.commitToArray(), affected, false);
    return true;
  }

  private void checkIncludes(LongList list, LongList otherList, boolean invert) {
    for (int i = 0; i < list.size(); i++) {
      long value = list.get(i);
      if (!otherList.contains(value) ^ invert) {
        if (invert) {
          assert false : "value " + value + " at A[" + i + "] is also in B[" + otherList.indexOf(value) + "]";
        } else {
          assert false : "value " + value + " at A[" + i + "] is not in B";
        }
        return;
      }
    }
  }

  public boolean isEmpty() {
    return myAffectedSorted.isEmpty();
  }

  public LongList getChangedSorted() {
    return myChangedSorted;
  }

  public LongList getAddedSorted() {
    return myAddedSorted;
  }

  public LongList getRemovedSorted() {
    return myRemovedSorted;
  }

  public LongList getAffectedSorted() {
    return myAffectedSorted;
  }

  public static DBEvent createAndUpdateCurrent(WritableLongList currentSorted, LongList affectedSorted,
    LongList acceptedSorted)
  {
    return create0(currentSorted, affectedSorted, acceptedSorted, true);
  }

  public static DBEvent create(LongList currentSorted, LongList affectedSorted, LongList acceptedSorted) {
    return create0(currentSorted, affectedSorted, acceptedSorted, false);
  }

  public static DBEvent create(LongList changedSorted) {
    return new DBEvent(changedSorted, LongList.EMPTY, LongList.EMPTY, changedSorted);
  }

  private static DBEvent create0(LongList currentSorted, LongList affectedSorted, LongList acceptedSorted,
    boolean updateCurrent)
  {
    WritableLongList writableCurrent = updateCurrent ? (WritableLongList) currentSorted : null;
    int idxCurrent = 0, idxAccepted = 0;
    LongArray changed = null, removed = null, added = null;
    for (int i = 0; i < affectedSorted.size(); i++) {
      long item = affectedSorted.get(i);
      int foundCurrent = currentSorted.binarySearch(item, idxCurrent, currentSorted.size());
      int foundAccepted = acceptedSorted.binarySearch(item, idxAccepted, acceptedSorted.size());
      boolean wasAccepted = foundCurrent >= 0;
      boolean isAccepted = foundAccepted >= 0;
      idxCurrent = wasAccepted ? foundCurrent : -foundCurrent - 1;
      idxAccepted = isAccepted ? foundAccepted : -foundAccepted - 1;
      if (wasAccepted) {
        if (isAccepted) {
          if (changed == null)
            changed = new LongArray();
          changed.add(item);
        } else {
          if (removed == null)
            removed = new LongArray();
          removed.add(item);
          if (updateCurrent) {
            writableCurrent.removeAt(foundCurrent);
          }
        }
      } else if (isAccepted) {
        if (added == null)
          added = new LongArray();
        added.add(item);
        if (updateCurrent) {
          writableCurrent.insert(idxCurrent, item);
          idxCurrent++;
        }
      }
    }
    if (changed == null && removed == null && added == null)
      return EMPTY;
    return new DBEvent(NN(changed), NN(added), NN(removed), buildAffected(changed, removed, added));
  }

  private static LongList buildAffected(LongList v1, LongList v2, LongList v3) {
    int n = 0;
    if (v1 == null)
      n++;
    if (v2 == null)
      n++;
    if (v3 == null)
      n++;
    assert n < 3 : n;
    if (n == 2) {
      if (v1 != null)
        return v1;
      if (v2 != null)
        return v2;
      assert v3 != null;
      return v3;
    }
    LongSetBuilder builder = new LongSetBuilder();
    if (v1 != null)
      builder.mergeFromSortedCollection(v1);
    if (v2 != null)
      builder.mergeFromSortedCollection(v2);
    if (v3 != null)
      builder.mergeFromSortedCollection(v3);
    return builder.commitToArray();
  }

  private static LongList NN(LongList list) {
    return list == null ? LongList.EMPTY : list;
  }

  public LongList getAddedAndChangedSorted() {
    if (myChangedSorted.isEmpty())
      return myAddedSorted;
    if (myAddedSorted.isEmpty())
      return myChangedSorted;
    LongList r = myAddedAndChangedSorted;
    if (r == null) {
      LongSetBuilder builder = new LongSetBuilder();
      builder.mergeFromSortedCollection(myChangedSorted);
      builder.mergeFromSortedCollection(myAddedSorted);
      myAddedAndChangedSorted = r = builder.commitToArray();
    }
    return r;
  }

  public LongList getRemovedAndChangedSorted() {
    if (myChangedSorted.isEmpty())
      return myRemovedSorted;
    if (myRemovedSorted.isEmpty())
      return myChangedSorted;
    LongList r = myRemovedAndChangedSorted;
    if (r == null) {
      LongSetBuilder builder = new LongSetBuilder();
      builder.mergeFromSortedCollection(myChangedSorted);
      builder.mergeFromSortedCollection(myRemovedSorted);
      myRemovedAndChangedSorted = r = builder.commitToArray();
    }
    return r;
  }

  @Override
  public String toString() {
    return
      "A:" + myAddedSorted + "\n" +
      "C:" + myChangedSorted + "\n" +
      "R:" + myRemovedSorted;
  }
}
