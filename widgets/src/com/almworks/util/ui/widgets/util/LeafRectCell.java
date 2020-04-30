package com.almworks.util.ui.widgets.util;

import com.almworks.util.ui.widgets.*;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public abstract class LeafRectCell<T> implements Widget<T> {
  @Override
  public int getPreferedWidth(@NotNull CellContext context, @Nullable T value) {
    return getPrefSize(context, value).width;
  }

  @Override
  public int getPreferedHeight(@NotNull CellContext context, int width, @Nullable T value) {
    return getPrefSize(context, value).height;
  }

  @NotNull
  protected abstract Dimension getPrefSize(CellContext context, T value);

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
  }

  @Override
  public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable T value) {
    return null;
  }

  @Override
  public void layout(LayoutContext context, T value, @Nullable ModifiableHostCell cell) {
  }

  @Override
  public WidgetAttach getAttach() {
    return null;
  }

  @Override
  public CellActivate getActivate() {
    return null;
  }

  @Override
  public void updateUI(HostCell cell) {
  }
}
