package com.almworks.explorer.tree;

import com.almworks.api.application.*;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintType;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.application.qb.EnumGrouping;
import com.almworks.api.application.tree.*;
import com.almworks.api.config.ConfigNames;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.explorer.qbuilder.filter.EnumConstraintKind;
import com.almworks.integers.IntArray;
import com.almworks.items.api.Database;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.sfs.StringFilterSet;
import com.almworks.util.threads.BottleneckJobs;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.ColorUtil;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DistributionFolderNodeImpl extends GenericNodeImpl implements DistributionFolderNode, RenamableNode, ModelAware {
  static final String REMOVED_PREFIX = "Removed: ";
  public static final Comparator<DistributionQueryNode> DISTRIBUTION_QUERY_ORDER = new DistributionComparator();
  public static final Comparator<DistributionGroupNode> DISTRIBUTION_GROUP_ORDER = new DistributionGroupComparator();

  protected static final BottleneckJobs<DistributionFolderNodeImpl> UPDATE_HYPERCUBE =
    new BottleneckJobs<DistributionFolderNodeImpl>(500, ThreadGate.STRAIGHT) {
      protected void execute(DistributionFolderNodeImpl job) {
        job.onHypercubeUpdated();
      }
    };

  private static final DistributionVisitor EMPTY_GROUPS_REMOVER = new DistributionVisitor() {
    protected Object collectQuery(DistributionQueryNodeImpl query, Object ticket, List list) {
      return ticket;
    }

    protected Object visitGroup(DistributionGroupNodeImpl group, Object ticket) {
      if (group.getChildrenCount() == 0)
        group.removeFromTree();
      return ticket;
    }
  };

  private final ParentResult myResult = new ParentResult(this);
  private ItemHypercube myLastCube = null;

  private final NodeParams myParams;

  private final Lifecycle myLife = new Lifecycle();
  private final Lifecycle myItemKeyModelLife = new Lifecycle();

  private boolean myExpandAfterNextUpdate = false;

  private boolean myShowExpandingNodeWhenInserted;
  private ExpandingProgressNode myExpandingProgressNode;
  private final DistributionChildrenUpdate myChildrenUpdate = new DistributionChildrenUpdate(this);
  private final HidingEmptyChildren myHidingChildren = new HidingEmptyChildren(this);

  private static final BottleneckJobs<DistributionFolderNodeImpl> UPDATE_ALL_GROUPS =
    new BottleneckJobs<DistributionFolderNodeImpl>(500, ThreadGate.AWT) {
      protected void execute(DistributionFolderNodeImpl job) {
        job.updateAllQueryGroups();
      }
    };

  public DistributionFolderNodeImpl(Database db,  String name, Configuration configuration) {
    super(db, new EditableText(name, Icons.NODE_DISTRIBUTION_FOLDER_OPEN), configuration);
    setPresentation(
      new HidingPresentation(
        name, Icons.NODE_DISTRIBUTION_FOLDER_OPEN,
        Icons.NODE_DISTRIBUTION_FOLDER_HIDING, this));

    addAllowedChildType(TreeNodeFactory.NodeType.DISTRIBUTION_QUERY);
    addAllowedChildType(TreeNodeFactory.NodeType.DISTRIBUTION_GROUP);
    beRemovable();
    myParams = NodeParams.create(this, configuration);
  }

  @ThreadAWT
  public void setParameters(@Nullable ConstraintDescriptor descriptor, @NotNull DistributionParameters parameters) {
    if (parameters == null) {
      assert false : this;
      return;
    }
    myLastCube = null; // force update
    myParams.setParameters(descriptor, parameters);
    fireSubtreeChanged();
    UPDATE_ALL_GROUPS.addJobDelayed(this);
    UPDATE_HYPERCUBE.addJobDelayed(this);

    if (descriptor != null) {
      String name = descriptor.getDisplayName();
      if (StringFilterSet.isFiltering(parameters.getGroupsFilter()) ||
        StringFilterSet.isFiltering(parameters.getValuesFilter()))
        name += " (filtering)";
      getPresentation().setText(name);
    }
  }

  public ConstraintDescriptor getDescriptor() {
    return myParams.getDescriptor();
  }

  @NotNull
  public DistributionParameters getParameters() {
    return myParams.getParameters();
  }

  @NotNull
  public QueryResult getQueryResult() {
    return myResult;
  }

  public boolean isCopiable() {
    return true;
  }

  public boolean isRenamable() {
    return true;
  }

  public EditableText getPresentation() {
    return (EditableText) super.getPresentation();
  }

  public void onInsertToModel() {
    super.onInsertToModel();
    if (myShowExpandingNodeWhenInserted) {
      myShowExpandingNodeWhenInserted = false;
      if (getChildrenCount() == 0) {
        RootNode root = getRoot();
        if (root == null) LogHelper.error("Missing root on insert", this);
        else {
          myExpandingProgressNode = new ExpandingProgressNode(root.getEngine().getDatabase(), getConfiguration());
          addChildNode(myExpandingProgressNode);
        }
      }
    }
    updateAndListenParent();
  }

  private void updateAndListenParent() {
    GenericNode parent = getParent();
    if (parent == null) {
      assert false : this;
      return;
    }
    ChangeListener onParentChanged = new ChangeListener() {
      public void onChange() {
        myParams.resolveAttribute();
        UPDATE_HYPERCUBE.addJobDelayed(DistributionFolderNodeImpl.this);
      }
    };
    parent.getQueryResult().addAWTChangeListener(myLife.lifespan(), onParentChanged);
    onParentChanged.onChange();
  }

  public void onRemoveFromModel() {
    myLife.cycle();
    myItemKeyModelLife.cycle();
    super.onRemoveFromModel();
  }

  @SuppressWarnings({"MethodOnlyUsedFromInnerClass"})
  private void onHypercubeUpdated() {
    RootNode root = getRoot();
    if (root == null || !isNode())
      return;
    ItemHypercube cube = getHypercube(false);
    if (!same(myLastCube, cube)) {
      myLastCube = cube;
      if (myLastCube != null) {
        ConstraintType type = myParams.getConstraintType();
        Database db = root.getEngine().getDatabase();
        if (type instanceof EnumConstraintType) onModelUpdated(db, (EnumConstraintType) type, cube);
        else onModelUpdated(db, null, cube);
      }
    }
    ExpandingProgressNode progressNode = myExpandingProgressNode;
    if (progressNode != null) {
      GenericNode parent = progressNode.getParent();
      if (parent == this) {
        myExpandingProgressNode = null;
        boolean wasSelected = false;
        wasSelected = root.getNodeFactory().getTree().getSelectionAccessor().isSelected(progressNode.getTreeNode());
        progressNode.removeFromTree();
        if (wasSelected) {
          root.getNodeFactory().selectNode(this, false);
        }
      }
    }
  }

  private boolean same(ItemHypercube cube1, ItemHypercube cube2) {
    if (cube1 == null)
      return cube2 == null;
    else
      return cube1.isSame(cube2);
  }

  private void onModelUpdated(Database db, @Nullable EnumConstraintType type, ItemHypercube cube) {
    myItemKeyModelLife.cycle();
    final Lifespan lifespan = myItemKeyModelLife.lifespan();
    DistributionState state = DistributionState.create(lifespan, this, type, cube, db);
    state.fullUpdate();
    if (myExpandAfterNextUpdate) {
      expandNode();
      myExpandAfterNextUpdate = false;
    }
    state.listenModel();
  }

  private void expandNode() {
    // kludge :(
    RootNode root = getRoot();
    if (root != null) {
      TreeNodeFactory factory = root.getNodeFactory();
      if (factory != null) {
        factory.expandNode(this);
      }
    }
  }


  boolean isItemKeyAccepted(ItemKey value) {
    if (isItemKeyRejected(value)) return false;
    ResolvedItem resolved = Util.castNullable(ResolvedItem.class, value);
    if (resolved == null) {
      ConstraintDescriptor descriptor = getDescriptor();
      if (descriptor == null) return false;
      EnumConstraintType type = Util.castNullable(EnumConstraintType.class, descriptor.getType());
      return type != null && type.isNotSetItem(value);
    }
    return isResolvedKeyAccepted(resolved);
  }

  private boolean isResolvedKeyAccepted(@Nullable ResolvedItem resolved) {
    if (StringFilterSet.isFiltering(getParameters().getGroupsFilter())) {
      EnumGrouping<?> grouping = getGrouping();
      if (grouping != null) {
        ItemKeyGroup group = resolved == null ? null : grouping.getGroup(resolved);
        if (!isItemKeyGroupAccepted(group)) {
          return false;
        }
      }
    }
    return true;
  }

  boolean isItemKeyRejected(ItemKey key) {
    if (key == null) return true;
    StringFilterSet valuesFilter = getParameters().getValuesFilter();
    if (StringFilterSet.isFiltering(valuesFilter)) {
      String name = getFilterConvertor().convert(key);
      if (name == null || name.trim().length() == 0) return true;
      assert valuesFilter != null;
      if (!valuesFilter.isAccepted(name)) return true;
    }
    return false;
  }

  private Convertor<ItemKey, String> getFilterConvertor() {
    EnumConstraintType type = Util.castNullable(EnumConstraintType.class, myParams.getConstraintType());
    if (type == null) return ItemKey.DISPLAY_NAME;
    Convertor<ItemKey, String> convertor = type.getFilterConvertor();
    if (convertor == null) convertor = ItemKey.DISPLAY_NAME;
    return convertor;
  }

  boolean isItemKeyGroupAccepted(@Nullable ItemKeyGroup group) {
    StringFilterSet groupsFilter = getParameters().getGroupsFilter();
    if (StringFilterSet.isFiltering(groupsFilter)) {
      String groupName = group == null ? null : group.getDisplayableName();
      assert groupsFilter != null;
      return groupsFilter.isAccepted(Util.NN(groupName, ItemKeyGroup.NULL_GROUP_SENTINEL.getDisplayableName()));
    }
    return true;
  }

  void removeEmptyGroups() {
    EMPTY_GROUPS_REMOVER.visit(this, null);
  }

  void removeQuerySafely(Database db, DistributionQueryNodeImpl query) {
    query.setPinned(false);
    if (query.getChildrenCount() == 0) {
      query.removeFromTree();
    } else {
      String text = query.getPresentation().getText();
      if (!text.startsWith(REMOVED_PREFIX))
        query.getPresentation().setText(REMOVED_PREFIX + text);
      query.getTreeNode().removeFromParent();
      placeQuery(db, query, getItemKeyGroup(query));
    }
  }

  void updateQueryGroup(Database db, DistributionQueryNodeImpl query) {
    updateQueryGroup(db, query, true);
  }

  private void updateQueryGroup(Database db, DistributionQueryNodeImpl query, boolean scheduleFullUpdateIfChanged) {
    GenericNode parent = query.getParent();
    assert query.isNode() && (parent == this || (parent != null && parent.getParent() == this));
    EnumGrouping<?> grouping = myParams.getGroupingForTree();
    ItemKeyGroup newGroup = null;
    if (grouping != null) {
      ResolvedItem artifact = getResolvedItemForQueryGroup(query);
      if (artifact != null) {
        newGroup = grouping.getGroup(artifact);
      }
    }
    ItemKeyGroup oldGroup = null;
    if (parent instanceof DistributionGroupNodeImpl) {
      DistributionGroupNodeImpl groupNode = (DistributionGroupNodeImpl) parent;
      oldGroup = groupNode.getGroup();
      if (oldGroup == null && newGroup != null) {
        if (groupNode.isSameGroup(newGroup)) {
          // group wasn't initialized yet
          groupNode.updateGroup(newGroup);
          sortChildrenLater();
          oldGroup = newGroup;
        }
      }
    }
    if (!Util.equals(newGroup, oldGroup)) {
      if (isItemKeyGroupAccepted(newGroup)) {
        query.getTreeNode().removeFromParent();
        placeQuery(db, query, newGroup);
      } else {
        removeQuerySafely(db, query);
      }
      if (scheduleFullUpdateIfChanged) {
        UPDATE_ALL_GROUPS.addJobDelayed(this);
      }
    }
  }

  private void updateAllQueryGroups() {
    final RootNode root = getRoot();
    if (root == null || !isNode()) return;
    if (getGrouping() == null)
      return;

    new DistributionVisitor() {
      protected Object visitQuery(DistributionQueryNodeImpl query, Object ticket) {
        updateQueryGroup(root.getEngine().getDatabase(), query, false);
        return false;
      }
    }.visit(this, null);

    removeEmptyGroups();
  }

  void addAcceptedValues(Collection<ItemKey> options) {
    ConstraintDescriptor descriptor = getDescriptor();
    if (descriptor == null)
      return;
    if (options.size() > 0) {
      RootNode root = getRoot();
      if (root == null) {
        assert false;
        Log.warn("no root");
        return;
      }
      Database db = root.getEngine().getDatabase();
      MultiMap<ItemKey, DistributionQueryNodeImpl> removed = collectRemovedChildren();

      MultiMap<ItemKeyGroup, DistributionQueryNodeImpl> queriesToPlace = MultiMap.create();
      for (ItemKey value : options) {
        if (!isItemKeyAccepted(value))
          continue;
        List<DistributionQueryNodeImpl> candidates = removed != null ? removed.getAll(value) : null;
        DistributionQueryNodeImpl query = findOrCreateQueryNode(value, candidates);
        if (query != null) queriesToPlace.add(getItemKeyGroup(query), query);
      }
      for (ItemKeyGroup group : queriesToPlace.keySet()) {
        // As we are inserting nodes from the end to the beginning of the children list, we want to have a list where the heaviest node is the first
        List<DistributionQueryNodeImpl> nodes = queriesToPlace.getAllEditable(group);
        if (nodes == null) continue;
        GenericNodeImpl parent = group == null ? this : findOrCreateGroup(db, group);
        LogHelper.assertError(parent != null);
        ItemHypercube cube = parent == null ? new ItemHypercubeImpl() : parent.getHypercube(false);
        Collections.sort(nodes, Containers.reverse(new DistributionComparator(cube)));
        int start = -1;
        for (DistributionQueryNodeImpl node : nodes) {
          start = DistributionFolderNodeImpl.placeQueryUnder(parent, node, start);
        }
      }
    }
  }

  private DistributionQueryNodeImpl findOrCreateQueryNode(ItemKey value, List<DistributionQueryNodeImpl> candidates) {
    DistributionQueryNodeImpl query = null;
    if (candidates != null && candidates.size() > 0) {
      query = candidates.get(0);
      String text = query.getPresentation().getText();
      if (text.startsWith(DistributionFolderNodeImpl.REMOVED_PREFIX)) {
        query.setPinned(true);
        query.getPresentation().setText(text.substring(DistributionFolderNodeImpl.REMOVED_PREFIX.length()).trim());
        query.getTreeNode().removeFromParent();
      } else {
        assert false : query;
        query = null;
      }
    }
    if (query == null) {
      ConstraintDescriptor descriptor = getDescriptor();
      if (descriptor == null) return null;
      RootNode root = getRoot();
      if (root == null) return null;
      TreeNodeFactory factory = root.getNodeFactory();
      Database db = root.getEngine().getDatabase();
      Configuration config = getConfiguration().createSubset(ConfigNames.KLUDGE_DISTRIBUTION_QUERY_TAG_NAME);
      query = new DistributionQueryNodeImpl(db, "", config, Pair.create(descriptor, value));
//          query.getPresentation().setText(value.getDisplayName());
      query.setPinned(true);
      applyPrototype(query, factory);
      query.setFilter(BaseEnumConstraintDescriptor.createNode(descriptor, value));
      query.updateName();
    }
    return query;
  }

  public EnumGrouping<?> getGrouping() {
    return myParams.getGrouping();
  }

  @Nullable
  MultiMap<ItemKey, DistributionQueryNodeImpl> collectRemovedChildren() {
    final ConstraintDescriptor descriptor = getDescriptor();
    if (descriptor == null)
      return null;
    return new DistributionVisitor<MultiMap<ItemKey, DistributionQueryNodeImpl>>() {
      protected MultiMap<ItemKey, DistributionQueryNodeImpl> visitQuery(DistributionQueryNodeImpl query,
        MultiMap<ItemKey, DistributionQueryNodeImpl> ticket)
      {
        if (!query.isPinned()) {
          String text = query.getPresentation().getText();
          if (text != null && text.startsWith(REMOVED_PREFIX)) {
            Pair<ConstraintDescriptor, ItemKey> pair = query.getAttributeValue();
            assert pair != null;
            if (!pair.getFirst().getId().equals(descriptor.getId()))
              Log.warn("Alive variant kept from different distribution: " + pair.getFirst().getId() + " " +
                descriptor.getId());
            ItemKey value = pair.getSecond();
            assert value != null : this;
            assert value != ItemKeyStub.ABSENT : this;
            if (ticket == null)
              ticket = MultiMap.create();
            ticket.add(value, query);
          }
        }
        return ticket;
      }
    }.visit(this, null);
  }

  public ReadonlyConfiguration createCopy(Configuration parentConfig) {
    // kludge: repetition from LazyDistributionNodeImpl
    Configuration copy = (Configuration) super.createCopy(parentConfig);
    Configuration thisConfig = getConfiguration();
    Configuration prototype = thisConfig.getSubset(ConfigNames.PROTOTYPE_TAG);
    if (!prototype.isEmpty()) {
      ConfigurationUtil.copyTo(prototype, copy.createSubset(ConfigNames.PROTOTYPE_TAG));
    }
    ConfigurationUtil.copySubsetsTo(thisConfig, copy, ConfigNames.DISTRIBUTION_VALUES_FILTER);
    ConfigurationUtil.copySubsetsTo(thisConfig, copy, ConfigNames.DISTRIBUTION_GROUPS_FILTER);
    return copy;
  }

  void applyPrototype(DistributionQueryNode query, TreeNodeFactory factory) {
    Configuration prototype = getConfiguration().getSubset(ConfigNames.PROTOTYPE_TAG);
    if (prototype.isEmpty())
      return;
    // todo - groups?
    CreateDefaultQueries.createDefaultQueries(prototype, query, factory);
  }

  @Nullable
  ItemKeyGroup getItemKeyGroup(DistributionQueryNodeImpl query) {
    if (query.isPinned()) {
      EnumGrouping<?> grouping = myParams.getGroupingForTree();
      ResolvedItem resolved = grouping == null ? null : getResolvedItemForQueryGroup(query);
      return resolved == null ? null : grouping.getGroup(resolved);
    }
    return null;
  }

  void placeQuery(Database db, DistributionQueryNode query, ItemKeyGroup group) {
    placeQueryUnder(group == null ? this : findOrCreateGroup(db, group), query, -1);
  }

  DistributionGroupNodeImpl findOrCreateGroup(Database db, ItemKeyGroup group) {
    DistributionGroupNodeImpl groupNode = findGroup(group);
    if (groupNode == null) {
      groupNode = createGroupNode(db, group);
    }
    return groupNode;
  }

  private DistributionGroupNodeImpl createGroupNode(Database db, ItemKeyGroup group) {
    Configuration config = getConfiguration().createSubset(ConfigNames.KLUDGE_DISTRIBUTION_GROUP_TAG_NAME);
    DistributionGroupNodeImpl node = new DistributionGroupNodeImpl(db, config);
    //= new DistributionQueryNodeImpl("", config, Pair.create(descriptor, value));
//          query.getPresentation().setText(value.getDisplayName());
    node.updateGroup(group);
    placeGroupUnder(this, node);
    return node;
  }

  private DistributionGroupNodeImpl findGroup(ItemKeyGroup group) {
    TreeModelBridge<GenericNode> treeNode = getTreeNode();
    int count = treeNode.getChildCount();
    for (int i = 0; i < count; i++) {
      GenericNode node = treeNode.getChildAt(i).getUserObject();
      if (node instanceof DistributionGroupNodeImpl) {
        DistributionGroupNodeImpl groupNode = ((DistributionGroupNodeImpl) node);
        if (groupNode.isSameGroup(group)) {
          groupNode.updateGroup(group);
          return groupNode;
        }
      }
    }
    return null;
  }

  static int placeQueryUnder(GenericNode node, DistributionQueryNode query, int startIdx) {
    ATreeNode<GenericNode> queryTreeNode = query.getTreeNode();
    ATreeNode<GenericNode> parentTreeNode = node.getTreeNode();
    int i = startIdx < 0 ? parentTreeNode.getChildCount() - 1 : startIdx;
    for (; i >= 0; i--) {
      ATreeNode<GenericNode> child = parentTreeNode.getChildAt(i);
      GenericNode sampleNode = child.getUserObject();
      if (!(sampleNode instanceof DistributionQueryNode))
        continue;
      DistributionQueryNode sampleQuery = ((DistributionQueryNode) sampleNode);
      int diff = DISTRIBUTION_QUERY_ORDER.compare(query, sampleQuery);
      if (diff >= 0)
        break;
    }
    parentTreeNode.insert(queryTreeNode, i + 1);
    return i + 1;
  }

  private void placeGroupUnder(GenericNode node, DistributionGroupNodeImpl group) {
    ATreeNode<GenericNode> groupTreeNode = group.getTreeNode();
    ATreeNode<GenericNode> parentTreeNode = node.getTreeNode();
    int count = parentTreeNode.getChildCount();
    int i;
    for (i = count - 1; i >= 0; i--) {
      ATreeNode<GenericNode> child = parentTreeNode.getChildAt(i);
      GenericNode sampleNode = child.getUserObject();
      if (!(sampleNode instanceof DistributionGroupNode))
        break;
      DistributionGroupNode sampleGroup = ((DistributionGroupNode) sampleNode);
      int diff = DISTRIBUTION_GROUP_ORDER.compare(group, sampleGroup);
      if (diff >= 0)
        break;
    }
    parentTreeNode.insert(groupTreeNode, i + 1);
  }

  @Nullable
  private ResolvedItem getResolvedItemForQueryGroup(DistributionQueryNodeImpl query) {
    ItemHypercube cube = getHypercube(false);
    Object obj = query.getItems(cube);
    if (obj instanceof ResolvedItem) {
      return (ResolvedItem) obj;
    } else if (obj instanceof List) {
      List list = (List) obj;
      if (list.size() > 0) {
        return Util.castNullable(ResolvedItem.class, list.get(0));
      }
    }
    return null;
  }

  public void expandAfterNextUpdate() {
    myExpandAfterNextUpdate = true;
  }

  public int compareChildren(GenericNode node1, GenericNode node2) {
    int classDiff = ViewWeightManager.compare(node1, node2);
    if (classDiff != 0) {
      return classDiff;
    }
    if (node1 == null || node2 == null) {
      assert false;
      return 0;
    }
    if ((node1 instanceof DistributionQueryNode) && (node2 instanceof DistributionQueryNode)) {
      return DISTRIBUTION_QUERY_ORDER.compare((DistributionQueryNode) node1, (DistributionQueryNode) node2);
    } else if ((node1 instanceof DistributionGroupNode) && (node2 instanceof DistributionGroupNode)) {
      return DISTRIBUTION_GROUP_ORDER.compare((DistributionGroupNode) node1, (DistributionGroupNode) node2);
    } else {
      return NavigationTreeUtil.compareNodes(node1, node2);
    }
  }

  public ChildrenOrderPolicy getChildrenOrderPolicy() {
    return ChildrenOrderPolicy.ORDER_ALWAYS;
  }

  public void showExpadingNodeWhenInserted() {
    myShowExpandingNodeWhenInserted = true;
  }

  @Override
  public boolean canHideEmptyChildren() {
    return true;
  }

  public void setHideEmptyChildren(boolean newValue) {
    if(getHideEmptyChildren() != newValue) {
      myParams.setHideEmptyChildren(newValue);
      fireSubtreeChanged();
    }
  }

  public boolean getHideEmptyChildren() {
    return getParameters().isHideEmptyQueries();
  }

  public boolean isHidingEmptyChildren() {
    return myHidingChildren.hasHiddenOrNotCounted();
  }

  static int comparePresentations(GenericNode o1, GenericNode o2) {
    return o1.getName().compareToIgnoreCase(o2.getName());
  }

  private static boolean invalidatePreview(GenericNodeImpl parent, Class<? extends GenericNodeImpl> stepInto) {
    Iterator<? extends GenericNode> it = parent.getChildrenIterator();
    IntArray indexes = new IntArray();
    int index = -1;
    while (it.hasNext()) {
      index++;
      GenericNode child = it.next();
      if (child.getClass() == ExpandingProgressNode.class) continue;
      DistributionQueryNodeImpl query = Util.castNullable(DistributionQueryNodeImpl.class, child);
      if (query != null) {
        query.setPreviewSilent(null);
        query.invalidateChildren();
        indexes.add(index);
      } else if (stepInto != null) {
        GenericNodeImpl subNode = Util.castNullable(stepInto, child);
        if (subNode != null) invalidatePreview(subNode, null);
        else Log.error("Unexpected node " + child);
      }
    }
    boolean hasChanges = !indexes.isEmpty();
    if (hasChanges) {
      TreeModelBridge<GenericNode> treeNode = parent.getTreeNode();
      treeNode.fireChildrenChanged(indexes);
      parent.fireTreeNodeChanged();
    }
    return hasChanges;
  }

  @Override
  protected void invalidatePreview(RootNode root) {
    myChildrenUpdate.setDisabled(true);
    try {
      boolean changed = invalidatePreview(this, DistributionGroupNodeImpl.class);
      if (changed) fireTreeNodeChanged();
    } finally {
      myChildrenUpdate.setDisabled(false);
    }
    myChildrenUpdate.forceUpdate();
  }

  public void scheduleChildPreview(ItemsPreviewManager manager, boolean cancelOngoing) {
    myChildrenUpdate.maybeUpdate();
  }

  private static class NodeParams {
    private final DistributionFolderNodeImpl myNode;
    @NotNull
    private DistributionParameters myParameters;
    @Nullable
    private ConstraintDescriptor myDescriptor;
    @Nullable
    private EnumGrouping myGrouping;
    private AtomicReference<DetachComposite> myResolveLife = new AtomicReference<DetachComposite>(null);

    private NodeParams(DistributionFolderNodeImpl node, ConstraintDescriptor descriptor, DistributionParameters parameters) {
      myNode = node;
      myDescriptor = descriptor;
      myParameters = parameters;
    }

    public static NodeParams create(DistributionFolderNodeImpl node, ReadonlyConfiguration configuration) {
      String attributeId = configuration.getSetting(ConfigNames.ATTRIBUTE_ID, null);
      ConstraintDescriptor descriptor = attributeId == null ? null :
        BaseEnumConstraintDescriptor.unresolvedDescriptor(attributeId, EnumConstraintKind.INCLUSION_OPERATION);
      DistributionParameters parameters = DistributionParameters.readConfig(configuration);
      return new NodeParams(node, descriptor, parameters);
    }

    @NotNull
    public DistributionParameters getParameters() {
      return myParameters;
    }

    @Nullable
    public ConstraintDescriptor getDescriptor() {
      return myDescriptor;
    }

    public ConstraintType getConstraintType() {
      return  myDescriptor != null ? myDescriptor.getType() : null;
    }

    private void endResolveLife() {
      DetachComposite life;
      while ((life = myResolveLife.get()) != null) {
        life.detach();
        myResolveLife.compareAndSet(life, null);
        LogHelper.debug("ResolveLife ended", myNode);
      }
    }

    public void setParameters(ConstraintDescriptor descriptor, DistributionParameters parameters) {
      myDescriptor = descriptor;
      endResolveLife();
      myParameters = parameters;
      myGrouping = null;
      resolveAttribute();
      writeConfig();
    }

    @Nullable
    private EnumGrouping<?> getGrouping() {
      String groupingName = myParameters.getGroupingName();
      if (groupingName == null)
        return null;
      if (myGrouping != null)
        return myGrouping;
      resolveGrouping();
      return myGrouping;
    }

    @Nullable
    private EnumGrouping<?> getGroupingForTree() {
      if (!myParameters.isArrangeInGroups())
        return null;
      else
        return getGrouping();
    }

    private boolean resolveAttribute() {
      // todo resolve grouping
      ConstraintDescriptor descriptor = myDescriptor;
      NameResolver resolver = myNode.getResolver();
      boolean resolved = false;
      if (descriptor != null) {
        if (resolver != null) {
          myDescriptor = descriptor.resolve(resolver, myNode.getHypercube(false), null);
          if (!(myDescriptor.getType() instanceof EnumConstraintType)) listenForDescriptor();
          else {
            LogHelper.debug("Descriptor resolved", myNode, myDescriptor);
            endResolveLife();
            resolved = true;
          }
          resolveGrouping();
        }
      }
      return resolved;
    }

    private void listenForDescriptor() {
      while (true) {
        DetachComposite life = myResolveLife.get();
        if (life != null) {
          if (life.isEnded()) {
            myResolveLife.compareAndSet(life, null);
            continue;
          }
          return;
        }
        life = new DetachComposite();
        if (!myResolveLife.compareAndSet(null, life)) continue;
        myNode.myLife.lifespan().add(life);
        LogHelper.debug("Waiting resolution", myNode);
        myNode.getResolver().getAllDescriptorsModel().addAWTChangeListener(life, new ChangeListener() {
          @Override
          public void onChange() {
            if (resolveAttribute()) {
              myNode.myLastCube = null;
              UPDATE_HYPERCUBE.addJob(myNode);
            }
          }
        });
      }
    }

    public void setHideEmptyChildren(boolean newValue) {
      myParameters = myParameters.setHideEmptyQueries(newValue);
      writeConfig();
    }

    private void writeConfig() {
      Configuration configuration = myNode.getConfiguration();
      if (myDescriptor != null) {
        configuration.setSetting(ConfigNames.ATTRIBUTE_ID, myDescriptor.getId());
      }
      myParameters.writeConfig(configuration);
    }

    private void resolveGrouping() {
      ConstraintDescriptor descriptor = getDescriptor();
      if (descriptor != null) {
        ConstraintType type = descriptor.getType();
        if (type instanceof EnumConstraintType) {
          myGrouping = null;
          String groupingName = getParameters().getGroupingName();
          if (groupingName != null) {
            List<EnumGrouping> groupings = ((EnumConstraintType) type).getAvailableGroupings();
            if (groupings != null) {
              for (EnumGrouping grouping : groupings) {
                if (Util.equals(groupingName, grouping.getDisplayableName())) {
                  myGrouping = grouping;
                  break;
                }
              }
            }
          }
        }
      }
    }

    @Override
    public String toString() {
      return "Params of " + myNode;
    }
  }


  private static class DistributionGroupComparator implements Comparator<DistributionGroupNode> {
    public int compare(DistributionGroupNode o1, DistributionGroupNode o2) {
      if (o1 == o2)
        return 0;
      GenericNode parent1 = o1.getParent();
      GenericNode parent2 = o2.getParent();
      GenericNode parent = parent1 == null ? parent2 : parent1;
      assert parent != null : o1 + " " + o2;
      assert parent1 == null || parent2 == null || parent1 == parent2 : parent1 + " " + parent2;
      if (!(parent instanceof DistributionFolderNodeImpl)) {
        // maybe both parents are null
        return 0;
      }
      DistributionFolderNodeImpl folder = ((DistributionFolderNodeImpl) parent);
      EnumGrouping grouping = folder.myParams.getGrouping();
      if (grouping == null) {
        return 0;
      }
      Comparator comparator = grouping.getComparator();
      if (comparator == null) {
        return comparePresentations(o1, o2);
      }

      ItemKeyGroup g1 = o1.getGroup();
      ItemKeyGroup g2 = o2.getGroup();

      if (g1 == null) {
        if (g2 == null) {
          return 0;
        } else {
          return 1;
        }
      } else {
        if (g2 == null) {
          return -1;
        } else {
          return comparator.compare(g1, g2);
        }
      }
    }
  }


  public static final class ExpandingProgressNode extends GenericNodeImpl {
    public ExpandingProgressNode(Database db, Configuration parentConfig) {
      super(db, new MyPresentation(), parentConfig.getOrCreateSubset(ConfigNames.EXPANDER_KEY));
    }

    @NotNull
    @ThreadSafe
    public QueryResult getQueryResult() {
      return QueryResult.NO_RESULT;
    }

    public boolean isCopiable() {
      return false;
    }

    private static final class MyPresentation implements CanvasRenderable {
      private Color myForeground;

      public void renderOn(Canvas canvas, CellState state) {
        if (!state.isSelected()) {
          if (myForeground == null)
            myForeground = ColorUtil.between(state.getDefaultForeground(), state.getDefaultBackground(), 0.5F);
          canvas.setForeground(myForeground);
        }
        canvas.appendText("Creating distribution\u2026");
      }
    }
  }
}