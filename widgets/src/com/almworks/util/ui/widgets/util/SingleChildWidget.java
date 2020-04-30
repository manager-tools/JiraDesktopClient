package com.almworks.util.ui.widgets.util;

import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public abstract class SingleChildWidget<T> implements Widget<T>, CellActivate {
  private static final Log<SingleChildWidget<?>> log = (Log)Log.get(SingleChildWidget.class);
  private final WidgetChildList<T> myChildren = new WidgetChildList<T>();
  private final ActiveCellCollector myCells = new ActiveCellCollector();

  protected SingleChildWidget() {
  }

  protected SingleChildWidget(Widget<? super T> widget) {
    if (widget != null) myChildren.addChild(widget);
  }

  protected void setChildWidget(@Nullable Widget<? super T> widget) {
    if (widget == null) {
      if (myChildren.size() != 0) myChildren.removeChild(0);
    } else if (myChildren.size() == 0) myChildren.addChild(widget);
    else myChildren.setChild(0, widget);
    myCells.deleteChild(0);
    myCells.revalidate();
  }

  protected List<HostCell> getActiveCells() {
    return myCells.getCells();
  }

  protected final int getChildPreferedWidth(CellContext context, T value) {
    return context.getChildPreferedWidth(0, getChildWidget(), value);
  }

  protected final int getChildPreferedHeight(CellContext context, T value, int width) {
    return context.getChildPreferedHeight(0, getChildWidget(), value, width);
  }

  protected final void setChildBounds(LayoutContext context, int x, int y, int width, int height) {
    context.setChildBounds(0, getChildWidget(), x, y, width, height);
  }

  @Nullable
  public static HostCell getChildCell(@Nullable CellContext ownCell) {
    return ownCell != null ? ownCell.findChild(0) : null;
  }

  @Nullable
  protected Widget<? super T> getChildWidget() {
    return myChildren.size() == 0 ? null : myChildren.get(0);
  }

  public int getPreferedWidth(@NotNull CellContext context, T value) {
    Widget<? super T> widget = getChildWidget();
    return widget != null ? context.getChildPreferedWidth(0, widget, value) : 0;
  }

  public int getPreferedHeight(@NotNull CellContext context, int width, T value) {
    Widget<? super T> widget = getChildWidget();
    return widget != null ? context.getChildPreferedHeight(0, widget, value, width) : 0;
  }

  public void paint(@NotNull GraphContext context, T value) {}

  @Override
  public WidgetAttach getAttach() {
    return myChildren;
  }

  @Override
  public void updateUI(HostCell cell) {}

  @Override
  public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable T value) {
    if (cellId != 0) log.error(this, "Wrong cellId", cellId);
    return value;
  }

  @Nullable
  @Override
  public final CellActivate getActivate() {
    return this;
  }

  @Override
  public void activate(@NotNull HostCell cell) {
    myCells.activate(cell);
  }

  @Override
  public void deactivate(@NotNull HostCell cell, JComponent liveComponent) {
    myCells.deactivate(cell, liveComponent);
  }

  @Override
  public void layout(LayoutContext context, T value, @Nullable ModifiableHostCell cell) {
    if (cell != null && cell.getId() != 0) {
      cell.deleteAll();
      log.error(this, "Wrong cell to layout");
      return;
    }
    Widget<? super T> widget = getChildWidget();
    if (widget == null) return;
    layout(context, value);
  }

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
    if (reason == FocusTraverse.KEY) {
      FocusTraverse focus = context.getData(FocusTraverse.KEY);
      //noinspection ConstantConditions
      if (!focus.hasPrevChild())
        focus.moveToChild(context.findChild(0));
    }
  }

  protected abstract void layout(LayoutContext context, T value);
}
