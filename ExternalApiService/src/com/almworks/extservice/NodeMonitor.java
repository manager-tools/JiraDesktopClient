package com.almworks.extservice;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.tracker.alpha.AlphaProtocol;
import com.almworks.tracker.eapi.alpha.ArtifactInfoStatus;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.TreeModelAdapter;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.*;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.xmlrpc.MessageOutbox;
import com.almworks.util.xmlrpc.SimpleOutgoingMessage;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.NotNull;
import util.concurrent.SynchronizedBoolean;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class NodeMonitor {
  private static final int WATCH_ATTEMPTS = 30;

  private final Database myDatabase;
  private final Engine myEngine;
  private final ExplorerComponent myExplorerComponent;
  private final MessageOutbox myOutbox;
  private final Map<String, Subscription> myWatchedNodes = Collections15.hashMap();

  private final BottleneckJobs<Subscription> mySendJobs = new BottleneckJobs<Subscription>(1000, ThreadGate.AWT) {
    protected void execute(Subscription job) {
      job.sendAWT();
    }
  };

  private final Bottleneck myNodesVerifier = new Bottleneck(10000, ThreadGate.AWT, new Runnable() {
    public void run() {
      verifyNodes();
    }
  });

  private final TreeModelAdapter myTreeModelListener = new TreeModelAdapter() {
    public void treeNodesRemoved(TreeModelEvent e) {
      // nodes may be reinserted
      myNodesVerifier.requestDelayed();
    }
  };

  private DefaultTreeModel mySubscribedModel;

  public NodeMonitor(Database database, ExplorerComponent explorerComponent, MessageOutbox outbox, Engine engine) {
    myDatabase = database;
    myEngine = engine;
    myExplorerComponent = explorerComponent;
    myOutbox = outbox;
  }

  @ThreadAWT
  public void unwatchNode(String nodeId) {
    final Subscription sub = myWatchedNodes.remove(nodeId);
    if(sub != null) {
      sub.dispose();
    }
  }

  @ThreadAWT
  public void watchNode(final String nodeId) {
    final Subscription sub = myWatchedNodes.get(nodeId);
    if(sub != null) {
      sub.schedule();
    } else {
      watchNodeWhenExplorerReady(nodeId);
    }
  }

  @ThreadAWT
  private void watchNodeWhenExplorerReady(final String nodeId) {
    myExplorerComponent.whenReady(ThreadGate.AWT_QUEUED, new Runnable() {
      public void run() {
        final Subscription sub = myWatchedNodes.get(nodeId);
        if(sub != null) {
          sub.schedule();
        } else {
          watchNodeAttempt(nodeId, 0);
        }
      }
    });
  }

  @ThreadAWT
  private void watchNodeAttempt(final String nodeId, final int attempt) {
    final RootNode rootNode = myExplorerComponent.getRootNode();
    final GenericNode node = rootNode == null ? null : rootNode.getNodeById(nodeId);
    final DefaultTreeModel model = node == null ? null : node.getTreeNode().getTreeModel();
    if(model == null) {
      if(attempt >= WATCH_ATTEMPTS) {
        sendInvalidNode(nodeId);
      } else {
        UIUtil.invokeLater(300, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            watchNodeAttempt(nodeId, attempt + 1);
          }
        });
      }
    } else {
      maybeSubscribeToTree(model);
      final Subscription sub = new Subscription(node);
      myWatchedNodes.put(nodeId, sub);
      sub.subscribeToNode();
      sub.schedule();
    }
  }

  @ThreadAWT
  private void maybeSubscribeToTree(@NotNull DefaultTreeModel treeModel) {
    if(mySubscribedModel == null) {
      mySubscribedModel = treeModel;
      treeModel.addTreeModelListener(myTreeModelListener);
    } else {
      assert mySubscribedModel == treeModel : mySubscribedModel + " " + treeModel;
    }
  }

  private void sendInvalidNode(String nodeId) {
    myOutbox.enqueue(new SimpleOutgoingMessage(
      AlphaProtocol.Messages.ToClient.COLLECTION_UPDATE, nodeId, Boolean.FALSE, new Hashtable(), new Vector()));
  }

  @ThreadAWT
  private void verifyNodes() {
    for(final Iterator<Map.Entry<String, Subscription>> ii = myWatchedNodes.entrySet().iterator(); ii.hasNext();) {
      final Map.Entry<String, Subscription> e = ii.next();
      final Subscription sub = e.getValue();
      if(!sub.getNode().isNode()) {
        sub.dispose();
        sendInvalidNode(e.getKey());
        ii.remove();
      }
    }
  }

  @ThreadAWT
  public void dispose() {
    Threads.assertAWTThread();

    final DefaultTreeModel subscribedModel = mySubscribedModel;
    if(subscribedModel != null) {
      subscribedModel.removeTreeModelListener(myTreeModelListener);
    }

    final Subscription[] subscriptions = myWatchedNodes.values().toArray(new Subscription[myWatchedNodes.size()]);
    myWatchedNodes.clear();
    for(final Subscription sub : subscriptions) {
      sub.dispose();
    }
    
    mySendJobs.abort();
    myNodesVerifier.abort();
  }


  private class Subscription implements ChangeListener {
    private final Lifecycle myNodeLife = new Lifecycle();
    private final Lifecycle myViewLife = new Lifecycle();
    private final GenericNode myNode;
    private final Hashtable myLastNodeProps = new Hashtable();
    private final Map<Long, Long> myLastItemIcns = Collections15.hashMap();
    private final SynchronizedBoolean mySending = new SynchronizedBoolean(false);

    private DBFilter myLastFilter;
    private String myLastNodeId;
    private volatile boolean myDisposed = false;

    public Subscription(GenericNode node) {
      myNode = node;
    }

    @Override
    public void onChange() {
      schedule();
    }

    private void dispose() {
      myViewLife.dispose();
      myNodeLife.dispose();
      myDisposed = true;
    }

    @CanBlock
    private void send() {
      if(!mySending.commit(false, true)) {
        // already sending - request later re-sending
        schedule();
        return;
      }

      try {
        if(myDisposed) {
          return;
        }

        final DBFilter filter;
        final String nodeId;
        final Hashtable nodeProps;
        synchronized(this) {
          filter = myLastFilter;
          nodeId = myLastNodeId;
          nodeProps = myLastNodeProps;
        }

        final AtomicBoolean readSuccess = new AtomicBoolean(false);

        final Set<String> urls = Collections15.hashSet();
        final Vector<Hashtable> itemData = new Vector<Hashtable>();
        final Map<Long, Long> sentIcns = Collections15.hashMap();

        myDatabase.readForeground(new ReadTransaction<Void>() {
          @Override
          public Void transaction(DBReader reader) throws DBOperationCancelledException {
            return queryAndRead(filter, reader, urls, itemData, sentIcns);
          }
        }).onSuccess(ThreadGate.STRAIGHT, new Procedure<Void>() {
          @Override
          public void invoke(Void arg) {
            readSuccess.compareAndSet(false, true);
          }
        }).waitForCompletion();

        if(myDisposed) {
          return;
        }

        if(filter != null && !readSuccess.get()) {
          Log.warn(String.format(
            "NodeMonitor: rescheduling due to a read transaction failure; nodeId = %s; expr = %s",
            nodeId, filter.getExpr()));
          schedule();
          return;
        }

        myOutbox.enqueue(new SimpleOutgoingMessage(
          AlphaProtocol.Messages.ToClient.COLLECTION_UPDATE, nodeId, Boolean.TRUE, nodeProps, new Vector(urls)));

        if(!itemData.isEmpty()) {
          myOutbox.enqueue(new SimpleOutgoingMessage(AlphaProtocol.Messages.ToClient.ARTIFACT_INFO, itemData));
        }

        myLastItemIcns.putAll(sentIcns);
      } finally {
        final boolean was = mySending.set(false);
        assert was : this;
      }
    }

    private Void queryAndRead(
      DBFilter filter, DBReader reader,
      Collection<String> urls, Collection<Hashtable> itemData, Map<Long, Long> sentIcns)
    {
      if(filter != null) {
        final LongList items = filter.query(reader).copyItemsSorted();
        for(final LongIterator it = items.iterator(); it.hasNext() && !myDisposed;) {
          readItem(SyncUtils.readTrunk(reader, it.nextValue()), urls, itemData, sentIcns);
        }
      }
      return null;
    }

    private void readItem(
      ItemVersion ver, Collection<String> urls, Collection<Hashtable> itemData, Map<Long, Long> sentIcns)
    {
      final Long connItem = ver.getValue(SyncAttributes.CONNECTION);
      if(connItem != null && connItem > 0) {
        final Connection connection = myEngine.getConnectionManager().findByItem(connItem);
        if(connection != null) {
          final String url = connection.getItemUrl(ver);
          if(url != null) {
            urls.add(url);
            final Long sentIcn = myLastItemIcns.get(ver.getItem());
            if(sentIcn == null || sentIcn < ver.getIcn()) {
              final Date timestamp = connection.getItemTimestamp(ver);
              final long time = timestamp == null ? -1 : timestamp.getTime();
              itemData.add(
                ExtServiceUtils.createArtifactInfoHashtable(
                  url, ArtifactInfoStatus.OK, time,
                  connection.getItemShortDescription(ver),
                  connection.getItemLongDescription(ver),
                  connection.getItemId(ver),
                  connection.getItemSummary(ver)));
              sentIcns.put(ver.getItem(), ver.getIcn());
            }
          }
        }
      }
    }

    @ThreadAWT
    private void sendAWT() {
      final QueryResult result = myNode.getQueryResult();
      boolean resubscribe = false;

      synchronized(this) {
        final DBFilter view = result.getDbFilter();
        if(!Util.equals(view, myLastFilter)) {
          myLastFilter = view;
          resubscribe = true;
        }
        myLastNodeId = myNode.getNodeId();
        myLastNodeProps.put(AlphaProtocol.Messages.ToClient.CollectionProps.NAME, myNode.getName());
      }

      if(resubscribe) {
        myViewLife.cycle();
        if(myLastFilter != null) {
          myLastFilter.liveQuery(myViewLife.lifespan(), new DBLiveQuery.Listener.Deaf() {
            @Override
            public void onDatabaseChanged(DBEvent event, DBReader reader) {
              schedule();
            }
          });
        }
      }

      ThreadGate.LONG(NodeMonitor.class).execute(new Runnable() {
        public void run() {
          send();
        }
      });
    }

    private GenericNode getNode() {
      return myNode;
    }

    @ThreadSafe
    private void schedule() {
      mySendJobs.addJobDelayed(this);
    }

    private void subscribeToNode() {
      myNode.getQueryResult().addAWTChangeListener(myNodeLife.lifespan(), this);
    }
  }
}
