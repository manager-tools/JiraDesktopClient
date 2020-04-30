package com.almworks.util.ui.widgets.util;

import com.almworks.util.components.SizeCalculator1D;
import com.almworks.util.ui.widgets.CellContext;
import com.almworks.util.ui.widgets.EventContext;
import com.almworks.util.ui.widgets.HostCell;
import org.almworks.util.TypedKey;

public interface ScrollPolicy {
  int getUnitScroll(CellContext context, int units);

  int getBlockScroll(EventContext context, int wheelRotation);

  void updateUI(HostCell cell);

  ScrollPolicy SINGLE_LINE = new Fixed(SizeCalculator1D.textLines(1), true);

  class Fixed implements ScrollPolicy {
    private static final TypedKey<Integer> PIXELS = TypedKey.create("scrollPixels");
    private final SizeCalculator1D myUnit;
    private final boolean myVertical;

    public Fixed(SizeCalculator1D unit, boolean vertical) {
      myUnit = unit;
      myVertical = vertical;
    }

    public int getUnitScroll(CellContext context, int units) {
      return getPixelUnit(context) * units;
    }

    private int getPixelUnit(CellContext context) {
      Integer counted = context.getStateValue(PIXELS);
      if (counted == null) {
        counted = myUnit.getPrefLength(context.getHost().getHostComponent());
        context.putStateValue(PIXELS, counted, false);
      }
      return counted;
    }

    public int getBlockScroll(EventContext context, int wheelRotation) {
      int unit = getPixelUnit(context);
      int block = myVertical ? context.getHeight() : context.getWidth();
      if (block > unit * 3)
        block -= unit;
      return block * wheelRotation;
    }

    public void updateUI(HostCell cell) {
      cell.putStateValue(PIXELS, null, false);
    }
  }
}
