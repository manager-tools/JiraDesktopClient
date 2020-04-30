package com.almworks.extservice;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.tracker.alpha.AlphaProtocol;
import com.almworks.tracker.eapi.alpha.ArtifactInfoStatus;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.xmlrpc.OutgoingMessage;
import com.almworks.util.xmlrpc.SimpleOutgoingMessage;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;

class ExtServiceUtils {
  public static GenericNode getNarrowingNode(ActionContext context) throws CantPerformException {
    GenericNode node;
    node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
    while (!node.isNarrowing()) {
      GenericNode parent = node.getParent();
      if (parent == null)
        break;
      node = parent;
    }
    return node;
  }

  static ThreadGate gate() {
    return ThreadGate.LONG(ExternalApiService.class);
  }

  static OutgoingMessage createArtifactInfoMessage(@NotNull String url, @NotNull ArtifactInfoStatus status,
    long timestamp, @Nullable String shortDescription, @Nullable String longDescription, @Nullable String id,
    @Nullable String summary)
  {
    Hashtable hashtable =
      createArtifactInfoHashtable(url, status, timestamp, shortDescription, longDescription, id, summary);
    return new SimpleOutgoingMessage(AlphaProtocol.Messages.ToClient.ARTIFACT_INFO, hashtable);
  }

  public static Hashtable createArtifactInfoHashtable(@NotNull String url, @NotNull ArtifactInfoStatus status,
    long timestamp, @Nullable String shortDescription, @Nullable String longDescription, @Nullable String id,
    @Nullable String summary)
  {
    Hashtable hashtable = new Hashtable();
    hashtable.put(AlphaProtocol.Messages.ToClient.ArtifactInfo.URL, url);
    hashtable.put(AlphaProtocol.Messages.ToClient.ArtifactInfo.STATUS, status.getExternalName());
    hashtable.put(AlphaProtocol.Messages.ToClient.ArtifactInfo.TIMESTAMP_SECONDS, (int) (timestamp / 1000L));
    hashtable.put(AlphaProtocol.Messages.ToClient.ArtifactInfo.SHORT_DESCRIPTION, Util.NN(shortDescription));
    hashtable.put(AlphaProtocol.Messages.ToClient.ArtifactInfo.LONG_DESCRIPTION, Util.NN(longDescription));
    if (id != null)
      hashtable.put(AlphaProtocol.Messages.ToClient.ArtifactInfo.ID, id);
    hashtable.put(AlphaProtocol.Messages.ToClient.ArtifactInfo.SUMMARY, Util.NN(summary));
    return hashtable;
  }
}