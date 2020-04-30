package com.almworks.dbproperties;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public abstract class ByteSerializer<T> {
  public static final byte TYPE_BOOLEAN = 0;
  public static final byte TYPE_TEXT = 1;
  public static final byte TYPE_MAP = 2;
  public static final byte TYPE_LIST = 3;
  public static final byte TYPE_INTEGER = 4;

  private final Class<T> myClass;

  protected ByteSerializer(Class<T> aClass) {
    myClass = aClass;
  }

  public Class<T> getDataClass() {
    return myClass;
  }

  public abstract boolean write(MapSerializer context, @NotNull T value, ByteArray target);

  public abstract T readValue(MapDeserializer context, ByteArray.Stream stream);

  public abstract void writeType(ByteArray target);

  public abstract boolean buildSchema(MapSerializer context, Object value);

  public static final ByteSerializer<Boolean> BOOLEAN = new ByteSerializer<Boolean>(Boolean.class) {
    @Override
    public boolean write(MapSerializer context, @NotNull Boolean value, ByteArray target) {
      target.addBoolean(value);
      return true;
    }

    @Override
    public Boolean readValue(MapDeserializer context, ByteArray.Stream stream) {
      return stream.nextBoolean();
    }

    @Override
    public void writeType(ByteArray target) {
      target.addByte(TYPE_BOOLEAN);
    }

    @Override
    public boolean buildSchema(MapSerializer context, Object value) {
      return true;
    }
  };

  public static final ByteSerializer<Integer> INTEGER = new ByteSerializer<Integer>(Integer.class) {
    @Override
    public boolean write(MapSerializer context, @NotNull Integer value, ByteArray target) {
      target.addInt(value);
      return true;
    }

    @Override
    public Integer readValue(MapDeserializer context, ByteArray.Stream stream) {
      return stream.nextInt();
    }

    @Override
    public void writeType(ByteArray target) {
      target.addByte(TYPE_INTEGER);
    }

    @Override
    public boolean buildSchema(MapSerializer context, Object value) {
      return true;
    }
  };

  public static final ByteSerializer<String> TEXT = new ByteSerializer<String>(String.class) {
    @Override
    public boolean write(MapSerializer context, @NotNull String value, ByteArray target) {
      target.addUTF8(value);
      return true;
    }

    @Override
    public String readValue(MapDeserializer context, ByteArray.Stream stream) {
      return stream.nextUTF8();
    }

    @Override
    public void writeType(ByteArray target) {
      target.addByte(TYPE_TEXT);
    }

    @Override
    public boolean buildSchema(MapSerializer context, Object value) {
      return true;
    }
  };

  @SuppressWarnings("unchecked")
  public static final MapToBytesSerializer MAP = new MapToBytesSerializer();

  private static final SerializerFactory FACTORY_BOOLEAN = new SerializerFactory.Scalar(BOOLEAN, TYPE_BOOLEAN);
  private static final SerializerFactory FACTORY_TEXT = new SerializerFactory.Scalar(TEXT, TYPE_TEXT);
  private static final SerializerFactory FACTORY_MAP = new SerializerFactory.Scalar(MAP, TYPE_MAP);
  private static final SerializerFactory FACTORY_INTEGER = new SerializerFactory.Scalar(INTEGER, TYPE_INTEGER);

  private static final Map<Byte, SerializerFactory> FACTORIES;
  static {
    HashMap<Byte, SerializerFactory> map = Collections15.hashMap();
    map.put(FACTORY_BOOLEAN.getType(), FACTORY_BOOLEAN);
    map.put(FACTORY_TEXT.getType(), FACTORY_TEXT);
    map.put(FACTORY_MAP.getType(), FACTORY_MAP);
    map.put(ListByteSerializer.FACTORY.getType(), ListByteSerializer.FACTORY);
    map.put(FACTORY_INTEGER.getType(), FACTORY_INTEGER);
    FACTORIES = map;
  }

  public static ByteSerializer<?> readSerializer(ByteArray.Stream stream) {
    byte type = stream.nextByte();
    SerializerFactory factory = FACTORIES.get(type);
    if (factory == null) {
      LogHelper.error("Unknown type", type);
      return null;
    }
    return factory.readSerializer(stream);
  }

  public static class MapToBytesSerializer extends ByteSerializer<Map<? extends TypedKey<?>, ?>> {
    private MapToBytesSerializer() {
      super((Class) Map.class);
    }

    @Override
    public void writeType(ByteArray target) {
      target.addByte(TYPE_MAP);
    }

    @Override
    public boolean buildSchema(MapSerializer context, Object value) {
      if (value == null) return true;
      Map valueMap = Util.castNullable(Map.class, value);
      if (valueMap == null) {
        LogHelper.error("Expected Map, but was", value);
        return false;
      }
      return context.buildSchema(valueMap);
    }

    @Override
    public boolean write(MapSerializer context, @NotNull Map<? extends TypedKey<?>, ?> map, ByteArray target) {
      int offset = target.size();
      target.addLong(0);
      for (MapSerializer.ToByteSerializer<?> serializer : context.getSerializers()) {
        if (!writeValue(context, target, serializer, map)) return false;
      }
      target.setLong(offset, target.size() - offset);
      return true;
    }

    private <T> boolean writeValue(MapSerializer context, ByteArray target, MapSerializer.ToByteSerializer<T> serializer, Map<? extends TypedKey<?>, ?> map) {
      TypedKey<T> key = context.getKnownKey(serializer);
      if (key == null) {
        LogHelper.error("Missing key", serializer.getName());
        return false;
      }
      T value = key.getFrom(map);
      if (value == null) return true;
      target.addInt(serializer.getId());
      return serializer.write(context, value, target);
    }

    @Override
    public Map<TypedKey<?>, ?> readValue(MapDeserializer context, ByteArray.Stream stream) {
      long length = stream.nextLong();
      ByteArray.Stream mapStream = stream.subStream((int) length - 8);
      if (mapStream.isErrorOccurred()) return null;
      HashMap<TypedKey<?>, ?> result = Collections15.hashMap();
      while (!mapStream.isAtEnd()) {
        int id = mapStream.nextInt();
        if (mapStream.isErrorOccurred()) return null;
        MapDeserializer.FromByteDeserializer deserializer = context.getDeserializer(id);
        if (deserializer == null) return null;
        if (!deserializer.restore(context, mapStream, result)) return null;
      }
      return result;
    }
  }
}
