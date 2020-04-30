package com.almworks.items.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.util.Map;

public class DelegatingReader implements DBReader {
  private final DBReader myReader;

  protected DelegatingReader(@NotNull DBReader reader) {
    //noinspection ConstantConditions
    if (reader == null) throw new NullPointerException();
    myReader = reader;
  }

  @Override
  public <T> T getValue(long item, DBAttribute<T> attribute) {
    return myReader.getValue(item, attribute);
  }

  @Override
  public DBQuery query(BoolExpr<DP> expr) {
    return myReader.query(expr);
  }

  @Override
  public long getTransactionIcn() {
    return myReader.getTransactionIcn();
  }

  @Override
  public long getItemIcn(long item) {
    return myReader.getItemIcn(item);
  }

  @Override
  public long findMaterialized(DBIdentifiedObject object) {
    return myReader.findMaterialized(object);
  }

  @Override
  public long assertMaterialized(DBIdentifiedObject object) {
    return myReader.assertMaterialized(object);
  }

  @Override
  public AttributeMap getAttributeMap(long item) {
    return myReader.getAttributeMap(item);
  }

  @Override
  @Nullable
  public DBAttribute getAttribute(String id) {
    return myReader.getAttribute(id);
  }

  @Override
  public Map<TypedKey<?>, ?> getTransactionCache() {
    return myReader.getTransactionCache();
  }

  @Override
  public UserDataHolder getDatabaseUserData() {
    return myReader.getDatabaseUserData();
  }

  @Override
  public LongList getChangedItemsSorted(long fromIcn) {
    return myReader.getChangedItemsSorted(fromIcn);
  }

  @Override
  public long getTransactionTime() {
    return myReader.getTransactionTime();
  }

  @Override
  public void serializeMap(AttributeMap map, DataOutputStream target) {
    myReader.serializeMap(map, target);
  }

  @Override
  public AttributeMap restoreMap(byte[] bytes) {
    return myReader.restoreMap(bytes);
  }
}
