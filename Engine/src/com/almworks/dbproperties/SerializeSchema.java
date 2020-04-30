package com.almworks.dbproperties;

import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SerializeSchema {
  private final Map<TypedKey<?>, ByteSerializer<?>> mySerializerMap = Collections15.hashMap();
  private final Map<String, TypedKey<?>> myKeyByName = Collections15.hashMap();

  public ByteSerializer<?> getSerializer(TypedKey<?> key) {
    return mySerializerMap.get(key);
  }

  public boolean addKey(TypedKey<?> key) {
    Class<?> valueClass = key.getValueClass();
    ByteSerializer<?> serializer = chooseScalar(valueClass);
    if (serializer != null) return addKey(key, serializer);
    else {
      LogHelper.error("Unknown class", valueClass, key);
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> ByteSerializer<T> chooseScalar(Class<T> valueClass) {
    if (Boolean.class == valueClass) return (ByteSerializer<T>) ByteSerializer.BOOLEAN;
    else if (String.class == valueClass) return (ByteSerializer<T>) ByteSerializer.TEXT;
    else if (Map.class == valueClass) return (ByteSerializer<T>) ByteSerializer.MAP;
    else if (Integer.class == valueClass) return (ByteSerializer<T>) ByteSerializer.INTEGER;
    else return null;
  }

  private boolean addKey(TypedKey<?> key, ByteSerializer<?> serializer) {
    if (mySerializerMap.get(key) != null) {
      LogHelper.error("Already known key", key);
      return false;
    }
    String name = key.getName();
    if (myKeyByName.get(key.getName()) != null) {
      LogHelper.error("Known key name", name);
      return false;
    }
    mySerializerMap.put(key, serializer);
    myKeyByName.put(name, key);
    return true;
  }

  public boolean isKnownKey(TypedKey<?> key) {
    return mySerializerMap.get(key) != null;
  }

  public Collection<? extends TypedKey<?>> getAllKeys() {
    return Collections.unmodifiableCollection(mySerializerMap.keySet());
  }

  public <T> boolean addListKey(TypedKey<List<T>> key, Class<T> elementClass) {
    ByteSerializer<T> serializer = chooseScalar(elementClass);
    if (serializer == null) {
      LogHelper.error("Unknown value class", elementClass, key);
      return false;
    }
    return addKey(key, ListByteSerializer.create(serializer));
  }

  public TypedKey<?> getKeyByName(String name) {
    return myKeyByName.get(name);
  }
}
