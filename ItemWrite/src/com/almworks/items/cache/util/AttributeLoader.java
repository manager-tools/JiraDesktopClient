package com.almworks.items.cache.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AttributeLoader<T> implements DataLoader<T> {
  private final DBAttribute<T> myAttribute;

  public AttributeLoader(DBAttribute<T> attribute) {
    myAttribute = attribute;
  }

  public static <T> AttributeLoader<T> create(DBAttribute<T> attribute) {
    return new AttributeLoader<T>(attribute);
  }

  public static AttributeLoader<?>[] createArray(DBAttribute<?> ... attributes) {
    AttributeLoader[] result = new AttributeLoader[attributes.length];
    for (int i = 0; i < attributes.length; i++) {
      DBAttribute<?> attribute = attributes[i];
      result[i] = create(attribute);
    }
    return result;
  }

  @Override
  public int hashCode() {
    return getAttribute().hashCode() ^ AttributeLoader.class.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    AttributeLoader other = Util.castNullable(AttributeLoader.class, obj);
    return other != null && getAttribute().equals(other.getAttribute());
  }

  @Override
  public String toString() {
    return "AttrLoader@" + getAttribute();
  }

  @Override
  public List<T> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
    List<T> values = Collections15.arrayList(items.size());
    for (int i = 0; i < items.size(); i++) values.add(reader.getValue(items.get(i), myAttribute));
    return values;
  }

  @NotNull
  public DBAttribute<T> getAttribute() {
    return myAttribute;
  }

  @NotNull
  public T getNNValue(@Nullable ItemVersion item, @NotNull T nullValue) {
    T val = item != null ? item.getValue(myAttribute) : null;
    return Util.NN(val, nullValue);
  }

  public T getValue(DBReader reader, DBIdentifiedObject object) {
    long item = reader.findMaterialized(object);
    return item > 0 ? reader.getValue(item, myAttribute) : null;
  }
  
  public void setValue(ItemVersionCreator item, T value) {
    item.setValue(myAttribute, value);
  }
}
