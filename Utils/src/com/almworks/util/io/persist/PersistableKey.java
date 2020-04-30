package com.almworks.util.io.persist;

import org.almworks.util.TypedKey;
import org.almworks.util.TypedKeyRegistry;
import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PersistableKey<K extends TypedKey> extends LeafPersistable<K> {
  private K myKey;
  private final TypedKeyRegistry<K> myRegistry;
  private final Class<K> myKeyClass;

  public PersistableKey(TypedKeyRegistry<K> registry, Class<K> keyClass) {
    assert registry != null;
    assert keyClass != null;
    myKeyClass = keyClass;
    myRegistry = registry;
  }

  public PersistableKey(K value, TypedKeyRegistry<K> registry, Class<K> keyClass) {
    this(registry, keyClass);
    set(value);
  }

  protected void doSet(K value) {
    myKey = value;
  }

  protected K doAccess() {
    return myKey;
  }

  protected K doCopy() {
    return myKey;
  }

  protected void doClear() {
    myKey = null;
  }

  protected void doRestore(DataInput in) throws IOException, FormatException {
    String name = CompactChar.readString(in);
    TypedKey key = myRegistry.getKey(name);
    if (key == null)
      throw new FormatException("cannot find key with name " + name);
    if (!myKeyClass.isInstance(key))
      throw new FormatException(key + " is not an instance of " + myKeyClass);
    myKey = (K) key;
  }

  protected void doStore(DataOutput out) throws IOException {
    assert myKey != null;
    checkKey(myKey);
    CompactChar.writeString(out, myKey.getName());
  }

  private void checkKey(K key) {
    TypedKey regKey = myRegistry.getKey(key.getName());
    if (key != regKey)
      throw new IllegalStateException("key " + key + " is not in registry");
  }

  public static <K extends TypedKey> PersistableKey<K> create(final TypedKeyRegistry<K> registry,
    final Class<K> keyClass)
  {
    return new PersistableKey<K>(registry, keyClass);
  }
}
