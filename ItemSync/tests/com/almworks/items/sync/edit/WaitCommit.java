package com.almworks.items.sync.edit;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.items.sync.util.TestReference;
import junit.framework.Assert;

public class WaitCommit implements EditCommit {
  private final TestReference<Boolean> myDone = TestReference.create();

  @Override
  public void performCommit(EditDrain drain) throws DBOperationCancelledException {}

  @Override
  public void onCommitFinished(boolean success) {
    Assert.assertTrue(myDone.compareAndSet(null, success));
  }

  public static WaitCommit addTo(AggregatingEditCommit aggregator) {
    WaitCommit wait = new WaitCommit();
    aggregator.addProcedure(null, wait);
    return wait;
  }

  public boolean waitForAllDone() throws InterruptedException {
    return myDone.waitForPublished();
  }
}
