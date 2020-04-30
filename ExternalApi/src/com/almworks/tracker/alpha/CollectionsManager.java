package com.almworks.tracker.alpha;

import com.almworks.dup.util.EventListener;
import com.almworks.dup.util.*;
import com.almworks.tracker.eapi.alpha.*;
import com.almworks.util.xmlrpc.CloningIncomingMessage;
import com.almworks.util.xmlrpc.EndPoint;
import com.almworks.util.xmlrpc.MessageProcessingException;
import com.almworks.util.xmlrpc.SimpleOutgoingMessage;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.util.*;

import static com.almworks.tracker.alpha.AlphaProtocol.Messages.ToClient;
import static com.almworks.tracker.alpha.AlphaProtocol.Messages.ToTracker;

class CollectionsManager
  implements ArtifactCollectionConfigurationManager, ArtifactCollectionMonitor, EventProcessorMaster
{
  private final EventProcessor<CollectionUpdateEvent> myEventProcessor =
    new EventProcessor<CollectionUpdateEvent>(this);
  private final EndPoint myEndPoint;
  private final ValueModel<TrackerConnectionStatus> myStatusModel;
  private final Map<String, Set<Object>> mySubscriptions = new HashMap<String, Set<Object>>();
  private final Lifecycle myLife = new Lifecycle();

  private AddCollectionAcceptor myAddCollectionAcceptor;

  public CollectionsManager(EndPoint endPoint, ValueModel<TrackerConnectionStatus> statusModel) {
    myEndPoint = endPoint;
    myStatusModel = statusModel;
    myEndPoint.addIncomingMessageFactory(new AddWatchedCollectionMessage());
    myEndPoint.addIncomingMessageFactory(new CollectionUpdateMessage());
  }

  public void start() {
    myLife.cycle();
    new ConnectionStatusWatcher(myStatusModel) {
      protected void onConnect(int connectionId) {
        flushSubscription();
      }
    }.start(myLife.lifespan());
  }

  public void shutdown() {
    myLife.cycle();
  }

  private synchronized void flushSubscription() {
    int size = mySubscriptions.size();
    if (size > 0) {
      String[] ids = mySubscriptions.keySet().toArray(new String[size]);
      for (String id : ids) {
        sendWatchMessage(id);
      }
    }
  }

  private CollectionData buildCollectionData(Vector parameters) {
    String collectionId = (String) parameters.get(0);
    boolean valid = (Boolean) parameters.get(1);
    Hashtable props = (Hashtable) parameters.get(2);
    Map unmarshalledProps = unmarshall(props);
    final CollectionData data = new CollectionData(collectionId, valid, unmarshalledProps);
    return data;
  }

  private Map unmarshall(Map props) {
    Map result = new HashMap();
    for (Object o : props.entrySet()) {
      Map.Entry entry = ((Map.Entry) o);
      Object key = entry.getKey();
      if (ToClient.CollectionProps.NAME.equals(key)) {
        CollectionData.COLLECTION_NAME.putToMap(result, (String) entry.getValue());
      } else if (ToClient.CollectionProps.ICON.equals(key)) {
        // todo get icon
      } else {
        ApiLog.debug("cannot understand collection property " + key);
      }
    }
    return result;
  }

  public synchronized void registerAddCollectionAcceptor(Lifespan life, final AddCollectionAcceptor acceptor) {
    assert myAddCollectionAcceptor == null;
    myAddCollectionAcceptor = acceptor;
    life.add(new Detach() {
      protected void doDetach() {
        synchronized (CollectionsManager.this) {
          if (myAddCollectionAcceptor == acceptor)
            myAddCollectionAcceptor = null;
        }
      }
    });
  }

  private synchronized AddCollectionAcceptor getAcceptor() {
    return myAddCollectionAcceptor;
  }

  public <E extends Event> void afterListenerAdded(EventProcessor processor, Class<E> eventClass,
    EventListener<E> listener, Lifespan life)
  {
  }

  public void requestAddCollectionAction() {
    SimpleOutgoingMessage message = new SimpleOutgoingMessage(ToTracker.REQUEST_ADD_COLLECTION_ACTION, port(), false);
    myEndPoint.getOutbox().enqueue(message);
  }

  public void requestAddDefaultCollections() {
    myEndPoint.getOutbox().enqueue(new SimpleOutgoingMessage(ToTracker.REQUEST_ADD_COLLECTION_ACTION, port(), true));
  }

  private int port() {
    return myEndPoint.getInboxPort();
  }

  public EventSource<CollectionUpdateEvent> events() {
    return myEventProcessor;
  }

  public synchronized void watchCollection(final Object key, Lifespan life, final String collectionId) {
    if (life.isEnded())
      return;
    Set<Object> keys = mySubscriptions.get(collectionId);
    if (keys == null) {
      keys = new LinkedHashSet<Object>();
      keys.add(key);
      mySubscriptions.put(collectionId, keys);
      sendWatchMessage(collectionId);
    } else {
      keys.add(key);
    }
    life.add(new Detach() {
      protected void doDetach() {
        unwatchCollection(key, collectionId);
      }
    });
  }

  private void sendWatchMessage(String collectionId) {
    myEndPoint.getOutbox().enqueue(new SimpleOutgoingMessage(ToTracker.WATCH_COLLECTION, port(), collectionId));
  }

  private synchronized void unwatchCollection(Object key, String collectionId) {
    Set<Object> keys = mySubscriptions.get(collectionId);
    if (keys != null) {
      boolean removed = keys.remove(key);
      if (removed && keys.isEmpty()) {
        mySubscriptions.remove(collectionId);
        myEndPoint.getOutbox().enqueue(new SimpleOutgoingMessage(ToTracker.UNWATCH_COLLECTION, port(), collectionId));
      }
    }
  }

  private class AddWatchedCollectionMessage extends CloningIncomingMessage {
    public AddWatchedCollectionMessage() {
      super(ToClient.ADD_WATCHED_COLLECTION);
    }

    protected void process() throws MessageProcessingException {
      final AddCollectionAcceptor acceptor = getAcceptor();
      if (acceptor != null) {
        Vector parameters = getParameters();
        final CollectionData data = buildCollectionData(parameters);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            acceptor.acceptAddCollection(data);
          }
        });
      }
    }
  }


  private class CollectionUpdateMessage extends CloningIncomingMessage {
    public CollectionUpdateMessage() {
      super(ToClient.COLLECTION_UPDATE);
    }

    protected void process() throws MessageProcessingException {
      Vector parameters = getParameters();
      final CollectionData data = buildCollectionData(parameters);
      final Vector urls = (Vector) parameters.get(3);
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          myEventProcessor.fireEvent(new CollectionUpdateEvent(urls, data));
        }
      });
    }
  }
}
