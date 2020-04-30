package com.almworks.explorer.tree;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintFilterNode;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.application.tree.*;
import com.almworks.api.config.ConfigNames;
import com.almworks.api.explorer.util.ItemKeys;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.explorer.qbuilder.filter.FilterGramma;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.threads.BottleneckJobs;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

class DistributionQueryNodeImpl extends AbstractQueryNode implements DistributionQueryNode {
  private Pair<ConstraintDescriptor, ItemKey> myElement;
  private boolean myPinned;
  private boolean myTreeNodeSilent = false;

  private static final BottleneckJobs<DistributionQueryNodeImpl> UPDATER =
    new BottleneckJobs<DistributionQueryNodeImpl>(100, ThreadGate.AWT) {
      protected void execute(DistributionQueryNodeImpl job) {
        job.updateName();
        job.updateGroup();
      }
    };

/*
  private static final Factory<ArtifactLockOwner> DISTRIBUTION_UPDATE_LOCK =
    new Factory.Const<ArtifactLockOwner>(new ArtifactLockOwner.Immortal());
*/


  private DistributionQueryNodeImpl(Database db, String name, Configuration configuration,
    Pair<ConstraintDescriptor, ItemKey> element, boolean loaded)
  {
    super(db, new MyPresentation(name), configuration);
    myElement = element;
    myPinned = configuration.getBooleanSetting(ConfigNames.PINNED_SETTING, true);
    setPinnedToPresentation();
    addAllowedChildType(TreeNodeFactory.NodeType.FOLDER);
    addAllowedChildType(TreeNodeFactory.NodeType.QUERY);
    addAllowedChildType(TreeNodeFactory.NodeType.DISTRIBUTION_FOLDER);
    addAllowedChildType(TreeNodeFactory.NodeType.LAZY_DISTRIBUTION);
    if (!loaded) {
      // todo ?
    }
  }

  public DistributionQueryNodeImpl(Database db, String name, Configuration configuration, Pair<ConstraintDescriptor, ItemKey> element) {
    this(db, name, configuration, element, false);
  }

  public DistributionQueryNodeImpl(Database db, String name, Configuration configuration) {
    this(db, name, configuration, getElementFromConfiguration(configuration), true);
  }

  public void setPinned(boolean pinned) {
    if (pinned != myPinned) {
      myPinned = pinned;
      setPinnedToPresentation();
      writeConfig();
      fireTreeNodeChanged();
      if (myPinned) {
        invalidatePreview();
      }
    }
  }

  protected void onQueryNodeResultStirred(boolean updated) {
    super.onQueryNodeResultStirred(updated);
    myElement = null;
  }

  @Nullable("when there's no valid filter or if filter is too complex (weird)")
  public Pair<ConstraintDescriptor, ItemKey> getAttributeValue() {
    if (myElement == null) {
      myElement = getElementFromFilterNode(getFilterStructure());
    }
    return myElement;
  }

  public boolean isPinned() {
    return myPinned;
  }

  public boolean isCopiable() {
    return !myPinned;
  }

  public boolean isRenamable() {
    return !myPinned;
  }

  public boolean isRemovable() {
    return !myPinned;
  }

  private void setPinnedToPresentation() {
    QueryPresentation p = getPresentation();
    if (p instanceof MyPresentation)
      ((MyPresentation) p).setPinned(myPinned);
    else
      assert false : this;
  }

  private void writeConfig() {
    getConfiguration().setSetting(ConfigNames.PINNED_SETTING, myPinned);
  }

  private static Pair<ConstraintDescriptor, ItemKey> getElementFromConfiguration(Configuration configuration) {
    String formula = configuration.getSetting(ConfigNames.QUERY_FORMULA, null);
    if (formula == null)
      return null;
    try {
      FilterNode node = FilterGramma.parse(formula);
      return getElementFromFilterNode(node);
    } catch (ParseException e) {
      return null;
    }
  }

  private static Pair<ConstraintDescriptor, ItemKey> getElementFromFilterNode(FilterNode node) {
    if (!(node instanceof ConstraintFilterNode))
      return null;
    List<ItemKey> subset = ((ConstraintFilterNode) node).getValue(EnumConstraintType.SUBSET);
    ConstraintDescriptor descriptor = ((ConstraintFilterNode) node).getDescriptor();
    ItemKey value = ItemKeys.unresolvedUnion(subset);
    return Pair.create(descriptor, value);
  }


  public void setFilter(FilterNode filter) {
    super.setFilter(filter);
    updateIcon();
  }

  /**
   * @return null or ItemKey or List&lt;ItemKey&gt;
   */
  @Nullable
  Object getItems() {
    return getItems(null);
  }


  /**
   * @return null or ItemKey or List&lt;ItemKey&gt;
   */
  @Nullable
  Object getItems(@Nullable ItemHypercube contextCube) {
    Pair<ConstraintDescriptor, ItemKey> element = getAttributeValue();
    if (element != null) {
      ItemKey key = element.getSecond();
      if (key instanceof ResolvedItem) {
        return ((ResolvedItem) key);
      } else {
        BaseEnumConstraintDescriptor descriptor = getDescriptor();
        EnumConstraintType constraintType = descriptor != null ? descriptor.getType() : null;
        if (constraintType != null) {
          ItemHypercube hypercube = contextCube;
          if (hypercube == null) {
            GenericNode parent = getParent();
            hypercube = parent == null ? new ItemHypercubeImpl() : parent.getHypercube(false);
          }
          if (hypercube != null) {
            List<ResolvedItem> items = constraintType.resolveKey(key.getId(), hypercube);
            if (items == null || items.isEmpty()) return key;
            else if (items.size() == 1) return items.get(0);
            else return items;
          }
        }
      }
    }
    return element != null ? element.getSecond() : null;
  }

  @Nullable
  public BaseEnumConstraintDescriptor getDescriptor() {
    ConstraintFilterNode filterNode = Util.castNullable(ConstraintFilterNode.class, getFilterStructure());
    if (filterNode == null) return null;
    return filterNode.getDescriptor().cast(BaseEnumConstraintDescriptor.class);
  }

  protected boolean updateIcon() {
    boolean superUpdated = super.updateIcon();
    Icon icon = null;
    Object resolved = getItems();
    if (resolved != null) {
      if (resolved instanceof ItemKey) {
        icon = ((ItemKey) resolved).getIcon();
      } else if (resolved instanceof List) {
        for (ItemKey artifact : (List<ItemKey>) resolved) {
          Icon ico = artifact.getIcon();
          if (icon == null) {
            icon = ico;
          } else if (!icon.equals(ico)) {
            icon = null;
            break;
          }
        }
      } else LogHelper.error(resolved);
    }
    boolean myUpdated = ((MyPresentation) getPresentation()).updateElementIcon(icon);
    return superUpdated | myUpdated;
  }

  public void updateName() {
    updateName(false);
  }

  public void updateName(boolean revive) {
    Object resolved = getItems();
    if (resolved != null) {
      String displayName = null;
      if (resolved instanceof ItemKey) {
        displayName = ((ItemKey) resolved).getDisplayName();
      } else if (resolved instanceof List) {
        Set<String> allNames = null;
        for (ItemKey artifact : (List<ItemKey>) resolved) {
          String name = artifact.getDisplayName();
          if (name != null && name.length() > 0) {
            if (displayName == null) {
              displayName = name;
            } else if (!displayName.equals(name)) {
              if (allNames == null) {
                allNames = Collections15.hashSet();
                allNames.add(displayName);
              }
              boolean added = allNames.add(name);
              if (added) {
                displayName = displayName + ", " + name;
              }
            }
          }
        }
      } else LogHelper.error("Failed to resolve distribution node name", resolved);
      if (displayName != null && displayName.length() > 0) {
        String name = getName();
        if (!Util.equals(displayName, name)) {
          if (name.startsWith(DistributionFolderNodeImpl.REMOVED_PREFIX) && !revive)
            displayName = DistributionFolderNodeImpl.REMOVED_PREFIX + displayName;
          getPresentation().setText(displayName);
          getConfiguration().setSetting(ConfigNames.NAME_SETTING, displayName);
        }
      }
    }
  }

  void updateGroup() {
    GenericNode parent = getParent();
    if (parent == null)
      return;
    if (parent instanceof DistributionGroupNode)
      parent = parent.getParent();
    if (parent instanceof DistributionFolderNodeImpl) {
      RootNode root = getRoot();
      if (root == null) LogHelper.error(this, "Not attached"); // if happen - may be store DB in field
      else ((DistributionFolderNodeImpl) parent).updateQueryGroup(root.getEngine().getDatabase(), this);
    } else {
      assert false : this + " " + parent;
    }
  }

  public void onInsertToModel() {
    super.onInsertToModel();
    // update through job because position may change
    UPDATER.addJobDelayed(this);
  }

  @Override
  public void fireTreeNodeChanged() {
    if (myTreeNodeSilent) return;
    super.fireTreeNodeChanged();
    final GenericNode parent = getParent();
    if(parent instanceof DistributionFolderNodeImpl || parent instanceof DistributionGroupNodeImpl) {
      parent.fireTreeNodeChanged();
    }
  }

  @Override
  protected ItemsPreview calculatePreview(Lifespan lifespan, DBReader reader) {
    Log.error("Should not happen");
    return new ItemsPreview.Unavailable();
  }

  @Override
  protected void invalidatePreview(RootNode root) {
    ItemsPreviewManager manager = root.getItemsPreviewManager();
    setPreview(null);
    DistributionFolderNodeImpl folder = getAncestorOfType(DistributionFolderNodeImpl.class);
    if (folder != null) folder.scheduleChildPreview(manager, false);

    invalidateChildren();
  }

  @Override
  protected void schedulePreview(ItemsPreviewManager manager, boolean cancelOngoing) {
    DistributionFolderNodeImpl folder = getAncestorOfType(DistributionFolderNodeImpl.class);
    if (folder == null) return;
    folder.scheduleChildPreview(manager, cancelOngoing);
  }

  public void setPreviewSilent(ItemsPreview preview) {
    myTreeNodeSilent = true;
    try {
      setPreview(preview);
    } finally {
      myTreeNodeSilent = false;
    }
  }

  private static class MyPresentation extends QueryPresentation {
    private boolean myPinned = true;
    private Icon myElementIcon = null;

    public MyPresentation(String name) {
      super(name, Icons.NODE_DISTRIBUTION_QUERY_PINNED, Icons.NODE_DISTRIBUTION_QUERY_PINNED);
    }

    public void renderOn(Canvas canvas, CellState state) {
      boolean pinned = myPinned;
      Icon icon = myElementIcon;
      if (!pinned)
        icon = Icons.NODE_DISTRIBUTION_QUERY_PINNED.getGrayed();
      if (icon == null)
        icon = Icons.NODE_DISTRIBUTION_QUERY_PINNED;
      setIcon(icon, icon);
      if (!pinned)
        canvas.setFontStyle(Font.ITALIC);
      super.renderOn(canvas, state);
    }

    public void setPinned(boolean pinned) {
      myPinned = pinned;
    }

    public boolean updateElementIcon(Icon icon) {
      if (myElementIcon == icon)
        return false;
      myElementIcon = icon;
      return true;
    }
  }
}
