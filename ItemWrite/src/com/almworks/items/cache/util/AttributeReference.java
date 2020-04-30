package com.almworks.items.cache.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.BadUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class AttributeReference<T> implements DataLoader<DBAttribute<T>> {
  private final ItemAttribute myAttribute;
  @Nullable
  private final Class<?> myScalarClass;
  @Nullable
  private final DBAttribute.ScalarComposition myComposition;

  public AttributeReference(ItemAttribute attribute, Class<?> scalarClass, DBAttribute.ScalarComposition composition) {
    myAttribute = attribute;
    myScalarClass = scalarClass;
    myComposition = composition;
  }

  @Override
  public List<DBAttribute<T>> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
    return collectValues(reader, items);
  }

  @NotNull
  public List<DBAttribute<T>> collectValues(DBReader reader, LongList items) {
    LongList attributes = myAttribute.collectValues(reader, items);
    List<DBAttribute<T>> result = Collections15.arrayList(attributes.size());
    for (int i = 0; i < attributes.size(); i++) {
      long attrItem = attributes.get(i);
      DBAttribute<?> attribute = BadUtil.getAttribute(reader, attrItem);
      DBAttribute<T> loaded;
      if (attribute == null) loaded = null;
      else if (myComposition != null && attribute.getComposition() != myComposition) loaded = null;
      else if (myScalarClass != null && !myScalarClass.isAssignableFrom(attribute.getScalarClass())) loaded = null;
      else //noinspection unchecked
        loaded = (DBAttribute<T>) attribute;
      LogHelper.assertError(attrItem == 0 || loaded != null, "Failed to load attribute", this, attrItem, attribute);
      result.add(loaded);
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("AttrRef[");
    if (myScalarClass == null && myComposition == null) builder.append("?");
    else {
      if (myScalarClass != null) builder.append(myScalarClass.getName());
      if (myComposition != null) {
        if (myScalarClass != null) builder.append(" ");
        builder.append(myComposition);
      }
    }
    builder.append(" <- ").append(myAttribute);
    builder.append("]");
    return builder.toString();
  }

  public static AttributeReference<?> create(DBAttribute<Long> attribute) {
    return create(attribute, null, null);
  }

  public static AttributeReference<String> string(DBAttribute<Long> attribute) {
    Class<String> scalarClass = String.class;
    DBAttribute.ScalarComposition composition = DBAttribute.ScalarComposition.SCALAR;
    //noinspection unchecked
    return (AttributeReference<String>) create(attribute, scalarClass, composition);
  }

  public static AttributeReference<?> create(DBAttribute<Long> attribute, @Nullable Class<?> scalarClass, @Nullable DBAttribute.ScalarComposition composition) {
    return new AttributeReference<String>(new ItemAttribute(attribute), scalarClass, composition);
  }

  public DBAttribute<T> getValue(DBReader reader, long item) {
    if (item <= 0) return null;
    return getValue(SyncUtils.readTrunk(reader, item));
  }

  public DBAttribute<T> getValue(ItemVersion item) {
    long attrItem =  myAttribute.getValue(item);
    if (attrItem <= 0) return null;
    DBAttribute<?> attribute = BadUtil.getAttribute(item.getReader(), attrItem);
    priCheckConstraint(attribute.getComposition(), attribute.getScalarClass());
    //noinspection unchecked
    return (DBAttribute<T>) attribute;
  }

  public <V> DBAttribute<Set<V>> getSetReference(ItemVersion itemVersion, Class<V> aClass) {
    priCheckConstraint(DBAttribute.ScalarComposition.SET, aClass);
    DBAttribute<?> attribute = getValue(itemVersion);
    if (attribute == null) return null;
    DBAttribute<Set<V>> setDBAttribute = BadUtil.castSetAttribute(aClass, attribute);
    LogHelper.assertError(setDBAttribute != null, attribute);
    return setDBAttribute;
  }
  
  public DBAttribute<Long> getReference(ItemVersion itemVersion) {
    priCheckConstraint(DBAttribute.ScalarComposition.SCALAR, Long.class);
    DBAttribute<?> attribute = getValue(itemVersion);
    if (attribute == null) return null;
    DBAttribute<Long> reference = BadUtil.castScalar(Long.class, attribute);
    LogHelper.assertError(reference != null, reference);
    return reference;
  }

  private void priCheckConstraint(@Nullable DBAttribute.ScalarComposition composition, @Nullable Class<?> scalarClass) {
    LogHelper.assertError(myComposition == null || composition == null || myComposition == composition, "Unexpected composition", myComposition, composition);
    LogHelper.assertError(myScalarClass == null || scalarClass == null || myScalarClass == scalarClass, "Unexpected scalar class", myScalarClass, scalarClass);
  }
}
