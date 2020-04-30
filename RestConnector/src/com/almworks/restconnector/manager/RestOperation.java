package com.almworks.restconnector.manager;

import com.almworks.api.connector.ConnectorException;
import com.almworks.restconnector.RestSession;

public interface RestOperation {
  void perform(RestSession session);

  /**
   * Call after {@link #perform(com.almworks.restconnector.RestSession)} exited with exception, but before {@link #onFinished(boolean)}.
   */
  void onException(ConnectorException e);

  /**
   * Called every time after operation is passed for perform. This is the last call when {@link #perform(com.almworks.restconnector.RestSession)} already exited (normally or with
   * exception)<br>
   * This method is call in any case even when {@link #onException(com.almworks.api.connector.ConnectorException)} is called.
   * @param success true if {@link #perform(com.almworks.restconnector.RestSession)} exited normally, false if exception has occurred.
   */
  void onFinished(boolean success);
}
