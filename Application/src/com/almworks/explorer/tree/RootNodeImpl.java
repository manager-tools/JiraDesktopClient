package com.almworks.explorer.tree;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.NameResolver;
import com.almworks.api.application.tree.*;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Engine;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.SyncRegistry;
import com.almworks.items.api.*;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.*;
import com.almworks.util.ui.TreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedBoolean;

import java.lang.ref.WeakReference;
import java.util.*;

class RootNodeImpl extends GenericNodeImpl implements RootNode {
  private final ComponentContainer myContainer;
  private final MyResult myResult = new MyResult();
  private final SyncRegistry mySyncRegistry;
  private final TreeNodeFactoryImpl myNodeFactory;
  private final ItemsPreviewManager myItemsPreviewManager;
  private DBFilter myRootView = null;

  private final Map<String, WeakReference<GenericNode>> myNodeByIdCache = Collections15.hashMap();

  private final Bottleneck myResolverBottleneck = new Bottleneck(10000, ThreadGate.AWT_QUEUED, new Runnable() {
    public void run() {
      updateResolverDependencies();
    }
  });

  private volatile SynchronizedBoolean myUpdateResolverDependenciesValid;

  private Map<DBIdentifiedObject, TagNode> myTagsCache;
  private final TagsFolderNode myTagsFolder;


  public RootNodeImpl(ComponentContainer container, Database db, String text, Configuration config, SyncRegistry syncRegistry,
    TreeNodeFactoryImpl nodeFactory)
  {
    super(container.getActor(Database.ROLE), FixedText.folder(text, Icons.NODE_FOLDER_OPEN, Icons.NODE_FOLDER_CLOSED), config);
    myContainer = container;
    mySyncRegistry = syncRegistry;
    myNodeFactory = nodeFactory;
//    myRecountManager = recountManager;
    myItemsPreviewManager = new ItemsPreviewManager(db);

    addAllowedChildType(TreeNodeFactory.NodeType.FOLDER);
    addAllowedChildType(TreeNodeFactory.NodeType.QUERY);
    addAllowedChildType(TreeNodeFactory.NodeType.COLLECTIONS_FOLDER);

    myTagsFolder = ensureTagsNodeExists();

//    Collection<ATreeNode<GenericNode>> children = getTreeNode().childrenToList();
//    GenericNode node = TreeUtil.detectUserObject(children, Condition.<GenericNode>isInstance(FolderNode.class));
//    int index = node == null ? -1 : getTreeNode().getIndex(node.getTreeNode());
//    if (index < 0) {
//      FolderNode folder = TreeNodeFactoryImpl.TYPE_FOLDER.insertNew(getTreeNode().getUserObject());
//      folder.getPresentation().setText("Global Queries");
//      children = getTreeNode().childrenToList();
//      node = TreeUtil.detectUserObject(children, Condition.<GenericNode>isEqual(folder));
//      index = node == null ? -1 : getTreeNode().getIndex(node.getTreeNode());
//      if (index > 1) {
//        TreeModelBridge<GenericNode> t = getTreeNode().remove(index);
//        getTreeNode().insert(t, 1);
//      }
//    }
  }

  public TagsFolderNode getTagsFolder() {
    return myTagsFolder;
  }

  private TagsFolderNode ensureTagsNodeExists() {
    Collection<ATreeNode<GenericNode>> children = getTreeNode().childrenToList();
    GenericNode node = TreeUtil.detectUserObject(children, Condition.<GenericNode>isInstance(TagsFolderNode.class));
    int index = node == null ? -1 : getTreeNode().getIndex(node.getTreeNode());
    if (index < 0) {
      Configuration subconfig = getConfiguration().createSubset(TreeNodeFactory.KLUDGE_COLLECTIONS_FOLDER);
      node = TreeNodeFactoryImpl.TAGS_FOLDER_TYPE.create(getEngine().getDatabase(), subconfig);
      getTreeNode().insert(node.getTreeNode(), 0);
    } else {
      assert node != null;
      getTreeNode().remove(index);
      getTreeNode().insert(node.getTreeNode(), 0);
    }
    return (TagsFolderNode)node;
  }

  public boolean isCopiable() {
    return false;
  }

  public boolean isNarrowing() {
    return true;
  }

  public List<GenericNode> getPathFromRoot() {
    return Collections15.emptyList();
  }

  protected void onFirstInsertToModel() {
    super.onFirstInsertToModel();
    myRootView = getEngine().getDatabase().filter(BoolExpr.<DP>TRUE());
    listenResolver();
  }

  private void listenResolver() {
    NameResolver resolver = getResolver();
    if (resolver != null) {
      resolver.getModifiable().addChangeListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, new ChangeListener() {
        public void onChange() {
          SynchronizedBoolean currentOperationValid = myUpdateResolverDependenciesValid;
          if (currentOperationValid != null) {
            currentOperationValid.set(false);
          }
          long now = System.currentTimeMillis();
//          Debug.out("lr:...req " + (now % Const.HOUR));
          myResolverBottleneck.request();
        }
      });
    }
  }

  private void updateResolverDependencies() {
    if (!isNode())
      return;
    long executionStart = System.currentTimeMillis();
//    Debug.out("lr:.start " + (executionStart % Const.HOUR));
    final SynchronizedBoolean valid = new SynchronizedBoolean(true);
    myUpdateResolverDependenciesValid = valid;
    boolean success = NavigationTreeUtil.visitTree(this, AbstractQueryNode.class, false,
      new ElementVisitor<AbstractQueryNode>() {
        public boolean visit(AbstractQueryNode node) {
          if (valid.get()) {
            if (node.isNode() && node.isFullyInitialized())
              node.getQueryResult().updateFilter(true);
          }
          return valid.get();
        }
      });
    if (myUpdateResolverDependenciesValid == valid) {
      myUpdateResolverDependenciesValid = null;
    }
    long executionFinish = System.currentTimeMillis();
//    Debug.out("lr:finish " + (executionFinish % Const.HOUR) + " " + success);
//    Debug.out("lr:..time " + (executionFinish - executionStart));
  }

  protected final DBFilter getRootNodeView() {
    DBFilter view;
    synchronized (this) {
      view = myRootView;
    }
    return view;
  }

//  public RecountManager getRecountManager() {
//    return myRecountManager;
//  }

  public ItemsPreviewManager getItemsPreviewManager() {
    return myItemsPreviewManager;
  }

  public ComponentContainer getContainer() {
    return myContainer;
  }

  public TreeNodeFactory getNodeFactory() {
    return myNodeFactory;
  }

  public List<GenericNode> collectNodes(final Condition<GenericNode> condition) {
    final List<GenericNode> result = Collections15.arrayList();
    NavigationTreeUtil.visitTree(this, GenericNode.class, false, new ElementVisitor<GenericNode>() {
      public boolean visit(GenericNode node) {
        if (condition.isAccepted(node))
          result.add(node);
        return true;
      }
    });
    return result;
  }

  @Nullable
  @ThreadAWT
  public GenericNode getNodeById(String nodeId) {
    Threads.assertAWTThread();
    if (nodeId == null || nodeId.length() == 0)
      return null;
    WeakReference<GenericNode> reference = myNodeByIdCache.get(nodeId);
    if (reference != null) {
      GenericNode node = reference.get();
      if (node != null) {
        return node;
      }
    }
    GenericNode node;
    if (nodeId.equals(getNodeId()))
      node = this;
    else
      node = findNodeById(this, nodeId);
    if (node != null) {
      myNodeByIdCache.put(nodeId, new WeakReference<GenericNode>(node));
    }
    return node;
  }

  private static GenericNode findNodeById(GenericNode node, String id) {
    for (int i = 0; i < node.getChildrenCount(); i++) {
      GenericNode subnode = node.getChildAt(i);
      if (id.equals(subnode.getNodeId()))
        return subnode;
    }
    for (int i = 0; i < node.getChildrenCount(); i++) {
      GenericNode subnode = node.getChildAt(i);
      GenericNode result = findNodeById(subnode, id);
      if (result != null)
        return result;
    }
    return null;
  }

  public Engine getEngine() {
    return myContainer.getActor(Engine.ROLE);
  }

  public SyncRegistry getSyncRegistry() {
    return mySyncRegistry;
  }

  public QueryResult getQueryResult() {
    return myResult;
  }

  public boolean isShowable() {
    return getNodeFactory().getTree().isRootVisible();
  }

  @CanBlock
  @Nullable
  protected ItemsPreview calculatePreview(Lifespan lifespan, DBReader reader) {
    // always done in preschedule
    assert false : this;
    return null;
  }

  @ThreadAWT
  protected boolean preschedulePreviewCalculation() {
    setPreview(new ItemsPreview.Unavailable() {
      public synchronized void invalidate() {
        // no need to invalidate root
      }
    });
    return false;
  }

  public void invalidateTagCache() {
    synchronized (this) {
      myTagsCache = null;
    }
  }

  @ThreadSafe
  public Map<DBIdentifiedObject, TagNode> getTags() {
    Map<DBIdentifiedObject, TagNode> result;
    synchronized (this) {
      result = myTagsCache;
      if (result != null)
        return result;
    }
    result = ThreadGate.AWT_IMMEDIATE.compute(new Computable<Map<DBIdentifiedObject, TagNode>>() {
      public Map<DBIdentifiedObject, TagNode> compute() {
        LinkedHashMap<DBIdentifiedObject, TagNode> map = Collections15.linkedHashMap();
        TagsFolderNode folderNode = Condition.isInstance(TagsFolderNode.class).detectUntyped(getChildren());
        if (folderNode == null)
          return Collections15.emptyMap();
        int count = folderNode.getChildrenCount();
        for (int i = 0; i < count; i++) {
          GenericNode child = folderNode.getChildAt(i);
          if (child instanceof TagNode) {
            TagNode tagNode = (TagNode) child;
            DBIdentifiedObject tagObj = tagNode.getTagDbObj();
            if (tagObj != null) {
              map.put(tagObj, tagNode);
            }
          }
        }
        return Collections.unmodifiableMap(map);
      }
    });
    synchronized (this) {
      myTagsCache = result;
    }
    return result;
  }


  private class MyResult extends AbstractQueryResult {
    public Constraint getValidConstraint() {
      return Constraint.NO_CONSTRAINT;
    }

    public boolean isRunnable() {
      return false;
    }

    @Nullable
    public ItemSource getItemSource() {
      return null;
    }

    @Nullable
    public ItemCollectionContext getCollectionContext() {
      return null;
    }

    @Override
    public DBFilter getDbFilter() {
      return getRootNodeView();
    }

    public long getVersion() {
      return 0;
    }

    @Nullable
    public ItemHypercube getHypercube(boolean precise) {
      return new ItemHypercubeImpl();
    }
  }
}
