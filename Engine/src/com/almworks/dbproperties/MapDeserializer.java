package com.almworks.dbproperties;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MapDeserializer {
  private final ByteArray.Stream myStream;
  private FromByteDeserializer[] myDeserializers;

  public MapDeserializer(ByteArray.Stream stream) {
    myStream = stream;
  }

  @Nullable
  public static Map<TypedKey<?>, ?> restore(ByteArray.Stream stream, SerializeSchema schema) {
    byte marker = stream.nextByte();
    if (marker != ByteSerializer.TYPE_MAP) return null;
    MapDeserializer deserializer = new MapDeserializer(stream);
    if (!deserializer.restoreSchema(schema)) return null;
    return deserializer.restoreMap();
  }

  @Nullable
  public static Map<TypedKey<?>, ?> restore(byte[] bytes, SerializeSchema schema) {
    return restore(new ByteArray.Stream(bytes), schema);
  }

  private Map<TypedKey<?>, ?> restoreMap() {
    return ByteSerializer.MAP.readValue(this, myStream);
  }

  @Nullable
  public FromByteDeserializer getDeserializer(int id) {
    if (id < 0 || id >= myDeserializers.length) return null;
    return myDeserializers[id];
  }

  private boolean restoreSchema(SerializeSchema schema) {
    if (schema == null) return false;
    int keyCount = myStream.nextInt();
    if (keyCount < 0) return false;
    myDeserializers = new FromByteDeserializer[keyCount];
    for (int i = 0; i < myDeserializers.length; i++) {
      String name = myStream.nextUTF8();
      TypedKey<?> key = schema.getKeyByName(name);
      if (key == null) {
        LogHelper.error("Unknown key", name);
        return false;
      }
      ByteSerializer<?> reader = ByteSerializer.readSerializer(myStream);
      if (myStream.isErrorOccurred() || reader == null) return false;
      if (key.getValueClass() != reader.getDataClass()) {
        LogHelper.error("Wrong value class", reader.getDataClass(), key.getValueClass(), key);
        return false;
      }
      //noinspection unchecked
      myDeserializers[i] = new FromByteDeserializer(key, reader);
    }
    return true;
  }

  public static class FromByteDeserializer<T> {
    private final TypedKey<T> myKey;
    private final ByteSerializer<T> myReader;

    private FromByteDeserializer(TypedKey<T> key, ByteSerializer<T> reader) {
      myKey = key;
      myReader = reader;
    }

    public boolean restore(MapDeserializer context, ByteArray.Stream stream, HashMap<TypedKey<?>, ?> target) {
      T value = myReader.readValue(context, stream);
      if (stream.isErrorOccurred() || value == null) return false;
      myKey.putTo(target, value);
      return true;
    }
  }
}
