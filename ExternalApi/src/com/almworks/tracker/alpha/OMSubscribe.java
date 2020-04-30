package com.almworks.tracker.alpha;

import com.almworks.tracker.eapi.alpha.ArtifactLoadOption;
import com.almworks.util.xmlrpc.OutgoingMessage;

import java.util.*;

class OMSubscribe extends OutgoingMessage {
  private final Map<String,Set<ArtifactLoadOption>> myMap;
  private final int myInboxPort;

  public OMSubscribe(Map<String, Set<ArtifactLoadOption>> urls, int inboxPort) {
    myInboxPort = inboxPort;
    myMap = new HashMap<String, Set<ArtifactLoadOption>>();
    for (Map.Entry<String, Set<ArtifactLoadOption>> entry : urls.entrySet()) {
      myMap.put(entry.getKey(), new HashSet<ArtifactLoadOption>(entry.getValue()));
    }
  }

  protected String getRpcMethod() {
    return AlphaProtocol.Messages.ToTracker.SUBSCRIBE;
  }

  protected Collection<?> getRpcParameters() {
    Hashtable table = new Hashtable();
    for (Map.Entry<String, Set<ArtifactLoadOption>> entry : myMap.entrySet()) {
      Vector options = new Vector();
      for (ArtifactLoadOption option : entry.getValue()) {
        options.add(option.getExternalName());
      }
      table.put(entry.getKey(), options);
    }
    Vector result = new Vector();
    result.add(myInboxPort);
    result.add(table);
    return result;
  }
}
