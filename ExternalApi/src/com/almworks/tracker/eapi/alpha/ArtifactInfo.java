package com.almworks.tracker.eapi.alpha;

import java.util.Collection;

/**
 * Provides information about a single artifact. Delivered with ArtifactInfoEvent.
 * <p>
 * First check the status - if it's not OK, then all other methods except getUrl() won't return
 * anything meaningful.
 *
 * @see ArtifactLoader
 */
public interface ArtifactInfo {
  /**
   * Returns the unique id of the artifact (URL).
   * May return null.
   * If null is returned, you may ignore this artifact info.
   */
  String getUrl();

  /**
   * Returns the status of the artifact. Not null.
   */
  ArtifactInfoStatus getStatus();

  /**
   * Returns modification timestamp. When status is not OK, return value is undefined.
   * <p>
   * NB: It is NOT true that the return value of this method will increase with time for a single artifact.
   */
  long getTimestamp();


  /**
   * Returns presentation of this artifact in a specified form, or null if presentation is not supported.
   */
  <T> T getPresentation(ArtifactPresentationKey<T> key);


  // ADDED FOR TRACKLINK 0.4

  /**
   * Returns a collection of keys that are applicable for this artifact.
   * When displaying a collection of artifacts, you have to union all cellPresentable keys to get all possible columns.
   *
   * @see ArtifactPresentationKey
   */
  Collection<ArtifactPresentationKey<?>> getApplicableKeys();
}
