package com.almworks.items.sync;

import com.almworks.integers.LongList;

public interface EditPrepare {
  /**
   * @return items to prepare for edit.<br>
   * <b>Note</b> This may be different from {@link #getControl() getControl()}.{@link EditControl#getItems() getItems()},
   * since this method returns only items requested for prepare during current transaction, but EditControl returns all
   * locked (allowed for edit) items.
   * @see #addItems(com.almworks.integers.LongList)
   */
  // todo :clarify: not clear if items added by addItems appear in the return list
  LongList getItems();

  EditControl getControl();

  /**
   * Try to add more items to the preparing edit.
   * @param items to add
   * @return true means that all (probably empty set) items are successfully locked and are available for edit.<br>
   * false means that at least one item cannot be locked for edit right now.<br>
   * <b>Note</b> If some items has been locked for edit earlier that the method may return true but the items don't appear in
   * {@link #getItems()} result.
   * @see #getItems() 
   */
  // todo :language: in the doc
  boolean addItems(LongList items);
}
