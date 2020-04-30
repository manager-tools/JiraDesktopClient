package com.almworks.extservice;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.items.api.Database;
import com.almworks.tracker.eapi.alpha.ArtifactLoadOption;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.xmlrpc.MessageOutbox;
import com.almworks.util.xmlrpc.OutgoingMessage;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import util.concurrent.SynchronizedBoolean;

import java.util.Map;
import java.util.Set;

public class EapiClient implements ItemSubscriptionController {
  private static final long MINIMUM_PONG_PERIOD = 4500;

  private final int myPort;
  private final Database myDatabase;
  private final Map<Configuration, Connection> myCreatingConnectionsLockMap;
  private final int myConnectionId = ((int) (System.currentTimeMillis() % 0xFFFFFFFFL) + new Object().hashCode());

  /**
   * Maps urls of subscribed items to Subscription
   */
  private final Map<String, ItemSubscription> mySubscriptions = Collections15.hashMap();
  private final NodeMonitor myNodeMonitor;
  private final Engine myEngine;
  private final MessageOutbox myOutbox;
  private final SynchronizedBoolean myStopped = new SynchronizedBoolean(false);

  private String myName = "";
  private String myShortName = "";
  private long myLastPongTime = 0;

  public EapiClient(int port, Database database,
    Map<Configuration, Connection> creatingConnectionsLockMap, Engine engine, ExplorerComponent explorerComponent)
  {
    myPort = port;
    myDatabase = database;
    myCreatingConnectionsLockMap = creatingConnectionsLockMap;
    myEngine = engine;
    myOutbox = new MessageOutbox(port);
    myNodeMonitor = new NodeMonitor(database, explorerComponent, myOutbox, engine);
  }

  public void start() {
    myOutbox.start();
  }

  @CanBlock
  public void dispose() {
    if (!myStopped.commit(false, true))
      return;
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        ItemSubscription[] subscriptions =
          mySubscriptions.values().toArray(new ItemSubscription[mySubscriptions.size()]);
        mySubscriptions.clear();
        for (ItemSubscription subscription : subscriptions) {
          subscription.dispose();
        }
        myNodeMonitor.dispose();
      }
    });
    try {
      myOutbox.shutdown();
    } catch (InterruptedException e) {
      // ignore
    }
  }


  @ThreadAWT
  public void updateAll() {
    if (myStopped.get())
      return;
    ItemSubscription[] subscriptions =
      mySubscriptions.values().toArray(new ItemSubscription[mySubscriptions.size()]);
    for (ItemSubscription subscription : subscriptions) {
      subscription.sync(false);
    }
  }

  @ThreadAWT
  public void subscribe(String url, Set<ArtifactLoadOption> options) {
    if (myStopped.get())
      return;
    ItemSubscription subscription = mySubscriptions.get(url);
    if (subscription == null) {
      subscription = new ItemSubscription(url, options, this);
      mySubscriptions.put(url, subscription);
      subscription.sync(true);
    } else {
      boolean changed = subscription.addOptions(options);
      if (changed) {
        subscription.sync(true);
      } else {
        subscription.send();
      }
    }
  }

  @ThreadAWT
  public void unsubscribe(String url) {
    if (myStopped.get())
      return;
    mySubscriptions.remove(url);
  }


  public Map<Configuration, Connection> getCreatingConnectionsLockMap() {
    return myCreatingConnectionsLockMap;
  }

  public Engine getEngine() {
    return myEngine;
  }

  public int getConnectionId() {
    return myConnectionId;
  }

  public void send(OutgoingMessage message) {
    if (myStopped.get())
      return;
    myOutbox.enqueue(message);
  }

  public boolean updateNames(String name, String shortName) {
    shortName = Util.NN(shortName);
    name = Util.NN(name);
    boolean changed = !shortName.equals(myShortName) || !name.equals(myName);
    myShortName = shortName;
    myName = name;
    return changed;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  public void unwatchNode(String nodeId) {
    if (!myStopped.get())
      myNodeMonitor.unwatchNode(nodeId);
  }

  public void watchNode(String nodeId) {
    if (!myStopped.get())
      myNodeMonitor.watchNode(nodeId);
  }

  @NotNull
  public String getShortName() {
    return myShortName;
  }


  public String toString() {
    return getName();
  }

  public boolean confirmPong() {
    long now = System.currentTimeMillis();
    boolean result = now - myLastPongTime > MINIMUM_PONG_PERIOD;
    if (result) {
      myLastPongTime = now;
    }
    return result;
  }

  @Override
  public Database getDatabase() {
    return myDatabase;
  }
}
