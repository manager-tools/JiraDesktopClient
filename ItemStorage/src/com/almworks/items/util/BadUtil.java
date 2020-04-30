package com.almworks.items.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class BadUtil {
  public static DBAttribute<?> getAttribute(DBReader reader, Long item) {
    return item != null && item > 0 ? getAttribute(reader, item.longValue()) : null;
  }

  public static DBAttribute<?> getAttribute(DBReader reader, long item) {
    if (item <= 0) return null;
    String id = reader.getValue(item, DBAttribute.ID);
    if (id != null) {
      DBAttribute attribute = reader.getAttribute(id);
      if (attribute == null) Log.error("Failed to load attribute " + item + " " + id);
      return attribute;
    }
    Log.error("Missing attribute id " + item);
    return null;
  }

  public static List<DBAttribute<?>> getAttributes(DBReader reader, Collection<Long> attrItems) {
    return getAttributes(reader, LongArray.create(attrItems));
  }

  public static List<DBAttribute<?>> getAttributes(DBReader reader, LongList attrItems) {
    if (attrItems == null || attrItems.isEmpty()) return Collections15.emptyList();
    List<DBAttribute<?>> result = Collections15.arrayList(attrItems.size());
    for (int i = 0; i < attrItems.size(); i++) {
      long item = attrItems.get(i);
      DBAttribute<?> attribute = getAttribute(reader, item);
      if (attribute != null) result.add(attribute);
    }
    return result;
  }

  public static DBItemType getItemType(DBReader reader, Long item) {
    if (item == null || item <= 0) return null;
    Long typeType = reader.getValue(item, DBAttribute.TYPE);
    if (typeType == null || typeType <= 0 || typeType != reader.findMaterialized(DBItemType.TYPE)) return null;
    String id = reader.getValue(item, DBAttribute.ID);
    if (id == null) return null;
    String name = reader.getValue(item, DBAttribute.NAME);
    return new DBItemType(id, name);
  }

  @Nullable
  public static <T> DBAttribute<T> castScalar(Class<T> scalarClass, @Nullable DBAttribute<?> attribute) {
    if (attribute == null || scalarClass == null) return null;
    if (attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR) return null;
    if (!scalarClass.equals(attribute.getScalarClass())) return null;
    //noinspection unchecked
    return (DBAttribute<T>) attribute;
  }

  public static <T> DBAttribute<T> readScalarAttribute(Class<T> scalarClass, DBReader reader, long item, DBAttribute<Long> attrAttribute) {
    Long attrItem = attrAttribute.getValue(item, reader);
    if (attrItem == null || attrItem <= 0) return null;
    DBAttribute<?> attribute = getAttribute(reader, attrItem);
    if (attribute == null) return null;
    return castScalar(scalarClass, attribute);
  }

  public static <T> DBAttribute<T> getScalarAttribute(DBReader reader, long item, Class<T> aClass) {
    DBAttribute<?> attribute = getAttribute(reader, item);
    if (attribute == null) {
      LogHelper.error("No attribute", item);
      return null;
    }
    if (attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR
      || !aClass.isAssignableFrom(attribute.getScalarClass())) {
      LogHelper.error("Wrong attribute", attribute, aClass);
      return null;
    }
    //noinspection unchecked
    return (DBAttribute<T>) attribute;
  }

  @Nullable
  public static <T> DBAttribute<? extends Collection<T>> castCollectionAttribute(Class<T> scalarClass,
    DBAttribute<?> attribute)
  {
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    if (composition != DBAttribute.ScalarComposition.LIST && composition != DBAttribute.ScalarComposition.SET) return null;
    Class<?> aClass = attribute.getScalarClass();
    if (!Util.equals(scalarClass, aClass)) return null;
    //noinspection unchecked
    return (DBAttribute<? extends Collection<T>>) attribute;
  }

  public static <T> DBAttribute<Set<T>> castSetAttribute(Class<T> scalarClass, DBAttribute<?> attribute) {
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    if (composition != DBAttribute.ScalarComposition.SET) return null;
    Class<?> aClass = attribute.getScalarClass();
    if (!Util.equals(scalarClass, aClass)) return null;
    //noinspection unchecked
    return (DBAttribute<Set<T>>) attribute;
  }

  public static <T> DBAttribute<List<T>> castListAttribute(Class<T> scalarClass, DBAttribute<?> attribute) {
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    if (composition != DBAttribute.ScalarComposition.LIST) return null;
    Class<?> aClass = attribute.getScalarClass();
    if (!Util.equals(scalarClass, aClass)) return null;
    //noinspection unchecked
    return (DBAttribute<List<T>>) attribute;
  }

  public static <T> Class<T> getScalarAttributeClass(DBAttribute<T> attribute) {
    if (attribute == null) return null;
    if (attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR) return null;
    return attribute.getValueClass();
  }
}
