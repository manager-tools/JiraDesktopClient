package com.almworks.tracker.alpha;

import com.almworks.dup.util.ApiLog;
import com.almworks.dup.util.EventProcessor;
import com.almworks.dup.util.EventSource;
import com.almworks.dup.util.ValueModel;
import com.almworks.tracker.eapi.alpha.*;
import com.almworks.util.xmlrpc.EndPoint;
import com.almworks.util.xmlrpc.IncomingMessage;
import com.almworks.util.xmlrpc.IncomingMessageFactory;
import com.almworks.util.xmlrpc.MessageProcessingException;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.util.*;

class ArtifactLoaderImpl implements ArtifactLoader {
  private final EndPoint myEndPoint;
  private final ValueModel<TrackerConnectionStatus> myStatusModel;
  private final Lifecycle myLife = new Lifecycle();
  private final Map<String, Subscription> mySubscriptions = new HashMap<String, Subscription>();
  private final EventProcessor myEventsProcessor = new EventProcessor(null);

  public ArtifactLoaderImpl(EndPoint endPoint, ValueModel<TrackerConnectionStatus> statusModel) {
    myEndPoint = endPoint;
    myStatusModel = statusModel;
    endPoint.addIncomingMessageFactory(new IncomingMessageFactory() {
      public String getRpcMethodName() {
        return AlphaProtocol.Messages.ToClient.ARTIFACT_INFO;
      }

      public IncomingMessage createMessage(Vector parameters) throws Exception {
        return new ArtifactInfoMessage(parameters);
      }
    });
  }

  private void flushSubscription() {
    Set<String> urls = mySubscriptions.keySet();
    Map<String, Set<ArtifactLoadOption>> subscribe = new HashMap<String, Set<ArtifactLoadOption>>();
    for (String url : urls) {
      subscribe.put(url, mySubscriptions.get(url).getOptions());
    }
    subscribe(subscribe);
  }

  private void subscribe(Map<String, Set<ArtifactLoadOption>> urls) {
    myEndPoint.getOutbox().enqueue(new OMSubscribe(urls, port()));
  }

  private void unsubscribe(Set<String> urls) {
    myEndPoint.getOutbox().enqueue(new OMUnsubscribe(urls, port()));
  }

  private int port() {
    return myEndPoint.getInboxPort();
  }

  public void subscribeArtifacts(final Object key, Lifespan life, Collection<String> urls,
    ArtifactLoadOption[] options)
  {
    if (life.isEnded())
      return;
    if (options == null)
      options = ArtifactLoadOption.NONE;
    final Set<String> set = new HashSet<String>(urls);
    Map<String, Set<ArtifactLoadOption>> subscribe = new HashMap<String, Set<ArtifactLoadOption>>();
    for (String url : set) {
      Subscription subscription = mySubscriptions.get(url);
      if (subscription == null) {
        subscription = new Subscription(url, key, options);
        mySubscriptions.put(url, subscription);
        subscribe.put(url, subscription.getOptions());
      } else {
        subscription.addKey(key);
        boolean changed = subscription.addOptions(options);
        if (changed) {
          subscribe.put(url, subscription.getOptions());
        }
      }
    }
    if (subscribe.size() != 0)
      subscribe(subscribe);
    life.add(new Detach() {
      protected void doDetach() {
        unsubscribeArtifacts(key, set);
      }
    });
  }

  public void unsubscribeArtifacts(Object key, Collection<String> urls) {
    Set<String> unsubscribe = null;
    for (String url : urls) {
      Subscription subscription = mySubscriptions.get(url);
      if (subscription != null) {
        subscription.removeKey(key);
        if (subscription.isOrphan()) {
          if (unsubscribe == null)
            unsubscribe = new HashSet<String>();
          unsubscribe.add(url);
          mySubscriptions.remove(url);
        }
      }
    }
    if (unsubscribe != null)
      unsubscribe(unsubscribe);
  }

  public EventSource events() {
    return myEventsProcessor;
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

  private static class Subscription {
    private final String myUrl;
    private final Set myKeys = new HashSet();
    private final Set<ArtifactLoadOption> myOptions = new HashSet();

    public Subscription(String url, Object key, ArtifactLoadOption[] options) {
      myUrl = url;
      addKey(key);
      addOptions(options);
    }

    public boolean addOptions(ArtifactLoadOption[] options) {
      boolean changed = false;
      for (ArtifactLoadOption option : options) {
        boolean b = myOptions.add(option);
        changed |= b;
      }
      return changed;
    }

    public void addKey(Object key) {
      myKeys.add(key);
    }

    public void removeKey(Object key) {
      myKeys.remove(key);
    }

    public boolean isOrphan() {
      return myKeys.isEmpty();
    }

    public Set<ArtifactLoadOption> getOptions() {
      return myOptions;
    }


    public String toString() {
      return myUrl + " (" + myKeys.size() + ")";
    }
  }


  private class ArtifactInfoMessage extends IncomingMessage {
    private final List<ArtifactInfoImpl> myInfos = new ArrayList<ArtifactInfoImpl>();

    public ArtifactInfoMessage(Vector parameters) {
      for (Object parameter : parameters) {
        Hashtable hashtable = (Hashtable) parameter;
        String url = (String) hashtable.get(AlphaProtocol.Messages.ToClient.ArtifactInfo.URL);
        Integer seconds = (Integer) hashtable.get(AlphaProtocol.Messages.ToClient.ArtifactInfo.TIMESTAMP_SECONDS);
        long timestamp = 1000L * seconds;
        String shortDesc = (String) hashtable.get(AlphaProtocol.Messages.ToClient.ArtifactInfo.SHORT_DESCRIPTION);
        String longDesc = (String) hashtable.get(AlphaProtocol.Messages.ToClient.ArtifactInfo.LONG_DESCRIPTION);
        String id = (String) hashtable.get(AlphaProtocol.Messages.ToClient.ArtifactInfo.ID);
        String summary = (String) hashtable.get(AlphaProtocol.Messages.ToClient.ArtifactInfo.SUMMARY);
        String statusName = (String) hashtable.get(AlphaProtocol.Messages.ToClient.ArtifactInfo.STATUS);
        ArtifactInfoStatus status = ArtifactInfoStatus.forExternalName(statusName);
        if (status == null) {
          ApiLog.warn("unknown status " + statusName);
          continue;
        }
        ArtifactInfoImpl info = new ArtifactInfoImpl(status, url, timestamp);
        info.setPresentation(GenericKeys.SHORT_DESCRIPTION_HTML, shortDesc);
        info.setPresentation(GenericKeys.LONG_DESCRIPTION_HTML, longDesc);
        info.setPresentation(GenericKeys.ID, id);
        info.setPresentation(GenericKeys.SUMMARY, summary);
        info.setReadOnly();
        myInfos.add(info);
      }
    }

    protected void process() throws MessageProcessingException {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          for (ArtifactInfoImpl info : myInfos) {
            myEventsProcessor.fireEvent(new ArtifactInfoEvent(info));
          }
        }
      });
    }
  }
}
