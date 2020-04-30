package com.almworks.util.io.persist;

import com.almworks.util.Pair;
import com.almworks.util.collections.MultiMap;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class PersistableMultiMap<K, V> extends LeafPersistable<MultiMap<K, V>> {
  private final Persistable<K> myKeyBuilder;
  private final Persistable<V> myValueBuilder;
  private MultiMap<K, V> myMap;

  public PersistableMultiMap(Persistable<K> keyBuilder, Persistable<V> valueBuilder) {
    assert keyBuilder != null;
    assert valueBuilder != null;
    myKeyBuilder = keyBuilder;
    myValueBuilder = valueBuilder;
    setInitialized(true);
  }

  public static <K, V> PersistableMultiMap<K, V> create(Persistable<K> keyBuilder, Persistable<V> valueBuilder) {
    return new PersistableMultiMap<K, V>(keyBuilder, valueBuilder);
  }

  protected void doClear() {
    myMap = null;
  }

  protected MultiMap<K, V> doAccess() {
    initMap();
    return myMap;
  }

  protected MultiMap<K, V> doCopy() {
    return myMap == null ? MultiMap.<K, V>create() : MultiMap.create(myMap);
  }

  protected void doSet(MultiMap<K, V> map) {
    assert map != null;
    myMap = MultiMap.create(map);
  }

  protected void doRestore(DataInput in) throws IOException, FormatException {
    initMap();
    myMap.clear();
    int count = CompactInt.readInt(in);
    if (count < 0)
      throw new FormatException("count " + count);
    for (int i = 0; i < count; i++) {
      PersistableUtil.restorePersistable(myKeyBuilder, in);
      PersistableUtil.restorePersistable(myValueBuilder, in);
      myMap.add(myKeyBuilder.copy(), myValueBuilder.copy());
    }
    myKeyBuilder.clear();
    myValueBuilder.clear();
  }

  protected void doStore(DataOutput out) throws IOException {
    initMap();
    List<Pair<K, V>> list = myMap.toPairList();
    int count = list.size();
    CompactInt.writeInt(out, count);
    for (Pair<K, V> entry : list) {
      myKeyBuilder.set(entry.getFirst());
      myValueBuilder.set(entry.getSecond());
      PersistableUtil.storePersistable(myKeyBuilder, out);
      PersistableUtil.storePersistable(myValueBuilder, out);
    }
    myKeyBuilder.clear();
    myValueBuilder.clear();
  }

  private void initMap() {
    if (myMap == null)
      myMap = MultiMap.create();
  }
}
