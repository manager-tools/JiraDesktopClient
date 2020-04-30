package com.almworks.util.collections;

/**
 * Stores N last time "touched" ints. When next one is "touched" returns the int that isn't "touched" for most time
 * @author dyoma
 */
public class IntLastTouchedQueue {
  private final int[] myQueue;
  private int myCount = 0;

  public IntLastTouchedQueue(int size) {
    myQueue = new int[size];
  }

  public Integer touch(int n) {
    if (myCount > 0 && n == myQueue[0])
      return null;
    Integer result;
    if (myCount < myQueue.length) {
      result = null;
      System.arraycopy(myQueue, 0, myQueue, 1, myCount);
      myCount++;
    } else {
      assert myCount == myQueue.length;
      int index = findIndex(n);
      if (index < 0) {
        result = myQueue[myQueue.length - 1];
        System.arraycopy(myQueue, 0, myQueue, 1, myQueue.length - 1);
      } else {
        assert n == myQueue[index];
        result = null;
        System.arraycopy(myQueue, 0, myQueue, 1, index);
      }
    }
    myQueue[0] = n;
    return result;
  }

  public boolean contains(int n) {
    return findIndex(n) >= 0;
  }

  public int getCount() {
    return myCount;
  }

  private int findIndex(int n) {
    for (int i = 0; i < myCount; i++) {
      int val = myQueue[i];
      if (val == n)
        return i;
    }
    return -1;
  }

  public boolean remove(int n) {
    int index = findIndex(n);
    if (index < 0)
      return false;
    assert index < myCount;
    System.arraycopy(myQueue, index + 1, myQueue, index, myCount - 1 - index);
    myCount--;
    return true;
  }
}
