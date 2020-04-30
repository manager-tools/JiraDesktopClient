package com.almworks.util.fx.layout;

import com.almworks.util.fx.test.FXTestCase;
import javafx.scene.layout.Region;

public class RowLayoutTest extends FXTestCase.FXThread {
  public void test() {
    RowLayout parent = new RowLayout();
  }

  public static class MockNode extends Region {
    private double myBaseline = BASELINE_OFFSET_SAME_AS_HEIGHT;
    @Override
    public double getBaselineOffset() {
      return myBaseline;
    }
  }
}
