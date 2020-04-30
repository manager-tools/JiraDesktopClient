package com.almworks.tracker.eapi.alpha;

import java.io.File;

/**
 * TrackerConnector is the main facade of Deskzilla external API (version Alpha).
 * <p/>
 * Connector and all of its subcomponents work by sending xml-rpc calls to Deskzilla and processing responses.
 * Everything is done asynchronously. All calls should be made from the AWT thread.
 */
public interface TrackerConnector {
  /**
   * TrackerStarter is used to launch Deskzilla and to see if it's connected to. You can also get Deskzilla version
   * information.
   */
  TrackerStarter getTrackerStarter();

  /**
   * ArtifactLoader is used to subscribe to artifacts by URL and receive artifact data.
   */
  ArtifactLoader getArtifactLoader();

  /**
   * ArtifactOpener is used to open specific artifacts in Deskzilla.
   */
  ArtifactOpener getArtifactOpener();

  /**
   * Starts the connector. Before start() call, you may get subcomponent interfaces, register factories, and do
   * other preparatory work. All active calls are not guaranteed to work.
   */
  void start();

  /**
   * Stops the connector. All active subscriptions are dropped, events are stopped.
   */
  void stop();


  // ADDED FOR TRACKLINK 0.3

  /**
   * Sets various (user-level) properties of this connector. These properties are
   * passed to Deskzilla to identify this connector.
   * <p>
   * For IDEA plug-in: set {@link ConnectorProperty#NAME} to identify the instance
   * of IDEA (maybe with project).
   */
  <T> void setProperty(ConnectorProperty<T> property, T value);

  /**
   * Use FindArtifactManager to listen for "Find Usages" commands from Deskzilla.
   */
  FindArtifactManager getFindArtifactManager();


  // ADDED FOR TRACKLINK 0.4

  /**
   * Use ArtifactCollectionMonitor to subscribe for a collection content (query, bug set, etc)
   * and register listeners to receive updates when a collection changes.
   */
  ArtifactCollectionMonitor getArtifactCollectionMonitor();

  /**
   * Use ArtifactCollectionConfigurationManager to interact with Deskzilla about
   * selection of a collection to add to monitoring.
   */
  ArtifactCollectionConfigurationManager getArtifactCollectionConfigurationManager();

  /**
   * Configures logging facility that is used by API service.
   * The directory should exist and be writable.
   *
   * @see com.almworks.dup.util.ApiLog
   */
  void configureLogging(File logDirectory);
}
