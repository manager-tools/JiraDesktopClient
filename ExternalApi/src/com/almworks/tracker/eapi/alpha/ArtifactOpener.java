package com.almworks.tracker.eapi.alpha;

import java.util.Collection;

/**
 * This interface allows to make Deskzilla open a tab with given URLs.
 */
public interface ArtifactOpener {
  void openArtifacts(Collection<String> urls);
}
