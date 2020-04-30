package com.almworks.items.gui.meta;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.order.*;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.DBCommons;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.cache.util.AttributeReference;
import com.almworks.items.cache.util.ItemAttribute;
import com.almworks.items.cache.util.ItemSetModel;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.applicability.Applicability;
import com.almworks.items.gui.meta.schema.applicability.SatisfyAll;
import com.almworks.items.gui.meta.schema.reorders.Reorders;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.util.BadUtil;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.models.TableColumnAccessor;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class OrdersCollector implements ItemSetModel.ItemWrapperFactory<OrdersCollector.LoadedReorder> {
  private static final AttributeLoader<String> ID = AttributeLoader.create(Reorders.ID);
  private static final AttributeLoader<String> DISPLAY_NAME = AttributeLoader.create(Reorders.DISPLAY_NAME);
  private static final ItemAttribute MODEL_KEY = new ItemAttribute(Reorders.MODEL_KEY);
  private static final ItemAttribute COLUMN = new ItemAttribute(Reorders.COLUMN);
  private static final AttributeReference<?> ATTRIBUTE = AttributeReference.create(Reorders.ATTRIBUTE, null, DBAttribute.ScalarComposition.SCALAR);
  private static final SerializedObjectAttribute<Applicability> APPLICABILITY = SerializedObjectAttribute.create(Applicability.class, Reorders.APPLICABILITY);
  private final TLongObjectHashMap<LoadedReorder> myReorders = new TLongObjectHashMap<>();
  private final ItemSetModel<LoadedReorder> myModel;
  private final GuiFeaturesManager myManager;

  public OrdersCollector(DBImage image, GuiFeaturesManager manager) {
    myManager = manager;
    myModel = new ItemSetModel<LoadedReorder>(image.createQuerySlice(DPEqualsIdentified.create(DBAttribute.TYPE, Reorders.DB_TYPE)), this);
  }

  public void start(Lifespan life) {
    ImageSlice slice = myModel.getSlice();
    slice.addData(DBCommons.OWNER, ID, DISPLAY_NAME, MODEL_KEY, COLUMN, ATTRIBUTE, APPLICABILITY);
    slice.ensureStarted(life);
    myModel.start(life);
  }

  @Override
  public LoadedReorder getForItem(ImageSlice slice, long item) {
    LoadedReorder reorder = myReorders.get(item);
    if (reorder == null) {
      String id = slice.getValue(item, ID);
      Long owner = slice.getNNValue(item, DBCommons.OWNER, 0l);
      if (id == null || owner <= 0) return null;
      reorder = new LoadedReorder(id, owner);
      update(item, reorder);
      myReorders.put(item, reorder);
    }
    return reorder;
  }

  @Override
  public void afterChange(LongList removed, LongList changed, LongList added) {
    for (LongIterator it : removed) myReorders.remove(it.value());
    for (LongIterator it : changed) {
      long item = it.value();
      LoadedReorder reorder = myReorders.get(item);
      if (reorder != null) update(item, reorder);
    }
  }

  private void update(long item, LoadedReorder reorder) {
    ImageSlice slice = myModel.getSlice();
    long modelKey = slice.getNNValue(item, MODEL_KEY, 0l);
    long column = slice.getNNValue(item, COLUMN, 0l);
    String displayName = slice.getValue(item, DISPLAY_NAME);
    DBAttribute<?> attribute = slice.getValue(item, ATTRIBUTE);
    Applicability applicability = slice.getValue(item, APPLICABILITY);
    reorder.update(myManager, modelKey, column, attribute, displayName, applicability);
  }

  public AListModel<LoadedReorder> getModel() {
    return myModel;
  }

  public static class LoadedReorder implements Order {
    private final String myId;
    private final long myOwner;
    private volatile String myDisplayName;
    private volatile NumericAttributeOrder<?> myDelegate;
    private volatile Applicability myApplicability = SatisfyAll.SATISFY_ANY;

    public LoadedReorder(String id, long owner) {
      myId = id;
      myOwner = owner;
    }

    @Override
    public String getId() {
      return myId;
    }

    @Override
    public String getDisplayName() {
      String displayName = myDisplayName;
      return displayName != null ? displayName : getId();
    }

    @Override
    public TableColumnAccessor<ReorderItem, ?> getColumn() {
      NumericAttributeOrder<?> order = getOrder();
      return order != null ? order.getColumn() : null;
    }

    @Override
    public boolean canOrder(ItemWrapper item) {
      if (item == null) return false;
      Connection connection = item.getConnection();
      Applicability applicability = myApplicability;
      return applicable(connection) && (applicability == null || applicability.isApplicable(item));
    }

    public boolean applicable(Connection connection) {
      if (connection == null) return false;
      if (myOwner == connection.getConnectionItem()) return true;
      return connection.getProviderItem() == myOwner;
    }

    @Override
    public void updateOrder(List<ReorderItem> list, int[] indices) {
      NumericAttributeOrder<?> order = getOrder();
      if (order != null) order.updateOrder(list, indices);
    }

    @Override
    public Comparator<LoadedItem> getComparator() {
      NumericAttributeOrder<?> order = getOrder();
      return order != null ? order.getComparator() : Containers.<LoadedItem>noOrder();
    }

    @Override
    public void updateItems(DBDrain drain, List<ReorderItem> items) {
      NumericAttributeOrder<?> order = getOrder();
      if (order != null) order.updateItems(drain, items);
    }

    private void updateString(ModelKey<String> key, TableColumnAccessor<LoadedItem, ?> column, DBAttribute<String> attribute) {
      myDelegate = new StringAttributeOrder(key, column, attribute);
    }

    private void updateNumber(ModelKey<BigDecimal> key, TableColumnAccessor<LoadedItem, ?> column, DBAttribute<BigDecimal> attribute) {
      myDelegate = new BigDecimalAttributeOrder(key, column, attribute);
    }

    public boolean update(GuiFeaturesManager feature, long modelKey, long columnItem, DBAttribute<?> attribute, String displayName, Applicability applicability) {
      if (attribute == null) {
        myDelegate = null;
        return true;
      }
      myDisplayName = displayName;
      myApplicability = Util.NN(applicability, SatisfyAll.SATISFY_ANY);
      LoadedModelKey<?> mk = feature.getModelKeyCollector().getKey(modelKey);
      TableColumnAccessor<LoadedItem, ?> column = feature.getColumns().get(columnItem);
      if (column == null || mk == null) {
        myDelegate = null;
        return true;
      }
      DBAttribute<String> string = BadUtil.castScalar(String.class, attribute);
      if (string != null) {
        LoadedModelKey<String> key = mk.castScalar(String.class);
        if (key == null) myDelegate = null;
        else updateString(key, column, string);
        return true;
      }
      DBAttribute<BigDecimal> number = BadUtil.castScalar(BigDecimal.class, attribute);
      if (number != null) {
        LoadedModelKey<BigDecimal> key = mk.castScalar(BigDecimal.class);
        if (key == null) myDelegate = null;
        else updateNumber(key, column, number);
        return true;
      }
      myDelegate = null;
      return true;
    }

    @Nullable
    private NumericAttributeOrder<?> getOrder() {
      return myDelegate;
    }

    @Override
    public String toString() {
      return "LoadedReorder@" + myDisplayName;
    }
  }
}
