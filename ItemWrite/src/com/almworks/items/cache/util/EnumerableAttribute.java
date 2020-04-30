package com.almworks.items.cache.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.Enumerable;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import java.util.ArrayList;
import java.util.List;

public class EnumerableAttribute<E extends Enumerable> implements DataLoader<E> {
  private final Class<E> myClass;
  private final DBAttribute<String> myAttribute;

  public EnumerableAttribute(DBAttribute<String> attribute, Class<E> aClass) {
    myAttribute = attribute;
    myClass = aClass;
  }
  
  public static <E extends Enumerable> EnumerableAttribute<E> create(DBAttribute<String> attribute, Class<E> aClass) {
    return new EnumerableAttribute<E>(attribute, aClass);
  }

  @Override
  public List<E> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
    List<String> values = myAttribute.collectValues(items, reader);
    ArrayList<E> result = Collections15.arrayList();
    for (String name : values) result.add(convertName(name));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    EnumerableAttribute other = Util.castNullable(EnumerableAttribute.class, obj);
    return other != null && other.myClass.equals(myClass) && other.myAttribute.equals(myAttribute);
  }

  @Override
  public int hashCode() {
    return myAttribute.hashCode();
  }

  public void setValue(ItemVersionCreator item, E value) {
    item.setValue(myAttribute, value != null ? value.getName() : null);
  }

  private E convertName(String name) {
    if (name == null) return null;
    E e = Enumerable.forName(myClass, name);
    if (e == null) LogHelper.error("Unknown enum value", myClass, name);
    return e;
  }

  public E getValue(DBReader reader, DBIdentifiedObject object) {
    long item = reader.findMaterialized(object);
    if (item <= 0) return null;
    return convertName(reader.getValue(item, myAttribute));
  }
}
