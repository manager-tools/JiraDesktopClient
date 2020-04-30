package com.almworks.explorer.loader;

import com.almworks.api.application.*;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.SyncProblem;
import com.almworks.api.explorer.ItemModelRegistry;
import com.almworks.explorer.loader.meta.IntrospectionSupport;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.SetHolder;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.Role;
import org.almworks.util.Collections15;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;
import util.external.BitSet2;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.almworks.util.collections.Functional.*;
import static com.almworks.util.model.SetHolderUtils.actualizingSequentialListener;

/**
 * @author : Dyoma
 */
public class ItemModelRegistryImpl implements Startable, ItemModelRegistry {
  private final FireEventSupport<Listener> myListeners = FireEventSupport.createSynchronized(Listener.class);
  private final Engine myEngine;
  private final Database myDb;
  private final ComponentContainer myContainer;

  private final DetachComposite myDetach = new DetachComposite();
  private final IntrospectionSupport myIntrospectionSupport = new IntrospectionSupport();
  private final List<ModelKey<?>> myAutoAddedKeys;

  public ItemModelRegistryImpl(Engine engine, Database db, ComponentContainer container, AutoAddedModelKey<?>[] autoAddedKeys) {
    myEngine = engine;
    myDb = db;
    myContainer = container;
    myAutoAddedKeys = Collections15.<ModelKey<?>>arrayList(autoAddedKeys);
  }

  public void start() {
    MyListener listener = new MyListener();
    myDb.addListener(myDetach, listener);
    myEngine.getSynchronizer().getProblems().addInitListener(myDetach, ThreadGate.LONG_QUEUED(this), actualizingSequentialListener(listener));
  }

  public void stop() {
    myDetach.detach();
  }

  @Nullable
  public ItemUiModelImpl createNewModel(long item, DBReader reader) {
    ItemVersion local = SyncUtils.readTrunk(reader, item);
    final PropertyMap values = extractValues(local);
    if (values == null) return null;
    LoadedItem.DBStatus dbStatus = calcDBStatus(item, reader);
    final ItemUiModelImpl model = new ItemUiModelImpl(dbStatus);
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        model.setValues(values);
      }
    });
    return model;
  }

  private LoadedItem.DBStatus calcDBStatus(long item, DBReader reader) {
    Long ci = reader.getValue(item, SyncAttributes.CONNECTION);
    assert ci != null : item;
    Connection connection = myEngine.getConnectionManager().findByItem(ci);
    return DBStatusKey.calcStatus(item, reader, connection);
  }

  public void addListener(Lifespan life, Listener listener) {
    myListeners.addStraightListener(life, listener);
  }

  public MetaInfo getMetaInfo(long artifact, DBReader reader) {
    return myIntrospectionSupport.getMetaInfo(artifact, reader);
  }

  @Nullable
  public PropertyMap extractValues(ItemVersion version) {
    PropertyMap values = new PropertyMap();

    LoadedItemServicesImpl services = LoadedItemServicesImpl.create(this, version);
    if (services == null) return null;
    MetaInfo metaInfo = services.getMetaInfo();
    LoadedItemServices.VALUE_KEY.setValue(values, services);

    BitSet2 allKeys = new BitSet2();
    ModelKeySetUtil.addKey(allKeys, LoadedItemServices.VALUE_KEY);
    for (ModelKey<?> key : metaInfo.getKeys()) loadModelKey(version, values, services, allKeys, key);
    // system keys that are not in the metainfo
    for (ModelKey<?> key : myAutoAddedKeys) loadModelKey(version, values, services, allKeys, key);
    ModelKeySetUtil.addKeysToMap(values, allKeys);

    values.put(ICN, version.getIcn());

    return values;
  }

  private void loadModelKey(ItemVersion version, PropertyMap values, LoadedItemServicesImpl services, BitSet2 allKeys, ModelKey<?> key) {
    key.extractValue(version, services, values);
    ModelKeySetUtil.addKey(allKeys, key);
  }

  public Engine getEngine() {
    return myEngine;
  }

  public <T> T getActor(Role<T> role) {
    T actor = myContainer.getActor(role);
    assert actor != null : role;
    return actor;
  }

  public Database getDatabase() {
    return myDb;
  }

  private class MyListener implements DBListener, SetHolder.Listener<SyncProblem> {
    private final Set<Long> myPrimaryTypes = Collections.synchronizedSet(Collections15.<Long>hashSet());
    private final Set<Long> myNotPrimaryTypes = Collections.synchronizedSet(Collections15.<Long>hashSet());

    public void onDatabaseChanged(DBEvent event, DBReader reader) {
      LongList items = event.getAffectedSorted();
      for (int i = 0; i < items.size(); i++) {
        long item = items.get(i);
        if (!isOfPrimaryType(item, reader))
          continue;
        ItemUpdateEvent ev = new ItemUpdateEvent(ItemModelRegistryImpl.this, item, reader);
        myListeners.getDispatcher().onItemUpdated(ev);
      }
    }

    private boolean isOfPrimaryType(long item, DBReader reader) {
      Long type = DBAttribute.TYPE.getValue(item, reader);
      if (type == null)
        return false;
      if (type == reader.findMaterialized(DBItemType.TYPE)) {
        // Type item has changed: clear information about that type from the caches
        myPrimaryTypes.remove(item);
        myNotPrimaryTypes.remove(item);
      } else {
        if (myPrimaryTypes.contains(type))
          return true;
        if (myNotPrimaryTypes.contains(type))
          return false;
      }
      Boolean primary = SyncAttributes.IS_PRIMARY_TYPE.getValue(type, reader);
      boolean isPrimary = primary != null && primary;
      (isPrimary ? myPrimaryTypes : myNotPrimaryTypes).add(type);
      return isPrimary;
    }

    @Override
    public void onSetChanged(@NotNull SetHolder.Event<SyncProblem> event) {
      processEvent(cat(
        convert(ItemSyncProblem.SELECT.invoke(event.getAdded()), ItemSyncProblem.TO_ITEM),
        convert(ItemSyncProblem.SELECT.invoke(event.getRemoved()), ItemSyncProblem.TO_ITEM)
      ));
    }

    private void processEvent(final Iterable<Long> items) {
      if (isEmpty(items)) return;
      // this listener is added with LONG_QUEUED gate, we're not in DB thread now
      myDb.readForeground(new ReadTransaction<Object>() {
        @Override
        public Object transaction(DBReader reader) {
          for (Iterator<Long> i = items.iterator(); i.hasNext();) {
            ItemUpdateEvent event = new ItemUpdateEvent(ItemModelRegistryImpl.this, i.next(), reader);
            myListeners.getDispatcher().onItemUpdated(event);
          }
          return null;
        }
      });
    }
  }
}