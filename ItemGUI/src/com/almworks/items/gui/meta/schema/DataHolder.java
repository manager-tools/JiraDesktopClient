package com.almworks.items.gui.meta.schema;

import com.almworks.util.LogHelper;
import org.almworks.util.ArrayUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.Arrays;

public class DataHolder {
  public static final DataHolder EMPTY = new DataHolder(TypedKey.EMPTY_ARRAY);
  private final TypedKey<?>[] myKeys;
  private final Object[] myValues;
  private int myHashCode = 0;

  public DataHolder(TypedKey<?>[] keys) {
    myKeys = keys;
    myValues = new Object[keys.length];
  }

  public <T> DataHolder setValue(TypedKey<T> key, T value) {
    int index = ArrayUtil.indexOf(myKeys, key);
    if (index < 0) LogHelper.error("Wrong key", key, myKeys);
    else myValues[index] = value;
    return this;
  }

  public <T> T getValue(TypedKey<T> key) {
    int index = ArrayUtil.indexOf(myKeys, key);
    if (index < 0) {
      LogHelper.error("Wrong key", key, myKeys);
      return null;
    }
    //noinspection unchecked
    return (T) myValues[index];
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("DataHolder[");
    String sep = "";
    for (int i = 0; i < myKeys.length; i++) {
      TypedKey<?> key = myKeys[i];
      builder.append(sep);
      sep = ",";
      builder.append(key.getName()).append("=").append(myValues[i]);
    }
    builder.append("]");
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    DataHolder other = Util.castNullable(DataHolder.class, obj);
    if (other == null) return false;
    if (hashCode() != other.hashCode()) return false;
    if (!ArrayUtil.equals(myKeys, other.myKeys)) return false;
    for (int i = 0; i < myValues.length; i++) {
      Object value = myValues[i];
      if (!Util.equals(value, other.myValues[i])) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = myHashCode;
    if (hashCode == 0) {
      hashCode = DataHolder.class.hashCode() ^ Util.hashCode(myKeys) ^ Util.hashCode(myValues);
      if (hashCode == 0) hashCode++;
      myHashCode = hashCode;
    }
    return hashCode;
  }

  public boolean update(DataHolder another) {
    if (another == null || !(Arrays.equals(another.myKeys, myKeys))) {
      LogHelper.warning("DH: cannot update", myKeys, another == null ? null : another.myKeys);
      return false;
    }
    for (int i = 0; i < myKeys.length; ++i)
      myValues[i] = another.myValues[i];
    return true;
  }
}
