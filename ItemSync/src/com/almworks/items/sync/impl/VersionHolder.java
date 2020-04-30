package com.almworks.items.sync.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.items.sync.edit.BaseDBDrain;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SyncAttributes;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class VersionHolder {
  private final long myItem;

  protected VersionHolder(long item) {
    myItem = item;
  }

  public long getItem() {
    return myItem;
  }

  @NotNull
  public abstract DBReader getReader();

  public abstract <T> T getValue(@NotNull DBAttribute<T> attribute);

  @NotNull
  public abstract AttributeMap getAllShadowableMap();

  public abstract DBAttribute<AttributeMap> getShadow();

  public AttributeMap getAllValues() {
    AttributeInfo info = AttributeInfo.instance(getReader());
    Collection<DBAttribute<?>> allAttributes = info.getAllAttributes();
    AttributeMap map = new AttributeMap();
    map.putAll(getAllShadowableMap());
    for (DBAttribute<?> attribute : allAttributes) {
      if (info.isWriteThrough(attribute)) this.putToMap(map, attribute);
    }
    return map;
  }

  private <T> void putToMap(AttributeMap map, DBAttribute<T> attribute) {
    T value = getValue(attribute);
    if (value == null) return;
    if (attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR) {
      if (value instanceof Collection) {
        if (((Collection) value).isEmpty()) return;
      } else {
        Log.error("Expected collection value of " +attribute + " but was " + value);
        return;
      }
    }
    map.put(attribute, value);
  }

  protected <T> T getTrunkValue(DBAttribute<T> attribute) {
    return getReader().getValue(getItem(), attribute);
  }

  @NotNull
  protected AttributeMap getTrunkAllShadowable() {
    AttributeMap map = getReader().getAttributeMap(getItem());
    return SyncSchema.filterShadowable(getReader(), map);
  }

  protected <T> T getShadowValue(DBAttribute<T> attribute, AttributeMap values) {
    T value = values.get(attribute);
    if (value != null) return value;
    AttributeInfo info = AttributeInfo.instance(getReader());
    return info.isWriteThrough(attribute) ? getTrunkValue(attribute) : null;
  }

  private static abstract class BaseRead extends VersionHolder {
    private final DBReader myReader;

    private BaseRead(DBReader reader, long item) {
      super(item);
      assert reader != null;
      myReader = reader;
    }

    @NotNull
    public DBReader getReader() {
      return myReader;
    }
  }

  static class Trunk extends BaseRead {
    Trunk(DBReader reader, long item) {
      super(reader, item);
    }

    @Override
    public <T> T getValue(@NotNull DBAttribute<T> attribute) {
      return getTrunkValue(attribute);
    }

    @Override
    @NotNull
    public AttributeMap getAllShadowableMap() {
      return getTrunkAllShadowable();
    }

    @Override
    public DBAttribute<AttributeMap> getShadow() {
      return null;
    }
  }

  static class Shadow extends BaseRead {
    private final AttributeMap myValues;
    private final DBAttribute<AttributeMap> myShadow;

    Shadow(DBReader reader, long item, AttributeMap values, DBAttribute<AttributeMap> shadow) {
      super(reader, item);
      myValues = values.copy();
      myShadow = shadow;
    }

    @Override
    public <T> T getValue(@NotNull DBAttribute<T> attribute) {
      return this.getShadowValue(attribute, myValues);
    }

    @Override
    @NotNull
    public AttributeMap getAllShadowableMap() {
      return myValues.copy();
    }

    @Override
    public DBAttribute<AttributeMap> getShadow() {
      return myShadow;
    }

    public void setValues(@Nullable AttributeMap map) {
      myValues.clear();
      if (map != null) myValues.putAll(map);
    }
  }

  public static abstract class Write extends VersionHolder {
    private final DBWriter myWriter;
    private final BaseDBDrain myDrain;
    private boolean myShadowChanged = false;

    protected Write(DBWriter writer, long item, BaseDBDrain drain) {
      super(item);
      if (item <= 0) throw new IllegalArgumentException(String.valueOf(item));
      assert writer != null;
      myWriter = writer;
      myDrain = drain;
    }

    @NotNull
    public DBWriter getReader() {
      return myWriter;
    }

    @Override
    public DBAttribute<AttributeMap> getShadow() {
      return myDrain.getBranch().getShadow();
    }

    protected BaseDBDrain getDrain() {
      return myDrain;
    }

    public <T> boolean setValue(DBAttribute<T> attribute, T value) {
      DBWriter writer = getReader();
      boolean shadowable = AttributeInfo.instance(writer).isShadowable(attribute);
      if (shadowable) return setShadowableValue(attribute, value);
      else {
        writer.setValue(getItem(), attribute, value);
        return true;
      }
    }

    protected void maybeShadowableChanged(DBAttribute<?> attribute, Object value) {
      if (myShadowChanged) return;
      if (SyncSchema.INVISIBLE.equals(attribute)) {
        boolean exists = Boolean.TRUE.equals(getValue(SyncAttributes.EXISTING));
        if (exists) { // If item does not exist add base shadow (the item is reviving)
          Boolean invisibleObj = getValue(SyncSchema.INVISIBLE);
          if (Boolean.TRUE.equals(invisibleObj) == Boolean.TRUE.equals(value)) return;
        }
      }
      myDrain.beforeShadowableChanged(getItem(), isNew());
      myShadowChanged = true;
    }

    protected abstract <T> boolean setShadowableValue(DBAttribute<T> attribute, T value);

    public abstract boolean isNew();
  }

  static class WriteTrunk extends Write {
    private final boolean myNew;

    WriteTrunk(DBWriter writer, long item, BaseDBDrain drain, boolean isNew) {
      super(writer, item, drain);
       myNew = isNew;
    }

    @Override
    public boolean isNew() {
      return myNew;
    }

    @Override
    protected <T> boolean setShadowableValue(DBAttribute<T> attribute, T value) {
      maybeShadowableChanged(attribute, value);
      getReader().setValue(getItem(), attribute, value);
      return true;
    }

    @Override
    public <T> T getValue(@NotNull DBAttribute<T> attribute) {
      return getTrunkValue(attribute);
    }

    @Override
    @NotNull
    public AttributeMap getAllShadowableMap() {
      return getTrunkAllShadowable();
    }
  }

  static class WriteShadow extends Write {
    private final VersionHolder myReadHolder;
    private AttributeMap myValues;

    WriteShadow(DBWriter writer, long item, VersionHolder readHolder, BaseDBDrain drain) {
      super(writer, item, drain);
      myReadHolder = readHolder;
    }

    @Override
    public boolean isNew() {
      return false;
    }

    @Override
    public <T> boolean setValue(DBAttribute<T> attribute, T value) {
      if (myValues != null) {
        T current = myValues.get(attribute);
        if (current != null && SyncUtils.isEqualValue(getReader(), attribute, current, value)) return false;
      }
      return super.setValue(attribute, value);
    }

    @Override
    protected <T> boolean setShadowableValue(DBAttribute<T> attribute, T value) {
      if (myValues == null) {
        T current = myReadHolder.getValue(attribute);
        if (SyncUtils.isEqualValue(getReader(), attribute, value, current)) return false;
        myValues = myReadHolder.getAllShadowableMap().copy();
      }
      maybeShadowableChanged(attribute, value);
      myValues.put(attribute, value);
      DBAttribute<AttributeMap> shadow = getShadow();
      if (shadow == null) Log.error("Missing shadow " + getDrain());
      getReader().setValue(getItem(), shadow, myValues);
      return true;
    }

    @Override
    public <T> T getValue(@NotNull DBAttribute<T> attribute) {
      if (myValues == null) return myReadHolder.getValue(attribute);
      return this.getShadowValue(attribute, myValues);
    }

    @Override
    @NotNull
    public AttributeMap getAllShadowableMap() {
      if (myValues == null) myValues = myReadHolder.getAllShadowableMap().copy();
      return myValues.copy();
    }

    public void setValues(AttributeMap map) {
      if (myValues == null) myValues = new AttributeMap();
      else myValues.clear();
      if (map != null) myValues.putAll(map);
    }
  }
}
