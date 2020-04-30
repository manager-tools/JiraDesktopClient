package com.almworks.api.engine;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface EngineListener {
  void onConnectionsChanged();

  /**
   * @param requireUI if true, all corresponding UI element (read: sync window) have to pop up.
   */
  void onSynchronizationRequested(boolean requireUI);

  public static abstract class Adapter implements EngineListener {
    public void onConnectionsChanged() {
    }

    public void onSynchronizationRequested(boolean requireUI) {
    }
  }
}
