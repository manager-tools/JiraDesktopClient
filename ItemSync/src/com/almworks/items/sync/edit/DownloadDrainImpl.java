package com.almworks.items.sync.edit;

import com.almworks.items.api.DBResult;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;

class DownloadDrainImpl extends BaseDownloadDrain {
  private final DownloadProcedure<? super DBDrain> myProcedure;

  public DownloadDrainImpl(SyncManagerImpl manager, DownloadProcedure<? super DBDrain> procedure) {
    super(manager);
    myProcedure = procedure;
  }

  @Override
  protected void onTransactionFinished(DBResult<?> result) {
    myProcedure.onFinished(result);
  }

  @Override
  protected void performTransaction() {
    myProcedure.write(this);
  }
}
