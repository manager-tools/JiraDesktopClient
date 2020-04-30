package com.almworks.util.properties;

import org.almworks.util.TypedKey;
import util.external.CompactChar;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * :todoc:
 *
 * @author sereda
 */
class StorableMapImpl extends PropertyMap implements StorableMap {
  private boolean myFixed = false;
  private static final int MARKER_NULL = 0;
  private static final int MARKER_STRING = 1;
  private static final int MARKER_INTEGER = 2;
  private static final int MARKER_ENTRY = 3;
  private static final int MARKER_MAPEND = 4;
  private int myHashCode = 0;
  private boolean myHashCodeSet = false;

  public StorableMapImpl() {
  }

  public StorableMap fix() {
    myFixed = true;
    return this;
  }

  public boolean isFixed() {
    return myFixed;
  }

  public <T> T put(TypedKey<T> key, T value) {
    checkNotFixed();
    return super.put(key, value);
  }

  public <T> void initValue(TypedKey<T> key, T value) {
    checkNotFixed();
    super.initValue(key, value);
  }

  public void clear() {
    checkNotFixed();
    super.clear();
  }

  public void restore(DataInput in, StorableKey sampleKey) throws IOException, StorableException {
    checkNotFixed();
    clear();
    while (true) {
      int marker = CompactInt.readInt(in);
      if (marker == MARKER_MAPEND)
        break;
      if (marker == MARKER_ENTRY) {
        StorableKey key = sampleKey.restore(in);
        Object value = restoreObject(in, key.getValueClass());
        put(key, value);
        continue;
      }
      throw new StorableException("bad marker " + marker);
    }
    fix();
  }

  private Object restoreObject(DataInput in, Class valueClass) throws IOException, StorableException {
    int marker = CompactInt.readInt(in);
    Object value = null;
    boolean set = true;
    switch (marker) {
    case MARKER_NULL:
      value = null;
      break;
    case MARKER_INTEGER:
      value = CompactInt.readInt(in);
      break;
    case MARKER_STRING:
      value = CompactChar.readString(in);
      break;
    default:
      set = false;
    }
    if (!set)
      throw new StorableException("unknown marker " + marker);
    if (value != null && !valueClass.isInstance(value))
      throw new StorableException("incompatible key class for value " + value);
    return value;
  }

  public void store(DataOutput out) throws IOException {
    checkFixed();
    for (TypedKey key : keySet()) {
      if (!(key instanceof StorableKey))
        continue;
      CompactInt.writeInt(out, MARKER_ENTRY);
      ((StorableKey) key).store(out);
      Object value = get(key);
      storeObject(out, value);
    }
    CompactInt.writeInt(out, MARKER_MAPEND);
  }

  private void checkFixed() {
    if (!isFixed())
      throw new IllegalStateException("map is not fixed");
  }

  private void storeObject(DataOutput out, Object value) throws IOException {
    if (value == null) {
      CompactInt.writeInt(out, MARKER_NULL);
    } else if (value instanceof String) {
      CompactInt.writeInt(out, MARKER_STRING);
      CompactChar.writeString(out, (String) value);
    } else if (value instanceof Integer) {
      CompactInt.writeInt(out, MARKER_INTEGER);
      CompactInt.writeInt(out, ((Integer) value).intValue());
    } else {
      throw new IllegalArgumentException("cannot understand value of class " + value.getClass());
    }
  }

  private void checkNotFixed() {
    if (isFixed())
      throw new IllegalStateException("map is fixed");
  }

  public synchronized int hashCode() {
    checkFixed();
    /*
     * Current hashcode implementation is quite bad, because we don't know
     * the order of elements.
     */
    if (!myHashCodeSet) {
      myHashCode = 0;
      for (TypedKey key : keySet()) {
        Object o = get(key);
        myHashCode = myHashCode + key.hashCode() * 11 + (o == null ? 0 : o.hashCode());
      }
      myHashCodeSet = true;
    }
    return myHashCode;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof StorableMapImpl))
      return false;
    if (obj == this)
      return true;
    checkFixed();
    StorableMapImpl that = (StorableMapImpl) obj;
    if (!that.isFixed())
      throw new IllegalArgumentException(that + " is not fixed");
    Map map = copyTo(new HashMap());
    for (TypedKey key : that.keySet()) {
      Object thatValue = that.get(key);
      Object thisValue = map.remove(key);
      if (thisValue == null) {
        // check for null value
        if (thatValue != null || !containsKey(key))
          return false;
      } else {
        if (!thisValue.equals(thatValue))
          return false;
      }
    }
    return map.size() == 0;
  }

  public StorableMap newMap() {
    return new StorableMapImpl();
  }
}
