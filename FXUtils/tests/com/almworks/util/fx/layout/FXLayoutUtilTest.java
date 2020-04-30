package com.almworks.util.fx.layout;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

import java.util.Arrays;
import java.util.function.LongToDoubleFunction;

public class FXLayoutUtilTest extends BaseTestCase {
  public static final CollectionsCompare CHECK = new CollectionsCompare();

  public void testDistribute() {
    checkDistribute(new double[]{0, 0, 0}, 4, SnapToPixel.SNAP, null, 1, 1, 2);
    checkDistribute(new double[]{0, 0, 0}, 4, SnapToPixel.SNAP, index -> index != 1 ? 1.5 : 2, 1, 2, 1);
  }

  private void checkDistribute(double[] initial, int amount, SnapToPixel snap, LongToDoubleFunction limitFunc, double ... expected) {
    double[] values = Arrays.copyOf(initial, initial.length);
    FXLayoutUtil.distribute(values, amount, snap, limitFunc);
    CHECK.order(values, expected);
  }
}
