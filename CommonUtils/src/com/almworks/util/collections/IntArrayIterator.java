package com.almworks.util.collections;

public class IntArrayIterator {
  private final int[] myInts;
  private final int myHiLimit;
  private final int myHiOutOfRange;
  private int myPos;
  private final int myLowLimit;
  private final int myLowOutOfRange;

  public IntArrayIterator(int[] ints, int lowLimit, int hiLimit, int lowOutOfRange, int hiOutOfRange) {
    assert hiLimit >= lowLimit;
    assert lowLimit >= 0;
    assert hiLimit <= ints.length;
    myInts = ints;
    myPos = lowLimit;
    myHiLimit = hiLimit;
    myLowLimit = lowLimit;
    myHiOutOfRange = hiOutOfRange;
    myLowOutOfRange = lowOutOfRange;
  }

  public static IntArrayIterator extremValue(int[] ints) {
    return new IntArrayIterator(ints, 0, ints.length, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  public int prev() {
    myPos--;
    return get();
  }

  public int next() {
    myPos++;
    return get();
  }

  public int forward() {
    int result = get();
    myPos++;
    return result;
  }

  public int backward() {
    int result = get();
    myPos--;
    return result;
  }

  public void setPos(int pos) {
    myPos = pos;
  }

  public void toStart() {
    myPos = myLowLimit;
  }

  public void toEnd() {
    myPos = myHiLimit - 1;
  }

  public int get() {
    if (beforeStart())
      return myLowOutOfRange;
    return afterEnd() ? myHiOutOfRange : myInts[myPos];
  }

  public boolean afterEnd() {
    return myPos >= myHiLimit;
  }

  public boolean beforeStart() {
    return myPos < myLowLimit;
  }

  public int forwardWhileDiff(int diff) {
    int start = get();
    int count = 0;
    while (start + diff * (count + 1) == next())
      count++;
    return count;
  }
}
