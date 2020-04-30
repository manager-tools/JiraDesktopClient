package com.almworks.tracker.eapi.alpha;

public enum ConnectionState {
  /**
   * No connection - didn't try to connect. Call TrackerConnector.start().
   */
  NOT_CONNECTED,

  /**
   * No connection - cannot connect or connection refused or whatever else reason there may be.
   */
  CONNECTION_FAILED,

  /**
   * Connection terminated by the API. Most probably because you have called TrackerConnector.stop().
   */
  CONNECTION_CLOSED,

  /**
   * Connection established. You can get Deskzilla name and version from TrackerConnectionStatus.
   */
  CONNECTED
}
