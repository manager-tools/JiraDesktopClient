package com.almworks.util.ui.widgets.util.list;

import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function2;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.ComponentCellState;
import com.almworks.util.components.renderer.ListCellState;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.widgets.*;
import com.almworks.util.ui.widgets.impl.IWidgetHostComponent;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class ListSelectionProcessor<T> implements ColumnListWidget.EventListener<T> {
  private static final TypedKey<Integer> SELECTED_ROW = TypedKey.create("selectedRow");
  private static final TypedKey<ListSelectionProcessor<?>> SELECTION = TypedKey.create("selectionProcessor");
  private Condition<? super T> mySelectable = null;

  public ListSelectionProcessor() {
  }

  public void install(ColumnListWidget<T> list) {
    if (!list.getUserData().putIfAbsent(SELECTION, this)) LogHelper.error("Already has selection");
    else list.addEventListener(Lifespan.FOREVER, this);
  }

  public void setSelectable(Condition<? super T> selectable) {
    mySelectable = selectable;
  }

  @Override
  public void processEvent(@NotNull EventContext context, @Nullable AListModel<T> value, TypedKey<?> reason, ColumnListWidget<T> widget) {
    if (reason == EventContext.MOUSE) onMouse(context, value, widget);
    if (reason == EventContext.KEY_EVENT) onKey(context, value, widget);
  }

  private void onKey(EventContext context, AListModel<T> model, ColumnListWidget<T> widget) {
    KeyEvent key = context.getData(EventContext.KEY_EVENT);
    HostCell cell = context.getActiveCell();
    if (key == null || cell == null) return;
    if (key.getID() != KeyEvent.KEY_PRESSED) return;
    int code = key.getKeyCode();
    int selected = cell.getStateValue(SELECTED_ROW, -1);
    int newRow = selected;
    int rowCount = model.getSize();
    switch (code) {
    case KeyEvent.VK_DOWN: newRow = selectThisOrNext(model, newRow + 1, 1); context.consume(); break;
    case KeyEvent.VK_UP: newRow = selectThisOrNext(model, newRow - 1, -1); context.consume(); break;
    case KeyEvent.VK_HOME: newRow = selectThisOrNext(model, 0, 1); context.consume(); break;
    case KeyEvent.VK_END: newRow = selectThisOrNext(model, rowCount - 1, -1); context.consume(); break;
    }
    if (newRow == selected || newRow < 0 || newRow >= rowCount) return;
    setSelected(cell, newRow);
  }

  private int selectThisOrNext(AListModel<T> model, int row, int step) {
    int count = model.getSize();
    while (row >= 0 && row < count && !canSelect(row, model)) row += step;
    return row;
  }

  private boolean canSelect(int row, AListModel<T> model) {
    return !(row < 0 || row >= model.getSize()) && canSelect(model.getAt(row));
  }

  public boolean canSelect(T value) {
    return mySelectable == null || mySelectable.isAccepted(value);
  }

  private void onMouse(EventContext context, AListModel<T> model, ColumnListWidget<T> widget) {
    MouseEventData mouse = context.getData(EventContext.MOUSE);
    HostCell cell = context.getActiveCell();
    if (mouse == null || cell == null) return;
    if (mouse.getEventId() != MouseEvent.MOUSE_PRESSED || mouse.getButton() != MouseEvent.BUTTON1) return;
    int row = findRow(cell, widget, mouse.getY());
    if (!canSelect(row, model)) return;
    setSelected(cell, row);
  }

  private void setSelected(HostCell listCell, int row) {
    if (row < 0) return;
    int selected = listCell.getStateValue(SELECTED_ROW, -1);
    if (selected == row) return;
    int[] bounds = ColumnListWidget.getCurrentRowBounds(listCell, row);
    if (bounds == null) {
      LogHelper.error("Row has no bounds", row, listCell);
      return;
    }
    listCell.putStateValue(SELECTED_ROW, row, true);
    listCell.repaint();
    Rectangle rowRect = listCell.getHostBounds(new Rectangle());
    rowRect.y = bounds[0];
    rowRect.height = bounds[1];
    JComponent hostComponent = listCell.getHost().getHostComponent();
    Rectangle visibleRect = hostComponent.getVisibleRect();
    if (visibleRect.y > rowRect.y || visibleRect.y + visibleRect.height < rowRect.y + rowRect.height)
      UIUtil.ensureRectVisiblePartially(hostComponent, rowRect);
  }

  private int findRow(HostCell cell, ColumnListWidget<?> widget, int y) {
    y += cell.getHostY();
    Rectangle rect = new Rectangle();
    for (HostCell child : cell.getChildrenList()) {
      rect = child.getHostBounds(rect);
      if (rect.y <= y && (rect.y + rect.height) > y) {
        return widget.getRow(child.getId());
      }
    }
    return -1;
  }

  @Override
  public void onActivate(CellContext context, AListModel<T> model, ColumnListWidget<T> widget) {
  }

  @Override
  public void onDeactive() {
  }

  public static boolean isSelected(HostCell descendantCell, ColumnListWidget<?> list) {
    RowState state = RowState.create(descendantCell, list);
    return state != null && state.isSelected();
  }

  public int getSelection(HostCell listCell) {
    ColumnListWidget<?> list = Util.castNullable(ColumnListWidget.class, listCell.getWidget());
    ListSelectionProcessor<?> selection = list != null ? list.getUserData().getUserData(SELECTION) : null;
    LogHelper.assertError(this == selection, "Wrong list cell", listCell, list, selection, this);
    return listCell.getStateValue(SELECTED_ROW, -1);
  }

  public static class PrePaintSelection implements Procedure2<GraphContext, Object> {
    private final ColumnListWidget<?> myList;

    public PrePaintSelection(ColumnListWidget<?> list) {
      myList = list;
    }

    @Override
    public void invoke(GraphContext context, Object value) {
      HostCell cell = context.getActiveCell();
      if (cell == null)
        return;
      if (isSelected(cell, myList)) {
        WidgetHost host = context.getHost();
        context.setColor(host.getWidgetData(IWidgetHostComponent.SELECTION_BACKGROUND));
        context.fillRect(0, 0, context.getWidth(), context.getHeight());
        context.setColor(host.getWidgetData(IWidgetHostComponent.SELECTION_FOREGROUND));
      }
    }
  }

  public static class StateFactory implements Function2<CellContext, Object, CellState> {
    private final ColumnListWidget<?> myList;

    public StateFactory(ColumnListWidget<?> list) {
      myList = list;
    }

    @Override
    public CellState invoke(CellContext context, Object o) {
      HostCell cell = context.getActiveCell();
      if (cell == null) return null;
      boolean focused = context.isFocused();
      RowState rowState = RowState.create(cell, myList);
      boolean selected;
      int row;
      int column;
      if (rowState != null) {
        selected = rowState.isSelected();
        row = rowState.getRow();
        column = rowState.getColumn();
      } else {
        selected = false;
        row = -1;
        column = 0;
      }
      return new ColumnWidgetCellState(context.getHost(), selected, focused, cell, row, column);
    }

    private static class ColumnWidgetCellState extends ComponentCellState {
      private final WidgetHost myHost;
      private final boolean mySelected;
      private final boolean myFocused;
      private final int myRow;
      private final int myColumn;
      private final HostCell myCell;

      protected ColumnWidgetCellState(WidgetHost host, boolean selected, boolean focused, HostCell cell, int row, int column) {
        super(host.getHostComponent());
        myHost = host;
        mySelected = selected;
        myFocused = focused;
        myCell = cell;
        myRow = row;
        myColumn = column;
      }

      @Override
      public Color getBackground() {
        return getBackground(true);
      }

      @Override
      public Color getBackground(boolean opaque) {
        Color selectionBg = getSelectionBackground();
        return ListCellState.getBackground(mySelected, true, opaque, selectionBg, getComponent().getBackground());
      }

      @NotNull
      @Override
      public Color getForeground() {
        Color selectionForeground = myHost.getWidgetData(IWidgetHostComponent.SELECTION_FOREGROUND);
        return ListCellState.getForeground(true, mySelected, selectionForeground, getComponent().getForeground());
      }

      @Override
      public Border getBorder() {
        return ListCellState.getBorder(myFocused);
      }

      @Override
      public Color getSelectionBackground() {
        return myHost.getWidgetData(IWidgetHostComponent.SELECTION_BACKGROUND);
      }

      @Override
      public boolean isExpanded() {
        return false;
      }

      @Override
      public boolean isFocused() {
        return myFocused;
      }

      @Override
      public boolean isLeaf() {
        return true;
      }

      @Override
      public boolean isSelected() {
        return mySelected;
      }

      @Override
      public int getCellColumn() {
        return myColumn;
      }

      @Override
      public int getCellRow() {
        return myRow;
      }

      @Override
      public int getComponentCellWidth() {
        return myCell.getWidth();
      }

      @Override
      public int getComponentCellHeight() {
        return myCell.getHeight();
      }
    }
  }

  public static class RowState {
    private final ColumnListWidget<?> myWidget;
    private final HostCell myListCell;
    /**
     * Direct child of myListCell, identifies the row (row may consist of several cells any is suitable)
     */
    private final HostCell myRowCell;

    public RowState(ColumnListWidget<?> list, HostCell listCell, HostCell rowCell) {
      myWidget = list;
      myListCell = listCell;
      myRowCell = rowCell;
    }

    public static RowState create(HostCell descendantCell, ColumnListWidget<?> list) {
      if (descendantCell == null) return null;
      ListSelectionProcessor<?> processor = list.getUserData().getUserData(SELECTION);
      if (processor == null) return null;
      HostCell listCell = descendantCell.getParent();
      HostCell childCell = descendantCell;
      while (listCell != null) {
        if (listCell.getWidget() == list) break;
        childCell = listCell;
        listCell = listCell.getParent();
      }
      if (listCell == null) {
        LogHelper.error("No ancestor list", descendantCell, list);
        return null;
      }
      return new RowState(list, listCell, childCell);
    }

    public boolean isSelected() {
      int selected = myListCell.getStateValue(SELECTED_ROW, -1);
      return selected >=0 && selected == getRow();
    }

    public int getRow() {
      return myWidget.getRow(myRowCell.getId());
    }

    public int getColumn() {
      return myWidget.getColumn(myRowCell.getId());
    }
  }
}
