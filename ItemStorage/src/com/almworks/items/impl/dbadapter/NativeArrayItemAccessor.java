package com.almworks.items.impl.dbadapter;

import com.almworks.util.collections.arrays.ArrayStorageAccessor;
import com.almworks.util.collections.arrays.PrimitiveIntArrayAccessor;
import com.almworks.util.collections.arrays.PrimitiveLongArrayAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NativeArrayItemAccessor implements ItemAccessor {
  private final List<SyncValueLoader> myAccessors;
  private final List<Object> myValues;
  private long myItem;
  private int myIndex;

  public NativeArrayItemAccessor(List<SyncValueLoader> accessors, List<Object> loadedObjects, int item, int index) {
    myAccessors = accessors;
    myValues = loadedObjects;
    myItem = item;
    myIndex = index;
  }

  public NativeArrayItemAccessor(List<SyncValueLoader> accessors, List<Object> loadedObjects) {
    this(accessors, loadedObjects, 0, -1);
  }

  public void setPosition(long item, int index) {
    myItem = item;
    myIndex = index;
  }

  @Nullable
  public Object getValue(SyncValueLoader attribute) {
    int idx = myAccessors.indexOf(attribute);
    if (idx < 0) {
      assert false;
      return null;
    }
    return attribute.getArrayAccessor().getObjectValue(myValues.get(idx), myIndex);
  }


  public int getInt(SyncValueLoader attribute, int missingValue) {
    int idx = myAccessors.indexOf(attribute);
    if (idx < 0) {
      assert false;
      return missingValue;
    }
    ArrayStorageAccessor accessor = attribute.getArrayAccessor();
    if (!(accessor instanceof PrimitiveIntArrayAccessor)) {
      assert false : attribute;
      return missingValue;
    }
    return ((PrimitiveIntArrayAccessor) accessor).getIntValue(myValues.get(idx), myIndex);
  }

  public long getItem() {
    return myItem;
  }

  public boolean hasValues() {
    return myIndex >= 0;
  }

  public long getLong(SyncValueLoader attribute, long missingValue) {
    int idx = myAccessors.indexOf(attribute);
    if (idx < 0) {
      assert false;
      return missingValue;
    }
    ArrayStorageAccessor accessor = attribute.getArrayAccessor();
    if (!(accessor instanceof PrimitiveLongArrayAccessor)) {
      assert false : attribute;
      return missingValue;
    }
    return ((PrimitiveLongArrayAccessor) accessor).getLongValue(myValues.get(idx), myIndex);
  }

  public boolean hasUptodateValue(SyncValueLoader attribute) {
    return myIndex >= 0 && myAccessors.indexOf(attribute) >= 0;
  }
}
