package com.almworks.explorer.loader;

import com.almworks.api.application.*;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.SyncProblem;
import com.almworks.api.explorer.ItemModelRegistry;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.DECL;
import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.collections.FactoryWithParameter;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.SetHolder;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.PropertyMapValueDecorator;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.external.BitSet2;

import java.util.List;

import static com.almworks.util.model.SetHolderUtils.actualizingSequentialListener;

/**
 * @author : Dyoma
 */

public class LoadedItemImpl extends BaseItemWrapper implements LoadedItem {
  /**
   * For performance reason {@link #myListeners} isn't final, and can be null. This lock allows multi-thread access to it.
   */
  private static final Object ourListenersLock = new Object();
  private List<ChangeListener1> myListeners = null;

  public static final Procedure<LoadedItemImpl> SET_LIVE_MODE = new Procedure<LoadedItemImpl>() {
    public void invoke(LoadedItemImpl arg) {
      arg.setLiveMode(true);
    }
  };

  public static final Procedure<LoadedItemImpl> SET_FROZEN_MODE = new Procedure<LoadedItemImpl>() {
    public void invoke(LoadedItemImpl arg) {
      arg.setLiveMode(false);
    }
  };

  public static final Procedure<LoadedItemImpl> UPDATE_ITEM = new Procedure<LoadedItemImpl>() {
    public void invoke(LoadedItemImpl arg) {
      arg.flushModifications();
    }
  };

  private final Object myLock = new Object();
  private UpdateMode myMode = LIFE_MODE;
  private final PropertyMap myValues = new PropertyMap();
  private final DBStatusHolder myDBStatus;

  private LoadedItemImpl(@NotNull PropertyMap values) {
    values.copyTo(myValues);
    myDBStatus = new DBStatusHolder(this);
  }

  void updateStatus(DBReader reader) {
    myDBStatus.updateStatus(reader);
  }

  private LoadedItemImpl(PropertyMap originalValues, DBStatusHolder originalStatus) {
    DECL.assumeThreadMayBeAWT();
    originalValues.copyTo(myValues);
    myDBStatus = new DBStatusHolder(this);
    myDBStatus.setStatus(originalStatus.getStatus());
  }

  @Nullable
  public static LoadedItemImpl create(ItemModelRegistry registry, long item, DBReader reader) {
    PropertyMap values = registry.extractValues(SyncUtils.readTrunk(reader, item));
    if (values == null) return null;
    LoadedItemImpl r = new LoadedItemImpl(values);
    r.updateStatus(reader);
    return r;
  }

  public PropertyMap getLastDBValues() {
    synchronized (myLock) {
      return myMode.getLastValues(this);
    }
  }

  @Nullable
  public static LoadedItemImpl createLive(Lifespan life, final ItemModelRegistry registry, final long item, DBReader reader) {
    return makeLive(life, registry, item, create(registry, item, reader));
  }

  @Nullable
  private static LoadedItemImpl makeLive(Lifespan life, final ItemModelRegistry registry, final long item, final LoadedItemImpl loadedItem) {
    if (loadedItem == null) return null;
    registry.addListener(life, new ItemModelRegistryImpl.Listener() {
      public void onItemUpdated(ItemUpdateEvent event) {
        if (event.getItem() == item) loadedItem.updateValues(event);
      }
    });
    SetHolder<SyncProblem> allProblems = registry.getActor(Engine.ROLE).getSynchronizer().getProblems();
    allProblems.addInitListener(life, ThreadGate.LONG_QUEUED(loadedItem), actualizingSequentialListener(new SetHolder.Listener<SyncProblem>() {
      @Override
      public void onSetChanged(@NotNull SetHolder.Event<SyncProblem> event) {
        if (
          ItemSyncProblem.TO_ITEM.detectEqual(ItemSyncProblem.SELECT.invoke(event.getAdded()), item) != null ||
          ItemSyncProblem.TO_ITEM.detectEqual(ItemSyncProblem.SELECT.invoke(event.getRemoved()), item) != null
        ) {
          loadedItem.services().getEngine().getDatabase().readForeground(new ReadTransaction<Object>() {
            @Override
            public Object transaction(DBReader reader) throws DBOperationCancelledException {
              loadedItem.updateValues(new ItemUpdateEvent(registry, item, reader));
              return null;
            }
          });
        }
      }
    }));
    return loadedItem;
  }

  public boolean isEditable() {
    return !getMetaInfo().getEditBlockKey().getValue(getValues());
  }

  @NotNull
  public DBStatus getDBStatus() {
    DBStatus status = myDBStatus.getStatus();
    assert status != null;
    return status;
  }

  public <T> T getModelKeyValue(ModelKey<? extends T> key) {
    synchronized (myLock) {
      return key.getValue(getLastDBValues());
    }
  }

  public boolean hasProblems() {
    return services().hasProblems();
  }

  public LoadedItemServices services() {
    return LoadedItemServices.VALUE_KEY.getValue(getLastDBValues());
  }

//  public boolean matches(@NotNull Constraint constraint) { // todo JC-308
//    return LegacyLoadedItemImpl.matches(constraint, getLastDBValues());
//  }

  public boolean isOutOfDate() {
    synchronized (myLock) {
      return myMode.isOutOfDate(this);
    }
  }

  public List<? extends AnAction> getWorkflowActions() {
    return getMetaInfo().getWorkflowActions();
  }

  public List<? extends AnAction> getActions() {
    return getMetaInfo().getActions();
  }

  private void setModeImpl(UpdateMode mode) {
    assert Thread.holdsLock(myLock);
    myMode = mode;
  }

  public PropertyMap getValues() {
    return myValues;
  }

  public <T> boolean setValue(TypedKey<T> key, T value) {
    T oldValue = value == null ? myValues.remove(key) : myValues.put(key, value);
    boolean changed = !Util.equals(oldValue, value);
    if (changed) {
      fireChanged();
    }
    return changed;
  }

  public void setValueDecorator(@Nullable PropertyMapValueDecorator decorator) {
    synchronized (myLock) {
      myValues.setDecorator(decorator);
    }
  }

  public void setLiveMode(boolean life) {
    synchronized (myLock) {
      myMode.setLifeMode(this, life);
    }
  }

  public boolean isLifeMode() {
    synchronized (myLock) {
      return myMode == LIFE_MODE;
    }
  }

  private void flushModifications() {
    synchronized (myLock) {
      myMode.flushModifications(this);
    }
  }

  public void updateValues(final ItemUpdateEvent event) {
    long newIcn = event.getIcn();
    synchronized (myLock) {
      if (Util.NN(myValues.get(ItemModelRegistry.ICN), -1L) > newIcn)
        return;
    }
    myDBStatus.updateStatus(event.getReader());
    final PropertyMap values = event.extractValues();
    if (values == null) return;
    ThreadGate.AWT_QUEUED.execute(new Runnable() {
      @Override
      public void run() {
        synchronized (myLock) {
          Long myIcn = myValues.get(ItemModelRegistry.ICN);
          Long newIcn = values.get(ItemModelRegistry.ICN);
          if (myIcn != null && newIcn != null && myIcn > newIcn)
            return;
          myMode.updateValues(LoadedItemImpl.this, values);
        }
        fireChanged();
      }
    });
  }

  public void addAWTListener(ChangeListener1<? extends LoadedItem> listener) {
    synchronized (ourListenersLock) {
      if (myListeners == null)
        myListeners = Collections15.arrayList(4);
      myListeners.add(listener);
    }
  }

  public void removeAWTListener(ChangeListener1<? extends LoadedItem> listener) {
    synchronized (ourListenersLock) {
      if (myListeners == null)
        return;
      boolean removed = myListeners.remove(listener);
      if (myListeners.isEmpty())
        myListeners = null;
    }
  }


  private void fireChanged() {
    Threads.assertAWTThread();
    ChangeListener1[] listeners;
    synchronized (ourListenersLock) {
      if (myListeners == null)
        return;
      listeners = new ChangeListener1[myListeners.size()];
      myListeners.toArray(listeners);
    }
    for (int i = 0; i < listeners.length; i++) {
      ChangeListener1<LoadedItem> listener = listeners[i];
      listener.onChange(this);
    }
  }

  public String toString() {
    return Util.NN(services().getItemUrl(), super.toString());
  }

  private interface UpdateMode {
    void updateValues(LoadedItemImpl loadedArtifact, PropertyMap values);

    PropertyMap getLastValues(LoadedItemImpl loadedArtifact);

    void setLifeMode(LoadedItemImpl loadedArtifact, boolean life);

    boolean isOutOfDate(LoadedItemImpl loadedArtifact);

    void flushModifications(LoadedItemImpl loaded);

    LoadedItemImpl copy(LoadedItemImpl parameter);
  }


  private static class FreezedMode implements UpdateMode {
    private PropertyMap myLastValues = null;

    public void updateValues(LoadedItemImpl loadedItem, PropertyMap values) {
      if (myLastValues == null)
        myLastValues = new PropertyMap();
      values.copyTo(myLastValues);
      BitSet2 oldKeys = ModelKey.ALL_KEYS.getValue(myLastValues);
      for (int i = oldKeys.nextSetBit(0); i >= 0; i = oldKeys.nextSetBit(i + 1)) {
        ModelKey<?> key = ModelKeySetUtil.getKey(i);
        if (key != null) {
          if (!key.getDataPromotionPolicy().canPromote(key, loadedItem.myValues, myLastValues))
            return;
        }
      }

      BitSet2 newKeys = ModelKey.ALL_KEYS.getValue(loadedItem.myValues);
      for (int i = newKeys.nextSetBit(0); i >= 0; i = newKeys.nextSetBit(i + 1)) {
        if (oldKeys.get(i))
          continue;
        ModelKey<?> key = ModelKeySetUtil.getKey(i);
        if (key != null) {
          if (!key.getDataPromotionPolicy().canPromote(key, loadedItem.myValues, myLastValues))
            return;
        }
      }

      flushModifications(loadedItem);
    }

    public PropertyMap getLastValues(LoadedItemImpl loadedArtifact) {
      return myLastValues != null ? myLastValues : loadedArtifact.myValues;
    }

    public void setLifeMode(LoadedItemImpl loadedArtifact, boolean life) {
      Threads.assertAWTThread();
      assert Thread.holdsLock(loadedArtifact.myLock);
      if (!life)
        return;
      if (myLastValues != null)
        myLastValues.copyTo(loadedArtifact.myValues);
      myLastValues = null;
      loadedArtifact.setModeImpl(LIFE_MODE);
    }

    public boolean isOutOfDate(LoadedItemImpl loadedArtifact) {
      return myLastValues != null;
    }

    public void flushModifications(LoadedItemImpl loaded) {
      Threads.assertAWTThread();
      if (myLastValues != null)
        myLastValues.copyTo(loaded.myValues);
      myLastValues = null;
    }

    public LoadedItemImpl copy(LoadedItemImpl parameter) {
      LoadedItemImpl copy = new LoadedItemImpl(parameter.myValues, parameter.myDBStatus);
      FreezedMode frozenMode = new FreezedMode();
      if (myLastValues != null) {
        frozenMode.myLastValues = new PropertyMap();
        myLastValues.copyTo(frozenMode.myLastValues);
      }
      copy.myMode = frozenMode;
      return copy;
    }
  }


  private static final UpdateMode LIFE_MODE = new UpdateMode() {
    public void updateValues(LoadedItemImpl loadedArtifact, PropertyMap values) {
      values.copyTo(loadedArtifact.myValues);
    }

    public PropertyMap getLastValues(LoadedItemImpl loadedArtifact) {
      return loadedArtifact.myValues;
    }

    public void setLifeMode(LoadedItemImpl loadedArtifact, boolean life) {
      if (life)
        return;
      loadedArtifact.setModeImpl(new FreezedMode());
    }

    public boolean isOutOfDate(LoadedItemImpl loadedArtifact) {
      return false;
    }

    public void flushModifications(LoadedItemImpl loaded) {
    }

    public LoadedItemImpl copy(LoadedItemImpl parameter) {
      LoadedItemImpl copy = new LoadedItemImpl(parameter.myValues, parameter.myDBStatus);
      copy.myMode = this;
      return copy;
    }
  };

  public static final FactoryWithParameter<LoadedItemImpl, LoadedItemImpl> COPIER =
    new FactoryWithParameter<LoadedItemImpl, LoadedItemImpl>() {
      public LoadedItemImpl create(LoadedItemImpl parameter) {
        return parameter.myMode.copy(parameter);
      }
    };
}
