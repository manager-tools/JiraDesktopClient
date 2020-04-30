package com.almworks.util.cache2;

import com.almworks.util.cache.Keyed;

/**
 * :TODO: Write javadoc comment about this class/interface.
 *
 * @author sereda
 */
public abstract class CachedObject <K> extends Keyed<K> {
  /**
   * Weight for the cache. Objects with lower weight access removed first.
   * This field is package-visible, but other classes may do only reading!
   * To modify this field, use methods of this class.
   * <p/>
   * Weight operation are intentionally not synchronized. This may lead to
   * the corruption of weight, but it's no big deal: all possible consequences
   * are that the object may have greater weight than it actually should have had.
   * Since corruption is possible on intensive access, which by itself means that
   * the object is popular, this is perfectly tolerable.
   */
  volatile int myWeight;

  public CachedObject(K key) {
    super(key);
  }

  final void accessed() {
    myWeight = myWeight + 1;
  }

  final void degradeWeight() {
    myWeight = myWeight >> 1;
  }

  final void setWeight(int weight) {
    myWeight = weight;
  }
}
