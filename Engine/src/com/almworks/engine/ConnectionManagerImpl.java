package com.almworks.engine;

import com.almworks.api.container.EventRouter;
import com.almworks.api.engine.*;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Condition;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.*;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;
import util.concurrent.SynchronizedBoolean;
import util.external.UID;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author : Dyoma
 */
class ConnectionManagerImpl implements ConnectionManager, Startable {
  private final ArrayListCollectionModel<Connection> myConnections = ArrayListCollectionModel.create(true, true);
  private final Map<ItemProvider, String> myIdByProvider = Collections15.hashMap();
  private final Map<String, ItemProvider> myProviderById = Collections15.hashMap();
  private final CollectionModel<Connection> myReadyConnections;
  private final SimpleModifiable myConnectionModifiable = new SimpleModifiable();

  private final Configuration myConfiguration;
  private final EngineListener myEventSink;
  private final Map<Connection, Detach> myConnectionsDetaches = Collections15.hashMap();
  private final List<Pair<ThreadGate, ConnectionChangeListener>> myChangeListeners = Collections15.arrayList();

  private final Map<Long, WeakReference<Connection>> myConnectionByItemCache = Collections15.hashMap();

  private final DetachComposite myLife = new DetachComposite();

  private final SynchronizedBoolean myLoaded = new SynchronizedBoolean(false);
  private final FireEventSupport<Runnable> myWaitingForLoading = FireEventSupport.createSynchronized(Runnable.class);

  public ConnectionManagerImpl(Configuration configuration, EventRouter eventRouter) {
    myConfiguration = configuration;
    myReadyConnections = createReadyConnectionsModel(myLife, myConnections);
    myConnections.getEventSource().addStraightListener(myLife, new CollectionModel.Adapter<Connection>() {
      @Override
      protected void onChange() {
        myConnectionModifiable.fireChanged();
      }
    });
    myEventSink = eventRouter.getEventSink(EngineListener.class, true);
  }

  private static CollectionModel<Connection> createReadyConnectionsModel(Lifespan life,
    CollectionModel<Connection> connections)
  {
    Convertor<Pair<Connection, Lifespan>, ScalarModel<Boolean>> convertor =
      new Convertor<Pair<Connection, Lifespan>, ScalarModel<Boolean>>() {
        public ScalarModel<Boolean> convert(Pair<Connection, Lifespan> value) {
          Connection connection = value.getFirst();
          Lifespan life = value.getSecond();
          return ModelUtils.convert(life, connection.getState(), Convertor.equality(ConnectionState.READY));
        }
      };
    return ModelUtils.filterDynamicSet(life, connections, convertor);
  }

  public void start() {
    initDetach();
  }

  @Override
  public void stop() {
  }

  @Nullable
  public Connection createConnection(ItemProvider provider, ReadonlyConfiguration config)
    throws ConfigurationException, ProviderDisabledException
  {
    Configuration subset = myConfiguration.createSubset(CommonConfigurationConstants.CONNECTION_TAG);
    ConfigurationUtil.copyTo(config, subset);
    subset.setSetting(CommonConfigurationConstants.CONNECTION_ID_TAG, createUniqueId());
    subset.setSetting(CommonConfigurationConstants.ITEM_PROVIDER_TAG, myIdByProvider.get(provider));
    return registerNewConnection(subset, provider, true);
  }

  public synchronized Connection getConnection(String connectionID) {
    for (Iterator<Connection> ii = myConnections.copyCurrent().iterator(); ii.hasNext();) {
      Connection connection = ii.next();
      if (connection.getConnectionID().equals(connectionID))
        return connection;
    }
    return null;
  }

  public String getConnectionName(String connectionID) {
    Configuration configuration = findConfiguration(connectionID);
    if (configuration == null)
      return "?";
    return configuration.getSetting(CommonConfigurationConstants.CONNECTION_NAME, "?");
  }

  public CollectionModel<Connection> getConnections() {
    return myConnections;
  }

  public synchronized Collection<ItemProvider> getProviders() {
    return Collections15.hashSet(myIdByProvider.keySet());
  }

  public void removeConnection(final Connection connection) {
    assert connection != null;
    final Configuration configuration = findConfiguration(connection.getConnectionID());
    connection.stopConnection();
    connection.removeConnection(new Runnable() { public void run() {
      if (configuration != null)
        configuration.removeMe();
      myConnections.getWritableCollection().remove(connection);
      myEventSink.onConnectionsChanged();
    }});
  }

  public void updateConnection(Connection connection, ReadonlyConfiguration newConfig) {
    assert connection != null;
    Configuration configuration = findConfiguration(connection.getConnectionID());
    if (configuration == null) {
      Log.warn("no configuration for " + connection);
      return;
    }
    ConfigurationUtil.copyTo(newConfig, configuration);
    try {
      connection.update(configuration);
    } catch(ConfigurationException e) {
      Log.warn(e);
      return;
    }
    configuration.setSetting(CommonConfigurationConstants.ITEM_PROVIDER_TAG, myIdByProvider.get(connection.getProvider()));
    configuration.setSetting(CommonConfigurationConstants.CONNECTION_ID_TAG, connection.getConnectionID());
    myEventSink.onConnectionsChanged();
  }

  @Nullable
  public Connection findByItem(long connectionItem) {
    return findByItem(connectionItem, false, false);
  }

  public Connection findByItem(long connectionItem, boolean wait, boolean ifReady) {
    if (connectionItem == 0)
      return null;
    synchronized (myConnectionByItemCache) {
      WeakReference<Connection> reference = myConnectionByItemCache.get(connectionItem);
      Connection result = reference == null ? null : reference.get();
      if (result == null) {
        result = findConnection(connectionItem, wait, ifReady);
        if (result != null) {
          myConnectionByItemCache.put(connectionItem, new WeakReference<Connection>(result));
        }
      }
      return result;
    }
  }

  private Connection findConnection(long connectionItem, boolean wait, boolean ifReady) {
//    if (wait) {
//      myLoaded.waitForValue(true);
//    }
    for (Connection connection : myConnections.copyCurrent()) {
      int attempt = 0;
      while (true) {
        try {
          ScalarModel<ConnectionState> stateModel = connection.getState();
          ConnectionState state;
          if (wait) {
            try {
              state = stateModel.waitValue(Condition.not(ConnectionState.GETTING_READY));
            } catch (InterruptedException e) {
              LogHelper.debug("Failed to wait for connection", connectionItem, connection);
              Thread.currentThread().interrupt();
              return null;
            }
          } else {
            state = stateModel.getValue();
          }
          if (state == ConnectionState.READY) {
            if (connection.getConnectionItem() == connectionItem) {
              return connection;
            }
          } else if (ifReady) return null;
          else {
            Log.debug("skipping connection " + connection, new Throwable());
          }
          break;
        } catch (IllegalStateException e) {
          // todo make checked exception
          // continue cycle
          if (++attempt > 5)
            break;
        }
      }
    }
    return null;
  }

  private void initDetach() {
    getConnections().getEventSource().addStraightListener(new CollectionModel.Adapter<Connection>() {
      public void onScalarsAdded(final CollectionModelEvent<Connection> event) {
        for (int i = 0; i < event.size(); i++) {
          final Connection connection = event.get(i);
          Detach detach =
            connection.getState().getEventSource().addStraightListener(new ScalarModel.Adapter<ConnectionState>() {
              public void onScalarChanged(ScalarModelEvent<ConnectionState> event) {
                ConnectionManagerImpl.this.onChange(connection, event.getOldValue(), event.getNewValue());
              }
            });
          synchronized (myConnectionsDetaches) {
            myConnectionsDetaches.put(connection, detach);
          }
        }
      }

      public void onScalarsRemoved(CollectionModelEvent<Connection> event) {
        //Set<Connection> connections = Collections15.unmodifiableSetCopy(event.getCollection());
        for (int i = 0; i < event.size(); i++) {
          final Connection connection = event.get(i);
          Detach detachable;
          synchronized (myConnectionsDetaches) {
            detachable = myConnectionsDetaches.remove(connection);
          }
          if (detachable != null) {
            try {
              detachable.detach();
            } catch (Exception e) {
              // ignore
              Log.warn(e);
            }
          }
        }
      }
    });
  }

  private void onChange(final Connection connection, final ConnectionState oldState, final ConnectionState newState) {
    Pair[] listeners;
    synchronized (myChangeListeners) {
      listeners = myChangeListeners.toArray(new Pair[myChangeListeners.size()]);
    }
    for (int i = 0; i < listeners.length; i++) {
      final Pair<ThreadGate, ConnectionChangeListener> pair = listeners[i];
      pair.getFirst().execute(new Runnable() {
        public void run() {
          pair.getSecond().onChange(connection, oldState, newState);
        }
      });
    }
  }

  public Detach addConnectionChangeListener(ThreadGate callBackGate, ConnectionChangeListener listener) {
    assert listener != null;
    assert callBackGate != null;
    if (callBackGate == null || listener == null)
      return Detach.NOTHING;
    final Pair<ThreadGate, ConnectionChangeListener> pair = Pair.create(callBackGate, listener);
    synchronized (myChangeListeners) {
      myChangeListeners.add(pair);
    }
    return new Detach() {
      protected void doDetach() {
        synchronized (myChangeListeners) {
          myChangeListeners.remove(pair);
        }
      }
    };
  }

  public CollectionModel<Connection> getReadyConnectionsModel() {
    return myReadyConnections;
  }

  public Modifiable getConnectionsModifiable() {
    return myConnectionModifiable;
  }

  public void fireConnectionsModifiable() {
    myConnectionModifiable.fireChanged();
  }

  public Connection getConnectionForUrl(String itemUrl) {
    Collection<Connection> connections = getReadyConnectionsModel().copyCurrent();
    for (Connection connection : connections) {
      if (connection.isItemUrl(itemUrl))
        return connection;
    }
    return null;
  }

  public ItemProvider getProviderForUrl(String url) {
    for (ItemProvider provider : myProviderById.values()) {
      try {
        if (provider.isItemUrl(url))
          return provider;
      } catch (ProviderDisabledException e) {
        // ignore
      }
    }
    return null;
  }

  @ThreadSafe
  public void waitUntilLoaded() throws InterruptedException {
    myLoaded.waitForValue(true);
  }

  public void whenConnectionsLoaded(Lifespan life, ThreadGate gate, Runnable runnable) {
    boolean runNow;
    synchronized (myLoaded.getLock()) {
      runNow = myLoaded.get();
      if (!runNow) {
        life.add(myWaitingForLoading.addListener(gate, runnable));
      }
    }
    if (runNow) {
      gate.execute(runnable);
    }
  }

  public synchronized void configureProviders(ItemProvider[] providers) {
    for (int i = 0; i < providers.length; i++) {
      if (providers[i] == null)
        continue;
      String id = providers[i].getProviderID();
      assert !myProviderById.containsKey(id);
      myProviderById.put(id, providers[i]);
      myIdByProvider.put(providers[i], id);
    }
  }

  public String createUniqueId() {
    return new UID().toString();
  }

  public void loadConnections() {
    List<? extends ReadonlyConfiguration> list =
      myConfiguration.getAllSubsets(CommonConfigurationConstants.CONNECTION_TAG);
    for (ReadonlyConfiguration connectionConfig : list) {
      try {
        loadConnection(connectionConfig);
      } catch (ConfigurationException e) {
        Log.warn("cannot load provider " +
          connectionConfig.getSetting(CommonConfigurationConstants.CONNECTION_ID_TAG, "<unknown>") + ", ignoring", e);
      }
    }
    if (myLoaded.get()) {
      assert false : this;
      return;
    }

    Runnable dispatcher;
    synchronized (myLoaded.getLock()) {
      myLoaded.set(true);
      dispatcher = myWaitingForLoading.getDispatcherSnapshot();
      myWaitingForLoading.noMoreEvents();
    }
    dispatcher.run();
  }

  @Nullable
  private Configuration findConfiguration(String connectionID) {
    List<Configuration> providerSubsets = myConfiguration.getAllSubsets(CommonConfigurationConstants.CONNECTION_TAG);
    for (Iterator<Configuration> iterator = providerSubsets.iterator(); iterator.hasNext();) {
      Configuration subset = iterator.next();
      try {
        if (connectionID.equals(subset.getMandatorySetting(CommonConfigurationConstants.CONNECTION_ID_TAG)))
          return subset;
      } catch (ReadonlyConfiguration.NoSettingException e) {
        Log.warn(e);
        return null;
      }
    }
    Log.warn("Not found: " + connectionID);
    return null;
  }

  @Nullable
  private Connection loadConnection(ReadonlyConfiguration config) throws ConfigurationException {
    ItemProvider descriptor;
    boolean set = config.isSet(CommonConfigurationConstants.ITEM_PROVIDER_TAG);
    if (!set)
      throw new ConfigurationException("no provider descriptor");
    String descriptorId = config.getSetting(CommonConfigurationConstants.ITEM_PROVIDER_TAG, "");
    if (descriptorId.length() != 0) {
      synchronized (this) {
        descriptor = myProviderById.get(descriptorId);
        if (descriptor == null) {
          Log.warn("No provider descriptor with id: " + descriptorId);
          return null;
        }
      }
    } else {
      throw new ConfigurationException("Provider id should not be empty");
    }
    String providerID = config.getMandatorySetting(CommonConfigurationConstants.CONNECTION_ID_TAG);
    if (providerID.length() == 0)
      throw new ConfigurationException("Provider id should not be empty");
    try {
      return registerNewConnection(config, descriptor, false);
    } catch (ProviderDisabledException e) {
      // should not be!
      Log.warn("Provider " + descriptorId + " is disabled (weird)");
      return null;
    }
  }

  @Nullable
  private Connection registerNewConnection(ReadonlyConfiguration c, ItemProvider provider, boolean isNew)
    throws ConfigurationException, ProviderDisabledException
  {
    String connectionID = c.getMandatorySetting(CommonConfigurationConstants.CONNECTION_ID_TAG);
    Connection connection = provider.createConnection(connectionID, c, isNew);

    myConnections.getWritableCollection().add(connection);
    connection.startConnection();

    myEventSink.onConnectionsChanged();
    return connection;
  }
}

