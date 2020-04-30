package com.almworks.util.properties;

import org.almworks.util.TypedKey;
import org.almworks.util.TypedKeyRegistry;
import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class StorableKey <T> extends TypedKey<T> {
  private final TypedKeyRegistry myRegistry;

  public StorableKey(String name, Class<T> valueClass, TypedKeyRegistry registry) {
    super(name, valueClass, registry);
    assert registry != null;
    myRegistry = registry;
  }

  public void put(StorableMap map, T value) {
    StorableMapImpl impl = getImpl(map);
    impl.put(this, value);
  }

  private StorableMapImpl getImpl(StorableMap map) {
    assert map != null;
    if (!(map instanceof StorableMapImpl))
      throw new IllegalArgumentException(map.toString());
    return (StorableMapImpl) map;
  }

  public T get(StorableMap map) {
    StorableMapImpl impl = getImpl(map);
    return impl.get(this);
  }

  public T remove(StorableMap map) {
    StorableMapImpl impl = getImpl(map);
    return impl.remove(this);
  }

  public void store(DataOutput out) throws IOException {
    CompactChar.writeString(out, getName());
  }

  public StorableKey restore(DataInput in) throws IOException, StorableException {
    String name = CompactChar.readString(in);
    if (name == null)
      return null;
    TypedKey key = myRegistry.getKey(name);
    if (key == null)
      throw new StorableException("unknown key " + name);
    if (!(key instanceof StorableKey))
      throw new StorableException("unknown key class " + key.getClass().getName());
    return (StorableKey) key;
  }

  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (!getClass().equals(obj.getClass()))
      return false;
    StorableKey that = ((StorableKey) obj);
    if (!getName().equals(that.getName()))
      return false;
    if (!myRegistry.equals(that.myRegistry))
      return false;
    return true;
  }

  public int hashCode() {
    return (getClass().hashCode() * 59 + getName().hashCode()) * 59 + myRegistry.hashCode();
  }
}
