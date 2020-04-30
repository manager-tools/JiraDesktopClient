package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.Event;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Received when collection content changes or when collection status changes.
 */
public class CollectionUpdateEvent extends Event {
  private final CollectionData myCollectionData;
  private final Collection<String> myArtifactUrls;

  public CollectionUpdateEvent(Collection<String> artifactUrls, CollectionData collectionData) {
    myArtifactUrls = new LinkedHashSet<String>(artifactUrls);
    myCollectionData = collectionData;
  }

  /**
   * Return urls of artifacts that currently belong to this connection.
   * Nullable.
   */
  public Collection<String> getArtifactUrls() {
    return myArtifactUrls;
  }

  /**
   * Returns collection information. Not null.
   */
  public CollectionData getCollectionData() {
    return myCollectionData;
  }
}
