package com.almworks.extservice;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.engine.*;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.tracker.eapi.alpha.ArtifactInfoStatus;
import com.almworks.tracker.eapi.alpha.ArtifactLoadOption;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.xmlrpc.OutgoingMessage;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static com.almworks.tracker.eapi.alpha.ArtifactInfoStatus.*;

class ItemSubscription {
  private static final long FETCH_FAIL_PAUSE = 300000;

  private final String myUrl;
  private final ItemSubscriptionController myController;

  private boolean myMayCreateConnection = false;
  private boolean myMayDownload = false;
  private Long myLastItem = null;
  private Long myLastIcn = null;
  private OutgoingMessage myLastMessage = null;
  private Connection myLastConnection = null;
  private long myLastItemNotFoundTime = 0;

  ItemSubscription(String url, Set<ArtifactLoadOption> options, ItemSubscriptionController controller) {
    myUrl = url;
    myController = controller;
    addOptions(options);
  }

  @ThreadAWT
  boolean addOptions(Set<ArtifactLoadOption> options) {
    Threads.assertAWTThread();
    boolean changed = false;
    for(final ArtifactLoadOption option : options) {
      if(option == ArtifactLoadOption.MAY_CREATE_CONNECTION) {
        changed |= !myMayCreateConnection;
        myMayCreateConnection = true;
      } else if(option == ArtifactLoadOption.MAY_DOWNLOAD) {
        changed |= !myMayDownload;
        myMayDownload = true;
      }
    }
    return changed;
  }

  @ThreadAWT
  void send() {
    Threads.assertAWTThread();
    if(myLastMessage != null) {
      myController.send(myLastMessage);
    }
  }

  @ThreadAWT
  void sync(final boolean forceSend) {
    Threads.assertAWTThread();
    final Engine engine = myController.getEngine();
    final Connection conn = engine.getConnectionManager().getConnectionForUrl(myUrl);
    if(!Util.equals(conn, myLastConnection)) {
      myLastItem = null;
      myLastConnection = conn;
    }

    if(myLastConnection == null) {
      final ItemProvider provider = engine.getConnectionManager().getProviderForUrl(myUrl);
      if(provider == null) {
        sendUpdated(UNRECOGNIZED_URL);
        return;
      }

      if(myMayCreateConnection) {
        try {
          if(createConnectionAndRepeat(provider)) {
            sendUpdated(WAIT_CONNECTION_SETUP);
            return;
          }
        } catch(ProviderDisabledException e) {
          // fall through
        }
      }
    }

    if(myLastConnection == null || !myLastConnection.getState().getValue().isReady()) {
      sendUpdated(NO_CONNECTION);
      return;
    }

    final Connection finalConn = myLastConnection;
    final Long finalItem = myLastItem;
    final Long finalIcn = myLastIcn;
    final boolean mayDownload = myMayDownload;
    ExtServiceUtils.gate().execute(new Runnable() {
      public void run() {
        myController.getDatabase().readForeground(new ReadTransaction<Void>() {
          @Override
          public Void transaction(DBReader reader) throws DBOperationCancelledException {
            syncStart(finalConn, mayDownload, finalItem, finalIcn, reader, forceSend);
            return null;
          }
        }).waitForCompletion();
      }
    });
  }

  @ThreadAWT
  private boolean createConnectionAndRepeat(ItemProvider provider) throws ProviderDisabledException {
    Threads.assertAWTThread();
    final Configuration cfg = provider.createDefaultConfiguration(myUrl);
    if(cfg != null) {
      try {
        final Connection conn = getOrCreateConnection(provider, cfg);
        if(conn == null) {
          return false;
        }
        EngineUtils.runWhenConnectionIsReady(conn, ThreadGate.AWT, new Runnable() {
          public void run() {
            myMayCreateConnection = false;
            if(myLastConnection == null) {
              myLastConnection = conn;
            }
            sync(true);
          }
        });
        return true;
      } catch (ConfigurationException e) {
        Log.debug(e);
        return false;
      }
    }
    return false;
  }

  @Nullable
  private Connection getOrCreateConnection(ItemProvider provider, final Configuration cfg)
    throws ConfigurationException, ProviderDisabledException
  {
    final Map<Configuration, Connection> locks = myController.getCreatingConnectionsLockMap();
    Connection conn = findConnectionBeingCreated(locks, cfg);
    if(conn == null) {
      conn = myController.getEngine().getConnectionManager().createConnection(provider, cfg);
      if(conn != null) {
        locks.put(cfg, conn);
        EngineUtils.runWhenConnectionIsReady(conn, ThreadGate.AWT, new Runnable() {
          public void run() {
            locks.remove(cfg);
          }
        });
      }
    }
    return conn;
  }

  private Connection findConnectionBeingCreated(Map<Configuration, Connection> locks, Configuration cfg) {
    for(final Map.Entry<Configuration, Connection> e : locks.entrySet()) {
      if(ConfigurationUtil.haveSameSettings(e.getKey(), cfg)) {
        return e.getValue();
      }
    }
    return null;
  }

  @CanBlock
  private void syncStart(
    final Connection conn, boolean mayDownload, Long item,
    final Long icn, final DBReader reader, final boolean forceSend)
  {
    boolean fetch = (item == null);
    if(fetch) {
      if(System.currentTimeMillis() < myLastItemNotFoundTime + FETCH_FAIL_PAUSE) {
        Log.debug("will fetch later: " + myUrl);
        fetch = false;
      }
    }
    if(fetch) {
      syncFetchItem(conn, reader, myUrl, mayDownload, new Procedure<Long>() {
        public void invoke(Long item) {
          syncPrepareResponse(conn, item, reader, icn, null, forceSend);
        }
      });
    } else {
      syncPrepareResponse(conn, item, reader, icn, icn, forceSend);
    }
  }

  @CanBlock
  private void syncPrepareResponse(Connection conn, final Long item, DBReader reader, Long lastIcn, Long currIcn, final boolean forceSend) {
    final Date timestamp;
    final String shortDescr;
    final String longDescr;
    final String id;
    final String summary;

    if(currIcn == null && item != null) {
      currIcn = reader.getItemIcn(item);
    }

    if(currIcn == null || currIcn <= 0 || Util.equals(currIcn, lastIcn)) {
      timestamp = null;
      shortDescr = null;
      longDescr = null;
      id = null;
      summary = null;
      synchronized(this) {
        myLastItemNotFoundTime = System.currentTimeMillis();
      }
    } else {
      final ItemVersion ver = SyncUtils.readTrunk(reader, item);
      timestamp = conn.getItemTimestamp(ver);
      shortDescr = conn.getItemShortDescription(ver);
      longDescr = conn.getItemLongDescription(ver);
      id = conn.getItemId(ver);
      summary = conn.getItemSummary(ver);
    }

    final Long finalVersion = currIcn;
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        update(item, finalVersion, timestamp, shortDescr, longDescr, id, summary, forceSend);
      }
    });
  }

  @CanBlock
  private void syncFetchItem(
    Connection connection, DBReader reader, final String url, boolean mayDownload, final Procedure<Long> proceed)
  {
    try {
      final Pair<DBFilter, Constraint> pair = connection.getViewAndConstraintForUrl(url);
      final DBFilter filter = pair.getFirst();
      assert filter != null : url;
      CantPerformException.ensureNotNull(filter);

      final LongList all = filter.query(reader).copyItemsSorted();
      if(!all.isEmpty()) {
        syncCallProceed(all, url, proceed);
        return;
      }

      if(!mayDownload) {
        throw new CantPerformException();
      }

      final Constraint constraint = pair.getSecond();
      CantPerformException.ensureNotNull(constraint);

      connection.synchronizeItemView(
        constraint, filter, all, "Plug-in Request",
        new Procedure<SyncTask>() {
          public void invoke(SyncTask task) {
            downloadFinished(task, filter, url, proceed, false);
          }
        });

      reportDownloading();
    } catch (CantPerformException e) {
      proceed.invoke(null);
    }
  }

  @CanBlock
  private void reportDownloading() {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        sendUpdated(WAIT_DOWNLOADING);
      }
    });
  }

  @CanBlock
  private void downloadFinished(
    final SyncTask task, final DBFilter filter, final String url, final Procedure<Long> proceed, boolean gated)
  {
    final ThreadGate gate = ExtServiceUtils.gate();
    if(!gated && EventQueue.isDispatchThread()) {
      gate.execute(new Runnable() {
        public void run() {
          downloadFinished(task, filter, url, proceed, true);
        }
      });
      return;
    }

    final LongList allItems = myController.getDatabase().readForeground(new ReadTransaction<LongList>() {
      @Override
      public LongList transaction(DBReader reader) throws DBOperationCancelledException {
        return filter.query(reader).copyItemsSorted();
      }
    }).waitForCompletion();

    if(allItems == null) {
      // todo: transaction failed?
      Log.warn("transaction failed");
      return;
    }

    if(!allItems.isEmpty()) {
      syncCallProceed(allItems, url, proceed);
      return;
    }

    final long needIcn = task.getLastCommittedCN();
    final DetachComposite life = new DetachComposite();
    filter.liveQuery(life, new DBLiveQuery.Listener.Deaf() {
      @Override
      public void onDatabaseChanged(DBEvent event, DBReader reader) {
        if(reader.getTransactionIcn() >= needIcn) {
          final LongArray all = filter.query(reader).copyItemsSorted();
          gate.execute(new Runnable() {
            @Override
            public void run() {
              if(!all.isEmpty()) {
                syncCallProceed(all, url, proceed);
              } else {
                proceed.invoke(null);
              }
            }
          });
          life.detach();
        }
      }
    });
  }

  @CanBlock
  private void syncCallProceed(LongList all, String url, Procedure<Long> proceed) {
    final int count = all.size();
    if(count > 1) {
      Log.warn(count + " items for " + url);
    }

    final LongIterator iterator = all.iterator();
    assert iterator.hasNext();
    if(iterator.hasNext()) {
      proceed.invoke(iterator.nextValue());
    } else {
      proceed.invoke(null);
    }
  }

  @ThreadAWT
  private void update(
    Long item, Long icn, Date timestamp, String shortDescr,
    String longDescr, String id, String summary, boolean forceSend)
  {
    boolean sent = false;
    myLastItem = item;

    if(!Util.equals(myLastIcn, icn)) {
      myLastIcn = icn;
      if(myLastIcn != null) {
        final long time = timestamp == null ? -1 : timestamp.getTime();
        sent = sendUpdated(ExtServiceUtils.createArtifactInfoMessage(
          myUrl, OK, time, shortDescr, longDescr, id, summary));
      }
    }

    if(myLastIcn == null) {
      sent |= sendUpdated(NO_ARTIFACT);
    }

    if(forceSend && !sent) {
      send();
    }
  }

  @ThreadAWT
  private boolean sendUpdated(@NotNull OutgoingMessage message) {
    Threads.assertAWTThread();
    boolean send = !message.equals(myLastMessage);
    if(send) {
      myLastMessage = message;
      send();
    }
    return send;
  }

  private boolean sendUpdated(ArtifactInfoStatus status) {
    return sendUpdated(ExtServiceUtils.createArtifactInfoMessage(myUrl, status, -1, null, null, null, null));
  }

  @ThreadAWT
  void dispose() {
    Threads.assertAWTThread();
    myLastItem = null;
    myLastConnection = null;
    myLastMessage = null;
    myLastIcn = null;
  }
}