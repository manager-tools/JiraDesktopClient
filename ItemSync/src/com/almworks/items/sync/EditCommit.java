package com.almworks.items.sync;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.util.threads.CanBlock;

public interface EditCommit {
  EditCommit DEAF = new Adapter();

  /**
   * Update db with user values
   * @throws com.almworks.items.api.DBOperationCancelledException if edit cannot be committed due to internal error or other reason
   */
  void performCommit(EditDrain drain) throws DBOperationCancelledException;

  /**
   * Called when transaction is committed.<br>
   * Called in db thread. All long or AWT jobs should be marshaled.
   * @param success commit transaction successfully committed, all data is stored to DB.
   */
  @CanBlock
  void onCommitFinished(boolean success);

  class Adapter implements EditCommit {
    @Override
    public void performCommit(EditDrain drain) throws DBOperationCancelledException {}
    @Override
    public void onCommitFinished(boolean success) {}
  }
}
