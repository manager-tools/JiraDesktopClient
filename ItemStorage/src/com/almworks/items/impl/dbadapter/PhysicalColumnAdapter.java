package com.almworks.items.impl.dbadapter;

/**
 * These interfaces provide necessary functionality to DBAdapter implementation
 * to convert database types to Java types.
 * <p>
 * Due to non-polymorphic primitive types in Java, each sub-interface has its own
 * methods.
 */
public interface PhysicalColumnAdapter {
  interface Long extends PhysicalColumnAdapter {
    Object storeNative(Object storage, int row, long value, boolean isNull);

    Object toUserValue(long value, boolean isnull);
  }


  interface Any extends PhysicalColumnAdapter {
    Object storeNative(Object storage, int row, Object value);

    Object toUserValue(Object value);
  }
}
