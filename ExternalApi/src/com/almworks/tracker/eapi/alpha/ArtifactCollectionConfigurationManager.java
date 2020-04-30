package com.almworks.tracker.eapi.alpha;

import org.almworks.util.detach.Lifespan;

/**
 * This interface allows to interact with Deskzilla to select collections (queries, other nodes)
 * for monitoring.
 */
public interface ArtifactCollectionConfigurationManager {
  /**
   * Registers acceptor for commands from Deskzilla. The acceptor will
   * be invoked when the user chooses a collection in Deskzilla.
   * <p>
   * Only one acceptor may be registered!
   */
  void registerAddCollectionAcceptor(Lifespan life, AddCollectionAcceptor acceptor);

  /**
   * Sends a request to Deskzilla to take user focus and ask to select a collection.
   */
  void requestAddCollectionAction();

  /**
   * Sends a request to Deskzilla to add default collections without user input.
   */
  void requestAddDefaultCollections();
}
