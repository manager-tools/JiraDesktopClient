package com.almworks.items.sync;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBResult;

public interface DownloadProcedure<D extends DBDrain> {
  /**
   * Asked to perform DB update
   */
  void write(D drain) throws DBOperationCancelledException;

  /**
   * Notified that DB transaction is finished 
   * @param result
   */
  void onFinished(DBResult<?> result);
}
