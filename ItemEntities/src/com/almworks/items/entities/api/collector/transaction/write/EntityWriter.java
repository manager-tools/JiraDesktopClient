package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EntityWriter {
  private long myResolvedVersion = -1;
  private final DBDrain myDrain;
  private final EntityTransaction myTransaction;
  private final List<EntityBag2> myBags;
  private final long myConnection;
  private final AttributeCache myAttributes;
  private final FakeCreate myFakeCreate = new FakeCreate();
  private final Map<Entity, Map<Integer, Set<DBAttribute<?>>>> myClearNoValue = Collections15.hashMap();
  private final Map<EntityPlace, Long> myExternalResolutions = Collections15.hashMap();
  private WriteState myState = null;

  public EntityWriter(DBDrain drain, EntityTransaction transaction, List<EntityBag2> bags, DBNamespace ns, long connection) {
    myDrain = drain;
    myTransaction = transaction;
    myBags = bags;
    myConnection = connection;
    myAttributes = new AttributeCache(ns, drain.getReader());
  }

  public DBNamespace getNamespace() {
    return myAttributes.getNamespace();
  }

  @NotNull
  public Collection<EntityHolder> getUncreatable() {
    WriteState writeState = ensureHasState();
    ArrayList<EntityHolder> result = Collections15.arrayList();
    Map<EntityTable, ResolutionState[]> resolutions = writeState.getResolutions();
    for (Map.Entry<EntityTable, ResolutionState[]> entry : resolutions.entrySet()) {
      EntityTable table = entry.getKey();
      ResolutionState[] states = entry.getValue();
      for (int i = 0; i < states.length; i++) {
        ResolutionState state = states[i];
        if (state == null) {
          LogHelper.error("No resolution attempt", i, table);
          continue;
        }
        if (!state.isResolved() && !state.isCanCreate()) result.add(new EntityHolder(myTransaction, state.getPlace()));
      }
    }
    return result;
  }

  @NotNull
  public ArrayList<EntityHolder> getUnresolved(Entity type) {
    WriteState state = ensureHasState();
    ArrayList<EntityHolder> result = Collections15.arrayList();
    for (EntityHolder holder : getAllEntities(type)) {
      long item = state.getItem(holder.getPlace());
      if (item <= 0) result.add(holder);
    }
    return result;
  }

  public void allowSearchCreate(EntityPlace place) {
    myResolvedVersion = -1;
    myFakeCreate.add(place);
  }

  public void addExternalResolution(EntityHolder holder, long item) {
    if (holder == null || item <= 0) return;
    EntityPlace place = holder.getPlace();
    Long prev = myExternalResolutions.get(place);
    if (prev != null) {
      LogHelper.assertError(prev == item, "Different external resolution known", place, prev, item);
      return;
    }
    LogHelper.assertWarning(myState == null, "Resolution dropped");
    myExternalResolutions.put(place, item);
    myState = null;
    myResolvedVersion = -1;
  }
  
  public void write() throws DBOperationCancelledException {
    WriteState state = ensureHasState();
    state.write(myDrain);
    ProcessBags.create(state, myDrain, myBags).perform();
    for (Procedure<EntityWriter> procedure : myTransaction.getPostWriteProcedures()) procedure.invoke(this);
  }

  public void ensureResolved() {
    ensureHasState();
  }

  public long getItem(EntityHolder holder) {
    if (holder == null) return 0;
    return ensureHasState().getItem(holder.getPlace());
  }

  public DBReader getReader() {
    return myDrain.getReader();
  }

  public DBDrain getDrain() {
    return myDrain;
  }

  public long getConnectionItem() {
    return myConnection;
  }

  public ItemVersion getConnection() {
    return SyncUtils.readTrunk(myDrain.getReader(), myConnection);
  }

  public List<EntityHolder> getAllEntities(Entity type) {
    return myTransaction.getAllEntities(type);
  }

  public EntityTransaction getTransaction() {
    return myTransaction;
  }

  public void clearNoValue(EntityHolder holder, Collection<? extends DBAttribute<?>> attributes) {
    EntityPlace place = holder.getPlace();
    Entity type = place.getTable().getItemType();
    Map<Integer, Set<DBAttribute<?>>> placeAttributes = myClearNoValue.get(type);
    if (placeAttributes == null) {
      placeAttributes = Collections15.hashMap();
      myClearNoValue.put(type, placeAttributes);
    }
    int index = place.getIndex();
    Set<DBAttribute<?>> allAttributes = placeAttributes.get(index);
    if (allAttributes == null) {
      allAttributes = Collections15.hashSet();
      placeAttributes.put(index, allAttributes);
    }
    allAttributes.addAll(attributes);
  }

  private WriteState ensureHasState() {
    if (myResolvedVersion == -1 || myResolvedVersion != calcCurrentVersion()) {
      LogHelper.assertWarning(myState == null, "Resolution is out of date");
      myState = null;
    }
    if (myState != null) return myState;
    myState = WriteState.create(this, myExternalResolutions);
    myResolvedVersion = calcCurrentVersion();
    return myState;
  }

  Map<Integer, Set<DBAttribute<?>>> getClearNotSet(Entity type) {
    return Util.NN(myClearNoValue.get(type), Collections.<Integer, Set<DBAttribute<?>>>emptyMap());
  }

  private long calcCurrentVersion() {
    int totalSize = 0;
    for (EntityTable table : myTransaction.getAllTables()) totalSize += table.getPlaceCount();
    long version = (((long) myTransaction.getIndexVersion()) << 32) | (long) totalSize;
    if (version == -1) version = -2;
    return version;
  }

  AttributeCache getAttributeCache() {
    return myAttributes;
  }

  FakeCreate getFakeCreate() {
    return myFakeCreate;
  }

  Collection<EntityTable> getAllTables() {
    return myTransaction.getAllTables();
  }

  public LongArray getAllItems(List<EntityHolder> holders) {
    LongArray result = new LongArray();
    for (EntityHolder holder : holders) result.add(getItem(holder));
    return result;
  }
}
