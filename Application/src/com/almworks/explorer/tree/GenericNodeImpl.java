package com.almworks.explorer.tree;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.tree.*;
import com.almworks.api.config.ConfigNames;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Connection;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.SyncRegistry;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.util.Getter;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Procedure;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * // getter is used for getting total count (kludge)
 */
abstract class GenericNodeImpl implements CanvasRenderable, GenericNode, ModelAware, Getter<Integer> {

  private static final BottleneckJobs<GenericNodeImpl> OUR_STORE_CHILDREN_JOBS =
    new BottleneckJobs<GenericNodeImpl>(1500, ThreadGate.AWT) {
      protected void execute(GenericNodeImpl job) {
        job.storeChildrenOrder();
      }
    };

  private static final DoubleBottleneckJobs<GenericNodeImpl> ourSortChildrenUpdater =
    new DoubleBottleneckJobs<GenericNodeImpl>(100, 1500, ThreadGate.AWT) {
      protected void execute(GenericNodeImpl job) {
        job.sortChildren();
      }
    };

  private static final DoubleBottleneckJobs<GenericNodeImpl> ourPresentationWithPreviewUpdater =
    new DoubleBottleneckJobs<GenericNodeImpl>(100, 500, ThreadGate.AWT) {
      protected void execute(GenericNodeImpl job) {
        job.updatePresentationWithPreview();
      }
    };

  private static final BottleneckJobs<GenericNodeImpl> ourPreviewInvalidater =
    new BottleneckJobs<GenericNodeImpl>(100, ThreadGate.AWT) {
      protected void execute(GenericNodeImpl job) {
        job.invalidatePreview();
      }
    };

  private static final DoubleBottleneckJobs<GenericNodeImpl> ourAfterInvalidateScheduleUpdater =
    new DoubleBottleneckJobs<GenericNodeImpl>(300, 1500, ThreadGate.AWT) {
      protected void execute(GenericNodeImpl job) {
        job.maybeSchedulePreview(true);
      }
    };


  private final Configuration myConfiguration;
  private final Object myPresentationLock = new Object();//todo
  private int myAllowedChildren = 0;
  private final String myNodeId;
  private final TreeModelBridge<GenericNode> myTreeNode;

  private CanvasRenderable myPresentation;
  private Detach myPresentationDetach = Detach.NOTHING;

  private boolean myEverInserted = false;
  private boolean myLoaded;
  private boolean myRemovable = false;

  private boolean myOrderCleared = false;

  private ItemsPreview myItemsPreview;
  private final Procedure2<Lifespan, DBReader> myCalcPreviewJob = new Procedure2<Lifespan, DBReader>() {
    @Override
    public void invoke(Lifespan lifespan, DBReader reader) {
      GenericNodeImpl.this.getOrCalculatePreview(lifespan, reader);
    }
  };

  public GenericNodeImpl(Database db, CanvasRenderable presentation, Configuration config) {
    myConfiguration = config;
    myTreeNode = new TreeModelBridge<GenericNode>(this);
    myLoaded = false;
    FolderTypes.loadNodes(db, this, config);
    myLoaded = true;
    myNodeId = getNodeId(config);
    setPresentation(presentation);
  }

  private static String getNodeId(Configuration config) {
    String id = config.getSetting(ConfigNames.NODE_ID, null);
    if (id == null) {
      id = NodeIdUtil.newUID();
      config.setSetting(ConfigNames.NODE_ID, id);
    }
    return id;
  }


  public static RootNode createRootNode(ComponentContainer container, Database db, String text, Configuration config,
    SyncRegistry syncRegistry, TreeNodeFactoryImpl nodeFactory)
  {
    return new RootNodeImpl(container, db, text, config, syncRegistry, nodeFactory);
  }

  public void renderOn(Canvas canvas, CellState state) {
    myPresentation.renderOn(canvas, state);
  }

  public int compareTo(GenericNode that) {
    return NavigationTreeUtil.askParentToCompareNodes(this, that);
  }

  public int compareChildren(GenericNode node1, GenericNode node2) {
    return NavigationTreeUtil.compareNodes(node1, node2);
  }

  public ChildrenOrderPolicy getChildrenOrderPolicy() {
    return ChildrenOrderPolicy.REORDER_ON_REQUEST;
  }

  public boolean canHideEmptyChildren() {
    return false;
  }

  public void setHideEmptyChildren(boolean newValue) {
    assert canHideEmptyChildren();
  }

  public boolean getHideEmptyChildren() {
    return false;
  }

  /**
   * @return Whether this node is indeed hiding empty children.
   */
  public boolean isHidingEmptyChildren() {
    return false;
  }

  @ThreadAWT
  public void fireSubtreeChanged() {
    Threads.assertAWTThread();
    fireTreeNodeChanged();
    for (int i = 0; i < getChildrenCount(); i++)
      getChildAt(i).fireSubtreeChanged();
  }

  public void addChildNode(GenericNode child) {
    createNodeInsert().invoke(child);
  }

  @Override
  public Procedure<GenericNode> createNodeInsert() {
    return new NodeInsertMethod(this);
  }

  public boolean allowsAnyChildren() {
    Threads.assertAWTThread();
    return myAllowedChildren != 0;
//    return !myAllowedChildren.isEmpty();
  }

  public boolean allowsChildren(TreeNodeFactory.NodeType childType) {
    Threads.assertAWTThread();
    return (myAllowedChildren & (1 << childType.ordinal())) != 0;
//    return myAllowedChildren.contains(childType);
  }

  public ReadonlyConfiguration createCopy(Configuration parentConfig) {
    Configuration copy = parentConfig.createSubset(myConfiguration.getName());
    Collection<String> settings = myConfiguration.getAllSettingNames();
    for (String setting : settings) {
      if (ConfigNames.NODE_ID.equals(setting))
        continue;
      copy.setSettings(setting, myConfiguration.getAllSettings(setting));
    }
    List<? extends GenericNode> children = getChildren();
    for (GenericNode child : children) {
      child.createCopy(copy);
    }
    return copy;
  }

  @Nullable
  public <T extends GenericNode> T getAncestorOfType(Class<? extends T> ancestorClass) {
    GenericNode ancestor = this;
    while (ancestor != null) {
      T result = Util.castNullable(ancestorClass, ancestor);
      if (result != null)
        return result;
      ancestor = ancestor.getParent();
    }
    return null;
  }

  public int getChildrenCount() {
    return myTreeNode.getChildCount();
  }

  public GenericNode getChildAt(int index) {
    GenericNode node = myTreeNode.getChildAt(index).getUserObject();
    assert node != null;
    return node;
  }

  public List<? extends GenericNode> getChildren() {
    return myTreeNode.childObjectsToList();
  }

  @Override
  public Iterator<? extends GenericNode> getChildrenIterator() {
    return myTreeNode.childObjectsIterator();
  }

  public Configuration getConfiguration() {
    return myConfiguration;
  }

  public Connection getConnection() {
    ConnectionNode connectionNode = getAncestorOfType(ConnectionNode.class);
    if (connectionNode == null)
      return null;
    Connection connection = connectionNode.getConnection();
    //noinspection ConstantConditions
    assert connection != null : connectionNode;
    return connection;
  }

  public ItemHypercube getHypercube(boolean strict) {
    GenericNode parent = getParent();
    if (parent != null)
      return parent.getHypercube(strict);
    else
      return new ItemHypercubeImpl();
  }

/*
  public Modifiable getHypercubeModifiable() {
    return myHypercubeModifiable.get();
  }
*/

  @Nullable
  public GenericNode getParent() {
    ATreeNode<GenericNode> parent = myTreeNode.getParent();
    return parent != null ? parent.getUserObject() : null;
  }

  public String getPositionId() {
    return getNodeId();
  }

  public CanvasRenderable getPresentation() {
    return myPresentation;
  }

  @Nullable
  public <T extends GenericNode> T getStrictAncestorOfType(Class<? extends T> ancestorClass) {
    GenericNode parent = getParent();
    return parent != null ? parent.getAncestorOfType(ancestorClass) : null;
  }

  @NotNull
  public final TreeModelBridge<GenericNode> getTreeNode() {
    return myTreeNode;
  }

  public final boolean isOrdered() {
    return getPositionId() != null;
  }

  public boolean isRemovable() {
    return myRemovable;
  }

  public int removeFromTree() {
    int index = myTreeNode.removeFromParent();
    myConfiguration.removeMe();
    return index;
  }

  public final NameResolver getResolver() {
    NameResolver resolver = getContainerActor(NameResolver.ROLE);
    LogHelper.assertError(resolver != null || !isNode(), "Missing resolver", this);
    return resolver;
  }

  private <T> T getContainerActor(Role<T> role) {
    RootNode root = getRoot();
    if (root == null) {
      assert false : this;
      return null;
    }
    T actor = root.getContainer().getActor(role);
    assert actor != null;
    return actor;
  }

  public String getNodeId() {
    return myNodeId;
  }

  public void onInsertToModel() {
    if (!myEverInserted) {
      myEverInserted = true;
      onFirstInsertToModel();
    }
    invalidatePreview();
    if (getChildrenOrderPolicy() == ChildrenOrderPolicy.ORDER_ALWAYS) {
      // careful not to affect the tree in this method
      sortChildrenLater();
    }
  }

  protected void sortChildrenLater() {
    ourSortChildrenUpdater.addJobDelayed(this);
  }

  public void onRemoveFromModel() {
    // todo
  }

  public void onChildrenChanged() {
    OUR_STORE_CHILDREN_JOBS.addJobDelayed(this);
  }

  @ThreadAWT
  private void storeChildrenOrder() {
    if (!myLoaded)
      return;
    Configuration order = myConfiguration.getSubset(ConfigNames.CHILD_NODE_ORDER);
    if (!order.isEmpty()) {
      // migration
      order.clear();
      order.removeMe();
    }
    order = myConfiguration.getOrCreateSubset(ConfigNames.CHILD_NODE_ORDER_2);
    order.clear();

    List<? extends GenericNode> children = getChildren();
    int i = 0;
    for (GenericNode child : children) {
      String position = child.getPositionId();
      if (position != null) {
        order.setSetting(String.valueOf(i++), position);
      }
    }
  }

  public void addAllowedChildType(TreeNodeFactory.NodeType childType) {
    Threads.assertAWTThread();
    myAllowedChildren |= 1 << childType.ordinal();
//    myAllowedChildren.add(childType);
  }

  public void beRemovable() {
    myRemovable = true;
  }

  @ThreadAWT
  public void fireTreeNodeChanged() {
    myTreeNode.fireChanged();
  }

  protected boolean areAllChildrenCopiable() {
    List<? extends GenericNode> children = getChildren();
    for (GenericNode node : children) {
      if (!node.isCopiable())
        return false;
    }
    return true;
  }

  @Nullable
  public RootNode getRoot() {
    ATreeNode<GenericNode> node = myTreeNode;
    while (true) {
      ATreeNode<GenericNode> parent = node.getParent();
      if (parent == null)
        break;
      node = (ATreeNode<GenericNode>) parent;
    }
    GenericNode ancestor = node.getUserObject();
    if (ancestor instanceof RootNode)
      return (RootNode) ancestor;
    return null;
  }

  public boolean isNode() {
    return myTreeNode.isAttachedToModel();
  }

  public void sortChildren() {
    if (!isNode())
      return;
    if (isSortedChildren())
      return;
    ATreeNode<GenericNode> parentNode = getTreeNode();
    int count = parentNode.getChildCount();
    assert count > 1;
    final GenericNode[] children = new GenericNode[count];
    Integer[] order = new Integer[count];
    for (int i = 0; i < count; i++) {
      children[i] = parentNode.getChildAt(i).getUserObject();
      order[i] = i;
    }
    Arrays.sort(order, new Comparator<Integer>() {
      public int compare(Integer i1, Integer i2) {
        return compareChildren(children[i1], children[i2]);
      }
    });

    // 1. remove all children that are not on their places
    for (int i = count - 1; i >= 0; i--) {
      if (order[i] != i) {
        parentNode.remove(i);
      }
    }

    // 2. insert these children back to their new place
    for (int i = 0; i < count; i++) {
      int k = order[i];
      if (k != i) {
        TreeModelBridge<GenericNode> node = children[k].getTreeNode();
        assert node.getParent() == null : node + " " + parentNode + " " + i + " " + k;
        parentNode.insert(node, i);
      }
    }
  }

  public boolean isSortedChildren() {
    int count = getChildrenCount();
    if (count <= 1)
      return true;
    GenericNode child = getChildAt(0);
    for (int i = 1; i < count; i++) {
      GenericNode nextChild = getChildAt(i);
      if (compareChildren(child, nextChild) > 0)
        return false;
      child = nextChild;
    }
    return true;
  }

  public boolean isCollapsed() {
    // kludge
    RootNode root = getRoot();
    if (root == null)
      return false;
    TreeNodeFactory nodeFactory = root.getNodeFactory();
    return !nodeFactory.isExpanded(this);
  }

  protected void onFirstInsertToModel() {
  }

  protected final void setPresentation(CanvasRenderable presentation) {
    assert presentation != null : "presentation == null";
    synchronized (myPresentationLock) {
      myPresentation = presentation;
      myPresentationDetach.detach();
      if (presentation instanceof EditableText) {
        final EditableText editableText = (EditableText) presentation;
        editableText.storeName(Lifespan.FOREVER, myConfiguration, ConfigNames.NAME_SETTING);
        DetachComposite life = new DetachComposite(true);
        editableText.getModifiable().addAWTChangeListener(life, new ChangeListener() {
          public void onChange() {
            fireTreeNodeChanged();
          }
        });
        myPresentationDetach = life;
      }
    }
    fireTreeNodeChanged();
  }


  public String toString() {
    return toString(new StringBuffer(), new PlainTextCanvas()).toString();
  }

  private StringBuffer toString(StringBuffer result, PlainTextCanvas canvas) {
    GenericNode parent = getParent();
    if (parent != null) {
      ((GenericNodeImpl) parent).toString(result, canvas);
      result.append(".");
    }
    result.append(getName());
    return result;
  }

  public boolean isSynchronized() {
    GenericNode parent = getParent();
    return parent != null && parent.isSynchronized();
  }

  @NotNull
  public String getName() {
    CanvasRenderable presentation = getPresentation();
    if (presentation == null)
      return "?Unnamed";
    if (presentation instanceof TextWithIcon) {
      return ((TextWithIcon) presentation).getText();
    } else {
      PlainTextCanvas canvas = new PlainTextCanvas();
      presentation.renderOn(canvas, CellState.LABEL);
      return canvas.getText();
    }
  }

  public boolean isNarrowing() {
    return false;
  }

  @ThreadSafe
  public final void invalidatePreviewSafe() {
    if (Context.isAWT())
      invalidatePreview();
    else
      ourPreviewInvalidater.addJob(this);
  }

  @ThreadAWT
  public final void invalidatePreview() {
    Threads.assertAWTThread();
    if (!isNode())
      return;
    RootNode root = getRoot();
    if (root == null) {
      assert false : this;
      return;
    }

    invalidatePreview(root);
  }

  protected void invalidatePreview(RootNode root) {
    if (isNarrowing()) {
      // not narrowing nodes do not need preview
      ItemsPreviewManager manager = root.getItemsPreviewManager();
      manager.cancel(this);
      setPreview(null);
      ourAfterInvalidateScheduleUpdater.addJob(this);
    }

    invalidateChildren();
  }

  protected void invalidateChildren() {
    for (int i = 0; i < getChildrenCount(); i++) {
      getChildAt(i).invalidatePreview();
    }
  }

  @ThreadAWT
  public void maybeSchedulePreview() {
    Threads.assertAWTThread();
    if (!isNode() || !isNarrowing())
      return;
    maybeSchedulePreview(false);
  }

  @ThreadAWT
  private void maybeSchedulePreview(boolean cancelOngoing) {
    assert isNarrowing() : this;
    ItemsPreview preview;
    synchronized (this) {
      preview = myItemsPreview;
    }
    if (preview != null) {
      if (preview.isValid()) {
        return;
      }
      setPreview(null);
    }

    if (!isSynchronized()) {
      return;
    }

    GenericNode parent = getParent();
    if (parent == null) {
      return;
    }

    if (!parent.isShowable() && !(parent instanceof RootNode)) {
      // if parent is not on the screen, we don't need to recount
      // if parent is Root, we have to recount anyway
      return;
    }

    schedulePreview(cancelOngoing);
  }

  private void schedulePreview(boolean cancelOngoing) {
    if (!preschedulePreviewCalculation()) {
      return;
    }
    RootNode root = getRoot();
    if (root == null)
      return;
    ItemsPreviewManager manager = root.getItemsPreviewManager();
    schedulePreview(manager, cancelOngoing);
  }

  protected void schedulePreview(ItemsPreviewManager manager, boolean cancelOngoing) {
    manager.schedule(this, myCalcPreviewJob, cancelOngoing);
  }

  public boolean isShowable() {
    TreeModelBridge<GenericNode> parentNode = getTreeNode().getParent();
    if (parentNode == null)
      return false;
    RootNode root = getRoot();
    if (root == null)
      return false;
    ATree tree = root.getNodeFactory().getTree();
    return tree.isExpanded(parentNode);
  }

  public List<GenericNode> getPathFromRoot() {
    List<GenericNode> ancestors = Collections15.arrayList();
    GenericNode node = this;
    while (node != null && node.getParent() != null) {
      ancestors.add(node);
      node = node.getParent();
    }
    Collections.reverse(ancestors);
    return ancestors;
  }

  public int compareToSame(GenericNode that) {
    return String.CASE_INSENSITIVE_ORDER.compare(Util.NN(this.getName()), Util.NN(that.getName()));
  }

  /**
   * This method is allowed to set a new preview if it can be counted in AWT thread
   *
   * @return true if caller should proceed and schedule long preview generation
   */
  @ThreadAWT
  protected boolean preschedulePreviewCalculation() {
    assert isNarrowing() : this;
    boolean proceed = isPreviewAvailable();
    if (!proceed) {
      // we don't need a recalculation
      setPreview(new ItemsPreview.Unavailable());
    }
    return proceed;
  }

  @ThreadAWT
  protected boolean isPreviewAvailable() {
    QueryResult result = getQueryResult();
    return result.canRunNow();
  }

  /**
   * Returns previous preview.
   */
  @ThreadSafe
  @Nullable
  protected ItemsPreview setPreview(ItemsPreview preview) {
    assert isNarrowing() : this;
    ItemsPreview oldPreview;
    synchronized (this) {
      oldPreview = myItemsPreview;
      if (preview != null) {
        if (!preview.isValid()) {
          // won't update with preview that's already invalid
          return oldPreview;
        }
      }
      myItemsPreview = preview;
    }
    if (oldPreview != null) {
      oldPreview.invalidate();
    }
    if (Context.isAWT()) {
      updatePresentationWithPreview();
    } else {
      ourPresentationWithPreviewUpdater.addJob(this);
    }
    return oldPreview;
  }

  @ThreadAWT
  private void updatePresentationWithPreview() {
    assert isNarrowing() : this;
    ItemsPreview preview;
    synchronized (this) {
      preview = myItemsPreview;
    }
    updatePreviewBasedValues(preview);
    Object presentation = getPresentation();
    if (presentation instanceof CounterPresentation) {
      CounterPresentation cp = ((CounterPresentation) presentation);
      if (preview != null && preview.isValid()) {
        if (preview.isAvailable()) {
          cp.setValidCounter(preview.getItemsCount());
        } else {
          cp.setCounterNotAvailable();
        }
      } else {
        cp.invalidateCounter();
      }
    }
  }

  @ThreadAWT
  protected void updatePreviewBasedValues(ItemsPreview preview) {
  }

  /**
   * This method is called from ArtifactsPreviewManager when it comes to creating an ArtifactsPreview
   */
  @Override
  @CanBlock
  @Nullable
  public ItemsPreview getOrCalculatePreview(Lifespan lifespan, DBReader reader) {
    if (!isNarrowing()) {
      GenericNode parent = getParent();
      return parent != null ? parent.getOrCalculatePreview(lifespan, reader) : null;
    }
    assert isNarrowing() : this;
    ItemsPreview preview;
    synchronized (this) {
      preview = myItemsPreview;
    }
    if (preview != null && preview.isValid())
      return preview;
    preview = calculatePreview(lifespan, reader);
    if (preview != null && preview.isValid() && !lifespan.isEnded()) {
      setPreview(preview);
      return preview;
    } else {
      return null;
    }
  }

  @CanBlock
  @Nullable
  protected ItemsPreview calculatePreview(Lifespan lifespan, DBReader reader) {
    assert !isNarrowing() : this + " has to override";
    assert isNarrowing() : this + " should not be called";
    return null;
  }

  /**
   * Returns current item preview for this node (or for its parent if this node is not narrowing, such as a folder).
   * If scheduleIfNotAvailable parameter is true and this method returns null, then preview gathering is initialized
   * and you can call this method once more a little bit later. (Use bottleneck to schedule another check.)
   *
   * @param scheduleIfNotAvailable if true, then you "require" preview. The method may still return null, but
   *                               a new recalculation is scheduled.
   * @return current preview or null if it is not available
   */
  @Nullable
  public ItemsPreview getPreview(boolean scheduleIfNotAvailable) {
    if (isNarrowing()) {
      ItemsPreview preview;
      synchronized (this) {
        preview = myItemsPreview;
      }
      if (scheduleIfNotAvailable && (preview == null || !preview.isValid())) {
        schedulePreview(false);
      }
      return preview;
    } else {
      GenericNode parent = getParent();
      return parent == null ? null : parent.getPreview(scheduleIfNotAvailable);
    }
  }

  /**
   * @param scheduleIfNotAvailable see {@link #getPreview(boolean)}
   * @return current number of items, or -1 if preview is not yet available or recounting.
   */
  public int getPreviewCount(boolean scheduleIfNotAvailable) {
    ItemsPreview preview = getPreview(scheduleIfNotAvailable);
    return preview == null || !preview.isValid() ? -1 : preview.getItemsCount();
  }

  public int getCusionedPreviewCount() {
    return getPreviewCount(false);
  }

  public Integer get() {
    int count = getPreviewCount(false);
    return count < 0 ? null : count;
  }
}
