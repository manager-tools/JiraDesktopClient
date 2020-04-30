package com.almworks.explorer.loader;

import com.almworks.api.application.*;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import util.external.BitSet2;

import java.util.Map;

/**
 * @author : Dyoma
 */
public class ModelMapImpl extends SimpleModifiable implements ModelMap {
  private static final int FIRE_CHANGES = -1;
  private final PropertyMap myValues = new PropertyMap();
  private final Map<String, ModelKey<?>> myKeys = Collections15.hashMap();
  private int myChangeCounter = FIRE_CHANGES;

  public <T> T get(TypedKey<? extends T> key) {
    return myValues.get(key);
  }

  public <T> void put(TypedKey<T> key, T value) {
    myValues.put((TypedKey) key, value);
  }

  public void registerKey(String name, ModelKey<?> key) {
    assert name != null;
    assert key != null;
    assert !myKeys.containsKey(name) || myKeys.get(name) == key : name + " " + key + " " + myKeys.get(name);
    myKeys.put(name, key);
  }

  public void valueChanged(ModelKey<?> key) {
    if (myChangeCounter == FIRE_CHANGES)
      fireChanged();
    else
      myChangeCounter++;
  }

  public MetaInfo getMetaInfo() {
    LoadedItemServices lis = LoadedItemServices.VALUE_KEY.getValue(this);
    if (lis == null) {
      assert false : this;
      return null;
    }
    return lis.getMetaInfo();
  }

  public boolean copyFrom(PropertyMap newValues) {
    assert checkSameKeys(newValues);
    myChangeCounter = 0;
    try {
      BitSet2 allKeys = ModelKey.ALL_KEYS.getValue(newValues);
      for (int i = allKeys.nextSetBit(0); i >= 0; i = allKeys.nextSetBit(i + 1)) {
        ModelKey<?> key = ModelKeySetUtil.getKey(i);
        if (key != null) {
          key.copyValue(this, newValues);
        }
      }
      if (myChangeCounter > 0) {
        fireChanged();
        return true;
      }
      return false;
    } finally {
      myChangeCounter = FIRE_CHANGES;
    }
  }

  private boolean checkSameKeys(PropertyMap values) {
    BitSet2 ownKeys = ModelKey.ALL_KEYS.getValue(this);
    BitSet2 otherKeys = ModelKey.ALL_KEYS.getValue(values);
    assert Util.equals(ownKeys, otherKeys) : "Own: " + otherKeys + " other: " + otherKeys;
    return true;
  }

  @Override
  public String toString() {
    return "MMI " + myValues;
  }
}
