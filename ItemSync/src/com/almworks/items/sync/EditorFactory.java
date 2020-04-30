package com.almworks.items.sync;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import org.jetbrains.annotations.Nullable;

/**
 * Lifecycle:<br>
 * After {@link EditControl#start(EditorFactory)} is called AND returns <b>true</b></b> one of two methods is guaranteed call:
 * <ul>
 *   <li>{@link #prepareEdit(com.almworks.items.api.DBReader, EditPrepare) prepareEdit} if the edit is permitted. If not null editor is create it is notified if the edit cancelled in the future.
 *   If null editor is created it means the edit is cancelled by this factory.</li>
 *   <li>{@link #editCancelled()} if the edit is not possible for some reason (other edit already in progress, some items do not exist, or other problem)</li>
 * </ul>
 */
public interface EditorFactory {
  /**
   * Collect data required for edit and create editor
   * @return prepared editor. Null means that edit has to be cancelled
   */
  @Nullable
  ItemEditor prepareEdit(DBReader reader, @Nullable EditPrepare prepare) throws DBOperationCancelledException;

  /**
   * Called if {@link #prepareEdit(com.almworks.items.api.DBReader, EditPrepare)} method wont ever be called.<br>
   * SyncManager does not guarantee that no EditFactory methods are called after this method is called, including that the method is called only once.<br>
   * This method may be called from any thread so it should finish quickly.
   */
  void editCancelled();
}
