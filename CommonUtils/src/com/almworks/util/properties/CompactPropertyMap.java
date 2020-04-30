package com.almworks.util.properties;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

/**
 * @author dyoma
 */
public class CompactPropertyMap {
  public static final CompactPropertyMap[] EMPTY_ARRAY = new CompactPropertyMap[0];
  
  private final EnumPropertySet<TypedKey<?>> mySet;
  private final Object[] myValues;

  public CompactPropertyMap(EnumPropertySet<TypedKey<?>> set) {
    mySet = set;
    myValues = new Object[set.size()];
  }

  public <T> void set(@NotNull TypedKey<? super T> key, T value) {
    myValues[getIndex(key)] = value;
  }

  public <T> T get(@NotNull TypedKey<? extends T> key) {
    return key.cast(myValues[getIndex(key)]);
  }

  public EnumPropertySet<TypedKey<?>> getPropertySet() {
    return mySet;
  }

  private int getIndex(@NotNull TypedKey<?> key) {
    return mySet.getSafeIndex(key, myValues.length);
  }
}
