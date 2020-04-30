package com.almworks.api.engine;

import com.almworks.util.Enumerable;
import com.almworks.util.commons.Condition;

/**
 * :todoc:
 *
 * @author sereda
 */
public final class ConnectionState extends Enumerable<ConnectionState> {
  public static final ConnectionState INITIAL = new ConnectionState("INITIAL");
  public static final ConnectionState STARTING = new ConnectionState("STARTING");
  public static final ConnectionState READY = new ConnectionState("READY");
  public static final ConnectionState STOPPING = new ConnectionState("STOPPING");
  public static final ConnectionState STOPPED = new ConnectionState("STOPPED");
  public static final ConnectionState REMOVED = new ConnectionState("REMOVED");

  public static final Condition<ConnectionState> GETTING_READY = new Condition<ConnectionState>() {
    public boolean isAccepted(ConnectionState connectionState) {
      return connectionState.isGettingReady();
    }
  };
  public static final Condition<ConnectionState> STABLE = new Condition<ConnectionState>() {
    public boolean isAccepted(ConnectionState connectionState) {
      return connectionState.isStable();
    }
  };
  public static final Condition<ConnectionState> IS_READY = new Condition<ConnectionState>() {
    public boolean isAccepted(ConnectionState connectionState) {
      return connectionState != null && connectionState.isReady();
    }
  };

  /**
   * Returns true if state is stable, i.e. Connection may remain in this state indefinitely long time.
   */
  public boolean isStable() {
    return this == READY || this == REMOVED || this == STOPPED;
  }

  public boolean isGettingReady() {
    return this == INITIAL || this == STARTING;
  }

  private ConnectionState(String name) {
    super(name);
  }

  public boolean isDegrading() {
    return  this == REMOVED || this == STOPPING || this == STOPPED;
  }

  public boolean isReady() {
    return this == READY;
  }
}
