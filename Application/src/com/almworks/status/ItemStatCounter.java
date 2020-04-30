package com.almworks.status;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ReplaceTabKey;
import com.almworks.api.application.TabKey;
import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.application.util.sources.ItemViewAdapter;
import com.almworks.api.engine.*;
import com.almworks.items.api.DBEvent;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBListener;
import com.almworks.items.api.DBReader;
import com.almworks.util.Getter;
import com.almworks.util.components.TreeModelAdapter;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeModel;

public abstract class ItemStatCounter extends StatComponent {
  private final TabKey myQueryKey = new ReplaceTabKey("counter");

  protected final Engine myEngine;
  protected final ExplorerComponent myExplorer;

  private boolean mySubscribedToTree;

  private DBFilter myView;

  public ItemStatCounter(@NotNull Icon icon, Engine engine, ExplorerComponent explorer, ThreadGate gate) {
    super(engine.getDatabase(), icon, gate);
    myEngine = engine;
    myExplorer = explorer;
  }

  public void attach() {
    super.attach();
    myEngine.getDatabase().addListener(lifespan(), new DBListener() {
      @Override
      public void onDatabaseChanged(DBEvent event, DBReader reader) {
        recount();
      }
    });
    ConnectionManager connectionManager = myEngine.getConnectionManager();
    lifespan().add(connectionManager.addConnectionChangeListener(ThreadGate.STRAIGHT, new ConnectionChangeListener() {
      public void onChange(Connection connection, ConnectionState oldState, ConnectionState newState) {
        recount();
      }
    }));
    if (!isUnclickable()) {
      addActionListener(new AnActionListener() {
        public void perform(ActionContext context) throws CantPerformException {
          Getter<Integer> getter = new Getter<Integer>() {
            public Integer get() {
              return myCount.get();
            }
          };
          ExplorerComponent explorer = context.getSourceObject(ExplorerComponent.ROLE);
          explorer.showItemsInTab(ItemViewAdapter.create(getView(), getter),
            ItemCollectionContext.createNoNode(getViewName(), null, myQueryKey), false);
          recount();
        }
      });
    }
  }

  @ThreadAWT
  private DBFilter getView() {
    if (myView == null)
      myView = createView();
    return myView;
  }

  @Override
  @ThreadAWT
  protected synchronized int count(DBReader reader) {
    Threads.assertAWTThread();
    RootNode rootNode = myExplorer.getRootNode();
    if (rootNode == null)
      return -1;
    maybeSubscribeToTree(rootNode);
    int childCount = rootNode.getChildrenCount();
    int total = 0;
    for (int i = 0; i < childCount; i++) {
      GenericNode node = rootNode.getChildAt(i);
      if (node instanceof ConnectionNode) {
        int count = getConnectionCount(node);
        if (count < 0)
          return -1;
        total += count;
      }
    }
    return total;
  }

  @Override
  protected boolean isCountNeedingDatabase() {
    return false;
  }

  private void maybeSubscribeToTree(RootNode rootNode) {
    if (mySubscribedToTree)
      return;
    Lifespan lifespan = lifespan();
    if (lifespan.isEnded())
      return;
    TreeModel treeModel = rootNode.getTreeNode().getTreeModel();
    if (treeModel != null) {
      UIUtil.addTreeModelListener(lifespan, treeModel, new TreeModelAdapter() {
        public void treeNodesChanged(TreeModelEvent e) {
          // ignore
        }

        protected void treeModelEvent(TreeModelEvent e) {
          Object[] path = e.getPath();
          if (path == null || path.length == 1) {
            // root affected
            recount();
          }
        }
      });
      lifespan.add(new Detach() {
        protected void doDetach() throws Exception {
          mySubscribedToTree = false;
        }
      });
      mySubscribedToTree = true;
    }
  }

  protected abstract int getConnectionCount(GenericNode node);

  protected abstract DBFilter createView();

  protected abstract String getViewName();
}
