package com.almworks.util.ui.widgets;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Widget is a behaviour of light-weigth component. The "instance" of widget is {@link com.almworks.util.ui.widgets.HostCell}.
 * Widget handles lifecycle and stores state in cell.<br><br>
 * <strong>Life cycle</strong><br>
 * Each widget can be attached to zero or more hosts. Host controls "static lifecycle" - time when widget can be used by host.<br>
 * <br><strong>Static lifecycle</strong><br>
 * Static life cycle starts with {@link com.almworks.util.ui.widgets.WidgetAttach#attach(WidgetHost)}. After lifecycle is started it is
 * guaranteed that lifecycle is ended in future. {@link com.almworks.util.ui.widgets.WidgetAttach#detach(WidgetHost)} notifiers that
 * given host doesn't requires the widget any more.
 * Static lifecycle can be used to perform expencive initialization and deinitialization. Note that after widget is detached from all
 * hosts it may (or may not) be attached to one another again in future.<br>
 * <br><strong>Cell lifecycle</strong><br>
 * When host allocate viewable part for widget it creates cell ({@link com.almworks.util.ui.widgets.HostCell}.
 * Cell has lifecycle. It starts with call to {@link com.almworks.util.ui.widgets.CellActivate#activate(HostCell)} and ends with
 * {@link com.almworks.util.ui.widgets.CellActivate#deactivate(HostCell, javax.swing.JComponent)}. Once lifecycle is started
 * it is going to be ended in future. During lifecycle widget can safely store any state in cell. All permanent state won't be
 * lost until cell deactivated. 
 * @param <V> type of value this widget can process
 */
public interface Widget<V> {
  /**
   * Calculates prefered width to show given value.<br>
   * Context may be not active.<br>
   * If one requires "default" prefered width it passes null value as argument
   */
  int getPreferedWidth(@NotNull CellContext context, @Nullable V value);

  /**
   * Calculates prefered height to show given value, width is fixed to <code>width</code>
   * Context may be not active.<br>
   * If one requires "default" prefered height it passes null value as argument
   */
  int getPreferedHeight(@NotNull CellContext context, int width, @Nullable V value);

  /**
   * Paint given value
   */
  void paint(@NotNull GraphContext context, @Nullable V value);

  /**
   * Process event.
   * @param reason kind of event. Use {@link com.almworks.util.ui.widgets.EventContext#getData(org.almworks.util.TypedKey)}
   * to obtain additional data
   * @see com.almworks.util.ui.widgets.EventContext
   * @see com.almworks.util.ui.widgets.MouseEventData
   * @see com.almworks.util.ui.widgets.FocusTraverse
   */
  void processEvent(@NotNull EventContext context, @Nullable V value, TypedKey<?> reason);

  /**
   * Convert own value to value for child cell identified by <code>id</code>
   * @param cellId child cell identifier
   * @param value own value
   * @return child cell value
   */
  @Nullable
  Object getChildValue(@NotNull CellContext context, int cellId, @Nullable V value);

  /**
   * Widget should layout child cells. It can layout all or only visible cells (cells that intersects target rectangle).
   * If widget lays out only visible cells it can be asked later to layout some invisible cells if they happen to be
   * active for some reason other then visibility.<br>
   * All cells not layout by widget are treated as removed from visible area. If the cell is useful for other purpose host
   * asks widget to layout the particular cell.
   * @param cell if not null only one cell is required to be layed out
   * @see com.almworks.util.ui.widgets.LayoutContext
   */
  void layout(LayoutContext context, V value, @Nullable ModifiableHostCell cell);

  /**
   * @return attach to host feature
   * @see com.almworks.util.ui.widgets.WidgetAttach
   */
  @Nullable
  WidgetAttach getAttach();

  /**
   * @return active cell tracker
   * @see com.almworks.util.ui.widgets.CellActivate
   */
  @Nullable
  CellActivate getActivate();

  /**
   * UI properties probably are changed. Update cached values and request revalidation if needed
   * @param cell
   * @see HostCell#invalidate()
   * @see HostCell#repaint()
   */
  void updateUI(HostCell cell);

  /**
   * Null widget. Does nothing can be used as null object
   */
  Widget<Object> EMPTY_WIDGET = new Widget<Object>() {
    @Override
    public int getPreferedWidth(@NotNull CellContext context, @Nullable Object value) {
      return 0;
    }

    @Override
    public int getPreferedHeight(@NotNull CellContext context, int width, @Nullable Object value) {
      return 0;
    }

    @Override
    public void paint(@NotNull GraphContext context, @Nullable Object value) {}

    @Override
    public void processEvent(@NotNull EventContext context, @Nullable Object value, TypedKey<?> reason) {}

    @Override
    public Object getChildValue(@NotNull CellContext context, int cellId, @Nullable Object value) {
      return value;
    }

    @Override
    public void layout(LayoutContext context, Object value, @Nullable ModifiableHostCell cell) {}

    @Nullable
    @Override
    public WidgetAttach getAttach() {
      return null;
    }

    @Nullable
    @Override
    public CellActivate getActivate() {
      return null;
    }

    @Override
    public void updateUI(HostCell cell) {}
  };
}
