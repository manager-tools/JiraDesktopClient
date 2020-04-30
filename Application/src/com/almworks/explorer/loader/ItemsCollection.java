package com.almworks.explorer.loader;

import com.almworks.api.application.LifeMode;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.StateIconHelper;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.SyncProblem;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ReadTransaction;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.util.advmodel.AListModelUpdater;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Functional;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.SetHolder;
import com.almworks.util.model.SetHolderUtils;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.threads.Computable;
import com.almworks.util.threads.MapMarshaller;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO[dyoma] move class?
/**
 * @author : Dyoma
 */
public class ItemsCollection implements ItemModelRegistryImpl.Listener {
  private final MapMarshaller<Long, LoadedItemImpl> myItems = new MapMarshaller<Long, LoadedItemImpl>();
  private final ItemModelRegistryImpl myRegistry;
  private final TimeTracker myTimeTracker;
  private final DetachComposite myDetach = new DetachComposite();
  private volatile ListMode myLiveMode = LIVE_MODE;
  private final BasicScalarModel<LifeMode> myLifeModeModel =
    BasicScalarModel.createModifiable(myLiveMode.isLife());
  private final AListModelUpdater<LoadedItemImpl> myListModelUpdater =
    new AListModelUpdater<LoadedItemImpl>(2000, 2000);

  public ItemsCollection(ItemModelRegistryImpl editRegistry, TimeTracker timeTracker) {
    myRegistry = editRegistry;
    myTimeTracker = timeTracker;
    myRegistry.addListener(myDetach, this);
    SetHolder<SyncProblem> allProblems = myRegistry.getActor(Engine.ROLE).getSynchronizer().getProblems();
    allProblems.addInitListener(myDetach, ThreadGate.AWT, SetHolderUtils.actualizingSequentialListener(new SetHolder.Listener<SyncProblem>() {
      @Override
      public void onSetChanged(@NotNull SetHolder.Event<SyncProblem> event) {
        for (Long item : Functional.convert(ItemSyncProblem.SELECT.invoke(event.getAdded()), ItemSyncProblem.TO_ITEM)) {
          updateIfExists(item);
        }
        for (Long item : Functional.convert(ItemSyncProblem.SELECT.invoke(event.getRemoved()), ItemSyncProblem.TO_ITEM)) {
          updateIfExists(item);
        }
      }
    }));
    myDetach.add(new Detach() {
      protected void doDetach() {
        ThreadGate.AWT.execute(new Runnable() {
          public void run() {
            myLiveMode.dispose();
            myListModelUpdater.removeAll();
          }
        });
      }
    });
    initTimeTracking(timeTracker);
  }

  private void initTimeTracking(final TimeTracker timeTracker) {
    if (timeTracker == null) return;
    ChangeListener listener = new ChangeListener() {
      private final Set<LoadedItemImpl> currentWork = Collections15.hashSet();
      public void onChange() {
        TimeTrackerTask task = timeTracker.getCurrentTask();
        if (task == null && currentWork.isEmpty()) return;
        boolean trackingActive = timeTracker.isTracking();
        List<LoadedItemImpl> updated = null;
        for (final LoadedItemImpl a : myListModelUpdater.getModel()) {
          boolean tracked = task != null && task.getKey() == a.getItem();
          boolean update = false;
          if (!tracked) {
            if (currentWork.remove(a)) {
              final PropertyMap values = a.getValues();
              values.remove(TimeTrackerTask.TIME_TRACKER_TASK);
              values.remove(TimeTrackerTask.TIME_TRACKER_ACTIVE);
              StateIconHelper.removeStateIcons(a.getValues(), TimeTrackerTask.TIME_TRACKING_ICONS);
              update = true;
            }
          } else {
            currentWork.add(a);
            final PropertyMap values = a.getValues();
            boolean changed = !Util.equals(values.put(TimeTrackerTask.TIME_TRACKER_TASK, task), task)
              || !Util.equals(values.put(TimeTrackerTask.TIME_TRACKER_ACTIVE, trackingActive), trackingActive);
            if (changed) {
              StateIconHelper.removeStateIcons(a.getValues(), TimeTrackerTask.TIME_TRACKING_ICONS);
              StateIconHelper.addStateIcon(a.getValues(), trackingActive ? TimeTrackerTask.TIME_TRACKING_STARTED : TimeTrackerTask.TIME_TRACKING_PAUSED);
              update = true;
            }
          }
          if (update) {
            if (updated == null) updated = Collections15.arrayList();
            updated.add(a);
          }
        }
        if (updated != null) {
          final List<LoadedItemImpl> finalUpdated = updated;
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              myListModelUpdater.updateElementsAndFlush(finalUpdated);
            }
          });
        }
      }
    };
    myListModelUpdater.getModel().addAWTChangeListener(myDetach, listener);
    timeTracker.getModifiable().addAWTChangeListener(myDetach, listener);
    listener.onChange();
  }

  private void updateIfExists(final long item) {
    final LoadedItemImpl loadedItem = myItems.getExisting(item);
    if (loadedItem == null)
      return;
    myRegistry.getDatabase().readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) {
        loadedItem.updateValues(new ItemUpdateEvent(myRegistry, item, reader));
        myListModelUpdater.updateElement(loadedItem);
        return null;
      }
    });
  }

  public void addItem(final long item, final DBReader reader) {
    LoadedItemImpl loadedItem = myItems.putNew(item, new Computable<LoadedItemImpl>() {
      public LoadedItemImpl compute() {
        return LoadedItemImpl.create(myRegistry, item, reader);
      }
    });
    if (loadedItem != null) {
      myLiveMode.addItem(myListModelUpdater, loadedItem);
      myLifeModeModel.setValue(myLiveMode.isLife());
    } else {
      LoadedItemImpl existing = myItems.getExisting(item);
      if (existing != null) {
        myLiveMode.reviveItem(existing);
        myLifeModeModel.setValue(myLiveMode.isLife());
      }
    }
  }

  @ThreadAWT
  public void forceUpdate() {
    myListModelUpdater.flush();
  }


  public void setLiveMode(final boolean live) {
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        ListMode mode = myLiveMode.setLive(myListModelUpdater, myItems, live);
        myLiveMode = mode;
        myLifeModeModel.setValue(mode.isLife());
      }
    });
  }

  public ScalarModel<LifeMode> getLifeModeModel() {
    return myLifeModeModel;
  }

  public AListModelUpdater<? extends LoadedItem> getListModelUpdater() {
    return myListModelUpdater;
  }

  public void onItemUpdated(ItemUpdateEvent event) {
    LoadedItemImpl loadedItem = null;
    try {
      // calling method that @CanBlock inside transaction: while usually not very good, here it never blocks because {@link MapMarshaller#getBuilding} waits for the construction lock, AND we add items to {@link #myItems} only from DB thread.
      loadedItem = myItems.getBuilding(event.getItem());
    } catch (InterruptedException e) {
      assert false : "must not occur";
      throw new RuntimeInterruptedException(e);
    }
    if (loadedItem == null)
      return;
    loadedItem.addAWTListener(new ChangeListener1<LoadedItemImpl>() {
      public void onChange(LoadedItemImpl changed) {
        myListModelUpdater.updateElement(changed);
        changed.removeAWTListener(this);
      }
    });
    loadedItem.updateValues(event);
  }

  public void dispose() {
    myDetach.detach();
  }

  public void removeItems(final long item) {
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        myLiveMode.removeItem(myListModelUpdater, myItems, item);
        myLifeModeModel.setValue(myLiveMode.isLife());
      }
    });
  }

  Collection<LoadedItemImpl> getAllInList() {
    return myListModelUpdater.getAllElements();
  }

  public void updateAllItems() {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        myLiveMode.flushModifications(myListModelUpdater, myItems);
        myLifeModeModel.setValue(myLiveMode.isLife());
      }
    });
  }

  public boolean isElementSetChanged() {
    Threads.assertAWTThread();
    return myLiveMode.isElementSetChanged();
  }

  public ItemsCollection createCopy() {
    final ItemsCollection copy = new ItemsCollection(myRegistry, myTimeTracker);
    myItems.copyTo(copy.myItems, LoadedItemImpl.COPIER);

    Collection<LoadedItemImpl> allItems = myListModelUpdater.getAllElements();
    List<LoadedItemImpl> listCopy = Collections15.arrayList(allItems.size());
    for (LoadedItemImpl item : allItems) {
      LoadedItemImpl itemCopy = copy.myItems.getExisting(item.getItem());
      assert itemCopy != null : item;
      listCopy.add(itemCopy);
    }
    copy.myListModelUpdater.addAll(listCopy);

    final ListMode mode = myLiveMode.copy(copy.myItems);
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        copy.myLiveMode = mode;
        copy.myLifeModeModel.setValue(mode.isLife());
      }
    });
    return copy;
  }


  private interface ListMode {
    ListMode setLive(AListModelUpdater<LoadedItemImpl> modelUpdater, MapMarshaller<Long, LoadedItemImpl> items, boolean live);

    void addItem(AListModelUpdater<LoadedItemImpl> collection, LoadedItemImpl loaded);

    void removeItem(AListModelUpdater<LoadedItemImpl> collection, MapMarshaller<Long, LoadedItemImpl> items, long item);

    void flushModifications(AListModelUpdater<LoadedItemImpl> modelUpdater, MapMarshaller<Long, LoadedItemImpl> items);

    boolean isElementSetChanged();

    void reviveItem(LoadedItemImpl loaded);

    LifeMode isLife();

    @NotNull
    ListMode copy(@NotNull MapMarshaller<Long, LoadedItemImpl> destItems);

    void dispose();
  }


  private static class FreezeMode implements ListMode {
    private final Map<Long, LoadedItemImpl> myNew = Collections15.hashMap();
    private final Set<Long> myRemoved = Collections15.hashSet();

    public String toString() {
      return "FROZEN";
    }

    public ListMode setLive(AListModelUpdater<LoadedItemImpl> modelUpdater,
      MapMarshaller<Long, LoadedItemImpl> items, boolean live)
    {
      Threads.assertAWTThread();
      if (!live)
        return this;
      flushImpl(modelUpdater, items, LoadedItemImpl.SET_LIVE_MODE);
      return LIVE_MODE;
    }

    private synchronized void flushImpl(AListModelUpdater<LoadedItemImpl> modelUpdater,
      MapMarshaller<Long, LoadedItemImpl> items, Procedure<LoadedItemImpl> procedure)
    {
//      modelUpdater.removeAll(toRemove);
      for (long item : myRemoved) {
        assert !myNew.containsKey(item);
        LoadedItemImpl loaded = items.clearExisting(item);
        if (loaded != null)
          modelUpdater.remove(loaded);
      }
      myRemoved.clear();
      Collection<LoadedItemImpl> newLoaded = myNew.values();
      Containers.apply(LoadedItemImpl.SET_FROZEN_MODE, newLoaded);
      modelUpdater.addAll(newLoaded);
      myNew.clear();
      Collection<LoadedItemImpl> allItems = modelUpdater.getAllElements();
      Containers.apply(procedure, allItems);
      modelUpdater.updateAll();
      modelUpdater.flush();
    }

    public void flushModifications(AListModelUpdater<LoadedItemImpl> modelUpdater,
      MapMarshaller<Long, LoadedItemImpl> items)
    {
      flushImpl(modelUpdater, items, LoadedItemImpl.UPDATE_ITEM);
    }

    public synchronized boolean isElementSetChanged() {
      return !myNew.isEmpty() || !myRemoved.isEmpty();
    }

    public synchronized void reviveItem(LoadedItemImpl loaded) {
      myRemoved.remove(loaded.getItem());
    }

    public LifeMode isLife() {
      return isElementSetChanged() ? LifeMode.HAS_NEW : LifeMode.FROZEN;
    }

    public synchronized void addItem(AListModelUpdater<LoadedItemImpl> collection, LoadedItemImpl loaded) {
      long item = loaded.getItem();
      assert !myRemoved.contains(item); // Should not happen
      if (myNew.containsKey(item))
        return;
      loaded.setLiveMode(true);
      myNew.put(item, loaded);
    }

    public synchronized void removeItem(AListModelUpdater<LoadedItemImpl> collection,
      MapMarshaller<Long, LoadedItemImpl> items, long item)
    {
      if (myNew.containsKey(item)) {
        LoadedItemImpl removed = myNew.remove(item);
        LoadedItemImpl existing = items.clearExisting(item);
        assert existing == null || existing == removed;
        return;
      }
      myRemoved.add(item);
    }

    @NotNull
    public ListMode copy(MapMarshaller<Long, LoadedItemImpl> destItems) {
      FreezeMode copy = new FreezeMode();
      for (Map.Entry<Long, LoadedItemImpl> entry : myNew.entrySet()) {
        long key = entry.getKey();
        LoadedItemImpl existing = destItems.getExisting(key);
        assert existing != null : entry;
        copy.myNew.put(key, existing);
      }
      copy.myRemoved.addAll(myRemoved);
      return copy;
    }

    public synchronized void dispose() {
      myNew.clear();
      myRemoved.clear();
    }
  }


  private static final ListMode LIVE_MODE = new ListMode() {
    public String toString() {
      return "LIVE";
    }

    public ListMode setLive(AListModelUpdater<LoadedItemImpl> modelUpdater,
      MapMarshaller<Long, LoadedItemImpl> items, boolean live)
    {
      Threads.assertAWTThread();
      if (live)
        return this;
      Collection<LoadedItemImpl> listModel = modelUpdater.getAllElements();
      for (LoadedItemImpl item : listModel) {
        item.setLiveMode(false);
      }
      modelUpdater.updateAll();
      return new FreezeMode();
    }

    public void addItem(final AListModelUpdater<LoadedItemImpl> collection, final LoadedItemImpl loaded) {
      if (loaded.isLifeMode()) {
        collection.addingElement(true);
        try {
          collection.add(loaded);
          return;
        } finally {
          collection.addingElement(false);
        }
      }
      collection.addingElement(true);
      ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
        public void run() {
          try {
            loaded.setLiveMode(true);
            collection.add(loaded);
          } finally {
            collection.addingElement(false);
          }
        }
      });
    }

    public void removeItem(final AListModelUpdater<LoadedItemImpl> collection,
      MapMarshaller<Long, LoadedItemImpl> items, long item)
    {
      final LoadedItemImpl loaded = items.clearExisting(item);
      if (loaded == null)
        return;
      collection.remove(loaded);
    }

    public void flushModifications(AListModelUpdater<LoadedItemImpl> modelUpdater,
      MapMarshaller<Long, LoadedItemImpl> items)
    {
    }

    public boolean isElementSetChanged() {
      return false;
    }

    public void reviveItem(LoadedItemImpl loaded) {
    }

    public LifeMode isLife() {
      return LifeMode.LIFE;
    }

    @NotNull
    public ListMode copy(MapMarshaller<Long, LoadedItemImpl> destItems) {
      return this;
    }

    public void dispose() {
    }
  };
}
