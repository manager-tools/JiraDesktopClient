package com.almworks.items.sync;

import com.almworks.integers.LongList;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.detach.Detach;

public interface EditorLock {
  /**
   * @return sorted list of locked artifacts
   */
  LongList getItems();

  /**
   * Alive edits are if:<br>
   * 1. There is alive editor UI<br>
   * 2. Edit is about to start (preparation is in progress)
   * 3. Commit is in progress
   * @return true if edit is alive
   */
  boolean isAlive();

  /**
   * @return true if edit is already started but editor UI is not ready yet
   */
  boolean isPreparing();

  /**
   * @return true if commit started but not complete yet
   */
  boolean isCommitting();

  /**
   * Causes {@link com.almworks.items.sync.ItemEditor#activate()} to be called if it is alive.
   */
  void activateEditor() throws CantPerformException;

  /**
   * Asynchronous request to release locked items and close the editor. An "emergency" method called in special situations, like removing of the connection edited items belong to.
   */
  void release();

  public static class ToDetach extends Detach {
    private final EditorLock myLock;

    public ToDetach(EditorLock lock) {
      myLock = lock;
    }

    @Override
    protected void doDetach() throws Exception {
      myLock.release();
    }
  }
}
