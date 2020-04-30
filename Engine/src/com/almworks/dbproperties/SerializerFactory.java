package com.almworks.dbproperties;

import com.almworks.util.collections.ByteArray;
import org.jetbrains.annotations.Nullable;

public interface SerializerFactory {
  byte getType();

  @Nullable
  ByteSerializer<?> readSerializer(ByteArray.Stream stream);

  public static class Scalar implements SerializerFactory {
    private final ByteSerializer<?> mySerializer;
    private final byte myType;

    public Scalar(ByteSerializer<?> serializer, byte type) {
      mySerializer = serializer;
      myType = type;
    }

    @Override
    public byte getType() {
      return myType;
    }

    @Override
    public ByteSerializer<?> readSerializer(ByteArray.Stream stream) {
      return mySerializer;
    }
  }
}
