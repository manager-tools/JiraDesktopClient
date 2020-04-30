package com.almworks.explorer.tree;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.Database;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Convertors;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.BottleneckJobs;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.*;

class DistributionState {
  private static final DistributionVisitor<Object> QUERIES_UPDATER = new DistributionVisitor<Object>() {
    @SuppressWarnings({"RefusedBequest"})
    protected Object visitQuery(DistributionQueryNodeImpl query, Object ticket) {
      query.updateName();
      query.updateGroup();
      return ticket;
    }
  };

  protected static final BottleneckJobs<DistributionState> PROCESS_MODEL_UPDATES =
    new BottleneckJobs<DistributionState>(500, ThreadGate.STRAIGHT) {
      protected void execute(DistributionState job) {
        job.processPendingUpdates();
      }
    };

  private final Lifespan myLife;
  private final DistributionFolderNodeImpl myFolder;
  private final EnumConstraintType myType;
  private final Database myDb;
  private final AListModel<ResolvedItem> myModel;

  // temporary sets for processing updates
  private Set<ItemKey> myPendingAdded;
  private Set<ItemKey> myPendingRemoved;
  private Set<ItemKey> myPendingChanged;
  private boolean myPendingProcessingRequested;

  private DistributionState(Lifespan life, DistributionFolderNodeImpl folder, EnumConstraintType type, Database db, AListModel<? extends ResolvedItem> model) {
    myLife = life;
    myFolder = folder;
    myType = type;
    myDb = db;
    myModel = (AListModel<ResolvedItem>) model;
  }

  public static DistributionState create(Lifespan life, DistributionFolderNodeImpl folder, EnumConstraintType type, ItemHypercube cube, Database db) {
    final AListModel<? extends ResolvedItem> model = type == null ? AListModel.EMPTY : type.getResolvedEnumModel(life, cube);
    return new DistributionState(life, folder, type, db, model);
  }
  
  private List<? extends ItemKey> getCurrentOptions() {
    return getCurrentOptions(myModel, myType);
  } 

  private static List<? extends ItemKey> getCurrentOptions(AListModel<? extends ResolvedItem> model, EnumConstraintType type) {
    List<? extends ItemKey> list = model.toList();
    if (type != null) {
      ItemKey missingItem = type.getMissingItem();
      if (missingItem != null && missingItem.getItem() > 0) {
        ArrayList<ItemKey> copy = Collections15.arrayList(list);
        copy.add(0, missingItem);
        list = copy;
      }
    }
    return list;
  }

  public void fullUpdate() {
    final Set<ItemKey> options = createAcceptedKeySet();

    Condition<ItemKey> condition = new Condition<ItemKey>() {
      public boolean isAccepted(ItemKey value) {
        return !options.remove(value);
      }
    };

    removeValues(myDb, condition);
    updateQueries();
    myFolder.addAcceptedValues(options);

    myFolder.removeEmptyGroups();
  }
  
  private void updateQueries() {
    if (myFolder.getDescriptor() != null) {
      QUERIES_UPDATER.visit(myFolder, null);
    }
  }

  private void removeValues(Database db, Collection<ItemKey> elements) {
    if (elements == null || elements.size() == 0)
      return;
    final Set<ItemKey> set;
    if (elements instanceof Set) {
      set = (Set<ItemKey>) elements;
    } else {
      set = Collections15.hashSet(elements);
    }
    Condition<ItemKey> condition = new Condition<ItemKey>() {
      public boolean isAccepted(ItemKey value) {
        return set.contains(value);
      }
    };
    removeValues(db, condition);
  }

  private void removeValues(final Database db, final Condition<ItemKey> condition) {
    final ConstraintDescriptor ownDescriptor = myFolder.getDescriptor();
    if (ownDescriptor == null)
      return;

    new DistributionVisitor<Object>() {
      protected Object visitQuery(DistributionQueryNodeImpl query, Object ticket) {
        maybeRemoveQuery(db, query, ownDescriptor, condition);
        return ticket;
      }
    }.visit(myFolder, null);

    myFolder.removeEmptyGroups();
  }

  private void maybeRemoveQuery(Database db, DistributionQueryNodeImpl query, ConstraintDescriptor folderDescriptor,
    Condition<ItemKey> condition)
  {
    if (!query.isPinned())
      return;
    Pair<ConstraintDescriptor, ItemKey> pair = query.getAttributeValue();
    if (pair != null) {
      ConstraintDescriptor descriptor = pair.getFirst();
      if (folderDescriptor.equals(descriptor)) {
        ItemKey value = pair.getSecond();
        boolean removing = condition.isAccepted(value);
        if (!removing) {
          // do not
          return;
        }
      }
    }
    myFolder.removeQuerySafely(db, query);
  }

  private Set<ItemKey> createAcceptedKeySet() {
    final Set<ItemKey> options = Collections15.linkedHashSet();
    for (ResolvedItem resolvedItem : myModel.toList()) {
      if (myFolder.isItemKeyAccepted(resolvedItem)) options.add(resolvedItem);
    }
    if (myType != null) {
      ItemKey missing = myType.getMissingItem();
      if (missing != null && missing.getItem() <= 0) {
        if (!myFolder.isItemKeyRejected(missing)) options.add(missing);
      }
    }
    return options;
  }

  public void listenModel() {
    myLife.add(myModel.addListener(new AListModel.Adapter<ResolvedItem>() {
      public void onInsert(int index, int length) {
        if (myLife.isEnded())
          return;
        Threads.assertAWTThread();
        if (length > 0) {
          pendingAdd(myModel.subList(index, index + length));
        }
      }

      public void onItemsUpdated(AListModel.UpdateEvent event) {
        if (myLife.isEnded())
          return;
        Threads.assertAWTThread();
        int length = event.getAffectedLength();
        if (length > 0) {
          int index = event.getLowAffectedIndex();
          pendingChange(myModel.subList(index, index + length));
        }
      }
    }));
    myLife.add(myModel.addRemovedElementListener(new AListModel.RemovedElementsListener<ResolvedItem>() {
      public void onBeforeElementsRemoved(AListModel.RemoveNotice<ResolvedItem> elements) {
        if (myLife.isEnded())
          return;
        Threads.assertAWTThread();
        pendingRemove(elements.getList());
      }
    }));
  }

  private void pendingChange(List<? extends ItemKey> keys) {
    Threads.assertAWTThread();
    for (ItemKey key : keys) {
      if (myPendingAdded != null && myPendingAdded.contains(key)) {
        // do nothing since it will already be updated
      } else if (myPendingRemoved != null && myPendingRemoved.contains(key)) {
        // do nothing since it will be removed anyway
      } else {
        if (myPendingChanged == null)
          myPendingChanged = Collections15.hashSet();
        myPendingChanged.add(key);
      }
    }
    requestProcessing();
  }

  private void pendingAdd(List<? extends ItemKey> keys) {
    Threads.assertAWTThread();
    for (ItemKey key : keys) {
      if (myPendingChanged != null && myPendingChanged.contains(key)) {
        // do nothing since it has already changed
      } else if (myPendingRemoved != null && myPendingRemoved.remove(key)) {
        // key was just removed, and added back - so it's changed
        if (myPendingChanged == null)
          myPendingChanged = Collections15.hashSet();
        myPendingChanged.add(key);
      } else {
        // key is inserted
        if (myPendingAdded == null)
          myPendingAdded = Collections15.hashSet();
        myPendingAdded.add(key);
      }
    }
    requestProcessing();
  }

  private void pendingRemove(List<ResolvedItem> keys) {
    Threads.assertAWTThread();
    if (myPendingChanged != null)
      myPendingChanged.removeAll(keys);
    if (myPendingAdded != null)
      myPendingAdded.removeAll(keys);
    if (myPendingRemoved == null)
      myPendingRemoved = Collections15.hashSet();
    myPendingRemoved.addAll(keys);
    requestProcessing();
  }

  private void requestProcessing() {
    Threads.assertAWTThread();
    if (myPendingProcessingRequested)
      return;
    myPendingProcessingRequested = true;
    // let events accumulate and possibly negate each other
    PROCESS_MODEL_UPDATES.addJobDelayed(this);
  }

  private void processPendingUpdates() {
    Threads.assertAWTThread();
    if (myLife.isEnded()) return;
    RootNode root = myFolder.getRoot();
    if (root == null) {
      LogHelper.error(this, "Node is not attached."); // If this happens - may be store DB in node field
      return;
    }
    Database db = root.getEngine().getDatabase();
    if (!myPendingProcessingRequested) {
      // Processing request was cancelled, nothing to do
      return;
    }
    myPendingProcessingRequested = false;
    if (myPendingRemoved != null) {
      removeValues(db, myPendingRemoved);
      myPendingRemoved.clear();
      myPendingRemoved = null;
    }
    if (myPendingAdded != null) {
      myFolder.addAcceptedValues(myPendingAdded);
      myPendingAdded.clear();
      myPendingAdded = null;
    }
    if (myPendingChanged != null) {
      updateValues(db, myPendingChanged);
      myPendingChanged.clear();
      myPendingChanged = null;
    }
  }

  private void updateValues(final Database db, final Collection<ItemKey> options) {
    final Map<ItemKey, ItemKey> keys = Convertors.<ItemKey>identity().assignKeys(options);
    final Set<ItemKey> acceptedKeys = createAcceptedKeySet();

    new DistributionVisitor<Object>() {
      protected Object visitQuery(DistributionQueryNodeImpl query, Object ticket) {
        Pair<ConstraintDescriptor, ItemKey> pair = query.getAttributeValue();
        if (pair != null) {
          ItemKey key = pair.getSecond();
          ItemKey newKey = keys.remove(key);
          if (newKey != null) {
            if (myFolder.isItemKeyAccepted(newKey)) {
              query.updateName(true);
              query.setPinned(true);
              myFolder.updateQueryGroup(db, query);
            } else {
              myFolder.removeQuerySafely(db, query);
            }
          } else if (!acceptedKeys.remove(key)) myFolder.removeQuerySafely(db, query);
        }
        return ticket;
      }

      protected Object visitGroup(DistributionGroupNodeImpl group, Object ticket) {
        group.sortChildrenLater();
        return ticket;
      }
    }.visit(myFolder, null);

    if (keys.size() > 0) {
      myFolder.addAcceptedValues(keys.keySet());
    }

    myFolder.removeEmptyGroups();
    myFolder.sortChildrenLater();
  }
}
