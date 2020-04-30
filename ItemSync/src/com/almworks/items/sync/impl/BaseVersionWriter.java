package com.almworks.items.sync.impl;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.edit.BaseDBDrain;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.jetbrains.annotations.NotNull;

import java.util.List;

abstract class BaseVersionWriter extends VersionReader implements ItemVersionCreator {
  private final BaseDBDrain myDrain;

  BaseVersionWriter(BaseDBDrain drain) {
    myDrain = drain;
  }

  @NotNull
  @Override
  public ItemVersion forItem(DBIdentifiedObject object) {
    return myDrain.forItem(object);
  }

  @NotNull
  @Override
  protected Branch getBranch() {
    return myDrain.getBranch();
  }

  @Override
  public ItemVersionCreator createItem() {
    return myDrain.createItem();
  }
  @Override
  public ItemVersionCreator changeItem(long item) {
    return myDrain.changeItem(item);
  }

  @Override
  public ItemVersionCreator changeItem(DBIdentifiedObject obj) {
    return myDrain.changeItem(obj);
  }

  @Override
  public ItemVersionCreator changeItem(ItemProxy proxy) {
    return myDrain.changeItem(proxy);
  }

  @Override
  public List<ItemVersionCreator> changeItems(LongList items) {
    return myDrain.changeItems(items);
  }

  @Override
  public void finallyDo(ThreadGate gate, Procedure<Boolean> procedure) {
    myDrain.finallyDo(gate, procedure);
  }

  @Override
  public long materialize(ItemProxy object) {
    return myDrain.materialize(object);
  }

  @Override
  public long materialize(DBIdentifiedObject object) {
    return myDrain.materialize(object);
  }

  @Override
  public String toString() {
    return "VersionWriter " + toStringItemInfo();
  }
}
