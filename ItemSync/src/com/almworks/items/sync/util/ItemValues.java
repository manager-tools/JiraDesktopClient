package com.almworks.items.sync.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DatabaseUtil;
import org.almworks.util.ArrayUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ItemValues {
  private final DBAttribute<?>[] myAttributes;
  private final Object[] myValues;

  private ItemValues(DBAttribute<?>[] attributes, Object[] values) {
    myAttributes = attributes;
    myValues = values;
  }

  public static ItemValues collect(List<DBAttribute<?>> attributes, AttributeMap map) {
    DBAttribute<?>[] attrArray = attributes.toArray(new DBAttribute[attributes.size()]);
    Object[] values = new Object[attributes.size()];
    for (int i = 0; i < attributes.size(); i++) {
      DBAttribute<?> attribute = attributes.get(i);
      values[i] = map.get(attribute);
    }
    return new ItemValues(attrArray, values);
  }

  public List<DBAttribute<?>> attributes() {
    return Arrays.asList(myAttributes);
  }

  @SuppressWarnings({"unchecked"})
  public boolean equalValues(AttributeMap map) {
    Set<DBAttribute<?>> attributes = map.keySet();
    for (DBAttribute<?> attribute : attributes) {
      Object value = getValue(attribute);
      if (!DatabaseUtil.isEqualValue((DBAttribute<Object>) attribute, value, map.get(attribute))) return false;
    }
    return true;
  }

  @SuppressWarnings({"unchecked"})
  public <T> T getValue(DBAttribute<T> attribute) {
    int index = indexOf(attribute);
    return index >= 0 ? (T) myValues[index] : null;
  }

  public int indexOf(DBAttribute<?> attribute) {
    return ArrayUtil.indexOf(myAttributes, attribute);
  }
}
