package com.almworks.items.impl.sqlite;

import com.almworks.integers.IntList;
import com.almworks.integers.LongList;
import com.almworks.items.impl.dbadapter.AbstractGroupsDist;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.jetbrains.annotations.Nullable;

// todo N-ary search and insert instead binary


class FindInsertIndex {
  private final SQLiteStatement myOrder;
  private final int myParamCount;
  private int myLow;
  private int myHigh;

  public FindInsertIndex(SQLiteStatement order, int paramCount) {
    myOrder = order;
    myParamCount = paramCount;
  }

  /**
   * Finds insertion index for the value in the list of items using the ordering SQL statement specified in the constructor.
   * If grouping is available, it also takes into consideration groupIds of the inserted value and the group ranges in the target items list.
   *
   * @param all      item list where to find the place to insert the value
   * @param value    value to insert
   * @param groupIds group IDs for the inserted value. must be not null if groups != null
   * @param groups   groupIds for items in all
   * @return index where to insert the specified value in the item list
   */
  public int findInsertIndex(LongList all, long value, @Nullable IntList groupIds, @Nullable AbstractGroupsDist groups)
    throws SQLiteException
  {
    assert myParamCount >= 3;
    if (all.size() < 1)
      return 0;
    myLow = 0;
    myHigh = all.size() - 1;
    if (groups != null) {
      assert groupIds != null;
      // narrow the insertion range to the value's full group
      for (int i = 0; i < groupIds.size(); ++i) {
        IntList grouping = groups.getGrouping(i).subList(myLow, myHigh + 1);
        int groupId = groupIds.get(i);
        int localLow = grouping.binarySearch(groupId);
        // if the group is currently empty, we've found the point of insertion
        if (localLow < 0) {
          return myLow + (-localLow - 1);
        }
        int localHigh = grouping.getNextDifferentValueIndex(localLow);
        // if there is no next different value, don't change myHigh; otherwise, myHigh is one position further after the last element of the currently chosen subrange.
        if (localHigh > 0) {
          myHigh = myLow + (localHigh - 1);
        }
        myLow += localLow;
      }
    }
    while (true) {
      for (int i = 0; i < myParamCount - 1; i++) {
        int pos = getPos(i);
        myOrder.bind(i + 1, all.get(pos));
      }
      myOrder.bind(myParamCount, value);
      int count = 0;
      while (myOrder.step() && (myOrder.columnInt(0) != value))
        count++;
      myOrder.reset();
      if (count == 0)
        return myLow;
      if ((count == myParamCount - 1) || (count == myHigh - myLow + 1))
        return myHigh + 1;
      int lowPos = getPos((count - 1));
      int highPos = getPos(count);
      assert highPos > lowPos : highPos + " " + lowPos;
      if (highPos - lowPos <= 1)
        return highPos;
      if (highPos - lowPos < 3) {
        assert highPos - lowPos == 2;
        myLow = lowPos + 1;
        myHigh = highPos;
      } else {
        myLow = lowPos + 1;
        myHigh = highPos - 1;
      }
    }
  }

  private int getPos(int i) {
    int interval = myHigh - myLow;
    if (interval < myParamCount - 1)
      return i < interval ? myLow + i : myHigh;
    return myLow + interval * i / (myParamCount - 2);
  }
}
