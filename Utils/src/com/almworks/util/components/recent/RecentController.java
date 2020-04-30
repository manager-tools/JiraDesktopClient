package com.almworks.util.components.recent;

import com.almworks.util.Env;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.BrokenLineBorder;
import com.almworks.util.ui.ColorUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.List;
import java.util.*;

public class RecentController<T> implements CanvasRenderer<T> {
  private static final int MAX_RECENT = Env.getInteger("form.recents.max", 5);
  private static final int RECENT_THRESHOLD = 6;
  private static final int RECENT_PER_COUNT = 4;
  private static final int MIN_RECENT = 2;
  private static final String SUBSET_ITEM = "item";
  private static final String SETTING_ORDER = "order";
  private static final String SETTING_ID = "id";
  
  private final SegmentedListModel<Object> myDecoratedModel;
  private final OrderListModel<Object> myRecentModel = new OrderListModel<Object>();
  private final List<T> myRecent = Collections15.arrayList();
  private final Lifecycle mySourceModelLife = new Lifecycle();
  private final AListModel.Adapter mySourceListener;
  private Convertor<? super T, String> myIdentityConvertor;
  @Nullable
  private Configuration myConfig;
  private CanvasRenderer myRenderer = null;
  private T myInitial = null;
  private boolean myWrapRecents;
  public static final Convertor UNWRAPPER = new Convertor() {
    @Override
    public Object convert(Object value) {
      return unwrap(value);
    }
  };

  public RecentController() {
    this(null, false);
  }

  public RecentController(AListModel<? extends T> model, boolean wrapRecents) {
    if (model == null) model = AListModel.EMPTY;
    myWrapRecents = wrapRecents && !Aqua.isAqua();
    myDecoratedModel = SegmentedListModel.create(myRecentModel, model);
    mySourceListener = new AListModel.Adapter() {
      @Override
      public void onInsert(int index, int length) {
        updateRecentModel();
      }

      @Override
      public void onRemove(int index, int length, AListModel.RemovedEvent event) {
        updateRecentModel();
      }
    };
    model.addListener(mySourceListener);
  }

  public CanvasRenderer getDecoratedRenderer() {
    return this;
  }

  public AListModel<T> getDecoratedModel() {
    return (AListModel<T>) myDecoratedModel;
  }

  public void setRenderer(CanvasRenderer renderer) {
    myRenderer = renderer;
    myDecoratedModel.updateAll();
  }

  public void setIdentityConvertor(Convertor<? super T, String> identityConvertor) {
    myIdentityConvertor = identityConvertor;
    restoreRecents();
  }

  public void setWrapRecents(boolean wrapRecents) {
    myWrapRecents = wrapRecents && !Aqua.isAqua();
    updateRecentModel();
  }

  public void setConfig(Configuration config) {
    myConfig = config;
    restoreRecents();
  }

  public void setup(AListModel<? extends T> model, @Nullable Configuration config) {
    myConfig = config;
    doSetModel(model);
    restoreRecents();
  }

  public void setInitial(T initial) {
    if (Util.equals(initial, myInitial)) return;
    myInitial = initial;
    updateRecentModel();
  }

  public CanvasRenderer getInnerRenderer() {
    return myRenderer;
  }

  public Detach createDetach() {
    return new Detach() {
      @Override
      protected void doDetach() throws Exception {
        clear();
      }
    };
  }

  private void clear() {
    mySourceModelLife.cycle();
    myDecoratedModel.setSegment(1, AListModel.EMPTY);
    myRecentModel.clear();
    myRecent.clear();
  }

  public void setModel(AListModel<? extends T> model) {
    doSetModel(model);
    restoreRecents();
  }

  private void doSetModel(AListModel<? extends T> model) {
    if (model == null) model = AListModel.EMPTY;
    clear();
    model.addListener(mySourceListener);
    myDecoratedModel.setSegment(1, model);
  }

  private void restoreRecents() {
    myRecent.clear();
    boolean canHasRecents = canHasRecents();
    if (!canHasRecents) return;
    AListModel<? extends T> model = getSourceModel();
    if (model.getSize() == 0) return;
    Map<String, T> restored = Collections15.hashMap();
    final Map<String, Integer> orders = Collections15.hashMap();
    List<Configuration> items = myConfig.getAllSubsets(SUBSET_ITEM);
    for (Configuration item : items) {
      String id = item.getSetting(SETTING_ID, null);
      if (id == null) item.removeMe();
      else {
        restored.put(id, null);
        String strOrder = item.getSetting(SETTING_ORDER, "");
        int intOrder;
        try {
          intOrder = Integer.parseInt(strOrder);
        } catch (NumberFormatException e) {
          intOrder = Integer.MAX_VALUE;
        }
        orders.put(id, intOrder);
      }
    }
    int left = restored.size();
    for (int i = 0; i < model.getSize() && left > 0; i++) {
      T t = model.getAt(i);
      String id = myIdentityConvertor.convert(t);
      if (id == null || !restored.containsKey(id) || restored.get(id) != null) continue;
      restored.put(id, t);
      left--;
    }
    List<Map.Entry<String, T>> entries = Collections15.arrayList(restored.entrySet());
    Collections.sort(entries, new Comparator<Map.Entry<String, T>>() {
      public int compare(Map.Entry<String, T> o1, Map.Entry<String, T> o2) {
        String key1 = o1.getKey();
        String key2 = o2.getKey();
        Integer order1 = orders.get(key1);
        Integer order2 = orders.get(key2);
        int ord1 = order1 != null ? order1 : Integer.MAX_VALUE;
        int ord2 = order2 != null ? order2 : Integer.MAX_VALUE;
        return ord1 < ord2 ? -1 : 1;
      }
    });
    for (Map.Entry<String, T> entry : entries) {
      T t = entry.getValue();
      if (t != null) myRecent.add(t);
    }
    updateRecentModel();
  }

  private boolean canHasRecents() {
    return myIdentityConvertor != null && myConfig != null && myConfig != Configuration.EMPTY_CONFIGURATION;
  }

  public void addToRecent(Object recentOrWrapper) {
    addToRecent(Collections.singleton(RecentController.<T>unwrap(recentOrWrapper)));
  }

  public void addToRecent(Collection<? extends T> recent) {
    if (recent.size() == 0 || !canHasRecents()) return;
    if (recent.size() > MAX_RECENT) {
      List<T> tmp = Collections15.arrayList(recent);
      tmp.subList(MAX_RECENT, recent.size()).clear();
      recent = tmp;
    }
    recent = Collections15.linkedHashSet(recent);
    List<T> current = Collections15.arrayList(myRecent);
    current.removeAll(recent);
    if (current.size() + recent.size() > MAX_RECENT) {
      current.subList(MAX_RECENT - recent.size(), current.size()).clear();
    }
    current.addAll(0, recent);
    if (current.equals(myRecent)) return;
    myRecent.clear();
    myRecent.addAll(current);
    // this class is used form awt thread only, so previous check via canHasRecents is sufficient
    //noinspection ConstantConditions
    myConfig.clear();
    HashSet<String> savedIds = Collections15.hashSet();
    for (int i = 0; i < current.size(); i++) {
      T t = current.get(i);
      String id = myIdentityConvertor.convert(t);
      if (!savedIds.add(id)) continue;
      //noinspection ConstantConditions
      Configuration item = myConfig.createSubset(SUBSET_ITEM);
      item.setSetting(SETTING_ORDER, i);
      item.setSetting(SETTING_ID, id);
    }
    updateRecentModel();
  }

  public void renderStateOn(CellState state, Canvas canvas, T item) {
    item = (T)unwrap(item);
    if (myRenderer != null) myRenderer.renderStateOn(state, canvas, item);
    int row = state.getCellRow();
    int recentCount = myRecentModel.getSize();
    if (row < 0 || recentCount <= row) return;
    if (recentCount - 1 == row) {
      Color lineColor = Aqua.isAqua() ? Aqua.MAC_BORDER_COLOR : state.getForeground();
      BrokenLineBorder line =
        new BrokenLineBorder(lineColor, 1, BrokenLineBorder.SOUTH, BrokenLineBorder.SOLID);
      BrokenLineBorder gap =
        new BrokenLineBorder(state.getDefaultBackground(), 1, BrokenLineBorder.SOUTH, BrokenLineBorder.SOLID);
      CompoundBorder border = new CompoundBorder(line, gap);
      canvas.setCanvasBorder(border);
    }
    if (!Aqua.isAqua() && Util.equals(state.getBackground(), state.getDefaultBackground())) {
      canvas.setCanvasBackground(ColorUtil.between(Color.GREEN, state.getBackground(), 0.9f));
      canvas.setBackground(null);
    }
  }

  public T getInitial() {
    return myInitial;
  }

  private int getVisibleRecentCount() {
    if (!canHasRecents()) return 0;
    AListModel<? extends T> fullModel = getSourceModel();
    int fullSize = fullModel.getSize();
    if (fullSize < RECENT_THRESHOLD) return 0;
    return Math.min(MAX_RECENT, (fullSize - RECENT_THRESHOLD) / RECENT_PER_COUNT + MIN_RECENT);
  }

  public AListModel<? extends T> getSourceModel() {
    return (AListModel<? extends T>) myDecoratedModel.getSegment(1);
  }

  private void updateRecentModel() {
    int count = getVisibleRecentCount();
    if (count == 0) {
      myRecentModel.clear();
      return;
    }
    List<T> visible = count < myRecent.size() ? myRecent.subList(0, count) : myRecent;
    AListModel<T> sourceModel = (AListModel<T>) getSourceModel();
    boolean visibleCopied = false;
    for (int i = 0; i < visible.size(); i++) {
      T t = visible.get(i);
      if (sourceModel.contains(t)) continue;
      if (!visibleCopied) {
        visible = Collections15.arrayList(visible);
        visibleCopied = true;
      }
      visible.remove(i);
      i--;
    }
    if (visible.isEmpty()) {
      myRecentModel.clear();
      return;
    }
    if (myInitial != null && !visible.contains(myInitial) && sourceModel.contains(myInitial)) {
      if (!visibleCopied) {
        visible = Collections15.arrayList(visible);
        visibleCopied = true;
      }
      visible.add(0, myInitial);
    }
    if (visible.size() == myRecentModel.getSize()) {
      boolean diff = false;
      for (int i = 0; i < visible.size(); i++) {
        T t = visible.get(i);
        Object recent = myRecentModel.getAt(i);
        if (recent instanceof RecentWrapper != myWrapRecents) {
          diff = true;
          break;
        }
        recent = unwrap(recent);
        if (!Util.equals(t, recent)) {
          diff = true;
          break;
        }
      }
      if (!diff) return;
    }
    if (myWrapRecents) {
      List<RecentWrapper> wrappers = Collections15.arrayList(visible.size());
      for (T t : visible) {
        wrappers.add(new RecentWrapper(t));
      }
      myRecentModel.setElements(wrappers);
    } else myRecentModel.setElements(visible);
  }

  public void duplicateSelection(Lifespan life, final SelectionAccessor<T> accessor) {
    SelectionDuplicator.install(life, accessor, myDecoratedModel);
  }

  public Object wrap(T item) {
    return new RecentWrapper(item);
  }

  public static <T> T unwrap(Object item) {
    if (item instanceof RecentWrapper) item = ((RecentWrapper) item).myItem;
    return (T) item;
  }

  private static class RecentWrapper {
    private final Object myItem;

    private RecentWrapper(Object item) {
      myItem = item;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof RecentWrapper) && (Util.equals(((RecentWrapper) obj).myItem, myItem));
    }

    @Override
    public int hashCode() {
      return myItem == null ? 0 : myItem.hashCode();
    }

    @Override
    public String toString() {
      return "RecentWrapper[" + myItem + "]";
    }
  }

  public SelectionInListModel<T> setupAComboBox(AComboBox<T> combo, Lifespan life) {
    combo.setCanvasRenderer(getDecoratedRenderer());
    final SelectionInListModel<T> cbModel = SelectionInListModel.create(life, getDecoratedModel(), null);
    combo.setModel(cbModel);
    return cbModel;
  }
}