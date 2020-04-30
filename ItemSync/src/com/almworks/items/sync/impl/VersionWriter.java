package com.almworks.items.sync.impl;

import com.almworks.integers.IntArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.edit.BaseDBDrain;
import com.almworks.items.sync.util.VersionWriterUtil;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.SyncAttributes;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class VersionWriter extends BaseVersionWriter {
  private final VersionHolder.Write myHolder;

  public VersionWriter(BaseDBDrain drain, VersionHolder.Write holder) {
    super(drain);
    myHolder = holder;
  }

  @Override
  public long getItem() {
    return myHolder.getItem();
  }

  @Override
  public <T> ItemVersionCreator setValue(DBAttribute<T> attribute, T value) {
    myHolder.setValue(attribute, value);
    return this;
  }

  @Override
  public ItemVersionCreator setValue(DBAttribute<Long> attribute, DBIdentifiedObject value) {
    return VersionWriterUtil.setValue(this, attribute, value);
  }

  @Override
  public void setValue(DBAttribute<Long> attribute, ItemProxy value) {
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
  public void delete() {
    VersionWriterUtil.delete(this);
  }

  @Override
  public void setAlive() {
    VersionWriterUtil.setAlive(this);
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

  @Override
  public VersionHolder getHolder() {
    return myHolder;
  }

  @Override
  public void markMerged() {
    assert false;
  }

  @Override
  public <T extends Collection<? extends Long>> void addValue(DBAttribute<T> attribute, DBIdentifiedObject value) {
    VersionWriterUtil.addValue(this, attribute, value);
  }

  @Override
  public void addHistory(ItemProxy kind, byte[] step) {
    HistoryRecord[] history = getHistory();
    IntArray ids = new IntArray();
    for (HistoryRecord record : history) ids.add(record.getRecordId());
    ids.sortUnique();
    int id = ids.isEmpty() ? 0 : ids.get(0);
    while (ids.contains(id)) id++;
    long kindItem = materialize(kind);
    HistoryRecord newStep = new HistoryRecord(kindItem, id, step);
    ArrayList<HistoryRecord> newHistory = Collections15.arrayList(history);
    newHistory.add(newStep);
    setValue(SyncAttributes.CHANGE_HISTORY, HistoryRecord.serialize(newHistory));
  }
}
