package com.almworks.util.components.renderer;

import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.datetime.DateUtil;

public class SecondsRenderer implements CanvasRenderer<Integer> {
  @Override
  public void renderStateOn(CellState state, Canvas canvas, Integer value ) {
    if (value == null || value <= 0) return;
    canvas.appendText(DateUtil.getFriendlyDuration(value, false));
  }
}
