package com.almworks.util.collections;

public interface PairVisitor<E1, E2> {
  /**
   * Passes next element to visitor.
   * @return hint to iterating agent whether to continue iteration. true - continue
   */
  boolean visitFirst(E1 element);

  /**
   * Passes next element to visitor.
   * @return hint to iterating agent whether to continue iteration. true - continue
   */
  boolean visitSecond(E2 element);
}
