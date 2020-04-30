package com.almworks.util.cache;

/**
 * :TODO: Write javadoc comment about this class/interface.
 *
 * @author sereda
 */
public interface CachedObjectLoader <K, T extends CachedObject<K>> {
  T loadObject(K key);
}
