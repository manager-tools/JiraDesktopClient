package com.almworks.util.ui.widgets.util;

import com.almworks.util.ui.widgets.CellContext;
import com.almworks.util.ui.widgets.GraphContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class GapWidget extends LeafRectCell<Object> {
  private final Dimension mySize;

  public GapWidget(Dimension size) {
    mySize = size;
  }

  @NotNull
  @Override
  protected Dimension getPrefSize(CellContext context, Object value) {
    return mySize;
  }

  @Override
  public void paint(@NotNull GraphContext context, @Nullable Object value) {
  }
}
