package com.almworks.items.sync;

import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.actions.CantPerformException;
import gnu.trove.TLongObjectHashMap;

public interface ItemEditor {
  ItemEditor STUB = new ItemEditor() {
    @Override
    public boolean isAlive() {
      return false;
    }

    @Override
    public void showEditor() throws CantPerformException {
    }

    @Override
    public void activate() throws CantPerformException {
    }

    @Override
    public void onEditReleased() {
    }

    @Override
    public void onItemsChanged(TLongObjectHashMap<ItemValues> newValues) {
      assert false;
    }
  };

  /**
   * This method is used to check if editor has failed and held item locks should be released. This is required to protect
   * application from holding item locked forever because of failure.
   * @return true if editor is functional now. false if something goes wrong so edit should be cancelled and lock released
   */
  @ThreadSafe
  boolean isAlive();

  /**
   * Begin interaction with user. This method must return before commit is requested.<br>
   * Edit state is switched to EDITING when method returns. In this state edit has can be checked for aliveness and can be committed.
   * Edit cannot be committed before entering state EDIT since edit subsystem is not sure if editor is ready.<br>
   * To open modal editor implementation has to:<br>
   * 1. Enqueue invocation event that shows the modal editor UI.<br>
   * 2. Implement {@link #isAlive()} to return true since the event is enqueued even if UI is not shown yet
   * <br><b>NB!</b> If you need to commit immediately, use {@link com.almworks.util.exec.ThreadGate.AWT_QUEUED} to guarantee that the method returns before commit is requested.
   * @throws com.almworks.util.ui.actions.CantPerformException if failed to create UI or editor makes no sense any more
   */
  @ThreadAWT
  void showEditor() throws CantPerformException;

  /**
   * Activates editor component: e.g., if it is a window, it is brought to front. <br>
   * Called if editor is alive.
   * @throws com.almworks.util.ui.actions.CantPerformException if edit cannot be activated.<br>
   * The exception has no consequences for the edit, it just informs that editor was not activated.
   */
  @ThreadAWT
  void activate() throws CantPerformException;

  /**
   * Notification that edit is released. This is guaranteed last notification. Release resources if any and shutdown.
   */
  @ThreadAWT
  void onEditReleased();

  /**
   * Notification that concurrent edit has occurred.
   * @param newValues updated new values. Does not contain not changed values.
   */
  @ThreadAWT
  void onItemsChanged(TLongObjectHashMap<ItemValues> newValues);
}
