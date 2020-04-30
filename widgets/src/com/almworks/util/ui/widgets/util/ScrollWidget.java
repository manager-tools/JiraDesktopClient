package com.almworks.util.ui.widgets.util;

import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class ScrollWidget<T> extends SingleChildWidget<T> {
  private static final Log<ScrollWidget<?>> log = (Log)Log.get(ScrollWidget.class);
  private static final TypedKey<BoundedRangeModel> POS_HORIZONTAL = TypedKey.create("posHorizontal");
  private static final TypedKey<BoundedRangeModel> POS_VERTICAL = TypedKey.create("posVertical");
  private static final TypedKey<ChangeListener> LISTENER = TypedKey.create("listener");

  public static final int UP_LEFT = 0;
  public static final int UP_RIGHT = 1;
  public static final int BOTTOM_LEFT = 2;
  public static final int BOTTOM_RIGHT = 3;

  private final ScrollPolicy myVerticalScroll;
  private final boolean myHorizontalScroll;
  private final CenterViewport<T> myCenter;
  private final SingleChildWidget<T> myUpStripe = AdditionalArea.stripe(false);
  private final SingleChildWidget<T> myLeftStripe = AdditionalArea.stripe(true);
  private static final int[] VERTICAL_BAR = new int[]{2, 1};
  private static final int[] HORIZONTAL_BAR = new int[]{1, 2};
  private static final int[] CENTER_VIEWPORT = new int[]{1, 1};
  private static final int[] LEFT_STRIPE = new int[]{0, 1};
  private static final int[] UP_STRIPE = new int[]{1, 0};

  @SuppressWarnings({"ThisEscapedInObjectConstruction", "ConstantConditions"})
  public ScrollWidget(Widget<? super T> center, ScrollPolicy verticalScroll, boolean horizontalScroll) {
    super(new GridWidget<T>());
    myVerticalScroll = verticalScroll;
    myHorizontalScroll = horizontalScroll;
    myCenter = new CenterViewport<T>(this, center);

    SegmentsLayout columns = new SegmentsLayout(0, 0);
    columns.setSegmentCount(3);
    columns.setSegmentPolicy(1, 1, 1);

    SegmentsLayout rows = new SegmentsLayout(0, 0);
    rows.setSegmentCount(3);
    rows.setSegmentPolicy(1, 1, 1);

    GridWidget<T> grid = getChildWidget();
    grid.setLayout(columns, rows);
    grid.setChild(VERTICAL_BAR, new BarWidget(this, SwingConstants.VERTICAL));
    grid.setChild(HORIZONTAL_BAR, new BarWidget(this, SwingConstants.HORIZONTAL));
    grid.setChild(CENTER_VIEWPORT, myCenter);
    grid.setChild(UP_STRIPE, myUpStripe);
    grid.setChild(LEFT_STRIPE, myLeftStripe);
  }

  public static <T> ScrollWidget<T> wrap(Widget<T> scrollable, ScrollPolicy vertical, boolean horizontal) {
    return new ScrollWidget<T>(scrollable, vertical, horizontal);
  }

  public void setCenter(Widget<? super T> center) {
    myCenter.setChildWidget(center);
  }

  public void setUpStripe(Widget<? super T> up) {
    myUpStripe.setChildWidget(up);
  }

  public void setLeftStripe(Widget<? super T> left) {
    myLeftStripe.setChildWidget(left);
  }

  @SuppressWarnings({"ConstantConditions"})
  public void setCorner(int corner, Widget<? super T> widget) {
    int column = (corner % 2) * 2;
    int row = (corner / 2) * 2;
    Widget<T> existing = (Widget<T>) getChildWidget().getChildWidget(column, row);
    if (!(existing instanceof AdditionalArea)) {
      existing = new AdditionalArea<T>(true, true);
      getChildWidget().setChild(column, row, existing);
    }
    ((SingleChildWidget<T>) existing).setChildWidget(widget);
  }

  @Nullable
  public JScrollBar getScrollBar(CellContext cell, boolean vertical) {
    HostCell barCell = findHolderCell(cell, vertical ? VERTICAL_BAR : HORIZONTAL_BAR);
    if (barCell == null) return null;
    JComponent bar = barCell.getLiveComponent();
    return bar instanceof JScrollBar ? (JScrollBar) bar : null;
  }

  @Nullable
  public HostCell getCenterCell(CellContext cell) {
    return findUserCell(cell, CENTER_VIEWPORT);
  }

  public HostCell getUpStripeCell(CellContext cell) {
    return findUserCell(cell, UP_STRIPE);
  }

  public HostCell getLeftStripeCell(CellContext cell) {
    return findUserCell(cell, LEFT_STRIPE);
  }

  public HostCell getConerCell(CellContext ownCell, int corner) {
    int column = (corner % 2) * 2;
    int row = (corner / 2) * 2;
    //noinspection ConstantConditions
    return getChildCell(getChildWidget().findCell(getChildCell(ownCell), column, row));
  }

  private HostCell findUserCell(CellContext ownCell, int[] what) {
    return getChildCell(findHolderCell(ownCell, what));
  }

  private HostCell findHolderCell(CellContext ownCell, int[] what) {
    //noinspection ConstantConditions
    return getChildWidget().findCell(getChildCell(ownCell), what);
  }

  @Override
  protected final GridWidget<T> getChildWidget() {
    return (GridWidget<T>) super.getChildWidget();
  }

  @Override
  protected void layout(LayoutContext context, T value) {
    setChildBounds(context, 0, 0, context.getWidth(), context.getHeight());
  }

  @Override
  public void processEvent(@NotNull EventContext context, T value, TypedKey<?> reason) {
    super.processEvent(context, value, reason);
    HostCell cell = context.getActiveCell();
    if (cell == null || reason != MouseEventData.KEY || !allowVertical())
      return;
    MouseEventData mouse = context.getData(MouseEventData.KEY);
    //noinspection ConstantConditions
    if (mouse.getEventId() != MouseEvent.MOUSE_WHEEL)
      return;

    int type = mouse.getScrollType();
    BoundedRangeModel model = getVerticalModel(cell);
    if (model == null) {
      log.error(this, "processEvent no model", cell);
      return;
    }
    int delta;
    if (type == MouseWheelEvent.WHEEL_UNIT_SCROLL) delta = myVerticalScroll.getUnitScroll(context, mouse.getUnitToScroll());
    else if (type == MouseWheelEvent.WHEEL_BLOCK_SCROLL) delta = myVerticalScroll.getBlockScroll(context, mouse.getWheelRotation());
    else {
      log.error(this, "processEvent wrong type", type);
      return;
    }
    model.setValue(model.getValue() + delta);
  }

  @Override
  public void updateUI(HostCell cell) {
    if (myVerticalScroll != null) {
      myVerticalScroll.updateUI(cell);
      cell.invalidate();
    }
  }

  private boolean allowVertical() {
    return myVerticalScroll != null;
  }

  private boolean allowHorizontal() {
    return myHorizontalScroll;
  }

  private boolean allowScroll(boolean vertical) {
    return vertical ? allowVertical() : allowHorizontal();
  }

  @Nullable
  private BoundedRangeModel getHorizontalModel(CellContext cell) {
    return getModel(cell, POS_HORIZONTAL);
  }

  private BoundedRangeModel getVerticalModel(CellContext cell) {
    return getModel(cell, POS_VERTICAL);
  }

  @Nullable
  private BoundedRangeModel getModel(CellContext context, TypedKey<BoundedRangeModel> key) {
    HostCell cell = context.getActiveCell();
    if (cell == null) return null;
    final HostCell mainCell = cell.getWidget() == this ? cell : cell.getAncestor(this);
    BoundedRangeModel model;
    if (mainCell != null) {
      model = mainCell.getStateValue(key);
      if (model == null && mainCell.isActive()) {
        ChangeListener listener = mainCell.getStateValue(LISTENER);
        if (listener == null) {
          listener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
              moveViewports(mainCell);
            }
          };
          mainCell.putStateValue(LISTENER, listener, true);
        }
        model = new DefaultBoundedRangeModel();
        model.addChangeListener(listener);
        mainCell.putStateValue(key, model, true);
      }
    } else
      model = null;
    return model;
  }

  @SuppressWarnings({"ConstantConditions"})
  private void moveViewports(HostCell mainCell) {
    HostCell gridCell = getChildCell(mainCell);
    HostCell center = getChildWidget().findCell(gridCell, CENTER_VIEWPORT);
    HostCell left = getChildWidget().findCell(gridCell, LEFT_STRIPE);
    HostCell up = getChildWidget().findCell(gridCell, UP_STRIPE);
    if (center != null) center.invalidate();
    if (left != null) left.invalidate();
    if (up != null) up.invalidate();
  }

  @Nullable
  private ChangeListener getBarModelListener(HostCell cell) {
    HostCell mainCell = cell.getAncestor(this);
    return mainCell != null ? mainCell.getStateValue(LISTENER) : null;
  }

  private static class BarWidget extends ComponentWidget<Object> {
    private final ScrollWidget<?> myMain;
    private final int myOrientation;
    private JScrollBar mySampleBar = null;

    private BarWidget(ScrollWidget<?> main, int orientation) {
      super(FILL_BOTH);
      myOrientation = orientation;
      myMain = main;
    }

    @Nullable
    protected Dimension getPreferedSize(CellContext context, Object value) {
      return isScrollAllowed() ? getSampleBar().getPreferredSize() : null;
    }

    private boolean isScrollAllowed() {
      return myMain.allowScroll(myOrientation == SwingConstants.VERTICAL);
    }

    @NotNull
    private JScrollBar getSampleBar() {
      if (mySampleBar == null)
        mySampleBar = new JScrollBar(myOrientation);
      return mySampleBar;
    }

    @Nullable
    protected JComponent obtainComponent(HostCell cell) {
      if (!isScrollAllowed())
        return null;
      JScrollBar bar = getSampleBar();
      mySampleBar = null;
      BoundedRangeModel model = getOrientedModel(cell);
      assert model != null : cell;
      bar.setModel(model);
      return bar;
    }

    @Nullable
    private BoundedRangeModel getOrientedModel(CellContext context) {
      TypedKey<BoundedRangeModel> key = isVertical() ? POS_VERTICAL : POS_HORIZONTAL;
      return myMain.getModel(context, key);
    }

    private boolean isVertical() {
      return myOrientation == SwingConstants.VERTICAL;
    }

    @Override
    protected void processReshape(CellContext context) {
      super.processReshape(context);
      BoundedRangeModel model = getOrientedModel(context);
      if (model == null) {
        log.error(myMain, "Missing bounds model", context);
        return;
      }
      int extent = isVertical() ? context.getHeight() : context.getWidth();
      int value = model.getValue();
      if (extent + value > model.getMaximum()) value = model.getMaximum() - extent;
      value = Math.max(0, value);
      if (model.getExtent() != extent || value != model.getValue())
        model.setRangeProperties(value, extent, 0, model.getMaximum(), false);
    }

    @Override
    protected void componentDiscarded(HostCell cell, JComponent component) {
      JScrollBar bar = (JScrollBar) component;
      BoundedRangeModel model = bar.getModel();
      ChangeListener[] listeners = ((DefaultBoundedRangeModel) model).getChangeListeners();
      ChangeListener mainListener = myMain.getBarModelListener(cell);
      bar.setModel(new DefaultBoundedRangeModel());
      for (ChangeListener listener : listeners) {
        if (listener != mainListener) model.removeChangeListener(listener);
      }
      if (mySampleBar == null)
        mySampleBar = bar;
    }

    @Override
    protected void processFocusTraverse(EventContext context, FocusTraverse focus) {}
  }

  private static class CenterViewport<T> extends SingleChildWidget<T> {
    private final ScrollWidget<T> myMain;

    protected CenterViewport(ScrollWidget<T> main, @Nullable Widget<? super T> widget) {
      super(widget);
      myMain = main;
    }

    protected void layout(LayoutContext context, T value) {
      Widget<? super T> widget = getChildWidget();
      if (widget == null) return;
      BoundedRangeModel vert = myMain.getVerticalModel(context);
      BoundedRangeModel hor = myMain.getHorizontalModel(context);
      if (vert == null || hor == null) {
        log.error(myMain, "center#layout", context, vert, hor);
        return;
      }
      int x = -hor.getValue();
      int y = -vert.getValue();
      int width;
      if (myMain.allowHorizontal()) width = Math.max(context.getWidth(), getChildPreferedWidth(context, value));
      else width = context.getWidth();
      int height;
      if (myMain.allowVertical()) height = Math.max(context.getHeight(), getChildPreferedHeight(context, value, width));
      else height = context.getHeight();
      vert.setMaximum(height);
      hor.setMaximum(width);
      setChildBounds(context, x, y, width, height);
    }

    @Override
    public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
      super.processEvent(context, value, reason);
      if (reason == EventContext.CELL_INVALIDATED) {
        context.consume();
        context.invalidate();
      }
    }
  }

  private static class AdditionalArea<T> extends SingleChildWidget<T> {
    private final boolean myIgnoreWidth;
    private final boolean myIgnoreHeight;

    private AdditionalArea(boolean ignoreWidth, boolean ignoreHeight) {
      myIgnoreWidth = ignoreWidth;
      myIgnoreHeight = ignoreHeight;
    }

    public static <T> AdditionalArea<T> stripe(boolean vertical) {
      return new AdditionalArea<T>(!vertical, vertical);
    }

    @Override
    public int getPreferedWidth(@NotNull CellContext context, T value) {
      return myIgnoreWidth ? 0 : super.getPreferedWidth(context, value);
    }

    @Override
    public int getPreferedHeight(@NotNull CellContext context, int width, T value) {
      return myIgnoreHeight ? 0 : super.getPreferedHeight(context, width, value);
    }

    @Override
    public void processEvent(@NotNull EventContext context, @Nullable T value, TypedKey<?> reason) {
      super.processEvent(context, value, reason);
      if (reason == EventContext.CELL_INVALIDATED && (!myIgnoreWidth || !myIgnoreHeight)) {
        context.consume();
        context.invalidate();
      }
    }

    @Override
    protected void layout(LayoutContext context, T value) {
      setChildBounds(context, 0, 0, context.getWidth(), context.getHeight());
    }
  }
}
