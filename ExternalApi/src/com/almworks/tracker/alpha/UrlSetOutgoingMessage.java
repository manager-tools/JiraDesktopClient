package com.almworks.tracker.alpha;

import com.almworks.util.xmlrpc.OutgoingMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

abstract class UrlSetOutgoingMessage extends OutgoingMessage {
  protected final Set<String> myUrls;
  private final int myPort;

  public UrlSetOutgoingMessage(int port, Collection<String> urls) {
    myPort = port;
    myUrls = new HashSet<String>(urls);
  }

  protected Collection<?> getRpcParameters() {
    if (myPort < 0) {
      return myUrls;
    } else {
      Vector vector = new Vector();
      vector.add(myPort);
      vector.addAll(myUrls);
      return vector;
    }
  }
}
