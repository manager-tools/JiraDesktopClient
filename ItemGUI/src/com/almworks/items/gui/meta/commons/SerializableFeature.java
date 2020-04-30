package com.almworks.items.gui.meta.commons;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @see SerializedObjectAttribute#restore(com.almworks.items.api.DBReader, com.almworks.util.collections.ByteArray.Stream, Class, com.almworks.util.commons.Procedure)
 */
public interface SerializableFeature<T> {
  T restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate);

  Class<T> getValueClass();

  class NoParameters<T> implements SerializableFeature<T> {
    private final T myConstant;
    private final Class<T> myValueClass;

    public NoParameters(T constant, Class<T> valueClass) {
      myConstant = constant;
      myValueClass = valueClass;
    }

    public static <T> SerializableFeature<T> create(T constant, Class<T> valueClass) {
      return new NoParameters<T>(constant, valueClass);
    }

    @Override
    public T restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      LogHelper.assertError(stream.isSuccessfullyAtEnd(), "Expected at end", stream.isErrorOccurred());
      return myConstant;
    }

    @Override
    public Class<T> getValueClass() {
      return myValueClass;
    }
  }


  public static class SequenceDeserializer<T> implements DataLoader<List<T>> {
    private final Class<T> myClass;
    private final DBAttribute<byte[]> myAttribute;

    public SequenceDeserializer(Class<T> aClass, DBAttribute<byte[]> attribute) {
      myClass = aClass;
      myAttribute = attribute;
    }

    @Override
    public List<List<T>> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
      ArrayList<List<T>> result = Collections15.arrayList();
      for (byte[] bytes : myAttribute.collectValues(items, reader)) {
        List<T> list;
        if (bytes == null) list = null;
        else list = restore(reader, ByteArray.createStream(bytes), myClass, invalidate, false);
        result.add(list);
      }
      return result;
    }

    public static <T> List<T> restore(DBReader reader, ByteArray.Stream stream, Class<T> aClass, Procedure<LongList> invalidate, boolean allowEmpty) {
      int count = stream.nextInt();
      if(count <= 0) {
        if (allowEmpty) {
          if (stream.isSuccessfullyAtEnd()) return Collections.emptyList();
          LogHelper.error("Empty list but not at end", aClass, stream);
        } else LogHelper.error("No positive count", aClass, count, stream);
        return null;
      }
      List<T> list = Collections15.arrayList();
      for(int i = 0; i < count; i++) {
        final T element = SerializedObjectAttribute.restore(reader, stream.nextSubstream(), aClass, invalidate);
        if (element == null) return null;
        list.add(element);
      }
      if(!stream.isSuccessfullyAtEnd()) {
        LogHelper.error("Not at end", stream, aClass);
        return null;
      }
      return list;
    }

    @Override
    public String toString() {
      return "SequenceLoader[" + myClass +"," + myAttribute + "]";
    }

    public static <T> SequenceDeserializer<T> create(Class<T> aClass, DBAttribute<byte[]> attribute) {
      return new SequenceDeserializer<T>(aClass, attribute);
    }
  }
}
