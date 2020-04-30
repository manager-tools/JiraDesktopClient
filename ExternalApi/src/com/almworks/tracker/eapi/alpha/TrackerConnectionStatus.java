package com.almworks.tracker.eapi.alpha;

/**
 * This class holds status information about Deskzilla connection, including
 * Deskzilla version.
 *
 * @see TrackerStarter
 */
public class TrackerConnectionStatus {
  public static final TrackerConnectionStatus NOT_CONNECTED =
    new TrackerConnectionStatus(ConnectionState.NOT_CONNECTED);

  private final ConnectionState myConnectionState;
  private final String myTrackerName;
  private final String myTrackerVersion;
  private final String myWorkspaceDirectory;
  private final int myTrackerConnectionId;

  public TrackerConnectionStatus(ConnectionState connectionState) {
    this(connectionState, null, null, null, 0);
  }

  public TrackerConnectionStatus(ConnectionState connectionState, String trackerName, String trackerVersion,
    String workspaceDirectory, int trackerConnectionId)
  {
    myConnectionState = connectionState;
    myTrackerName = trackerName;
    myTrackerVersion = trackerVersion;
    myWorkspaceDirectory = workspaceDirectory;
    myTrackerConnectionId = trackerConnectionId;
  }

  public boolean isConnected() {
    return getConnectionState() == ConnectionState.CONNECTED;
  }

  public ConnectionState getConnectionState() {
    return myConnectionState;
  }

  /**
   * Returns the name of the connected application. ("Deskzilla")
   * Returns null if there's no connection.
   */
  public String getTrackerName() {
    return myTrackerName;
  }

  /**
   * Returns the connected application's version or null if there's
   * no connection.
   */
  public String getTrackerVersion() {
    return myTrackerVersion;
  }

  public String getWorkspaceDirectory() {
    return myWorkspaceDirectory;
  }

  public int getTrackerConnectionId() {
    return myTrackerConnectionId;
  }

  public String toString() {
    return myConnectionState.name() + ":" + myTrackerName + ":" + myTrackerVersion + ":" + myWorkspaceDirectory + ":" +
      myTrackerConnectionId;
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    final TrackerConnectionStatus that = (TrackerConnectionStatus) o;

    if (myTrackerConnectionId != that.myTrackerConnectionId)
      return false;
    if (myConnectionState != that.myConnectionState)
      return false;
    if (myTrackerName != null ? !myTrackerName.equals(that.myTrackerName) : that.myTrackerName != null)
      return false;
    if (myTrackerVersion != null ? !myTrackerVersion.equals(that.myTrackerVersion) : that.myTrackerVersion != null)
      return false;
    if (myWorkspaceDirectory != null ? !myWorkspaceDirectory.equals(that.myWorkspaceDirectory) :
      that.myWorkspaceDirectory != null)
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myConnectionState.hashCode();
    result = 29 * result + (myTrackerName != null ? myTrackerName.hashCode() : 0);
    result = 29 * result + (myTrackerVersion != null ? myTrackerVersion.hashCode() : 0);
    result = 29 * result + (myWorkspaceDirectory != null ? myWorkspaceDirectory.hashCode() : 0);
    result = 29 * result + myTrackerConnectionId;
    return result;
  }
}
