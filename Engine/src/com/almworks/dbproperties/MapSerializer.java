package com.almworks.dbproperties;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MapSerializer {
  private static final Comparator<? super ToByteSerializer> BY_NAME = new Comparator<ToByteSerializer>() {
    @Override
    public int compare(ToByteSerializer o1, ToByteSerializer o2) {
      return o1.myName.compareTo(o2.myName);
    }
  };

  private final Map<TypedKey<?>, ?> mySource;
  private final Map<TypedKey<?>, ToByteSerializer> mySchema = Collections15.hashMap();
  private final List<Map<?, ?>> myStack = Collections15.arrayList();
  private final SerializeSchema mySerializeSchema;
  private List<ToByteSerializer> mySerializers;

  private MapSerializer(Map<TypedKey<?>, ?> source, SerializeSchema serializeSchema) {
    mySource = source;
    mySerializeSchema = serializeSchema;
  }

  @NotNull
  public List<ToByteSerializer> getSerializers() {
    return mySerializers;
  }

  @Nullable
  public <T> TypedKey<T> getKnownKey(@NotNull ToByteSerializer<T> serializer) {
    //noinspection unchecked
    return (TypedKey<T>) mySerializeSchema.getKeyByName(serializer.getName());
  }

  @Nullable
  public static byte[] serialize(Map<TypedKey<?>, ?> source, @NotNull SerializeSchema serializeSchema) {
    MapSerializer serializer = new MapSerializer(source, serializeSchema);
    if (!serializer.buildSchema()) return null;
    ByteArray array = serializer.write();
    return array != null ? array.toNativeArray() : null;
  }

  private ByteArray write() {
    ByteArray target = new ByteArray();
    target.addByte(ByteSerializer.TYPE_MAP);
    target.addInt(mySerializers.size());
    for (ToByteSerializer serializer : mySerializers) serializer.appendDescription(target);
    if (ByteSerializer.MAP.write(this, mySource, target)) return target;
    LogHelper.error("Write failed");
    return null;
  }

  private boolean buildSchema() {
    if (!buildSchema(mySource)) return false;
    ToByteSerializer[] serializers = mySchema.values().toArray(new ToByteSerializer[mySchema.size()]);
    Arrays.sort(serializers, BY_NAME);
    for (int i = 0; i < serializers.length; i++) serializers[i].setId(i);
    mySerializers = Collections15.unmodifiableListCopy(serializers);
    return true;
  }

  public boolean buildSchema(Map<?, ?> map) {
    if (mySerializers != null) throw new IllegalStateException("Schema already built");
    for (Map<?, ?> prev : myStack) {
      if (map == prev) {
        LogHelper.error("Loop detected");
        return false;
      }
    }
    myStack.add(map);
    try {
      return doBuildSchema(map);
    } finally {
      myStack.remove(myStack.size() - 1);
    }
  }

  private boolean doBuildSchema(Map<?, ?> map) {
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Object k = entry.getKey();
      TypedKey<?> key = Util.castNullable(TypedKey.class, k);
      if (key == null) {
        LogHelper.error("Can not serialize key", k);
        return false;
      }
      ToByteSerializer<?> serializer = mySchema.get(key);
      if (serializer == null) {
        ByteSerializer<?> writer = mySerializeSchema.getSerializer(key);
        if (writer == null) {
          LogHelper.error("Unknown key", key);
          return false;
        }
        serializer = ToByteSerializer.create(key.getName(), writer);
        mySchema.put(key, serializer);
      }
      if (!serializer.mySerializer.buildSchema(this, entry.getValue())) return false;
    }
    return true;
  }

  public static class ToByteSerializer<T> {
    private int myId = -1;
    private final String myName;
    private final ByteSerializer<T> mySerializer;

    protected ToByteSerializer(String name, ByteSerializer<T> serializer) {
      myName = name;
      mySerializer = serializer;
    }

    public static <T> ToByteSerializer<T> create(String name, ByteSerializer<T> serializer) {
      return new ToByteSerializer<T>(name, serializer);
    }

    public void appendDescription(ByteArray target) {
      target.addUTF8(myName);
      mySerializer.writeType(target);
    }

    public String getName() {
      return myName;
    }

    public int getId() {
      return myId;
    }

    public void setId(int id) {
      myId = id;
    }

    public boolean write(MapSerializer context, T value, ByteArray target) {
      return mySerializer.write(context, value, target);
    }
  }
}
