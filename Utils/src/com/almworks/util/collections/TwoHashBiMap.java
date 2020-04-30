package com.almworks.util.collections;

import org.almworks.util.Collections15;

import java.util.Map;

public class TwoHashBiMap<L, R> implements BiMap<L, R> {
  private final Map<L, R> myLeftMap = Collections15.hashMap();
  private final Map<R, L> myRightMap = Collections15.hashMap();

  public void put(L left, R right) {
    assert left != null;
    assert right != null;
    R oldRight = myLeftMap.put(left, right);
    if (oldRight != null && !oldRight.equals(right)) {
      L removedLeft = myRightMap.remove(oldRight);
      assert left.equals(removedLeft) : left + " " + removedLeft + " (" + oldRight + ")";
    }
    L oldLeft = myRightMap.put(right, left);
    if (oldLeft != null && !oldLeft.equals(left)) {
      R removedRight = myLeftMap.remove(oldLeft);
      assert right.equals(removedRight) : right + " " + removedRight + " (" + oldLeft + ")";
    }
  }

  public R getRight(L left) {
    return myLeftMap.get(left);
  }

  public L getLeft(R right) {
    return myRightMap.get(right);
  }
}
