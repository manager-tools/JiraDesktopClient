package com.almworks.util.components;

import com.almworks.util.advmodel.*;
import com.almworks.util.commons.Procedure;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.dnd.*;
import com.almworks.util.ui.actions.globals.GlobalData;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class BaseAList<T> extends ScrollableWrapper<JListAdapter> implements FlatCollectionComponent<T>, DndTarget,
  DropHintProvider<ListDropHint, JListAdapter>
{
  private final Lifecycle mySwingLife = new Lifecycle(false);
  protected final SelectionAccessor<T> mySelection = new ListSelectionAccessor<T>(this);
  private PrefSizePolicy myPrefsizePolicy = PrefSizePolicy.DEFAULT;
  private final Lifecycle myPrefSizePolicyLife = new Lifecycle();
  protected AListModelDecorator<T> myModelDecorator;
  protected final DndHelper<ListDropHint, JListAdapter> myDndHelper;
  private Detach myFollowLastEntryDetach = Detach.NOTHING;

  protected BaseAList(ListModelHolder<T> holder) {
    super(new JListAdapter(new ListModelAdapter<T>(holder)));
    myDndHelper = new DndHelper<ListDropHint, JListAdapter>(this);
    ListSelectionModelAdapter.createListening(holder, getSelectionModel(), false);
    SelectionDataProvider.installTo(this);
    UIUtil.addPopupTriggerListener(this);
  }

  public ListSelectionModel getSelectionModel() {
    return getScrollable().getSelectionModel();
  }

  public AListModel<T> getCollectionModel() {
    return getModelHolder();
  }

  public int getElementIndexAt(int x, int y) {
    return getScrollable().locationToIndex(new Point(x, y));
  }

  @Nullable
  public T getElementAt(Point point) {
    return AComponentUtil.getElementAtPoint(this, point);
  }

  public int getScrollingElementAt(int x, int y) {
    return getElementIndexAt(x, y);
  }

  public Rectangle getElementRect(int elementIndex) {
    Rectangle cellBounds = getScrollable().getCellBounds(elementIndex, elementIndex);
    assert cellBounds != null : "index:" + elementIndex + " modeSize:" + getCollectionModel().getSize();
    return cellBounds;
  }

  public void scrollSelectionToView() {
    int index = getSelectionAccessor().getSelectedIndex();
    if (index == -1)
      return;
    UIUtil.ensureRectVisiblePartially(this, getScrollable().getCellBounds(index, index));
  }

  public void setDataRoles(DataRole... roles) {
    SelectionDataProvider.setRoles(this, roles);
  }

  public void addGlobalRoles(DataRole<?>... roles) {
    SelectionDataProvider.addRoles(this, roles);
    GlobalData.KEY.addClientValue(this.toComponent(), roles);
  }

  public int getSelectedIndex() {
    return getScrollable().getSelectedIndex();
  }

  /**
   * @see javax.swing.JList#setSelectionMode(int) 
   */
  public void setSelectionMode(int mode) {
    getScrollable().setSelectionMode(mode);
  }

  public Detach addListSelectionListener(final ListSelectionListener listener) {
    final ListSelectionModel model = getSelectionModel();
    model.addListSelectionListener(listener);
    return new Detach() {
      protected void doDetach() {
        model.removeListSelectionListener(listener);
      }
    };
  }

  public void setSelectionIndex(int index) {
    ListSelectionModel model = getSelectionModel();
    model.addSelectionInterval(index, index);
  }

  protected ListModelHolder<T> getModelHolder() {
    return (ListModelHolder<T>) getModelAdapter().getModel();
  }

  private ListModelAdapter<T> getModelAdapter() {
    return ((ListModelAdapter<T>) getScrollable().getModel());
  }

  @NotNull
  public SelectionAccessor<T> getSelectionAccessor() {
    return mySelection;
  }

  public JComponent toComponent() {
    return this;
  }

  public void setEnabled(boolean enabled) {
    JList list = getScrollable();
    list.setEnabled(enabled);
    if (list.isOpaque()) {
      list.setBackground(enabled ? UIManager.getColor("List.background") : AwtUtil.getPanelBackground());
    }
  }

  public void setBackground(Color bg) {
    getScrollable().setBackground(bg);
  }

  public void setPrefSizePolicy(@NotNull PrefSizePolicy policy) {
    myPrefSizePolicyLife.cycle();
    myPrefsizePolicy = policy;
    myPrefsizePolicy.attach(myPrefSizePolicyLife.lifespan(), this);
  }

  public Dimension getPreferredScrollableViewportSize() {
    return myPrefsizePolicy.getSize(this);
  }

  /**
   * Sets "follow last entry" property. When set, a new item that is appended to the end of the list will be
   * automatically scrolled to.
   */
  public void setFollowLastEntry(boolean followLastEntry) {
    if (followLastEntry) {
      if (myFollowLastEntryDetach != Detach.NOTHING)
        return;
      final ListModelHolder<T> model = getModelHolder();
      myFollowLastEntryDetach = model.addListener(new AListModel.Adapter() {
        public void onInsert(int index, int length) {
          int size = model.getSize();
          if (size <= index + length)
            scrollToLastEntry();
        }
      });
      scrollToLastEntry();
    } else {
      myFollowLastEntryDetach.detach();
      myFollowLastEntryDetach = Detach.NOTHING;
    }
  }

  public boolean getFollowLastEntry() {
    return myFollowLastEntryDetach != Detach.NOTHING;
  }

  public void scrollToLastEntry() {
    final JList list = getScrollable();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        int last = list.getModel().getSize() - 1;
        if (last >= 0) {
          Rectangle bounds = list.getCellBounds(last, last);
          if (bounds != null)
            list.scrollRectToVisible(bounds);
        }
      }
    });
  }

  public Rectangle getCellBounds(int index) {
    JList list = getScrollable();
    return list.getCellBounds(index, index);
  }

  public void setLayoutOrientation(int orientation) {
    getScrollable().setLayoutOrientation(orientation);
  }

  public void setVisibleRowCount(int rowCount) {
    getScrollable().setVisibleRowCount(rowCount);
  }

  public int getVisibleRowCount() {
    return getScrollable().getVisibleRowCount();
  }

  public void setPrototypeCellValue(T prototype) {
    getScrollable().setPrototypeCellValue(prototype);
  }

  public void setFont(Font font) {
    getScrollable().setFont(font);
  }

  public Font getFont() {
    return getScrollable().getFont();
  }

  public JListAdapter getScrollable() {
    return super.getScrollable();
  }

  public void addDoubleClickListener(Lifespan life, final CollectionCommandListener<T> listener) {
    if (life.isEnded())
      return;
    final JList list = (JList) getSwingComponent();
    final MouseAdapter mouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1 && e.getButton() == MouseEvent.BUTTON1) {
          Point point = e.getPoint();
          int index = list.locationToIndex(point);
          if (index >= 0 && index < list.getModel().getSize()) {
            Rectangle bounds = list.getCellBounds(index, index);
            if (bounds != null && bounds.contains(point)) {
              T element = getCollectionModel().getAt(index);
              listener.onCollectionCommand(BaseAList.this, index, element);
            }
          }
        }
      }
    };
    list.addMouseListener(mouseListener);
    life.add(new Detach() {
      protected void doDetach() {
        list.removeMouseListener(mouseListener);
      }
    });
  }

  public void addKeyCommandListener(Lifespan lifespan, final CollectionCommandListener<T> listener, final int keyCode) {
    if (lifespan.isEnded())
      return;
    final JList list = (JList) getSwingComponent();
    final KeyAdapter keyListener = new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == keyCode && (e.getModifiersEx() &
          (KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) == 0)
        {
          int index = list.getSelectedIndex();
          if (index >= 0 && index < list.getModel().getSize()) {
            T element = getCollectionModel().getAt(index);
            listener.onCollectionCommand(BaseAList.this, index, element);
          }
        }
      }
    };
    list.addKeyListener(keyListener);
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        list.removeKeyListener(keyListener);
      }
    });
  }

  public void setTransfer(ContextTransfer transfer) {
    super.setTransfer(transfer);
    if (!GraphicsEnvironment.isHeadless())
      getScrollable().setDragEnabled(true);
    if (isDisplayable()) {
      registerInDndManager();
    }
  }

  private void registerInDndManager() {
    DndManager dndManager = DndManager.require();
    Lifespan lifespan = mySwingLife.lifespan();
    dndManager.registerSource(lifespan, this);
    dndManager.registerTarget(lifespan, this);
  }

  public void addNotify() {
    super.addNotify();
    if (mySwingLife.cycleStart()) {
      if (getTransfer() != null) {
        registerInDndManager();
      }
    }
  }

  public void removeNotify() {
    mySwingLife.cycleEnd();
    super.removeNotify();
  }

  public JComponent getTargetComponent() {
    return getSwingComponent();
  }

  public void dragNotify(DndEvent event) {
    myDndHelper.dragNotify(event, getTransfer(), (JListAdapter) getSwingComponent());
  }

  public boolean prepareDropHint(JListAdapter component, Point p, DragContext context, ContextTransfer transfer) {
    Rectangle r;
    Container parent = getParent();
    if (parent instanceof JViewport) {
      r = ((JViewport) parent).getViewRect();
    } else {
      r = getBounds();
      r.x = r.y = 0;
    }
    if (!r.contains(p)) {
      return cleanDropPoint(context);
    }

    int row = component.locationToIndex(new Point(1, p.y));
    AListModel<? extends T> model = getCollectionModel();
    if (row >= model.getSize())
      row = -1;
    // only insert supported now
    boolean insert = true;
    if (row < 0) {
      row = 0;
    } else {
      Rectangle bounds = component.getCellBounds(row, row);
      if (bounds == null) {
        row = 0;
      } else {
        int mid = bounds.y + (bounds.height >> 1);
        if (p.y > mid) {
          // insert after
          row++;
        }
      }
    }

    ListDropPoint lastPoint = context.getValue(DndUtil.LIST_DROP_POINT);
    if (row < 0 && lastPoint == null) {
      return false;
    }
    if (row < 0) {
      context.putValue(DndUtil.LIST_DROP_POINT, null);
      return true;
    }

    ListDropPoint newPoint = new ListDropPoint(this, null, row, insert);
    boolean result = !Util.equals(newPoint, lastPoint);
    if (result) {
      context.putValue(DndUtil.LIST_DROP_POINT, newPoint);
    }
    return result;
  }

  public ListDropHint createDropHint(JListAdapter component, DragContext context) {
    ListDropPoint point = context.getValue(DndUtil.LIST_DROP_POINT);
    return point == null || !point.isValid() ? null : new ListDropHint(point);
  }

  public void cleanContext(DragContext context) {
    cleanDropPoint(context);
  }

  private boolean cleanDropPoint(DragContext context) {
    if (context != null) {
      ListDropPoint point = context.getValue(DndUtil.LIST_DROP_POINT);
      if (point != null && point.getList() == this) {
        context.putValue(DndUtil.LIST_DROP_POINT, null);
        return true;
      }
    }
    return false;
  }

  public Detach setCollectionModel(AListModel<? extends T> model) {
    return setCollectionModel(model, false);
  }

  public void setModelDecorator(AListModelDecorator<T> decorator) {
    AListModel<? extends T> currentModel;
    if (myModelDecorator != null) {
      currentModel = myModelDecorator.getSource();
      myModelDecorator.setSource(null);
      myModelDecorator = null;
    } else
      currentModel = getModelHolder().getModel();
    if (decorator != null) {
      myModelDecorator = decorator;
      decorator.setSource(currentModel);
      getModelHolder().setModel(decorator);
    } else
      getModelHolder().setModel(currentModel);
  }

  public Detach setCollectionModel(AListModel<? extends T> model, boolean keepSelection) {
    java.util.List<T> selected = null;
    if (keepSelection) {
      selected = mySelection.getSelectedItems();
      mySelection.setEventsInhibited(true);
    }
    try {
      if (myModelDecorator != null) {
        assert getModelHolder().getModel() == myModelDecorator;
        return myModelDecorator.setSource(model);
      } else {
        return getModelHolder().setModel(model);
      }
    } finally {
      if (keepSelection) {
        mySelection.setSelected(selected);
        mySelection.setEventsInhibited(false);
        mySelection.fireSelectionChanged();
        if (mySelection.getSelectedCount() > 0) {
          mySelection.fireSelectedItemsChanged();
          scrollSelectionToView();
        }
      }
    }
  }

  protected ListCellRenderer getSwingListRenderer() {
    return getScrollable().getCellRenderer();
  }

  /**
   * Sets whether Swing's default JList speed search is used
   * for this list; {@code true} by default.
   * @param b Use Swing's default speed search?
   */
  public void setSwingSpeedSearchEnabled(boolean b) {
    ((JListAdapter)getScrollable()).setGetNextMatchOverridden(!b);
  }

  @NotNull
  public Detach getClearModelDetach() {
    return new Detach() {
      protected void doDetach() throws Exception {
        setCollectionModel(null, false);
      }
    };
  }

  public abstract void setCanvasRenderer(CanvasRenderer<? super T> renderer);

  public abstract CanvasRenderer<? super T> getCanvasRenderer();

  public void setCellRenderer(CollectionRenderer<? super T> renderer) {
    getScrollable().setCellRenderer(new ListCellRendererAdapter<T>(renderer));
  }

  public interface PrefSizePolicy {
    AList.PrefSizePolicy DEFAULT = new AList.PrefSizePolicy() {
      public Dimension getSize(BaseAList<?> component) {
        JList list = component.getScrollable();
        Object prototype = list.getPrototypeCellValue();
        Dimension size = list.getPreferredScrollableViewportSize();
        if (prototype == null)
          return size;
        Component renderer = list.getCellRenderer().getListCellRendererComponent(list, prototype, 0, false, false);
        int rows = list.getVisibleRowCount();
        return new Dimension(size.width, rows * renderer.getPreferredSize().height);
      }

      public void attach(Lifespan life, BaseAList<?> component) {
      }
    };

    AList.PrefSizePolicy FULL_MODEL = new AList.PrefSizePolicy.FullModel();


    public class FullModel implements AList.PrefSizePolicy {
      private static final ComponentProperty<Dimension> CALCULATED_SIZE = ComponentProperty.createProperty("wholeSize");
      private static final ComponentProperty<java.util.List<Dimension>> CELL_SIZES =
        ComponentProperty.createProperty("cellSizes");
      private final int myMaxWidth;

      public FullModel() {
        this(0);
      }

      public FullModel(int maxWidth) {
        myMaxWidth = maxWidth;
      }

      public Dimension getSize(BaseAList<?> component) {
        Dimension size = CALCULATED_SIZE.getClientValue(component);
        if (size != null)
          return size;
        updateCellSizes(component);
        Dimension result = new Dimension();
        java.util.List<Dimension> sizes = CELL_SIZES.getClientValue(component);
        for (Dimension dimension : sizes) {
          result.height += dimension.height;
          result.width = Math.max(result.width, dimension.width);
        }
        if (myMaxWidth > 0) {
          if (result.width > myMaxWidth) {
            result.width = myMaxWidth;
            int w = UIManager.getInt("ScrollBar.width");
            if (w <= 0)
              w = 16;
            result.height += w;
          }
        }
        CALCULATED_SIZE.putClientValue(component, result);
        return result;
      }

      private <T> void updateCellSizes(BaseAList<T> list) {
        AListModel<T> model = list.getCollectionModel();
        java.util.List<Dimension> sizes = CELL_SIZES.getClientValue(list);
        int modelSize = model.getSize();
        for (int i = sizes.size() - 1; i >= modelSize; i--)
          sizes.remove(i);
        for (int i = sizes.size(); i < modelSize; i++)
          sizes.add(null);
        assert sizes.size() == modelSize;
        ListCellRenderer adapter = list.getSwingListRenderer();
        for (int i = 0; i < modelSize; i++) {
          Dimension cellSize = sizes.get(i);
          if (cellSize != null)
            continue;
          T item = model.getAt(i);
          Component component = adapter.getListCellRendererComponent(list.getScrollable(), item, i, false, true);
          sizes.set(i, component.getPreferredSize());
        }
      }

      public void attach(Lifespan life, BaseAList<?> component) {
        CALCULATED_SIZE.remove(life, component);
        CELL_SIZES.putClientValue(life, component, Collections15.<Dimension>arrayList());
      }
    }

    Dimension getSize(BaseAList<?> component);

    void attach(Lifespan life, BaseAList<?> component);
  }

  public static Procedure<Integer> createEnsureIndexVisible(final JList list) {
    return new Procedure<Integer>() {
      public void invoke(Integer arg) {
        list.ensureIndexIsVisible(arg);
      }
    };
  }
}
