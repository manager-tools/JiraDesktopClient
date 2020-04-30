package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.Event;

/**
 * An event that holds ArtifactInfo.
 */
public class ArtifactInfoEvent extends Event {
  private final ArtifactInfo myArtifactInfo;

  public ArtifactInfoEvent(ArtifactInfo artifactInfo) {
    myArtifactInfo = artifactInfo;
  }

  public ArtifactInfo getArtifactInfo() {
    return myArtifactInfo;
  }
}
