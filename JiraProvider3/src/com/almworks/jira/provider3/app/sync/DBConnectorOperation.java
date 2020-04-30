package com.almworks.jira.provider3.app.sync;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.SyncTask;
import com.almworks.jira.provider3.sync.ConnectorOperation;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;

import java.util.Collection;

interface DBConnectorOperation extends ConnectorOperation {
  DBConnectorOperation[] EMPTY_ARRAY = new DBConnectorOperation[0];

  /** @throws ConnectorException see JC-829 */
  long getLastIcn() throws ConnectorException;

  /**
   * Composite operation. When perform it ignores (not cancelled) exceptions thrown by elements.
   */
  class Composite implements DBConnectorOperation {
    private final DBConnectorOperation[] myOperations;

    public Composite(Collection<? extends DBConnectorOperation> operations) {
      myOperations = operations != null ? operations.toArray(new DBConnectorOperation[operations.size()]) : EMPTY_ARRAY;
    }

    @Override
    public long getLastIcn() throws ConnectorException {
      long lastIcn = 0;
      ConnectorException exception = null;
      for (DBConnectorOperation operation : myOperations) {
        try {
          lastIcn = Math.max(lastIcn, operation.getLastIcn());
        } catch (ConnectorException e) {
          exception = e;
        }
      }
      if (lastIcn == 0 && exception != null) throw exception;
      return lastIcn;
    }

    @Override
    public void onCancelled() {
      for (DBConnectorOperation operation : myOperations) operation.onCancelled();
    }

    @Override
    public void perform(RestSession session) throws CancelledException {
      CancelledException cancelled = null;
      for (DBConnectorOperation operation : myOperations) {
        try {
          if (cancelled != null) operation.onCancelled();
          else operation.perform(session);
        } catch (CancelledException c) {
          cancelled = c;
          operation.onCancelled();
        } catch (ConnectorException e) {
          LogHelper.debug("Ignoring exception in composite", e);
          operation.onError(e);
        }
      }
      if (cancelled != null) throw cancelled;
    }

    @Override
    public void onCompleted(SyncTask.State result) {
      for (DBConnectorOperation operation : myOperations) operation.onCompleted(result);
    }

    @Override
    public void onError(ConnectorException e) {
      LogHelper.error("Should not happen");
    }
  }
}
