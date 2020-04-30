package com.almworks.items.sync;

import com.almworks.integers.LongList;
import org.jetbrains.annotations.Nullable;

public interface EditControl extends EditorLock {
  /**
   * Locks items and invokes editor factory.
   * @param factory prepares edit
   * @return true iff lock succeeds, false means no items locked, factory was not called, and edit is not started 
   */
  boolean start(@Nullable EditorFactory factory);

  /**
   * Starts edit commit.
   * If edit commit fails, stays in "editing" state (as if commit has not started).
   * @param commit commit procedure
   * @return true if commit is started. false means that edit cannot be committed now because of it is already done or
   * not prepared (probably not started at all)
   */
  boolean commit(EditCommit commit);

  /**
   * Forces release lock. After calling this method surely be released in short time. But the lock probably wont be release
   * immediately (if commit is in progress).
   */
  void release();

  /**
   * Use this method to add items to this edit. It works only if this edit is in the "editing" state (has started and not committed). <br>
   * The editor is allowed to change new items just after it has loaded the values or successfully locks additional items
   * during prepare via {@link com.almworks.items.sync.EditPrepare#addItems(com.almworks.integers.LongList)}.<br><br>
   * <b>Note on exceptions during load</b><br>
   * If exception is thrown during load of items state all items which are already locked (initially provided and add
   * via {@link com.almworks.items.sync.EditPrepare#addItems(com.almworks.integers.LongList)} are kept locked for this edit.<br>
   * The reason. In case of composite loader it load may happen to be partially done and made available to user. The part
   * than has already succeeded may not get know (or be able to rollback) in case of other load part fails.<br>
   * On the other hand if some items are mistakenly locked due to exception the problem has time limited scope - when the
   * whole edit is done all items are released.
   * @param items items to be edited with current editor
   * @param loader the code to load actual items state before edit. If
   * {@link com.almworks.items.sync.EditorFactory#prepareEdit(com.almworks.items.api.DBReader, EditPrepare)}
   * returns null then the whole inclusion is cancelled. To avoid cancel loader has to return not null result.
   * It is recommended to {@link com.almworks.items.sync.ItemEditor#STUB}. The {@link ItemEditor#onEditReleased()}
   * is called immediatly when loading done.
   * @return false means that inclusion of specified items surely cannot be performed right now due to improper edit
   * state or due at least one item is already locked for another edit.
   */
  boolean include(LongList items, EditorFactory loader);
}
