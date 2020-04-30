package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.util.tests.BaseTestCase;

public class DBEventTests extends BaseTestCase {
  public void testRegression1(){
    LongList current = LongArray.create(5, 6, 7);
    LongList affected = LongArray.create(1, 2, 3, 4, 5, 6, 7);
    LongList accepted = LongArray.create(2, 5, 6, 7);
    DBEvent event = DBEvent.create(current, affected, accepted);
    check(event.getRemovedSorted());
    check(event.getChangedSorted(), 5, 6, 7);
    check(event.getAddedSorted(), 2);
  }

  public void testRegression2() {
    LongArray current = LongArray.create(1);
    DBEvent event = DBEvent.createAndUpdateCurrent(current, LongArray.create(1), LongList.EMPTY);
    assertEquals(LongArray.create(1), event.getRemovedSorted());
    assertEquals(LongList.EMPTY, current);
  }

  private void check(LongList list, long... values) {
    assertEquals(values.length, list.size());
    for (int i = 0; i < values.length; i++) {
      assertEquals("[" + i + "]", values[i], list.get(i));
    }
  }
}
