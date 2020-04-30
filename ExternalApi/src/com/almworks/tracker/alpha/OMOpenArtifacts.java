package com.almworks.tracker.alpha;

import java.util.Collection;

class OMOpenArtifacts extends UrlSetOutgoingMessage {
  public OMOpenArtifacts(Collection<String> urls, int port) {
    super(port, urls);
  }

  protected String getRpcMethod() {
    return AlphaProtocol.Messages.ToTracker.OPEN_ARTIFACTS;
  }
}
