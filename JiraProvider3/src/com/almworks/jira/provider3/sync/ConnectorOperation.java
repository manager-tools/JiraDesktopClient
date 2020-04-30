package com.almworks.jira.provider3.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.SyncTask;
import com.almworks.restconnector.RestSession;

/**
 * An interface to perform sync operations. ConnectorManager calls {@link #perform(com.almworks.restconnector.RestSession)} method,
 * and after perform exists notifies the operation about the result.
 * {@link #onCompleted(com.almworks.api.engine.SyncTask.State)} method is call in any case, error (generic or cancelled) are called if the error happens.
 */
public interface ConnectorOperation {
  /**
   * Called after operation is cancelled or can not be started because of mis configured
   */
  void onCancelled();

  /**
   * Called to perform the operation.
   * @param session
   */
  void perform(RestSession session) throws ConnectorException;

  /**
   * Last call of the object. Called in any case (if the object passed to ConnectorManager)
   */
  void onCompleted(SyncTask.State result);

  /**
   * Call if an (not cancel) exception occurred.
   */
  void onError(ConnectorException e);
}
