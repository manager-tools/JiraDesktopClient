package com.almworks.api.application;

import com.almworks.util.properties.PropertyMap;
import javolution.util.SimplifiedFastMap;
import javolution.util.SimplifiedFastTable;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import util.external.BitSet2;

import java.util.List;
import java.util.Map;

public class ModelKeySetUtil {
  private static final ModelKeySetUtil INSTANCE = new ModelKeySetUtil();
  
  private final Map<ModelKey<?>, Integer> myBackward = new SimplifiedFastMap<ModelKey<?>, Integer>().setShared(true);
  private final List<ModelKey<?>> myForward = new SimplifiedFastTable<ModelKey<?>>();
  private final Map<BitSet2, BitSet2> myCachedSets = new SimplifiedFastMap<BitSet2, BitSet2>();

  @Nullable
  public static ModelKey<?> getKey(int index) {
    if (index < 0)
      return null;
    try {
      return INSTANCE.myForward.get(index);
    } catch (IndexOutOfBoundsException e) {
      assert false;
      Log.warn("no key for index " + index);
      return null;
    }
  }

  public static void addKey(BitSet2 keys, ModelKey<?> key) {
    Integer index = INSTANCE.myBackward.get(key);
    if (index == null) {
      synchronized(INSTANCE) {
        index = INSTANCE.myBackward.get(key);
        if (index == null) {
          assert INSTANCE.myBackward.size() == INSTANCE.myForward.size();
          index = INSTANCE.myBackward.size();
          INSTANCE.myBackward.put(key, index);
          INSTANCE.myForward.add(key);
        }
      }
    }
    assert index != null;
    keys.set(index);
  }

  public static void addKeysToMap(PropertyMap values, BitSet2 keys) {
    synchronized(values) {
      BitSet2 oldValue = ModelKey.ALL_KEYS.getValue(values);
      BitSet2 union;
      if (oldValue == null) {
        if (keys == null) {
          union = new BitSet2();
        } else {
          union = keys;
        }
      } else {
        if (keys == null) {
          union = oldValue;
        } else {
          union = new BitSet2();
          union.or(oldValue);
          union.or(keys);
        }
      }
      BitSet2 result = INSTANCE.myCachedSets.get(union);
      if (result == null) {
        synchronized(INSTANCE) {
          result = INSTANCE.myCachedSets.get(union);
          if (result == null) {
            result = union.unmodifiableCopy();
            INSTANCE.myCachedSets.put(result, result);
          }
        }
      }
      ModelKey.ALL_KEYS.setValue(values, result);
    }
  }

  public static void cleanupForTest() {
    INSTANCE.myBackward.clear();
    INSTANCE.myForward.clear();
    INSTANCE.myCachedSets.clear();
  }

  public static boolean contains(BitSet2 set, ModelKey<?> key) {
    for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
      if (Util.equals(ModelKeySetUtil.getKey(i), key))
        return true;
    }
    return false;
  }
}
