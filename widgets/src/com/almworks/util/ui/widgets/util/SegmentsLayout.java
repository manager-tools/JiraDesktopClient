package com.almworks.util.ui.widgets.util;

import com.almworks.util.LogHelper;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.ArrayUtil;
import util.external.BitSet2;

import java.util.Arrays;

public class SegmentsLayout {
  private static final Log<SegmentsLayout> log = Log.get(SegmentsLayout.class);
  private int[] myGrow = new int[]{1};
  private int[] myShrink = new int[]{1};
  private int[] myMinSize = new int[] {0};
  private int myDefaultGrow = 0;
  private int myDefaultShrink = 0;
  private int myDefaultMinSize = 0;
  private int mySize = 1;

  public SegmentsLayout(int defaultGrow, int defaultShrink) {
    myDefaultGrow = defaultGrow;
    myDefaultShrink = defaultShrink;
    myGrow[0] = myDefaultGrow;
    myShrink[0] = myDefaultShrink;
  }

  public void reset(int defaultGrow, int defaultShrink) {
    reset(defaultGrow, defaultShrink, 0);
  }

  public void reset(int defaultGrow, int defaultShrink, int minSize) {
    myDefaultGrow = defaultGrow;
    myDefaultShrink = defaultShrink;
    myDefaultMinSize = minSize;
    reset();
  }

  public void reset() {
    Arrays.fill(myGrow, myDefaultGrow);
    Arrays.fill(myShrink, myDefaultShrink);
    Arrays.fill(myMinSize, myDefaultMinSize);
  }

  public void setSegmentPolicy(int index, int grow, int shrink) {
    setSegmentPolicy(index, grow, shrink, -1);
  }

  public void setSegmentPolicy(int index, int grow, int shrink, int minSize) {
    if (index < 0) {
      LogHelper.error(index);
      return;
    }
    if (grow < 0) grow = Math.max(0, getWeight(index, myGrow, myDefaultGrow));
    if (shrink < 0) shrink = Math.max(0, getWeight(index, myShrink, myDefaultShrink));
    if (minSize < 0) minSize = Math.max(0, getMinSize(index));
    myGrow = ensureCapasity(myGrow, index + 1, myDefaultGrow);
    myShrink = ensureCapasity(myShrink, index + 1, myDefaultShrink);
    myMinSize = ensureCapasity(myMinSize, index + 1, myDefaultMinSize);
    mySize = Math.max(mySize, index + 1);
    myGrow[index] = grow;
    myShrink[index] = shrink;
    myMinSize[index] = minSize;
  }

  public void addSegmentPolicy(int grow, int shrink) {
    setSegmentPolicy(mySize, grow, shrink);
  }

  public void addSegmentPolicy(int grow, int shrink, int minSize) {
    setSegmentPolicy(mySize, grow, shrink, minSize);
  }

  public void removeSegment(int index) {
    if (index >= mySize) return;
    if (index < mySize - 1) {
      System.arraycopy(myGrow, index + 1, myGrow, index, myGrow.length - index - 1);
      System.arraycopy(myShrink, index + 1, myShrink, index, myShrink.length - index - 1);
      System.arraycopy(myMinSize, index + 1, myMinSize, index, myMinSize.length - index - 1);
    }
    mySize--;
    myGrow[mySize] = myDefaultGrow;
    myShrink[mySize] = myDefaultShrink;
    myMinSize[mySize] = myDefaultMinSize;
  }

  public void setSegmentCount(int size) {
    if (size == mySize) return;
    if (size > mySize) {
      myGrow = ensureCapasity(myGrow, size, myDefaultGrow);
      myShrink = ensureCapasity(myShrink, size, myDefaultShrink);
      myMinSize = ensureCapasity(myMinSize, size, myDefaultMinSize);
    } else {
      Arrays.fill(myGrow, size, myGrow.length, myDefaultGrow);
      Arrays.fill(myShrink, size, myShrink.length, myDefaultShrink);
      Arrays.fill(myMinSize, size, myMinSize.length, myDefaultMinSize);
    }
    mySize = size;
  }

  public int getSegmentCount() {
    return mySize;
  }

  public void setSegments(SegmentsLayout segments) {
    myGrow = ArrayUtil.ensureCapacity(myGrow, segments.myGrow.length);
    myShrink = ArrayUtil.ensureCapacity(myShrink, segments.myShrink.length);
    myMinSize = ArrayUtil.ensureCapacity(myMinSize, segments.myMinSize.length);
    Arrays.fill(myGrow, segments.myDefaultGrow);
    Arrays.fill(myShrink, segments.myDefaultShrink);
    Arrays.fill(myMinSize, segments.myDefaultMinSize);
    System.arraycopy(segments.myGrow, 0, myGrow, 0, segments.myGrow.length);
    System.arraycopy(segments.myShrink, 0, myShrink, 0, segments.myShrink.length);
    System.arraycopy(segments.myMinSize, 0, myMinSize, 0, segments.myMinSize.length);
    myDefaultGrow = segments.myDefaultGrow;
    myDefaultShrink = segments.myDefaultShrink;
    myDefaultMinSize = segments.myDefaultMinSize;
    mySize = segments.mySize;
  }

  private static int[] ensureCapasity(int[] array, int size, int defaultValue) {
    int oldLength = array.length;
    array = ArrayUtil.ensureCapacity(array, size);
    if (array.length != oldLength)
      Arrays.fill(array, oldLength, array.length, defaultValue);
    return array;
  }

  public void layout(int expectedSum, int[] values, int length, int gap) {
    if (length <= 0) return;
    for (int i = 0; i < values.length; i++) if (values[i] < 0) values[i] = 0;
    LayoutMethod layout = new LayoutMethod(values, length, expectedSum);
    layout.perform();
    System.arraycopy(layout.myTarget, 0, values, 0, layout.myTarget.length);
  }

  private int getWeight(int index, int[] weights, int defaultWeight) {
    return Math.max(0, weights.length > index ? weights[index] : defaultWeight);
  }

  public int getMinSize(int index) {
    return Math.max(0, myMinSize.length > index ? myMinSize[index] : myDefaultMinSize);
  }

  private class LayoutMethod {
    private final int[] myInitial;
    private final int[] myTarget;
    private final int myExpectedSum;
    private final BitSet2 myUpdatables;
    private boolean myNotUpdatableFound = false;
    private boolean myHasChanges = false;

    public LayoutMethod(int[] initial, int length, int expectedSum) {
      myInitial = initial;
      myTarget = new int[length];
      myExpectedSum = expectedSum;
      myUpdatables = new BitSet2(length);
      myUpdatables.set(0, length, true);
      System.arraycopy(myInitial, 0, myTarget, 0, length);
    }

    public void perform() {
      while (true) {
        myHasChanges = false;
        myNotUpdatableFound = false;
        doStep();
        if (!myHasChanges && !myNotUpdatableFound) break;
      }
    }

    public void doStep() {
      int initialSum = 0;
      for (int i = 0; i < myTarget.length; i++) {
        int value = isUpdatable(i) ? myInitial[i] : myTarget[i];
        initialSum += value;
      }
      int excess = myExpectedSum - initialSum;
      int[] weights = excess > 0 ? myGrow : myShrink;
      int defaultWeight = excess > 0 ? myDefaultGrow : myDefaultShrink;
      int weightTotal = 0;
      int lastWeighing = -1;
      for (int i = 0; i < myTarget.length; i++)
        if (isUpdatable(i)) {
          int weight = getWeight(i, weights, defaultWeight);
          if (weight <= 0) myUpdatables.set(i, false);
          else {
            weightTotal += weight;
            lastWeighing = i;
          }
        }
      if (weightTotal > 0) {
        int left = excess;
        for (int i = 0; i <= lastWeighing; i++) {
          if (!isUpdatable(i)) continue;
          int w = getWeight(i, weights, defaultWeight);
          if (w <= 0) {
            log.error(SegmentsLayout.this, "Positive weight expected");
            myHasChanges = false;
            myNotUpdatableFound = false;
            continue;
          }
          int prevValue = myInitial[i];
          int add = i == lastWeighing ? left : excess * w / weightTotal;
          setTargetValue(i, prevValue + add);
          left -= add;
        }
        if (left != 0) log.error(SegmentsLayout.this, "Not zero left " + left);
      }
    }

    private void setTargetValue(int i, int newValue) {
      int minSize = getMinSize(i);
      if (newValue < minSize) {
        myUpdatables.set(i, false);
        newValue = minSize;
        myNotUpdatableFound = true;
      }
      int prev = myTarget[i];
      if (prev != newValue) {
        myTarget[i] = newValue;
        myHasChanges = true;
      }
    }

    private boolean isUpdatable(int i) {
      return myUpdatables.get(i);
    }
  }
}
