package com.almworks.tracker.alpha;

import java.util.Set;

class OMUnsubscribe extends UrlSetOutgoingMessage {
  public OMUnsubscribe(Set<String> urls, int port) {
    super(port, urls);
  }

  protected String getRpcMethod() {
    return AlphaProtocol.Messages.ToTracker.UNSUBSCRIBE;
  }
}
