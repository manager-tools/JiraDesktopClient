package com.almworks.util.ui.swing;

import com.almworks.util.collections.ElementVisitor;

/**
 * External iterator for trees. Supports termination and subtree skipping.
 * @param <T>
 */
public interface TreeElementVisitor<T> {

  /**
   * Accepts the current item and return controlling value for farther iteration
   * @param item
   * @return iteration control value
   * @see com.almworks.util.ui.swing.TreeElementVisitor.Result
   */
  Result visit(T item);

  enum Result {
    /**
     * Terminate iteration
     */
    STOP,
    /**
     * Skip subtree of current element
     */
    SKIP_SUBTREE,
    /**
     * Continue iteration.
     */
    GO_ON
  }


  /**
   * Adaptor for {@link com.almworks.util.collections.ElementVisitor} to walk through the whole tree
   */
  class WholeTree<T> implements TreeElementVisitor<T> {
    private final ElementVisitor<T> myIterator;

    public WholeTree(ElementVisitor<T> iterator) {
      myIterator = iterator;
    }

    public Result visit(T item) {
      if (!myIterator.visit(item)) return Result.STOP;
      return Result.GO_ON;
    }

    public ElementVisitor<T> getIterator() {
      return myIterator;
    }
  }
}
