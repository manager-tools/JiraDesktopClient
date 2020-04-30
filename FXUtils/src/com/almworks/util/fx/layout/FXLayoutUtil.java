package com.almworks.util.fx.layout;

import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.function.LongToDoubleFunction;

public class FXLayoutUtil {
  /**
   * Distributes given amount over the array of values.<br>
   * Snap is used to snap changes, does not affect original values. If snapped amount is zero - the amount is not distributed.<br>
   * If limit function is specified, it determines the limit for each value. The distribution respects the limits.
   * @param values original value to distribute over. Also this array contains output result
   * @param amount amount to distribute. Can be positive or negative
   * @param snap used to snap changes. Original values are not snapped.
   * @param limitFunc if not null - provides limit for value at index. The value will not be change over the limit
   *                  (e.g. if original is greater then limit, the result is greater or equal too)
   */
  public static void distribute(double[] values, double amount, SnapToPixel snap, @Nullable LongToDoubleFunction limitFunc) {
    BitSet limited = new BitSet();
    while (amount != 0 && values.length > limited.cardinality()) {
      int nodeCount = values.length - limited.cardinality();
      for (int i = 0; i < values.length; i++) {
        if (limited.get(i)) continue;
        double change = snap.change(amount / nodeCount);
        if (change > 0) {
          double original = values[i];
          double size = original + change;
          double constrained;
          if (limitFunc != null) {
            double limitValue = limitFunc.applyAsDouble(i);
            if (original == limitValue) constrained = limitValue;
            else constrained = (original > limitValue) == (size > limitValue) ? size : limitValue;
          } else constrained = size;
          double actualChange = constrained - original;
          if (actualChange != change) limited.set(i);
          actualChange = snap.change(actualChange);
          constrained = original + actualChange;
          values[i] = constrained;
          amount -= actualChange;
        }
        nodeCount--;
        if (nodeCount == 0) break;
        amount = snap.change(amount);
      }
    }
  }
}
