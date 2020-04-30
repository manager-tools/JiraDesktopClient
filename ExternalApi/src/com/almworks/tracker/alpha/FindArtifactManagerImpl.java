package com.almworks.tracker.alpha;

import com.almworks.tracker.eapi.alpha.FindArtifactAcceptor;
import com.almworks.tracker.eapi.alpha.FindArtifactManager;
import com.almworks.util.xmlrpc.CloningIncomingMessage;
import com.almworks.util.xmlrpc.EndPoint;
import com.almworks.util.xmlrpc.MessageProcessingException;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

class FindArtifactManagerImpl implements FindArtifactManager {
  private final EndPoint myEndPoint;
  private FindArtifactAcceptor myAcceptor;

  public FindArtifactManagerImpl(EndPoint endPoint) {
    myEndPoint = endPoint;
    myEndPoint.addIncomingMessageFactory(new CloningIncomingMessage(AlphaProtocol.Messages.ToClient.FIND_ARTIFACTS) {
      protected void process() throws MessageProcessingException {
        Vector parameters = getParameters();
        final Set<String> set = new LinkedHashSet<String>();
        for (Object parameter : parameters) {
          set.add((String) parameter);
        }
        final FindArtifactAcceptor acceptor = getAcceptor();
        if (acceptor != null) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              acceptor.acceptFindArtifacts(set);
            }
          });
        }
      }
    });
  }

  public synchronized void registerFindArtifactAcceptor(Lifespan life, final FindArtifactAcceptor acceptor) {
    assert myAcceptor == null;
    myAcceptor = acceptor;
    life.add(new Detach() {
      protected void doDetach() {
        synchronized (FindArtifactManagerImpl.this) {
          if (myAcceptor == acceptor)
            myAcceptor = null;
        }
      }
    });
  }

  private synchronized FindArtifactAcceptor getAcceptor() {
    return myAcceptor;
  }
}
