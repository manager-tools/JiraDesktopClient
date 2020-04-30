package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.ValueModel;

import java.io.File;
import java.io.IOException;

/**
 * TrackerStarter allows to launch and connect to Deskzilla, track interconnection status, get version
 * information about Deskzilla.
 * <p>
 * TrackerStarter is also used to set up a method for finding Deskzilla instance.
 */
public interface TrackerStarter {
  /**
   * Returns value model for tracking connection status and version of Deskzilla on the other side of the wire.
   */
  ValueModel<TrackerConnectionStatus> getConnectionStatus();

  /**
   * Used to launch Deskzilla. The Process returns may not be Deskzilla, so use with care. Deskzilla launching
   * may fail for any reason.
   */
  Process startTracker(TrackerApplication application) throws IOException;

  /**
   * Used to launch Deskzilla with a explicit workspace directory set. The Process returns may not be Deskzilla, so use with care. Deskzilla launching
   * may fail for any reason.
   */
  Process startTracker(TrackerApplication application, File workspaceDirectory) throws IOException;

  /**
   * Requests that a connection with Deskzilla should be attempted.
   * <p>
   * TrackerStarter automatically tries to connect to Deskzilla. It will periodically try to learn the announced
   * Deskzilla API port and connect to it.
   * <p>
   * Use this method to force connection attempt right now.
   * <p>
   * This method does nothing if a connection is already established.
   */
  void tryConnect();

  /**
   * Sets the port to which to connect. Until there's a Deskzilla instance at this port, connection will not happen.
   */
  void useTrackerPort(int port);

  /**
   * Sets deskzilla workspace to which to connect. Until there's a Deskzilla instance that is running this workspace,
   * connection will not happen.
   *
   * @param workspaceDirectory not nullable deskzilla workspace directory
   */
  void useTrackerWorkspace(File workspaceDirectory);

  /**
   * Sets "discovery" method for finding Deskzilla. This method will find all currently running Deskzillas
   * and ask handler which instance to connect to.
   * <p>
   * If the handler is null, any Deskzilla will be selected.
   * <p>
   * This is the default method for connecting to a Deskzilla. (default handler is null)
   * <p>
   * Invoking this method when connection is established will cause re-connection
   * <p>
   * application parameter tells which flavour of tracker to run. setting it to null will
   * effectively disable connectivity
   */
  void useTrackerDiscovery(TrackerApplication application, TrackerDiscoveryHandler handler);

  /**
   * Reconnect using currently set connection method.
   */
  void reconnect();
}
