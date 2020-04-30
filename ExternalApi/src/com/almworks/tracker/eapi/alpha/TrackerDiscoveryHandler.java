package com.almworks.tracker.eapi.alpha;

import java.io.File;
import java.util.List;

/**
 * Implement this interface to help connector to select a Deskzilla which to connect to.
 */
public interface TrackerDiscoveryHandler {
  /**
   * Tracker starter asks the interface implementor to select a workspace from the available list.
   * <p>
   * It is expected that after the selection is made (which may take time, e.g. to display a dialog), the implementor
   * will call {@link TrackerStarter#useTrackerWorkspace(java.io.File)}.
   * <p>
   * Any workspace may be selected, but only those listed in workspaces parameter are active at the moment.
   */
  void onTrackerSelectionRequired(List<File> workspaces, TrackerStarter starter);
}
