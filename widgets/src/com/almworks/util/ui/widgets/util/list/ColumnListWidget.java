package com.almworks.util.ui.widgets.util.list;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Procedure;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.util.SegmentsLayout;
import com.almworks.util.ui.widgets.util.WidgetUtil;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ColumnListWidget<T> implements Widget<AListModel<T>>, WidgetAttach, CellActivate {
  private final List<Widget<T>> myWidgets = Collections15.arrayList();
  private final SegmentsLayout myColumnLayout = new SegmentsLayout(0, 0);
//  private final SegmentsLayout myRowLayout = new SegmentsLayout(0, 0);
  @SuppressWarnings("unchecked")
  private final FireEventSupport<Procedure<HostCell>> myListeners = (FireEventSupport)FireEventSupport.create(Procedure.class);
  @SuppressWarnings("unchecked")
  private final FireEventSupport<EventListener<T>> myEventListeners = (FireEventSupport)FireEventSupport.create(EventListener.class);
  private final UserDataHolder myUserData = new UserDataHolder();
  private final TableLayoutPolicy myLayoutPolicy;
  private final TablePaintPolicy myPaintPolicy;

  public ColumnListWidget(TableLayoutPolicy layoutPolicy, TablePaintPolicy paintPolicy) {
    myLayoutPolicy = layoutPolicy;
    myPaintPolicy = paintPolicy;
  }

  public void addWidget(Widget<T> widget) {
    //noinspection unchecked
    addWidgets(widget);
  }

  public void addWidgets(Widget<T> ... widgets) {
    myWidgets.addAll(Arrays.asList(widgets));
  }

  public void addLayoutListener(Lifespan life, ThreadGate gate, Procedure<HostCell> listener) {
    myListeners.addListener(life, gate, listener);
  }

  public void addEventListener(Lifespan life, EventListener<? super T> listener) {
    //noinspection unchecked
    myEventListeners.addListener(life, ThreadGate.STRAIGHT, (EventListener<T>)listener);
  }

  public UserDataHolder getUserData() {
    return myUserData;
  }

  @Override
  public int getPreferedWidth(@NotNull CellContext context, @Nullable AListModel<T> list) {
    int[] width = getWidths(context, list);
    return ArrayUtil.sum(width, 0, width.length);
  }

  @Override
  public int getPreferedHeight(@NotNull CellContext context, int width, @Nullable AListModel<T> list) {
    int[] heights = getHeights(context, width, list);
    return ArrayUtil.sum(heights, 0, heights.length);
  }

  private int[] getHeights(CellContext context, int width, AListModel<T> model) {
    return MyState.getHeights(context, width, model, this);
  }

  private int[] getWidths(CellContext context, AListModel<T> model) {
    return MyState.getWidths(context, model, this);
  }

  public int getCellId(int column, int row) {
    return row * getColumnCount() + column;
  }

  public int getRow(int cellId) {
    return cellId / getColumnCount();
  }

  public int getColumn(int cellId) {
    return cellId % getColumnCount();
  }

  @Override
  public void paint(@NotNull GraphContext context, @Nullable AListModel<T> value) {
    if (myPaintPolicy == null) return;
    CellState state = MyState.getInstance(context, value, this);
    if (state == null) return;
    myPaintPolicy.paintTable(context, state);
  }

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable AListModel<T> value, TypedKey<?> reason) {
    if (reason == EventContext.VALUE_CHANGED) MyState.detach(context);
    myEventListeners.getDispatcher().processEvent(context, value, reason, this);
  }

  @Override
  public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable AListModel<T> value) {
    return value != null ? value.getAt(getRow(cellId)) : null;
  }

  @Override
  public void layout(LayoutContext context, AListModel<T> list, @Nullable ModifiableHostCell cell) {
    MyState<T> state = MyState.getInstance(context);
    if (state == null) return;
    int[] width = ArrayUtil.arrayCopy(state.getWidths());
    int[] heights = ArrayUtil.arrayCopy(state.getHeights(context.getWidth()));
    myColumnLayout.layout(context.getWidth(), width, width.length, 0);
    if (heights.length != 0) {
//      myRowLayout.layout(rowHeightSum, heights, heights.length, 0);
      int y = 0;
      int[] margine = null;
      for (int i = 0; i < list.getSize(); i++) {
        int x = 0;
        int h = heights[i];
        margine = myLayoutPolicy.getMargin(heights, i, margine);
        for (int j = 0; j < myWidgets.size(); j++) {
          Widget<T> widget = myWidgets.get(j);
          int w = width[j];
          context.setChildBounds(getCellId(j, i), widget, x, y + margine[0], w, Math.max(0, h - margine[0] - margine[1]));
          x += w;
        }
        y += h;
      }
    }
    state.onLayout(width, heights);
  }

  @Override
  public WidgetAttach getAttach() {
    return this;
  }

  @Override
  public void attach(@NotNull WidgetHost host) {
    for (Widget<T> widget : myWidgets) WidgetUtil.attachWidget(widget, host);
  }

  @Override
  public void detach(@NotNull WidgetHost host) {
    for (Widget<T> widget : myWidgets) WidgetUtil.detachWidget(widget, host);
  }

  @Override
  public CellActivate getActivate() {
    return this;
  }

  @Override
  public void activate(@NotNull HostCell cell) {
    AListModel<T> model = cell.restoreValue(this);
    MyState.activate(cell, model, this);
  }

  @Override
  public void deactivate(@NotNull HostCell cell, @Nullable JComponent liveComponent) {
    MyState.detach(cell);
  }

  @Override
  public void updateUI(HostCell cell) {
    // todo
  }

  public void setColumnPolicy(Widget<?> widget, int grow, int shrink, int minSize) {
    @SuppressWarnings("SuspiciousMethodCalls")
    int index = myWidgets.indexOf(widget);
    if (index < 0) Log.error("No column " + widget);
    else {
      for (int i = index + 1; i < myWidgets.size(); i++) {
        Widget<T> w =  myWidgets.get(i);
        if (w == widget) {
          Log.error("Column not unique " + widget);
          break;
        }
      }
      setColumnPolicy(index, grow, shrink, minSize);
    }
  }

  public void setColumnPolicy(int index, int grow, int shrink, int minSize) {
    myColumnLayout.setSegmentPolicy(index, grow, shrink, minSize);
  }

  public void resetColumnLayout(int grow, int shrink, int minSize) {
    myColumnLayout.reset(grow, shrink, minSize);
  }

  public int getColumnCount() {
    return myWidgets.size();
  }

  public Widget<T> getColumnWidget(int column) {
    return myWidgets.get(column);
  }

  @Nullable
  public static int[] copyCurrentColumnLayout(HostCell cell) {
    MyState<?> state = MyState.getInstance(cell);
    return state != null ? state.copyCurrentLayoutWidth() : null;
  }

  @Nullable
  public static int[] getCurrentRowBounds(HostCell cell, int row) {
    MyState<?> state = MyState.getInstance(cell);
    if (state == null) return null;
    return state.getCurrentRowBounds(row);
  }

  public void notifyLayoutChanged(HostCell cell) {
    myListeners.getDispatcher().invoke(cell);
  }

  public int calcMinWidth() {
    int sum = 0;
    for (int i = 0; i < myWidgets.size(); i++) {
      int minSize = myColumnLayout.getMinSize(i);
      sum += minSize;
    }
    return sum;
  }

  public interface CellState {

    int getRowCount();

    int[] copyCurrentLayoutWidth();

    int getRowHeight(int rowIndex);
  }

  private static class MyState<T> implements AListModel.Listener<T>, CellState {
    private static final TypedKey<MyState<?>> STATE = TypedKey.create("state");
    private final ListLayoutState myLayoutState;
    private final HostCell myCell;
    private final ColumnListWidget<T> myWidget;
    private final AListModel<T> myModel;
    private final DetachComposite myLife = new DetachComposite();
    private int[] myWidths = null;
    private int myLastWidth = -1;
    private int[] myHeights = null;


    private MyState(HostCell cell, ColumnListWidget<T> widget, AListModel<T> model) {
      myCell = cell;
      myWidget = widget;
      myModel = model;
      myLayoutState = new ListLayoutState(widget.getColumnCount(), myWidget);
    }

    @Nullable
    private static <T> MyState<T> getInstance(CellContext context) {
      //noinspection unchecked
      return context != null ? (MyState<T>) context.getStateValue(STATE) : null;
    }

    public static <T> MyState<T> getInstance(CellContext context, AListModel<T> model, ColumnListWidget<T> widget) {
      HostCell cell = context.getActiveCell();
      if (cell == null) return null;
      if (cell.getWidget() != widget) return null;
      MyState<T> state = getInstance(context);
      if (state == null) return null;
      ensureRightValue(context, model, widget, state);
      state = getInstance(context);
      return state;
    }

    @Override
    public void onInsert(int index, int length) {
      if (length == 0) return;
      myWidths = null;
      myHeights = null;
      myLastWidth = -1;
      remapIds(index, length);
      myCell.invalidate();
    }

    @Override
    public void onRemove(int index, int length, AListModel.RemovedEvent<T> tRemovedEvent) {
      int[] ids = myCell.getChildrenCellIds();
      int firstIndex = index * myWidget.getColumnCount();
      int lastIndex = (index + length) * myWidget.getColumnCount();
      for (int id : ids) {
        if (id < firstIndex || id >= lastIndex) continue;
        HostCell child = myCell.findChild(id);
        if (child != null) child.deleteAll();
      }
      remapIds(index, -length);
      myWidths = null;
      myHeights = null;
      myLastWidth = -1;
      myCell.invalidate();
    }

    @Override
    public int getRowCount() {
      return myLayoutState.getRowCount();
    }

    private void remapIds(int index, int length) {
      int[] ids = myCell.getChildrenCellIds();
      int delta = length * myWidget.getColumnCount();
      int firstId = index * myWidget.getColumnCount();
      for (int i = 0; i < ids.length; i++) {
        int id = ids[i];
        if (id >= firstId) ids[i] = id + delta;
      }
      myCell.remapChildrenIds(ids);
    }

    @Override
    public void onListRearranged(AListModel.AListEvent event) {
      int[] ids = myCell.getChildrenCellIds();
      int[] newIds = new int[ids.length];
      for (int i = 0; i < ids.length; i++) {
        int id = ids[i];
        int row = myWidget.getRow(id);
        if (!event.isAffected(row)) newIds[i] = id;
        else newIds[i] = getCellId(getColumn(id), event.getNewIndex(row));
      }
      myCell.remapChildrenIds(newIds);
      myCell.invalidate();
      myWidths = null;
      myHeights = null;
      myLastWidth = -1;
      WidgetUtil.postEventToAllDescendants(myCell, EventContext.VALUE_CHANGED, null);
    }

    @Override
    public void onItemsUpdated(AListModel.UpdateEvent event) {
      myWidths = null;
      myHeights = null;
      myLastWidth = -1;
      myCell.invalidate();
      myCell.repaint();
      WidgetUtil.postEventToAllDescendants(myCell, EventContext.VALUE_CHANGED, null);
    }

    public void detach() {
      myWidget.myEventListeners.getDispatcher().onDeactive();
      myLife.detach();
      myCell.putStateValue(STATE, null, true);
    }

    public static <T> void activate(CellContext context, AListModel<T> model, ColumnListWidget<T> widget) {
      MyState<Object> prev = getInstance(context);
      if (prev != null) {
        if (prev.myModel == model) return;
        prev.detach();
      }
      if (model == null) return;
      if (context == null) return;
      HostCell cell = context.getActiveCell();
      if (cell == null) return;
      MyState<T> state = new MyState<T>(cell, widget, model);
      context.putStateValue(STATE, state, true);
      state.myLife.add(model.addListener(state));
      widget.myEventListeners.getDispatcher().onActivate(context, model, widget);
    }

    public static <T> int[] getHeights(CellContext context, int width, AListModel<T> model, ColumnListWidget<T> widget) {
      ensureRightValue(context, model, widget);
      MyState<T> state = getInstance(context);
      if (state == null) {
        int[] widths = calcWidths(context, model, widget);
        return calcHeights(context, model, widget, widths);
      }
      return state.getHeights(width);
    }

    public static <T> int[] getWidths(CellContext context, AListModel<T> model, ColumnListWidget<T> widget) {
      ensureRightValue(context, model, widget);
      MyState<T> state = getInstance(context);
      if (state == null) return calcWidths(context, model, widget);
      return state.getWidths();
    }

    private int[] getHeights(int width) {
      if (myHeights == null || myLastWidth != width) {
        int[] widths = getWidths();
        myLastWidth = width;
        myHeights = calcHeights(myCell, myModel, myWidget, widths);
      }
      return myHeights;
    }

    private int[] getWidths() {
      if (myWidths == null) {
        myWidths = calcWidths(myCell, myModel, myWidget);
        myHeights = null;
      }
      return myWidths;
    }

    @Override
    public int getRowHeight(int rowIndex) {
      int[] heights = getHeights(myCell.getWidth());
      return heights[rowIndex];
    }

    private static <T> int[] calcWidths(CellContext context, AListModel<T> model, ColumnListWidget<T> widget) {
      List<T> list = model != null && model.getSize() > 0 ? model.toList() : Collections.<T>singletonList(null);
      int[] width = new int[widget.getColumnCount()];
      for (int i = 0; i < list.size(); i++) {
        T value = list.get(i);
        for (int j = 0; j < width.length; j++) {
          Widget<T> columnWidget = widget.getColumnWidget(j);
          int w = context.getChildPreferedWidth(widget.getCellId(j, i), columnWidget, value);
          width[j] = Math.max(width[j], w);
        }
      }
      return width;
    }

    private static <T> int[] calcHeights(CellContext context, AListModel<T> model, ColumnListWidget<T> widget, int[] widths) {
      int size = model.getSize();
      int[] heights = new int[size];
      TableLayoutPolicy policy = widget.myLayoutPolicy;
      policy.invalidateLayoutCache(context);
      int[] widthCopy = ArrayUtil.arrayCopy(widths);
      for (int i = 0; i < size; i++) {
        T value = model.getAt(i);
        System.arraycopy(widths, 0, widthCopy, 0, widths.length);
        heights[i] = policy.getPreferedCellHeight(context, widget, i, value, widthCopy, size);
      }
      return heights;
    }

    private int getCellId(int column, int row) {
      return myWidget.getCellId(column, row);
    }

    private static <T> void ensureRightValue(CellContext context, AListModel<T> list, ColumnListWidget<T> widget) {
      MyState<?> state = MyState.getInstance(context);
      ensureRightValue(context, list, widget, state);
    }

    private static <T> void ensureRightValue(CellContext context, AListModel<T> list, ColumnListWidget<T> widget,
      MyState<?> state)
    {
      if (state == null) return;
      if (state.myModel == list) return;
      Log.error("Value changed without notification", new Throwable());
      state.detach();
      activate(context, list, widget);
    }

    public int getColumn(int cellId) {
      return cellId % myWidget.getColumnCount();
    }

    public static void detach(CellContext context) {
      MyState<Object> state = MyState.getInstance(context);
      if (state != null) state.detach();
    }

    public void onLayout(int[] width, int[] heights) {
      myLayoutState.onLayout(width, heights, myCell);
    }

    public int[] copyCurrentLayoutWidth() {
      return myLayoutState.copyCurrentWidth();
    }

    public int[] getCurrentRowBounds(int row) {
      return myLayoutState.getRowBounds(row);
    }
  }

  public interface EventListener<T> {
    void processEvent(@NotNull EventContext context, @Nullable AListModel<T> value, TypedKey<?> reason, ColumnListWidget<T> widget);

    void onActivate(CellContext context, AListModel<T> model, ColumnListWidget<T> widget);

    void onDeactive();
  }
}
