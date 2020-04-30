package com.almworks.util.collections;

public class QuickLargeList<T> {
  private final int myDimension;
  private final int myDimensionSize;
  private final int myMaxSize;
  private final int myLowMask;
  private final Object[][] myData;
  private final boolean mySafe;

  private volatile int mySize;

  public QuickLargeList(int dimensionBits) {
    this(dimensionBits, false);
  }

  public QuickLargeList(int dimensionBits, boolean safe) {
    assert dimensionBits >= 5 && dimensionBits < 16;
    myDimension = dimensionBits;
    myDimensionSize = 1 << dimensionBits;
    myMaxSize = 1 << (dimensionBits * 2);
    myLowMask = myDimensionSize - 1;
    myData = new Object[myDimensionSize][];
    mySafe = safe;
  }

  /**
   * returns index of added value
   */
  public int add(T value) {
    int i = mySize;
    if (mySize >= myMaxSize)
      throw new IllegalStateException();
    int p = i >> myDimension;
    Object[] subdata = myData[p];
    if (subdata == null) {
      myData[p] = subdata = new Object[myDimensionSize];
    }
    subdata[i & myLowMask] = value;
    mySize = i + 1;
    return i;
  }

  public int size() {
    return mySize;
  }

  public T get(int index) {
    if (index < 0)
      throw new ArrayIndexOutOfBoundsException(index);
    if (index >= mySize) {
      if (mySafe) {
        return null;
      } else {
        throw new ArrayIndexOutOfBoundsException(index);
      }
    }
    Object[] subdata = myData[index >> myDimension];
    if (subdata == null) {
      assert mySafe : index;
      return null;
    } else {
      return (T) subdata[index & myLowMask];
    }
  }

  public T set(int index, T value) {
    if (index < 0)
      throw new ArrayIndexOutOfBoundsException(index);
    if (index >= mySize) {
      if (!mySafe) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
    }
    int p = index >> myDimension;
    Object[] subdata = myData[p];
    if (subdata == null) {
      myData[p] = subdata = new Object[myDimensionSize];
    }
    int q = index & myLowMask;
    Object oldValue = subdata[q];
    subdata[q] = value;
    if (index >= mySize)
      mySize = index + 1;
    return (T) oldValue;
  }
}
