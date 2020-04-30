package com.almworks.util.ui.widgets.util;

import com.almworks.util.commons.Procedure2;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Base widget for hosts life Swing components in widget host.
 */
public abstract class ComponentWidget<T> implements Widget<T>, CellActivate {
  public static final Procedure2<CellContext, JComponent> FILL_BOTH = new Procedure2<CellContext, JComponent>() {
    @Override
    public void invoke(CellContext context, JComponent component) {
      int x = context.getHostX();
      int y = context.getHostY();
      int width = context.getWidth();
      int height = context.getHeight();
      AwtUtil.setBounds(component, x, y, width, height);
    }
  };
  public static final Procedure2<CellContext, JComponent> FILL_H_CENTER_V = new FillOneLayout(InlineLayout.HORISONTAL, 0.5f, true);
  private static final Log<ComponentWidget<?>> log = (Log) Log.get(ComponentWidget.class);
  private static final TypedKey<Dimension> LAST_PREF_SIZE = TypedKey.create("lastPrefSize");
  private final List<HostCell> myActiveCells = Collections15.arrayList();
  private final Procedure2<CellContext, JComponent> myLayout;

  protected ComponentWidget(Procedure2<CellContext, JComponent> layout) {
    myLayout = layout;
  }

  public int getPreferedWidth(@NotNull CellContext context, T value) {
    JComponent component = context.getLiveComponent();
    Dimension size = component != null ? component.getPreferredSize() : getPreferedSize(context, value);
    int width = size != null ? size.width : 1;
    storeLastPrefSize(context, width, InlineLayout.HORISONTAL);
    return width;
  }

  public int getPreferedHeight(@NotNull CellContext context, int width, T value) {
    JComponent component = context.getLiveComponent();
    Dimension size = component != null ? component.getPreferredSize() : getPreferedSize(context, value);
    int height = size != null ? size.height : 1;
    storeLastPrefSize(context, height, InlineLayout.VERTICAL);
    return height;
  }

  private static void storeLastPrefSize(CellContext context, int value, InlineLayout.Orientation orientation) {
    Dimension last = context.getStateValue(LAST_PREF_SIZE);
    if (last == null) {
      last = new Dimension();
      context.putStateValue(LAST_PREF_SIZE, last, true);
    }
    orientation.setAlong(last, value);
  }

  @Override
  public void activate(@NotNull HostCell cell) {
    doActivate(cell);
    showComponent(cell);
  }

  protected boolean showComponent(@Nullable HostCell cell) {
    if (cell == null || !cell.isActive()) return false;
    if (cell.getLiveComponent() != null) return false;
    JComponent component = obtainComponent(cell);
    if (component == null) return false;
    Dimension lastPrefSize = cell.getStateValue(LAST_PREF_SIZE);
    cell.setLiveComponent(component);
    if (lastPrefSize != null) {
      Dimension size = component.getPreferredSize();
      if (!lastPrefSize.equals(size)) cell.invalidate();
    } else cell.invalidate();
    return true;
  }

  protected void removeComponent(CellContext context) {
    HostCell cell = context.getActiveCell();
    if (cell == null) return;
    JComponent component = cell.getLiveComponent();
    if (component == null) return;
    cell.removeLiveComponent();
    componentDiscarded(cell, component);
  }

  protected void doActivate(HostCell cell) {
    assert cell.getLiveComponent() == null;
    myActiveCells.add(cell);
  }

  @Override
  public void deactivate(@NotNull HostCell cell, JComponent liveComponent) {
    assert liveComponent == null || liveComponent.getParent() == null;
    myActiveCells.remove(cell);
    if (liveComponent != null) componentDiscarded(cell, liveComponent);
  }

  @Override
  public void updateUI(HostCell cell) {
    JComponent component = cell.getLiveComponent();
    if (component != null) {
      Dimension size = component.getPreferredSize();
      component.updateUI();
      if (!size.equals(component.getPreferredSize())) cell.invalidate();
      else cell.repaint();
    }
  }

  @Override
  public CellActivate getActivate() {
    return this;
  }

  @Nullable
  @Override
  public WidgetAttach getAttach() {
    return null;
  }

  @Override
  public void layout(LayoutContext context, T value, @Nullable ModifiableHostCell cell) {}

  @Override
  public void paint(@NotNull GraphContext context, @Nullable T value) {}

  @Override
  public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable T value) {
    log.error(this, "Child value requested", cellId);
    return value;
  }

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
    if (EventContext.CELL_RESHAPED == reason) processReshape(context);
    else if (EventContext.FOCUS_GAINED == reason) processFocusGained(context);
    else if (FocusTraverse.KEY == reason) processFocusTraverse(context, context.getData(FocusTraverse.KEY));
  }

  protected void processFocusTraverse(EventContext context, FocusTraverse traverse) {
    JComponent component = context.getLiveComponent();
    if (component != null && component.isFocusable()) traverse.focusMe();
  }

  private static void processFocusGained(EventContext context) {
    JComponent component = context.getLiveComponent();
    if (component != null && component.isFocusable()) component.requestFocusInWindow();
  }

  protected void revalidate() {
    for (HostCell cell : myActiveCells) cell.invalidate();
  }

  protected void processReshape(CellContext context) {
    JComponent component = context.getLiveComponent();
    if (component == null) return;
    myLayout.invoke(context, component);
  }

  @Nullable
  protected abstract JComponent obtainComponent(HostCell cell);

  protected void componentDiscarded(HostCell cell, JComponent component) {}

  @Nullable
  protected abstract Dimension getPreferedSize(CellContext context, T value);

  public static void setComponentBounds(Component component, int x, int y, int width, int height)
  {
    if (component.getX() != x || component.getY() != y || component.getWidth() != width || component.getHeight() != height)
      component.setBounds(x, y, width, height);
  }

  public static class FillOneLayout implements Procedure2<CellContext, JComponent> {
    private final InlineLayout.Orientation myFill;
    private final boolean myShrink;
    private final float myPosition;

    public FillOneLayout(InlineLayout.Orientation fill, float position, boolean shrink) {
      myFill = fill;
      myPosition = position;
      myShrink = shrink;
    }

    @Override
    public void invoke(CellContext context, JComponent component) {
      Rectangle bounds = context.getHostBounds(null);
      int acrossBounds = myFill.getAcrossSize(bounds);
      int acrossPref = myFill.getAcross(component.getPreferredSize());
      if (acrossBounds <= acrossPref) {
        if (!myShrink) myFill.setAcrossSize(bounds, acrossPref);
      } else {
        myFill.setAcrossSize(bounds, acrossPref);
        int dAcross = (int) ((acrossBounds - acrossPref) * myPosition);
        myFill.setAcrossPosition(bounds, myFill.getAcrossPosition(bounds) + dAcross);
      }
      AwtUtil.setBounds(component, bounds);
    }
  }
}
