package com.almworks.items.sync.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.*;
import com.almworks.items.sync.impl.HolderCache;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DatabaseUtil;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class WriterToDrainAdapter implements DBDrain {
  private final DBWriter myWriter;
  private final Map<DBAttribute<?>, Boolean> myIsShadowable = Collections15.hashMap();

  public WriterToDrainAdapter(DBWriter writer) {
    myWriter = writer;
  }

  @Override
  public ItemVersionCreator createItem() {
    long item = myWriter.nextItem();
    return new MyItem(this, item);
  }

  @NotNull
  @Override
  public DBReader getReader() {
    return myWriter;
  }

  @NotNull
  @Override
  public ItemVersion forItem(long item) {
    return item > 0 ? new MyItem(this, item) : new IllegalItem(this, item);
  }

  @NotNull
  @Override
  public ItemVersion forItem(DBIdentifiedObject object) {
    return forItem(materialize(object));
  }

  @Override
  public long findMaterialized(DBIdentifiedObject object) {
    return getReader().findMaterialized(object);
  }

  @NotNull
  @Override
  public List<ItemVersion> readItems(LongList items) {
    return VersionWriterUtil.readItems(this, items);
  }

  @Override
  public <T> List<T> collectValues(DBAttribute<T> attribute, LongList items) {
    return VersionWriterUtil.collectValues(this, attribute, items);
  }

  @Override
  public ItemVersionCreator changeItem(long item) {
    if (item <= 0) throw new DBException("Wrong item " + item);
    return new MyItem(this, item);
  }

  @Override
  public ItemVersionCreator changeItem(DBIdentifiedObject obj) {
    return VersionWriterUtil.changeItem(this, obj);
  }

  @Override
  public ItemVersionCreator changeItem(ItemProxy proxy) {
    return VersionWriterUtil.changeItem(this, proxy);
  }

  @Override
  public long materialize(ItemProxy object) {
    return object != null ? object.findOrCreate(this) : 0;
  }

  @Override
  public long materialize(DBIdentifiedObject object) {
    return object != null ? myWriter.materialize(object) : 0;
  }

  @Override
  public List<ItemVersionCreator> changeItems(LongList items) {
    return VersionWriterUtil.changeItems(this, items);
  }

  @Override
  public void finallyDo(ThreadGate gate, Procedure<Boolean> procedure) {
    myWriter.finallyDo(gate, procedure);
  }

  private boolean isShadowable(DBAttribute<?> attribute) {
    Boolean shadowable = myIsShadowable.get(attribute);
    if (shadowable == null) {
      shadowable = SyncAttributes.isShadowable(attribute, myWriter);
      myIsShadowable.put(attribute, shadowable);
    }
    return shadowable;
  }

  private final LongSet myMayChangeInvisible = new LongSet();
  private final LongSet myCannotChangeInvisible = new LongSet();
  private boolean mayChangeInvisible(long item) {
    if (myMayChangeInvisible.contains(item)) return true;
    if (myCannotChangeInvisible.contains(item)) return false;
    boolean can = true;
    AttributeMap map = getReader().getAttributeMap(item);
    for (DBAttribute<?> attribute : map.keySet()) {
      if (SyncAttributes.INVISIBLE.equals(attribute)) continue;
      if (isShadowable(attribute)) {
        can = false;
        break;
      }
      if (HolderCache.SERVER_SHADOWS.contains(attribute)) {
        can = false;
        break;
      }
    }
    (can ? myMayChangeInvisible : myCannotChangeInvisible).add(item);
    return can;
  }

  private static class MyItem implements ItemVersionCreator {
    private final long myItem;
    private final WriterToDrainAdapter myAdapter;

    private MyItem(WriterToDrainAdapter adapter, long item) {
      myAdapter = adapter;
      myItem = item;
    }

    private DBWriter getWriter() {
      return myAdapter.myWriter;
    }

    // Unsupported
    @Override
    public void markMerged() {
      throw new DBException("Unsupported operation");
    }

    @Override
    public void addHistory(ItemProxy kind, byte[] step) {
      throw new DBException("Unsupported operation");
    }

    // Branch operations
    @NotNull
    @Override
    public ItemVersion readTrunk(long item) {
      return myAdapter.forItem(item);
    }

    @NotNull
    @Override
    public ItemVersion switchToServer() {
      LogHelper.error("Should not happen");
      return this;
    }

    @NotNull
    @Override
    public ItemVersion switchToTrunk() {
      return this;
    }

    // Read values
    @Override
    public boolean equalItem(ItemReference reference) {
      return ItemVersionCommonImpl.equalItem(getReader(), getItem(), reference);
    }

    @NotNull
    @Override
    public HistoryRecord[] getHistory() {
      return ItemVersionCommonImpl.getHistory(this);
    }

    @Override
    public boolean isInvisible() {
      return ItemVersionCommonImpl.isInvisible(getValue(SyncSchema.INVISIBLE));
    }

    @Override
    public boolean isAlive() {
      return !isInvisible() && Boolean.TRUE.equals(getValue(SyncAttributes.EXISTING));
    }

    @Override
    public boolean equalValue(DBAttribute<Long> attribute, DBIdentifiedObject object) {
      return SyncUtils.equalValue(getReader(), getValue(attribute), object);
    }

    @Override
    public <T> T mapValue(DBAttribute<Long> attribute, Map<? extends DBIdentifiedObject, T> map) {
      return SyncUtils.mapValue(getReader(), getValue(attribute), map);
    }

    @Override
    public ItemVersion readValue(DBAttribute<Long> attribute) {
      Long value = getValue(attribute);
      return value != null ? forItem(value) : null;
    }

    @Override
    public long getItem() {
      return myItem;
    }

    @Override
    public long getIcn() {
      return getReader().getItemIcn(getItem());
    }

    @Override
    public <T> T getValue(DBAttribute<T> attribute) {
      return getReader().getValue(myItem, attribute);
    }

    @Override
    public <T> T getNNValue(DBAttribute<T> attribute, T nullValue) {
      return VersionWriterUtil.getNNValue(this, attribute, nullValue);
    }

    @Override
    public AttributeMap getAllShadowableMap() {
      AttributeMap map = getReader().getAttributeMap(getItem());
      return SyncSchema.filterShadowable(getReader(), map);
    }

    @Override
    public AttributeMap getAllValues() {
      return getReader().getAttributeMap(myItem);
    }

    @Override
    public LongList getSlavesRecursive() {
      return SyncUtils.getSlavesSubtree(getReader(), getItem());
    }

    @NotNull
    @Override
    public LongList getLongSet(DBAttribute<? extends Collection<? extends Long>> attribute) {
      return ItemVersionCommonImpl.getLongSet(getValue(attribute));
    }

    @NotNull
    @Override
    public LongArray getSlaves(DBAttribute<Long> masterReference) {
      return ItemVersionCommonImpl.getSlaves(getReader(), getItem(), masterReference);
    }

    @NotNull
    @Override
    public SyncState getSyncState() {
      LogHelper.error("Should not happen", myItem);
      return SyncState.SYNC;
    }

    // Set values
    @Override
    public void setAlive() {
      VersionWriterUtil.setAlive(this);
    }

    @Override
    public void delete() {
      VersionWriterUtil.delete(this);
    }

    @Override
    public <T> ItemVersionCreator setValue(DBAttribute<T> attribute, @Nullable T value) {
      if (myAdapter.isShadowable(attribute)) {
        T prevValue = getValue(attribute);
        setShadowableValue(attribute, prevValue, value);
      } else getWriter().setValue(myItem, attribute, value);
      return this;
    }

    private <T> void setShadowableValue(DBAttribute<T> attribute, T prevValue, T value) {
      if (DatabaseUtil.isEqualValue(attribute, prevValue, value)) return;
      if (!SyncAttributes.INVISIBLE.equals(attribute)) throw new DBException("Can not change shadowable " + attribute + " from " + prevValue + " to " + value);
      if (!myAdapter.mayChangeInvisible(myItem)) throw new DBException("Can not change Invisible of " + myItem + " to " + value);
      if (Boolean.TRUE.equals(value)) getWriter().clearItem(myItem);
      else getWriter().setValue(myItem, SyncAttributes.EXISTING, true);
    }

    @Override
    public ItemVersionCreator setValue(DBAttribute<Long> attribute, @Nullable DBIdentifiedObject value) {
      return VersionWriterUtil.setValue(this, attribute, value);
    }

    @Override
    public void setValue(DBAttribute<Long> attribute, @Nullable ItemProxy value) {
      VersionWriterUtil.setValue(this, attribute, value);
    }

    @Override
    public void setValue(DBAttribute<Long> attribute, @Nullable ItemVersion value) {
      VersionWriterUtil.setValue(this, attribute, value);
    }

    @Override
    public void setList(DBAttribute<List<Long>> attribute, long[] value) {
      VersionWriterUtil.setList(this, attribute, value);
    }

    @Override
    public void setList(DBAttribute<List<Long>> attribute, LongList value) {
      VersionWriterUtil.setList(this, attribute, value);
    }

    @Override
    public void setSet(DBAttribute<? extends Collection<? extends Long>> attribute, LongList value) {
      VersionWriterUtil.setSet(this, attribute, value);
    }

    @Override
    public void setSet(DBAttribute<? extends Collection<? extends Long>> attribute, Collection<? extends ItemProxy> value) {
      VersionWriterUtil.setSet(this, attribute, value);
    }

    @Override
    public ItemVersionCreator setSequence(DBAttribute<byte[]> attribute, ScalarSequence sequence) {
      return VersionWriterUtil.setSequence(this, attribute, sequence);
    }

    // Implement Drain
    @Override
    public ItemVersionCreator createItem() {
      return myAdapter.createItem();
    }

    @NotNull
    @Override
    public DBReader getReader() {
      return myAdapter.getReader();
    }

    @NotNull
    @Override
    public ItemVersion forItem(long item) {
      return myAdapter.forItem(item);
    }

    @NotNull
    @Override
    public ItemVersion forItem(DBIdentifiedObject object) {
      return myAdapter.forItem(object);
    }

    @Override
    public long findMaterialized(DBIdentifiedObject object) {
      return myAdapter.findMaterialized(object);
    }

    @NotNull
    @Override
    public List<ItemVersion> readItems(LongList items) {
      return myAdapter.readItems(items);
    }

    @Override
    public <T> List<T> collectValues(DBAttribute<T> attribute, LongList items) {
      return myAdapter.collectValues(attribute, items);
    }

    @Override
    public ItemVersionCreator changeItem(long item) {
      return myAdapter.changeItem(item);
    }

    @Override
    public ItemVersionCreator changeItem(DBIdentifiedObject obj) {
      return myAdapter.changeItem(obj);
    }

    @Override
    public ItemVersionCreator changeItem(ItemProxy proxy) {
      return myAdapter.changeItem(proxy);
    }

    @Override
    public long materialize(ItemProxy object) {
      return myAdapter.materialize(object);
    }

    @Override
    public List<ItemVersionCreator> changeItems(LongList items) {
      return myAdapter.changeItems(items);
    }

    @Override
    public long materialize(DBIdentifiedObject object) {
      return myAdapter.materialize(object);
    }

    @Override
    public void finallyDo(ThreadGate gate, Procedure<Boolean> procedure) {
      myAdapter.finallyDo(gate, procedure);
    }

    @Override
    public <T extends Collection<? extends Long>> void addValue(DBAttribute<T> attribute, DBIdentifiedObject value) {
      VersionWriterUtil.addValue(this, attribute, value);
    }
  }
}
