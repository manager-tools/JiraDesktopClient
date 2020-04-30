package com.almworks.items.gui.meta.commons;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Restores feature object from byte array via {@link #restore(com.almworks.items.api.DBReader, com.almworks.util.collections.ByteArray.Stream, Class, com.almworks.util.commons.Procedure)}
 */
public class SerializedObjectAttribute<T> implements DataLoader<T> {
  private final Class<T> myClass;
  private final DBAttribute<byte[]> myAttribute;

  public SerializedObjectAttribute(Class<T> aClass, DBAttribute<byte[]> attribute) {
    myClass = aClass;
    myAttribute = attribute;
  }

  public static <T> SerializedObjectAttribute<T> create(Class<T> aClass, DBAttribute<byte[]> attribute) {
    return new SerializedObjectAttribute<T>(aClass, attribute);
  }

  public DBAttribute<byte[]> getAttribute() {
    return myAttribute;
  }

  @Override
  public List<T> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
    List<T> result = Collections15.arrayList();
    for (byte[] array : myAttribute.collectValues(items, reader)) result.add(array != null ? restore(reader, new ByteArray.Stream(array), myClass, invalidate) : null);
    return result;
  }

  public T getValue(DBReader reader, long item) {
    byte[] bytes = myAttribute.getValue(item, reader);
    return getValue(reader, bytes);
  }

  private T getValue(DBReader reader, byte[] bytes) {
    if (bytes == null) return null;
    ByteArray.Stream stream = new ByteArray.Stream(bytes);
    T result = restore(reader, stream, myClass, null);
    return stream.isSuccessfullyAtEnd() ? result : null;
  }

  @Override
  public String toString() {
    return "Deserialize " + myAttribute + " as " + myClass;
  }

  /**
   * 1. Loads long (8 bytes)
   * 2. Resolves it to {@link com.almworks.items.gui.meta.commons.SerializableFeature}&lt;T&gt; via {@link com.almworks.items.gui.meta.commons.FeatureRegistry}
   * 3. Passes tail of the stream bytes to serializable feature and returns it result
   * @param stream serialized object
   * @param clazz desired restored object class
   * @param invalidate update notification (not used yet)
   * @param <T> desired result type
   * @return restored object
   */
  public static <T> T restore(DBReader reader, ByteArray.Stream stream, Class<T> clazz, @Nullable Procedure<LongList> invalidate) {
    final SerializableFeature<T> loader = FeatureRegistry.getSerializableFeature(reader, stream.nextLong(), clazz);
    if (loader == null) return null;
    T restored = loader.restore(reader, stream, invalidate);
    LogHelper.assertError(restored != null, "Failed to restore", loader);
    return restored;
  }

  public T getValue(@Nullable ItemVersion item) {
    if (item == null) return null;
    return getValue(item.getReader(), item.getValue(myAttribute));
  }

  public T getNNValue(@Nullable ItemVersion item, T nullValue) {
    return Util.NN(getValue(item), nullValue);
  }

  public void setIfChanged(ItemVersionCreator item, ScalarSequence sequence) {
    byte[] newValue;
    if (sequence == null) newValue = null;
    else newValue = sequence.serialize(item);
    byte[] prevValue = item.getValue(myAttribute);
    if (Arrays.equals(newValue, prevValue)) return;
    item.setValue(myAttribute, newValue);
  }
}
