package com.almworks.util.io.persist;

import org.almworks.util.Collections15;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * :todoc:
 *
 * @author sereda
 */
public class PersistableHashMap <K, V> extends LeafPersistable<Map<K, V>> {
  private final Persistable<K> myKeyBuilder;
  private final Persistable<V> myValueBuilder;
  private Map<K, V> myMap;

  public PersistableHashMap(Persistable<K> keyBuilder, Persistable<V> valueBuilder) {
    assert keyBuilder != null;
    assert valueBuilder != null;
    myKeyBuilder = keyBuilder;
    myValueBuilder = valueBuilder;
    setInitialized(true);
  }

  public static <K, V> PersistableHashMap<K, V> create(Persistable<K> keyBuilder, Persistable<V> valueBuilder) {
    return new PersistableHashMap<K, V>(keyBuilder, valueBuilder);
  }

  protected void doClear() {
    myMap = null;
  }

  protected Map<K, V> doAccess() {
    initMap();
    return myMap;
  }

  protected Map<K, V> doCopy() {
    return myMap == null ? Collections15.<K, V>hashMap() : Collections15.hashMap(myMap);
  }

  protected void doSet(Map<K, V> map) {
    assert map != null;
    myMap = Collections15.hashMap(map);
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
      myMap.put(myKeyBuilder.copy(), myValueBuilder.copy());
    }
    myKeyBuilder.clear();
    myValueBuilder.clear();
  }

  protected void doStore(DataOutput out) throws IOException {
    initMap();
    int count = myMap.size();
    CompactInt.writeInt(out, count);
    for (Iterator<Map.Entry<K, V>> ii = myMap.entrySet().iterator(); ii.hasNext();) {
      Map.Entry<K, V> entry = ii.next();
      myKeyBuilder.set(entry.getKey());
      myValueBuilder.set(entry.getValue());
      PersistableUtil.storePersistable(myKeyBuilder, out);
      PersistableUtil.storePersistable(myValueBuilder, out);
    }
    myKeyBuilder.clear();
    myValueBuilder.clear();
  }

  private void initMap() {
    if (myMap == null)
      myMap = Collections15.hashMap();
  }
}
