package com.almworks.util.collections;

import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
public interface ElementVisitor<E> {
  /**
   * Passes next element to visitor.
   * @return hint to iterating agent whether to continue iteration. true - continue
   */
  boolean visit(E element);

  class Collector<E> implements ElementVisitor<E> {
    private E[] myCollected = (E[]) Const.EMPTY_OBJECTS;
    private int mySize = 0;

    public boolean visit(E element) {
      myCollected = ArrayUtil.ensureCapacity(myCollected, mySize + 1);
      myCollected[mySize] = element;
      mySize++;
      return true;
    }

    public final void clear() {
      if (mySize == 0) return;
      Arrays.fill(myCollected, 0, mySize, null);
      mySize = 0;
    }

    public List<E> copyCollectedAndClear() {
      List<E> result = Collections15.arrayList(mySize);
      for (int i = 0; i < mySize; i++) result.add(myCollected[i]);
      clear();
      return result;
    }

    public List<E> notEmptyCopyAndClear() {
      if (mySize == 0) return Collections15.emptyList();
      if (mySize == 1) {
        List<E> result = Collections.singletonList(myCollected[0]);
        clear();
        return result;
      }
      return copyCollectedAndClear();
    }
  }
}
