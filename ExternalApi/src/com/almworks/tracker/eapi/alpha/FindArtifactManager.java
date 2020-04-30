package com.almworks.tracker.eapi.alpha;

import org.almworks.util.detach.Lifespan;

/**
 * Allows to register FindArtifactAcceptor to receive commands from Deskzilla
 * to "search for" artifacts.
 */
public interface FindArtifactManager {
  /**
   * Registers an acceptor. Only one acceptor may be registered at a time!
   *
   * @see FindArtifactAcceptor
   */
  void registerFindArtifactAcceptor(Lifespan life, FindArtifactAcceptor acceptor);
}
