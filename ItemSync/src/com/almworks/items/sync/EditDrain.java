package com.almworks.items.sync;

public interface EditDrain extends DBDrain {
  /**
   * Discard local changes for the item and whole slave subtree
   * @return true if the whole subtree is successfully discarded (is in sync state now).<br>
   * false means that discard failed and no item is changed
   */
  boolean discardChanges(long item);

  /**
   * Marks the item manually merged - current conflict values becomes new base
   * @return creator to write merge result to trunk
   */
  ItemVersionCreator markMerged(long item);

  /**
   * Creates an item and immediatly marks it as local only (BASE = {Invisible=true}).<br>
   * This method is workaround to create new local items without any shadowable attribute set. If such an item is created
   * via {@link #createItem()} no BASE version is written for it and it becomes SYNCHRONIZED right after creation.
   */
  ItemVersionCreator createLocalItem();

  /**
   * Allows to change not locked item. <br>
   * If the item is not locked by another editor behaves like the item is locked at the moment when this method is invoked.
   * The editor edits the item state on the <u>commit</u> transaction (safely locked items are edited at the moment
   * of <u>edit begin</u> transaction).<br>
   * If the item is already locked by another editor behaves as if the item is edited by that editor (original state
   * corresponds to the moment when another editor has been started). <b>NOTE:</b> if another editor is committed later
   * it may overwrite the change.
   */
  ItemVersionCreator unsafeChange(long item);
}
