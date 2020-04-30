package com.almworks.tracker.eapi.alpha;

import java.util.Collection;

/**
 * Implement this interface to receive requests from Deskzilla
 * to "search" for artifact links.
 */
public interface FindArtifactAcceptor {
  void acceptFindArtifacts(Collection<String> urls);
}
