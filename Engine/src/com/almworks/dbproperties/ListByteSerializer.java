package com.almworks.dbproperties;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ListByteSerializer<T> extends ByteSerializer<List<T>> {
  public static final SerializerFactory FACTORY = new SerializerFactory() {
    @Override
    public byte getType() {
      return ByteSerializer.TYPE_LIST;
    }

    @Override
    public ByteSerializer<?> readSerializer(ByteArray.Stream stream) {
      ByteSerializer<?> elementSerializer = ByteSerializer.readSerializer(stream);
      return elementSerializer != null ? create(elementSerializer) : null;
    }
  };

  private final ByteSerializer<T> myElementSerializer;

  private ListByteSerializer(ByteSerializer<T> elementSerializer) {
    super((Class)List.class);
    myElementSerializer = elementSerializer;
  }

  static <T> ByteSerializer<List<T>> create(ByteSerializer<T> elementSerializer) {
    return new ListByteSerializer<T>(elementSerializer);
  }

  @Override
  public boolean write(MapSerializer context, @NotNull List<T> value, ByteArray target) {
    int offset = target.size();
    target.addLong(0);
    for (T element : value) if (element == null || !myElementSerializer.write(context, element, target)) return false;
    target.setLong(offset, target.size() - offset);
    return true;
  }

  @Override
  public List<T> readValue(MapDeserializer context, ByteArray.Stream stream) {
    long length = stream.nextLong();
    if (length < 8) return null;
    if (length == 8) return Collections.emptyList();
    ByteArray.Stream subStream = stream.subStream((int) length - 8);
    if (stream.isErrorOccurred()) return null;
    ArrayList<T> result = Collections15.arrayList();
    while (!subStream.isAtEnd()) {
      T element = myElementSerializer.readValue(context, subStream);
      if (element == null) return null;
      result.add(element);
      if (subStream.isErrorOccurred()) return null;
    }
    return result;
  }

  @Override
  public void writeType(ByteArray target) {
    target.addByte(ByteSerializer.TYPE_LIST);
    myElementSerializer.writeType(target);
  }

  @Override
  public boolean buildSchema(MapSerializer context, Object value) {
    if (value == null) return true;
    List<?> list = Util.castNullable(List.class, value);
    if (list == null) {
      LogHelper.error("Expected list but was", value);
      return false;
    }
    for (Object e : list) if (!myElementSerializer.buildSchema(context, e)) return false;
    return true;
  }
}
